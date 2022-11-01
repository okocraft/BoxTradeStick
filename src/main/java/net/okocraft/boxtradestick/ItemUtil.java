package net.okocraft.boxtradestick;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.StreamSupport;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.translation.GlobalTranslator;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class ItemUtil {

    private ItemUtil() {}

    public static void addLoreOfStock(Player trader, ItemStack itemToCheck, ItemStack itemToApply, boolean useInventoryIfInvalidItem) {
        List<Component> lore = itemToApply.lore();
        if (lore == null) {
            lore = new ArrayList<>();
        } else {
            lore = new ArrayList<>(lore);
        }

        if (BoxUtil.getBoxItem(itemToCheck).isPresent()) {
            lore.add(Translatables.GUI_CURRENT_STOCK.apply(trader, itemToCheck));
        } else if (useInventoryIfInvalidItem) {
            int stock = StreamSupport.stream(trader.getInventory().spliterator(), false)
                    .filter(itemToCheck::isSimilar)
                    .map(ItemStack::getAmount)
                    .reduce(Integer::sum).orElse(0);
            lore.add(Translatables.GUI_CURRENT_STOCK_RAW.apply(stock));
        }
        lore(trader.locale(), itemToApply, lore);
    }

    public static void lore(Locale locale, @NotNull ItemStack item, @Nullable List<Component> lore) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (lore != null) {
                lore = new ArrayList<>(lore);
                lore.replaceAll(line -> GlobalTranslator.render(line, locale));
            }
            meta.lore(lore);
            item.setItemMeta(meta);
        }
    }

    public static void displayName(Locale locale, @NotNull ItemStack item, @Nullable Component displayName) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (displayName != null) {
                displayName = GlobalTranslator.render(displayName, locale);
            }
            meta.displayName(displayName);
            item.setItemMeta(meta);
        }
    }

    @Nullable
    public static ItemStack create(@NotNull Material material) {
        if (!material.isItem()) {
            return null;
        }
        return new ItemStack(material);
    }

    @Nullable
    public static ItemStack create(Locale locale, @NotNull Material material, @Nullable Component displayName) {
        ItemStack created = create(material);
        if (created == null) {
            return null;
        }
        displayName(locale, created, displayName);
        return created;
    }

    @Nullable
    public static ItemStack create(Locale locale, @NotNull Material material, @Nullable Component displayName, @Nullable List<Component> lore) {
        ItemStack created = create(locale, material, displayName);
        if (created == null) {
            return null;
        }
        lore(locale, created, lore);
        return created;
    }
}
