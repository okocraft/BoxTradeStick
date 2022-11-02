package net.okocraft.boxtradestick;

import io.papermc.paper.event.player.PlayerPurchaseEvent;
import io.papermc.paper.event.player.PlayerTradeEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.okocraft.box.api.model.item.BoxItem;
import net.okocraft.box.api.transaction.InventoryTransaction;
import net.okocraft.box.api.transaction.TransactionResult;
import net.okocraft.box.storage.api.factory.item.BoxItemFactory;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.AbstractVillager;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Merchant;
import org.bukkit.inventory.MerchantRecipe;
import org.jetbrains.annotations.NotNull;

public class MerchantRecipesGUI implements InventoryHolder {

    private final Inventory inventory;

    private final Player trader;
    private final Merchant merchant;

    public MerchantRecipesGUI(Player trader, Merchant merchant) {
        this.inventory = Bukkit.createInventory(this, 54, Translatables.GUI_TITLE.apply(merchant));
        this.trader = trader;
        this.merchant = merchant;
        initialize();
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    public int getMaxScroll() {
        return Math.max(0, merchant.getRecipeCount() - 6);
    }

    public void scrollDown() {
        int scroll = getScroll();
        if (scroll + 1 <= getMaxScroll()) {
            setScroll(scroll + 1);
        }
    }

    public void scrollUp() {
        int scroll = getScroll();
        if (1 <= scroll) {
            setScroll(scroll - 1);
        }
    }

    public void updateTradeItem(int row) {
        int recipeIndex = row + getScroll();

        if (recipeIndex == getCurrentSelected()) {
            inventory.setItem(row * 9, ItemUtil.create(
                    trader.locale(),
                    Material.GREEN_WOOL,
                    Translatables.GUI_RECIPE_SELECTED,
                    Translatables.GUI_RECIPE_SELECTED_LORE
            ));
        } else {
            inventory.setItem(row * 9, ItemUtil.create(
                    trader.locale(),
                    Material.RED_WOOL,
                    Translatables.GUI_RECIPE_NOT_SELECTED,
                    Translatables.GUI_RECIPE_SELECTED_LORE
            ));
        }

        MerchantRecipe recipe = merchant.getRecipe(recipeIndex);
        List<ItemStack> ingredients = new ArrayList<>(recipe.getIngredients());
        for (int i = 0; i < ingredients.size(); i++) {
            ItemStack originalIngredient = ingredients.get(i);
            ItemStack ingredient = originalIngredient.clone();

            if (i == 0) {
                int originalPrice = ingredient.getAmount();
                recipe.adjust(ingredient);
                ingredients.set(i, ingredient.clone());
                int currentPrice = ingredient.getAmount();
                if (originalPrice != currentPrice) {
                    ItemUtil.lore(trader.locale(), ingredient,
                            List.of(Translatables.GUI_PRICE_DIFF.apply(originalPrice, currentPrice)));
                }
            }

            ItemUtil.addLoreOfStock(trader, originalIngredient, ingredient, false);
            inventory.setItem(row * 9 + 2 + i, ingredient);
        }

        ItemStack result;
        int leftUses = recipe.getMaxUses() - recipe.getUses();
        if (0 < leftUses) {
            result = recipe.getResult().clone();
            ItemUtil.addLoreOfStock(trader, recipe.getResult(), result, true);
            inventory.setItem(row * 9 + 5, result.clone());
            ItemUtil.displayName(trader.locale(), result, Translatables.GUI_RESULT_BULK_TRADE.apply(result));
            result.setAmount(Math.max(1, Math.min(leftUses, BoxUtil.calcConsumedAmount(trader, ingredients))));
        } else {
            result = Objects.requireNonNull(ItemUtil.create(
                    trader.locale(),
                    Material.BARRIER,
                    Translatables.GUI_RESULT_NAME_OUT_OF_STOCK.apply(recipe.getResult()))
            );
            ItemUtil.addLoreOfStock(trader, recipe.getResult(), result, true);
            inventory.setItem(row * 9 + 5, result.clone());
        }

        inventory.setItem(row * 9 + 6, result);
    }

    public void initialize() {
        ItemStack[] filled = inventory.getContents();
        Arrays.fill(filled, createNonButton());
        getInventory().setContents(filled);

        inventory.setItem(17, createArrow(Translatables.GUI_SCROLL_UP_ARROW));
        inventory.setItem(44, createArrow(Translatables.GUI_SCROLL_DOWN_ARROW));

        update();
    }

    public void update() {
        for (int row = 0; row < Math.min(merchant.getRecipeCount(), 6); row++) {
            updateTradeItem(row);
        }
    }

    private ItemStack createNonButton() {
        return ItemUtil.create(
                trader.locale(),
                Material.GRAY_STAINED_GLASS_PANE,
                Component.empty().color(NamedTextColor.BLACK)
        );
    }

    private ItemStack createArrow(Component name) {
        return ItemUtil.create(trader.locale(), Material.ARROW, name);
    }

    public void onClick(int slot) {
        int column = slot % 9;
        int row = slot / 9;

        int recipeIndex = row + getScroll();

        if (column == 0) {
            if (recipeIndex < merchant.getRecipeCount()) {
                setCurrentSelected(recipeIndex != getCurrentSelected() ? recipeIndex : -1);
            }
        } else if (column == 5) {
            boolean tradeSuccess = trade(recipeIndex);

            if (merchant instanceof AbstractVillager villager) {
                if (tradeSuccess) {
                    trader.playSound(villager, Sound.ENTITY_VILLAGER_TRADE, 1, 1);
                } else {
                    trader.playSound(villager, Sound.ENTITY_VILLAGER_NO, 1, 1);
                }
            }
        } else if (column == 6) {
            boolean tradeSuccess = tradeForMaxUses(recipeIndex) > 0;

            if (merchant instanceof AbstractVillager villager) {
                if (tradeSuccess) {
                    trader.playSound(villager, Sound.ENTITY_VILLAGER_TRADE, 1, 1);
                } else {
                    trader.playSound(villager, Sound.ENTITY_VILLAGER_NO, 1, 1);
                }
            }
        } else if (column == 8) {
            if (row == 1) {
                scrollUp();
            } else if (row == 4) {
                scrollDown();
            }
        }
    }

    public int tradeForMaxUses(int recipeIndex) {
        if (!BoxUtil.checkPlayerCondition(trader, "boxtradestick.trade")) {
            return 0;
        }

        if (recipeIndex < 0 || recipeIndex >= merchant.getRecipeCount()) {
            return 0;
        }

        MerchantRecipe merchantOffer = merchant.getRecipe(recipeIndex);
        int traded = 0;
        for (int i = merchantOffer.getUses(); i < merchantOffer.getMaxUses(); i++) {
            if (trade0(merchantOffer)) {
                traded++;
            }
        }
        if (traded > 0) {
            update();
            trader.sendActionBar(Translatables.RESULT_TIMES.apply(traded, merchantOffer.getResult()));
        }
        return traded;
    }

    public boolean trade(int recipeIndex) {
        if (!BoxUtil.checkPlayerCondition(trader, "boxtradestick.trade")) {
            return false;
        }

        if (recipeIndex < 0 || recipeIndex >= merchant.getRecipeCount()) {
            return false;
        }

        if (trade0(merchant.getRecipe(recipeIndex))) {
            update();
            return true;
        } else {
            return false;
        }
    }

    private boolean trade0(MerchantRecipe merchantOffer) {
        if (merchantOffer.getUses() >= merchantOffer.getMaxUses()) {
            return false;
        }

        PlayerPurchaseEvent event;
        if (merchant instanceof AbstractVillager abstractVillager) {
            event = new PlayerTradeEvent(trader, abstractVillager, merchantOffer, true, true);
        } else {
            event = new PlayerPurchaseEvent(trader, merchantOffer, false, true);
        }
        if (!event.callEvent()) {
            return false;
        }
        merchantOffer = event.getTrade();
        List<ItemStack> ingredients = new ArrayList<>(merchantOffer.getIngredients());
        if (ingredients.size() >= 1) {
            ingredients.set(0, merchantOffer.getAdjustedIngredient1());
        }
        if (!BoxUtil.tryConsumingStockMulti(trader, ingredients)) {
            return false;
        }

        var resultBukkit = merchantOffer.getResult();

        var result = BoxUtil.getBoxItem(resultBukkit);
        if (result.isEmpty()) {
            // painful api usage!
            BoxItem resultBoxItem = BoxItemFactory.createCustomItem(
                    resultBukkit.clone(),
                    PlainTextComponentSerializer.plainText().serialize(resultBukkit.displayName()),
                    -1
            );
            TransactionResult transaction = InventoryTransaction.withdraw(
                    trader.getInventory(),
                    resultBoxItem,
                    resultBukkit.getAmount()
            );
            ItemStack drop = resultBukkit.clone();
            if (transaction.getType().isModified()) {
                if (drop.getAmount() != transaction.getAmount()) {
                    drop.setAmount(drop.getAmount() - transaction.getAmount());
                    drop(trader, drop);
                }
            } else {
                drop(trader, drop);
            }
        } else {
            // stock is definitely present.
            BoxUtil.getStock(trader).ifPresent(stock -> stock.increase(result.get(), resultBukkit.getAmount()));
        }

        NMSUtil.processTrade(trader, merchant, merchantOffer, event);
        return true;
    }

    private static void drop(Player player, ItemStack item) {
        ItemStack hand = player.getInventory().getItemInMainHand().clone();
        player.getInventory().setItemInMainHand(item);
        player.dropItem(true);
        player.getInventory().setItemInMainHand(hand);
    }

    public int getCurrentSelected() {
        return getTradeStickData().getOfferSelections().getOrDefault(trader.getUniqueId(), -1);
    }

    public void setCurrentSelected(int index) {
        TradeStickData data = getTradeStickData();
        data.getOfferSelections().put(trader.getUniqueId(), index);
        setTradeStickData(data);
        update();
    }

    public int getScroll() {
        return Math.max(0, Math.min(getMaxScroll(), getTradeStickData().getScrolls().getOrDefault(trader.getUniqueId(), 0)));
    }

    public void setScroll(int scroll) {
        TradeStickData data = getTradeStickData();
        data.getScrolls().put(trader.getUniqueId(), scroll);
        setTradeStickData(data);
        update();
    }

    private TradeStickData getTradeStickData() {
        if (!(merchant instanceof AbstractVillager villager)) {
            return new TradeStickData();
        }

        if (!villager.getPersistentDataContainer().has(
                TradeStickData.TRADE_STICK_DATA_KEY,
                TradeStickData.TRADE_STICK_DATA_TYPE)) {
            villager.getPersistentDataContainer().remove(TradeStickData.TRADE_STICK_DATA_KEY);
        }
        return villager.getPersistentDataContainer().getOrDefault(
                TradeStickData.TRADE_STICK_DATA_KEY,
                TradeStickData.TRADE_STICK_DATA_TYPE,
                new TradeStickData()
        );
    }

    private void setTradeStickData(TradeStickData tradeStickData) {
        if (merchant instanceof AbstractVillager villager) {
            villager.getPersistentDataContainer().set(
                    TradeStickData.TRADE_STICK_DATA_KEY,
                    TradeStickData.TRADE_STICK_DATA_TYPE,
                    tradeStickData
            );
        }
    }
}
