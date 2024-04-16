package net.okocraft.boxtradestick;

import io.papermc.paper.event.player.PlayerPurchaseEvent;
import io.papermc.paper.event.player.PlayerTradeEvent;
import java.util.ArrayList;
import java.util.List;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import net.okocraft.box.api.BoxAPI;
import net.okocraft.box.api.model.item.BoxItem;
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
        if (!canTrade(trader, villager, recipeIndex)) {
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
        if (canTrade(trader, villager, recipeIndex)) {
            return trade0(trader, villager, recipeIndex);
        } else {
            return false;
        }
    }

    private static boolean trade0(@NotNull Player trader, @NotNull AbstractVillager villager, int recipeIndex) {
        if (!BoxAPI.api().getBoxPlayerMap().isLoaded(trader)) {
            return false;
        }

        var merchantOffer = villager.getRecipe(recipeIndex);

        if (merchantOffer.getUses() >= merchantOffer.getMaxUses()) {
            return false;
        }

        PlayerPurchaseEvent event = new PlayerTradeEvent(trader, villager, merchantOffer, true, true);

        if (!event.callEvent()) {
            return false;
        }

        merchantOffer = event.getTrade();

        var ingredients = merchantOffer.getIngredients();

        if (ingredients.isEmpty()) {
            return false;
        }

        var ingredientMap = new Object2IntArrayMap<BoxItem>(ingredients.size());

        for (int i = 0, size = ingredients.size(); i < size; i++) {
            var ingredient = ingredients.get(i);

            if (ingredient.getType().isAir()) {
                continue;
            }

            var boxItem = BoxAPI.api().getItemManager().getBoxItem(ingredient);

            if (boxItem.isEmpty()) {
                return false;
            } else if (i == 0) {
                var adjusted = merchantOffer.getAdjustedIngredient1();
                if (adjusted != null) {
                    ingredientMap.put(boxItem.get(), adjusted.getAmount());
                } else {
                    return false;
                }
            } else {
                ingredientMap.put(boxItem.get(), ingredient.getAmount());
            }
        }

        var stockHolder = BoxAPI.api().getBoxPlayerMap().get(trader).getCurrentStockHolder();
        var cause = new TradeCause(trader, merchantOffer);

        if (stockHolder.decreaseIfPossible(ingredientMap, cause)) {
            var resultBukkit = merchantOffer.getResult();
            var result = BoxUtil.getBoxItem(resultBukkit);
            if (result.isEmpty()) {
                int remaining = InventoryUtil.putItems(trader.getInventory(), resultBukkit, resultBukkit.getAmount());

                if (0 < remaining) {
                    drop(trader, resultBukkit.asQuantity(remaining));
                }
            } else {
                stockHolder.increase(result.get(), resultBukkit.getAmount(), cause);
            }

            NMSUtil.processTrade(trader, villager, merchantOffer, event);
            return true;
        } else {
            return false;
        }
    }

    private static void drop(Player player, ItemStack item) {
        ItemStack hand = player.getInventory().getItemInMainHand().clone();
        player.getInventory().setItemInMainHand(item);
        player.dropItem(true);
        player.getInventory().setItemInMainHand(hand);
    }

    private static boolean canTrade(Player player, @NotNull AbstractVillager villager, int recipeIndex) {
        return BoxAPI.api().canUseBox(player) &&
               player.hasPermission("boxtradestick.trade") &&
               0 <= recipeIndex &&
               recipeIndex < villager.getRecipeCount();
    }

    private TradeProcessor() {
    }

    public record TradeResult(int count, @NotNull MerchantRecipe recipe) {
    }
}
