package net.okocraft.boxtradestick;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.StreamSupport;

import com.github.siroshun09.messages.minimessage.base.MiniMessageBase;
import com.github.siroshun09.messages.minimessage.source.MiniMessageSource;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.okocraft.box.api.BoxAPI;
import net.okocraft.box.feature.gui.api.util.ItemEditor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.AbstractVillager;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MerchantRecipesGUI implements InventoryHolder {

    private static final ItemStack NON_BUTTON = ItemEditor.create().displayName(Component.empty().color(NamedTextColor.BLACK)).createItem(Material.GRAY_STAINED_GLASS_PANE);
    private static final Class<?> CUSTOM_INVENTORY_CLASS;

    static {
        CUSTOM_INVENTORY_CLASS = Bukkit.createInventory(null, 54, Component.empty()).getClass();
    }

    public static MerchantRecipesGUI fromInventory(Inventory inventory) {
        return CUSTOM_INVENTORY_CLASS.isInstance(inventory) && inventory.getHolder() instanceof MerchantRecipesGUI gui ? gui : null;
    }

    private final Player trader;
    private final MiniMessageSource messageSource;
    private final CachedItems cachedItems;
    private final UUID worldUid;
    private final UUID villagerUuid;

    private final Inventory inventory;

    private boolean closed;

    public MerchantRecipesGUI(@NotNull Player trader, @NotNull MiniMessageSource messageSource, @NotNull AbstractVillager villager) {
        this.trader = trader;
        this.messageSource = messageSource;
        this.cachedItems = new CachedItems(messageSource);
        this.worldUid = villager.getWorld().getUID();
        this.villagerUuid = villager.getUniqueId();
        this.inventory = Bukkit.createInventory(this, 54, Languages.GUI_TITLE.apply(villager, villager).create(messageSource));

        initialize(villager);
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    private void initialize(@NotNull AbstractVillager villager) {
        ItemStack[] filled = this.inventory.getContents();
        Arrays.fill(filled, NON_BUTTON);
        this.inventory.setContents(filled);

        this.inventory.setItem(17, this.createArrow(Languages.SCROLL_UP));
        this.inventory.setItem(44, this.createArrow(Languages.SCROLL_DOWN));

        this.update(villager, TradeStickData.loadFrom(villager));
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

    private ItemStack createArrow(MiniMessageBase displayName) {
        return ItemEditor.create().displayName(displayName.create(this.messageSource)).createItem(Material.ARROW);
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
        this.inventory.setItem(row * 9, selected ? this.cachedItems.recipeSelected : this.cachedItems.recipeNotSelected);

        MerchantRecipe recipe = villager.getRecipe(recipeIndex);

        List<ItemStack> ingredients = recipe.getIngredients();
        int size = ingredients.size();

        if (size == 1 || size == 2) {
            var firstIngredient = ingredients.getFirst();
            this.inventory.setItem(row * 9 + 2, this.createIngredientIcon(firstIngredient, recipe, true).applyTo(firstIngredient.clone()));

            if (size == 2) {
                var secondIngredient = ingredients.getFirst();
                this.inventory.setItem(row * 9 + 3, this.createIngredientIcon(secondIngredient, recipe, false).applyTo(secondIngredient.clone()));
            } else {
                this.inventory.setItem(row * 9 + 3, null);
            }
        } else {
            return;
        }

        ItemStack resultIcon;
        ItemEditor<ItemMeta> editor;

        int leftUses = recipe.getMaxUses() - recipe.getUses();
        if (0 < leftUses) {
            resultIcon = recipe.getResult().asQuantity(Math.max(1, Math.min(leftUses, this.calcConsumedAmount(ingredients))));
            editor = ItemEditor.create().displayName(Languages.GUI_RESULT_BULK_TRADE.apply(resultIcon).create(this.messageSource)).copyLoreFrom(resultIcon);
        } else {
            resultIcon = new ItemStack(Material.BARRIER);
            editor = ItemEditor.create().displayName(Languages.GUI_RESULT_NAME_AND_OUT_OF_STOCK.apply(recipe.getResult()).create(this.messageSource));
        }

        editor.loreLine(Languages.GUI_CURRENT_STOCK.apply(this.getCurrentStock(recipe.getResult(), true)).create(this.messageSource)).applyTo(resultIcon);
        this.inventory.setItem(row * 9 + 5, resultIcon.getAmount() == 1 ? resultIcon : resultIcon.asOne());
        this.inventory.setItem(row * 9 + 6, resultIcon);
    }

    private @NotNull ItemEditor<ItemMeta> createIngredientIcon(ItemStack ingredient, MerchantRecipe recipe, boolean adjustPrice) {
        var editor = ItemEditor.create();
        editor.copyLoreFrom(ingredient);

        if (adjustPrice) {
            int originalPrice = ingredient.getAmount();
            var adjusted = recipe.getAdjustedIngredient1();

            if (adjusted != null && originalPrice != adjusted.getAmount()) {
                editor.loreLine(Languages.GUI_PRICE_DIFF.apply(ingredient.getAmount(), adjusted.getAmount()).create(this.messageSource));
                ingredient.setAmount(adjusted.getAmount());
            }
        }

        int currentStock = this.getCurrentStock(ingredient, false);
        if (currentStock != -1) {
            editor.loreLine(Languages.GUI_CURRENT_STOCK.apply(currentStock).create(this.messageSource));
        }

        return editor;
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
        return !closed && this != MerchantRecipesGUI.fromInventory(trader.getOpenInventory().getTopInventory());
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

    private int getCurrentStock(@NotNull ItemStack item, boolean useInventoryIfNotBoxItem) {
        var boxItem = BoxAPI.api().getItemManager().getBoxItem(item);
        var playerMap = BoxAPI.api().getBoxPlayerMap();

        if (boxItem.isPresent() && playerMap.isLoaded(this.trader)) {
            return playerMap.get(this.trader).getCurrentStockHolder().getAmount(boxItem.get());
        } else if (useInventoryIfNotBoxItem) {
            return StreamSupport.stream(this.trader.getInventory().spliterator(), false)
                .filter(item::isSimilar)
                .mapToInt(ItemStack::getAmount)
                .reduce(Integer::sum).orElse(0);
        } else {
            return -1;
        }
    }

    private static class CachedItems {
        private final ItemStack recipeSelected;
        private final ItemStack recipeNotSelected;

        private CachedItems(@NotNull MiniMessageSource messageSource) {
            var selectedLore = Languages.RECIPE_LORE.create(messageSource);
            this.recipeSelected = ItemEditor.create().displayName(Languages.RECIPE_SELECTED.create(messageSource)).loreLines(selectedLore).createItem(Material.LIME_WOOL);
            this.recipeNotSelected = ItemEditor.create().displayName(Languages.RECIPE_NOT_SELECTED.create(messageSource)).loreLines(selectedLore).createItem(Material.RED_STAINED_GLASS);
        }
    }
}
