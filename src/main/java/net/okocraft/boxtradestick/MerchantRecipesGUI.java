package net.okocraft.boxtradestick;

import io.papermc.paper.event.player.PlayerPurchaseEvent;
import io.papermc.paper.event.player.PlayerTradeEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.okocraft.box.api.BoxProvider;
import net.okocraft.box.api.model.item.BoxItem;
import net.okocraft.box.api.transaction.InventoryTransaction;
import net.okocraft.box.storage.api.factory.item.BoxItemFactory;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.AbstractVillager;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Merchant;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

public class MerchantRecipesGUI implements InventoryHolder {

    private static final NamespacedKey SELECTED_RECIPE_KEY =
            Objects.requireNonNull(NamespacedKey.fromString("boxtradestick:selected_recipe"));

    private final Inventory inventory;

    private int scroll = 0;
    private final Player trader;
    private final Merchant merchant;
    private final Function<ItemStack, Component> loreCreator;

    public MerchantRecipesGUI(Player trader, Merchant merchant) {
        this.inventory = Bukkit.createInventory(this, 54, Translatables.GUI_TITLE.apply(merchant));
        this.trader = trader;
        this.merchant = merchant;

        loreCreator = item -> BoxProvider.get().getItemManager()
                .getBoxItem(item)
                .flatMap(i -> BoxUtil.getStock(trader).map(stock -> stock.getAmount(i)))
                .map(Translatables.GUI_CURRENT_STOCK::apply)
                .orElse(Translatables.GUI_CURRENT_STOCK.apply(0));

        update();
    }

    public int getMaxScroll() {
        return Math.max(0, merchant.getRecipeCount() - 6);
    }

    public int getScroll() {
        return this.scroll;
    }

    public void scrollDown() {
        if (scroll + 1 <= getMaxScroll()) {
            scroll++;
            update();
        }
    }

    public void scrollUp() {
        if (1 <= scroll) {
            scroll--;
            update();
        }
    }

    public void updateTradeItem(int row) {
        int recipeIndex = row + scroll;

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
        for (int j = 0; j < recipe.getIngredients().size(); j++) {
            ItemStack ingredient = recipe.getIngredients().get(j).clone();
            if (j == 0) {
                recipe.adjust(ingredient);
            }
            ItemUtil.lore(trader.locale(), ingredient, Collections.singletonList(loreCreator.apply(ingredient)));
            inventory.setItem(row * 9 + 2 + j, ingredient);
        }

        ItemStack result;
        if (recipe.getUses() < recipe.getMaxUses()) {
            result = recipe.getResult().clone();
        } else {
            result = Objects.requireNonNull(ItemUtil.create(
                    trader.locale(),
                    Material.BARRIER,
                    Translatables.GUI_RESULT_NAME_OUT_OF_STOCK.apply(recipe.getResult()))
            );
        }
        ItemUtil.lore(trader.locale(), result, Collections.singletonList(loreCreator.apply(recipe.getResult())));
        inventory.setItem(row * 9 + 5, result);
    }

    public void update() {
        ItemStack[] filled = inventory.getContents();
        Arrays.fill(filled, createNonButton());
        getInventory().setContents(filled);

        inventory.setItem(16, createArrow(Translatables.GUI_SCROLL_UP_ARROW));
        inventory.setItem(43, createArrow(Translatables.GUI_SCROLL_DOWN_ARROW));

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
        return ItemUtil.create(
                trader.locale(),
                Material.ARROW,
                name.decorate(TextDecoration.BOLD)
                        .decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE)
                        .color(NamedTextColor.GOLD)
        );
    }

    public void onClick(int slot) {
        int column = slot % 9;
        int row = slot / 9;

        int recipeIndex= row + scroll;

        if (column == 0) {
            if (row < merchant.getRecipeCount()) {
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
        } else if (column == 7) {
            if (row == 1) {
                scrollUp();
            } else if (row == 4) {
                scrollDown();
            }
        }
    }

    public int tradeForMaxUses(int recipeIndex) {
        if (!BoxUtil.checkPlayerCondition(trader, "boxtradestick.trade0")) {
            return 0;
        }

        if (recipeIndex < 0 || recipeIndex >= merchant.getRecipeCount()) {
            return 0;
        }

        MerchantRecipe merchantOffer = merchant.getRecipe(recipeIndex);
        int traded = 0;
        for (int i = merchantOffer.getUses(); i <= merchantOffer.getMaxUses(); i++) {
            if (trade0(merchantOffer)) {
                traded++;
            }
        }
        if (traded > 0) {
            updateTradeItem(recipeIndex - scroll);
            trader.sendActionBar(Translatables.RESULT_TIMES.apply(traded, merchantOffer.getResult()));
        }
        return traded;
    }

    public boolean trade(int recipeIndex) {
        if (!BoxUtil.checkPlayerCondition(trader, "boxtradestick.trade0")) {
            return false;
        }

        if (recipeIndex < 0 || recipeIndex >= merchant.getRecipeCount()) {
            return false;
        }

        if (trade0(merchant.getRecipe(recipeIndex))) {
            updateTradeItem(recipeIndex - scroll);
            return true;
        } else {
            return false;
        }
    }

    private boolean trade0(MerchantRecipe merchantOffer) {
        if (merchantOffer.getUses() > merchantOffer.getMaxUses()) {
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
            ItemStack drop = resultBukkit.clone();
            drop.setAmount(drop.getAmount() - InventoryTransaction
                    .withdraw(trader.getInventory(), resultBoxItem, resultBukkit.getAmount()).getAmount());
            trader.getWorld().dropItemNaturally(trader.getLocation(), drop);
        } else {
            // stock is definitely present.
            BoxUtil.getStock(trader).ifPresent(stock -> stock.increase(result.get(), resultBukkit.getAmount()));
        }

        NMSUtil.processTrade(trader, merchant, merchantOffer, event);
        return true;
    }

    public int getCurrentSelected() {
        if (merchant instanceof AbstractVillager villager) {
            return villager.getPersistentDataContainer()
                    .getOrDefault(SELECTED_RECIPE_KEY, PersistentDataType.INTEGER, -1);
        }

        return -1;
    }

    public void setCurrentSelected(int index) {
        if (merchant instanceof AbstractVillager villager) {
            villager.getPersistentDataContainer().set(SELECTED_RECIPE_KEY, PersistentDataType.INTEGER, index);
            update();
        }
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
