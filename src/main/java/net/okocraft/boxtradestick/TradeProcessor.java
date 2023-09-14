package net.okocraft.boxtradestick;

import io.papermc.paper.event.player.PlayerPurchaseEvent;
import io.papermc.paper.event.player.PlayerTradeEvent;
import java.util.ArrayList;
import java.util.List;
import net.okocraft.box.api.util.InventoryUtil;
import org.bukkit.Sound;
import org.bukkit.entity.AbstractVillager;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class TradeProcessor {

    public static boolean canTradeByStick(@NotNull AbstractVillager villager) {
        return villager.getRecipeCount() < TradeStickData.MAXIMUM_INDEX;
    }

    public static int processSelectedOffersForMaxUses(@NotNull Player trader, @NotNull AbstractVillager villager) {
        int[] recipeIndices = TradeStickData.loadFrom(villager).getSelectedIndices(trader.getUniqueId());

        if (recipeIndices.length == 0) {
            trader.playSound(villager, Sound.ENTITY_VILLAGER_NO, 1, 1);
            return 0;
        }

        List<TradeResult> results = new ArrayList<>(recipeIndices.length);

        for (int index : recipeIndices) {
            TradeResult result = tradeForMaxUses(trader, villager, index);
            if (result != null) {
                results.add(result);
            }
        }

        if (results.isEmpty()) {
            trader.playSound(villager, Sound.ENTITY_VILLAGER_NO, 1, 1);
            trader.sendActionBar(Translatables.OUT_OF_STOCK);
            return 0;
        }

        trader.playSound(villager, Sound.ENTITY_VILLAGER_TRADE, 1, 1);

        if (results.size() == 1) {
            TradeResult result = results.get(0);
            trader.sendActionBar(Translatables.RESULT_TIMES.apply(result.count, result.recipe.getResult()));
        } else {
            trader.sendActionBar(Translatables.MULTIPLE_RESULT_TIMES.apply(
                    results.stream().mapToInt(TradeResult::count).sum(),
                    results.size()
            ));
        }

        return results.size();
    }

    public static @Nullable TradeResult tradeForMaxUses(@NotNull Player trader, @NotNull AbstractVillager villager, int recipeIndex) {
        if (!BoxUtil.checkPlayerCondition(trader, "boxtradestick.trade")) {
            return null;
        }

        if (recipeIndex < 0 || recipeIndex >= villager.getRecipeCount()) {
            return null;
        }

        MerchantRecipe merchantOffer = villager.getRecipe(recipeIndex);
        int traded = 0;

        for (int i = merchantOffer.getUses(); i < merchantOffer.getMaxUses(); i++) {
            if (trade0(trader, villager, recipeIndex)) {
                traded++;
            }
        }

        return traded > 0 ? new TradeResult(traded, merchantOffer) : null;
    }

    public static boolean trade(@NotNull Player trader, @NotNull AbstractVillager villager, int recipeIndex) {
        if (!BoxUtil.checkPlayerCondition(trader, "boxtradestick.trade")) {
            return false;
        }

        if (recipeIndex < 0 || recipeIndex >= villager.getRecipeCount()) {
            return false;
        }

        return trade0(trader, villager, recipeIndex);
    }

    private static boolean trade0(@NotNull Player trader, @NotNull AbstractVillager villager, int recipeIndex) {
        var merchantOffer = villager.getRecipe(recipeIndex);

        if (merchantOffer.getUses() >= merchantOffer.getMaxUses()) {
            return false;
        }

        PlayerPurchaseEvent event = new PlayerTradeEvent(trader, villager, merchantOffer, true, true);

        if (!event.callEvent()) {
            return false;
        }

        merchantOffer = event.getTrade();
        List<ItemStack> ingredients = new ArrayList<>(merchantOffer.getIngredients());
        if (ingredients.size() >= 1) {
            ingredients.set(0, merchantOffer.getAdjustedIngredient1());
        }
        var cause = new BoxUtil.Trade(trader, merchantOffer);
        if (!BoxUtil.tryConsumingStockMulti(trader, ingredients, cause)) {
            return false;
        }

        var resultBukkit = merchantOffer.getResult();

        var result = BoxUtil.getBoxItem(resultBukkit);
        if (result.isEmpty()) {
            int remaining = InventoryUtil.putItems(trader.getInventory(), resultBukkit, resultBukkit.getAmount());

            if (0 < remaining) {
                drop(trader, resultBukkit.asQuantity(remaining));
            }
        } else {
            // stock is definitely present.
            BoxUtil.getStock(trader).ifPresent(stock -> stock.increase(result.get(), resultBukkit.getAmount(), cause));
        }

        NMSUtil.processTrade(trader, villager, merchantOffer, event);
        return true;
    }

    private static void drop(Player player, ItemStack item) {
        ItemStack hand = player.getInventory().getItemInMainHand().clone();
        player.getInventory().setItemInMainHand(item);
        player.dropItem(true);
        player.getInventory().setItemInMainHand(hand);
    }

    private TradeProcessor() {
    }

    public record TradeResult(int count, @NotNull MerchantRecipe recipe) {
    }
}
