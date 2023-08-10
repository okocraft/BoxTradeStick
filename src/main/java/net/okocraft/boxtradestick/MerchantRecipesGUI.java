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
import org.bukkit.inventory.MerchantRecipe;
import org.jetbrains.annotations.NotNull;

public class MerchantRecipesGUI implements InventoryHolder {

    private static final Class<?> CUSTOM_INVENTORY_CLASS;

    static {
        CUSTOM_INVENTORY_CLASS = Bukkit.createInventory(null, 54, Component.empty()).getClass();
    }

    private final Inventory inventory;

    private final Player trader;
    private final AbstractVillager villager;
    private final TradeStickData data;

    public MerchantRecipesGUI(Player trader, AbstractVillager villager) {
        this.inventory = Bukkit.createInventory(this, 54, Translatables.GUI_TITLE.apply(villager));
        this.trader = trader;
        this.villager = villager;
        this.data = TradeStickData.loadFrom(villager);

        initialize();
    }

    public static boolean canTradeByStick(@NotNull AbstractVillager villager) {
        return villager.getRecipeCount() < TradeStickData.MAXIMUM_INDEX;
    }

    public static boolean isGUI(Inventory topInventory) {
        return fromTopInventory(topInventory) != null;
    }

    public static MerchantRecipesGUI fromTopInventory(Inventory topInventory) {
        if (CUSTOM_INVENTORY_CLASS.isInstance(topInventory) && topInventory.getHolder() instanceof MerchantRecipesGUI gui) {
            return gui;
        } else {
            return null;
        }
    }

    public AbstractVillager getMerchant() {
        return this.villager;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    public int getMaxScroll() {
        return Math.max(0, villager.getRecipeCount() - 6);
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

        if (data.isSelected(trader.getUniqueId(), recipeIndex)) {
            inventory.setItem(row * 9, ItemUtil.create(
                    trader.locale(),
                    Material.LIME_WOOL,
                    Translatables.GUI_RECIPE_SELECTED,
                    Translatables.GUI_RECIPE_SELECTED_LORE
            ));
        } else {
            inventory.setItem(row * 9, ItemUtil.create(
                    trader.locale(),
                    Material.RED_STAINED_GLASS,
                    Translatables.GUI_RECIPE_NOT_SELECTED,
                    Translatables.GUI_RECIPE_SELECTED_LORE
            ));
        }

        MerchantRecipe recipe = villager.getRecipe(recipeIndex);
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
        for (int row = 0; row < Math.min(villager.getRecipeCount(), 6); row++) {
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
            if (recipeIndex < villager.getRecipeCount()) {
                data.toggleOfferSelection(trader.getUniqueId(), recipeIndex);
                data.saveTo(villager);
            }
        } else if (column == 5) {
            boolean tradeSuccess = trade(recipeIndex);

            if (tradeSuccess) {
                trader.playSound(villager, Sound.ENTITY_VILLAGER_TRADE, 1, 1);
            } else {
                trader.playSound(villager, Sound.ENTITY_VILLAGER_NO, 1, 1);
            }
        } else if (column == 6) {
            boolean tradeSuccess = tradeForMaxUses(recipeIndex) > 0;

            if (tradeSuccess) {
                trader.playSound(villager, Sound.ENTITY_VILLAGER_TRADE, 1, 1);
            } else {
                trader.playSound(villager, Sound.ENTITY_VILLAGER_NO, 1, 1);
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

        if (recipeIndex < 0 || recipeIndex >= villager.getRecipeCount()) {
            return 0;
        }

        MerchantRecipe merchantOffer = villager.getRecipe(recipeIndex);
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

        if (recipeIndex < 0 || recipeIndex >= villager.getRecipeCount()) {
            return false;
        }

        if (trade0(villager.getRecipe(recipeIndex))) {
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

    /**
     * @deprecated use {@link #getCurrentSelectedIndices()}
     */
    @Deprecated(forRemoval = true)
    public int getCurrentSelected() {
        int[] selected = data.getSelectedIndices(trader.getUniqueId());
        return selected.length == 0 ? -1 : selected[0];
    }

    public int[] getCurrentSelectedIndices() {
        return data.getSelectedIndices(trader.getUniqueId());
    }

    public int getScroll() {
        return Math.max(0, Math.min(getMaxScroll(), data.getScroll(trader.getUniqueId())));
    }

    public void setScroll(int scroll) {
        data.setScroll(trader.getUniqueId(), scroll);
        data.saveTo(villager);
        update();
    }
}
