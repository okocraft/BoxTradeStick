package net.okocraft.boxtradestick;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.okocraft.box.api.BoxAPI;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.AbstractVillager;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MerchantRecipesGUI implements InventoryHolder {

    private static final Class<?> CUSTOM_INVENTORY_CLASS;

    static {
        CUSTOM_INVENTORY_CLASS = Bukkit.createInventory(null, 54, Component.empty()).getClass();
    }

    public static MerchantRecipesGUI fromTopInventory(Inventory topInventory) {
        if (CUSTOM_INVENTORY_CLASS.isInstance(topInventory) && topInventory.getHolder() instanceof MerchantRecipesGUI gui) {
            return gui;
        } else {
            return null;
        }
    }

    private final Player trader;
    private final UUID worldUid;
    private final UUID villagerUuid;

    private final Inventory inventory;

    private boolean closed;

    public MerchantRecipesGUI(@NotNull Player trader, @NotNull AbstractVillager villager) {
        this.trader = trader;
        this.worldUid = villager.getWorld().getUID();
        this.villagerUuid = villager.getUniqueId();
        this.inventory = Bukkit.createInventory(this, 54, Translatables.GUI_TITLE.apply(villager));

        initialize(villager);
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    private void initialize(@NotNull AbstractVillager villager) {
        ItemStack[] filled = inventory.getContents();
        Arrays.fill(filled, createNonButton());
        getInventory().setContents(filled);

        inventory.setItem(17, createArrow(Translatables.GUI_SCROLL_UP_ARROW));
        inventory.setItem(44, createArrow(Translatables.GUI_SCROLL_DOWN_ARROW));

        update(villager, TradeStickData.loadFrom(villager));
    }

    public void scheduleWatchingTask() {
        trader.getScheduler().runAtFixedRate(
                JavaPlugin.getPlugin(BoxTradeStickPlugin.class),
                task -> {
                    if (!closed) {
                        if (isSilentlyClosed()) {
                            onClose();
                        } else if (shouldClose()) {
                            trader.closeInventory();
                        }
                    }

                    if (closed) {
                        task.cancel();
                    }
                },
                this::onClose, 1, 1
        );
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

    private void update(@NotNull AbstractVillager villager, @NotNull TradeStickData data) {
        int recipeCount = villager.getRecipeCount();

        int maxScroll = getMaxScroll(recipeCount);
        int scroll = getScroll(maxScroll, data);

        for (int row = 0; row < Math.min(recipeCount, 6); row++) {
            int recipeIndex = row + scroll;
            updateTradeItem(villager, row, recipeIndex, data.isSelected(trader.getUniqueId(), recipeIndex));
        }
    }

    private void updateTradeItem(@NotNull AbstractVillager villager, int row, int recipeIndex, boolean selected) {
        if (selected) {
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
            result.setAmount(Math.max(1, Math.min(leftUses, this.calcConsumedAmount(ingredients))));
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

    public void onClick(int slot) {
        var villager = getVillager();

        if (shouldClose(villager)) {
            trader.closeInventory();
            return;
        }

        var data = TradeStickData.loadFrom(villager);

        int column = slot % 9;
        int row = slot / 9;

        int maxScroll = getMaxScroll(villager.getRecipeCount());
        int scroll = getScroll(maxScroll, data);
        int recipeIndex = row + scroll;

        if (column == 0) {
            if (recipeIndex < villager.getRecipeCount()) {
                data.toggleOfferSelection(trader.getUniqueId(), recipeIndex);
                data.saveTo(villager);
                update(villager, data);
            }
        } else if (column == 5) {
            boolean tradeSuccess = TradeProcessor.trade(trader, villager, recipeIndex);

            if (tradeSuccess) {
                update(villager, data);
                trader.playSound(villager, Sound.ENTITY_VILLAGER_TRADE, 1, 1);
            } else {
                trader.playSound(villager, Sound.ENTITY_VILLAGER_NO, 1, 1);
            }
        } else if (column == 6) {
            boolean tradeSuccess = TradeProcessor.tradeForMaxUses(trader, villager, recipeIndex) != null;

            if (tradeSuccess) {
                update(villager, data);
                trader.playSound(villager, Sound.ENTITY_VILLAGER_TRADE, 1, 1);
            } else {
                trader.playSound(villager, Sound.ENTITY_VILLAGER_NO, 1, 1);
            }
        } else if (column == 8) {
            if (row == 1) {
                scroll--; // scroll up
            } else if (row == 4) {
                scroll++; // scroll down
            } else {
                return;
            }

            if (0 <= scroll && scroll <= maxScroll) {
                data.setScroll(trader.getUniqueId(), scroll);
                data.saveTo(villager);
                update(villager, data);
            }
        }
    }

    public void onClose() {
        if (this.closed) {
            return;
        }

        this.closed = true;

        var villager = getVillagerUnsafe();

        if (villager != null) {
            if (Bukkit.isOwnedByCurrentRegion(villager)) {
                tryStopTrading(villager);
            } else {
                villager.getScheduler().run(JavaPlugin.getPlugin(BoxTradeStickPlugin.class), task -> tryStopTrading(), null);
            }
        }
    }

    public @Nullable AbstractVillager getVillager() {
        var villager = getVillagerUnsafe();
        return villager != null && Bukkit.isOwnedByCurrentRegion(villager) ? villager : null;
    }

    private @Nullable AbstractVillager getVillagerUnsafe() {
        var world = Bukkit.getWorld(worldUid);

        if (world != null && world.getEntity(villagerUuid) instanceof AbstractVillager villager) {
            return villager;
        } else {
            return null;
        }
    }

    private int getMaxScroll(int recipeCount) {
        return Math.max(0, recipeCount - 6);
    }

    private int getScroll(int maxScroll, @NotNull TradeStickData data) {
        return Math.max(0, Math.min(maxScroll, data.getScroll(trader.getUniqueId())));
    }

    private boolean isSilentlyClosed() {
        return !closed && this != MerchantRecipesGUI.fromTopInventory(trader.getOpenInventory().getTopInventory());
    }

    private boolean shouldClose() {
        return shouldClose(getVillager());
    }

    @Contract("null -> true")
    private boolean shouldClose(@Nullable AbstractVillager villager) {
        return villager == null ||
                villager.isDead() ||
                !villager.getWorld().equals(trader.getWorld()) ||
                100 < villager.getLocation().distanceSquared(trader.getLocation()) ||
                !trader.equals(villager.getTrader());
    }

    private void tryStopTrading() {
        var villager = getVillager();

        if (villager != null) {
            tryStopTrading(villager);
        }
    }

    private void tryStopTrading(@NotNull AbstractVillager villager) {
        if (trader.equals(villager.getTrader())) {
            NMSUtil.stopTrading(villager);
        }
    }

    private int calcConsumedAmount(List<ItemStack> ingredients) {
        if (!BoxAPI.api().getBoxPlayerMap().isLoaded(this.trader)) {
            return 0;
        }

        var stockHolder = BoxAPI.api().getBoxPlayerMap().get(this.trader).getCurrentStockHolder();
        int consumingAmount = Integer.MAX_VALUE;

        for (var ingredient : ingredients) {
            if (ingredient.getType().isAir()) {
                continue;
            }

            var boxItem = BoxAPI.api().getItemManager().getBoxItem(ingredient);

            if (boxItem.isEmpty()) {
                return 0;
            } else {
                consumingAmount = Math.min(consumingAmount, stockHolder.getAmount(boxItem.get()) / ingredient.getAmount());
            }
        }

        return consumingAmount;
    }
}
