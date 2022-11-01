package net.okocraft.boxtradestick;

import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.okocraft.box.api.message.argument.DoubleArgument;
import net.okocraft.box.api.message.argument.SingleArgument;
import org.bukkit.entity.AbstractVillager;
import org.bukkit.entity.Villager;
import org.bukkit.entity.WanderingTrader;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Merchant;

public final class Translatables {

    private Translatables() {}

    public static final SingleArgument<Merchant> GUI_TITLE =
            merchant -> Component.translatable("gui.title")
                    .args(getProfession(merchant), getMerchantNameOrUUID(merchant));

    public static final SingleArgument<ItemStack> GUI_RESULT_NAME_OUT_OF_STOCK =
            item -> nonItalic("gui.result-name-out-of-stock")
                    .args(Component.translatable().key(item))
                    .color(NamedTextColor.YELLOW);

    public static final SingleArgument<Integer> GUI_CURRENT_STOCK =
            stock -> nonItalic("gui.current-stock")
                    .args(Component.text(stock).color(NamedTextColor.AQUA))
                    .color(NamedTextColor.GRAY);

    public static final DoubleArgument<Integer, ItemStack> RESULT_TIMES =
            (traded, result) -> Component.translatable("result-times", NamedTextColor.YELLOW).args(
                    Component.text(traded),
                    Component.text(result.getAmount() * traded),
                    Component.translatable(result)
            );

    public static final List<Component> GUI_RECIPE_SELECTED_LORE =
            List.of(
                    nonItalic("gui.recipe-selected-lore-1").color(NamedTextColor.GRAY),
                    nonItalic("gui.recipe-selected-lore-2").color(NamedTextColor.GRAY)
            );

    public static final Component GUI_SCROLL_UP_ARROW = nonItalic("gui.scroll-up-arrow")
            .color(NamedTextColor.GOLD);

    public static final Component GUI_SCROLL_DOWN_ARROW = nonItalic("gui.scroll-down-arrow")
            .color(NamedTextColor.GOLD);

    public static final Component GUI_RECIPE_SELECTED = nonItalic("gui.recipe-selected");

    public static final Component GUI_RECIPE_NOT_SELECTED = nonItalic("gui.recipe-not-selected");

    public static final Component OUT_OF_STOCK = nonItalic("gui.result-out-of-stock")
            .color(NamedTextColor.YELLOW);

    private static TranslatableComponent nonItalic(String key) {
        return Component.translatable(key).decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }

    private static Component getMerchantNameOrUUID(Merchant merchant) {
        if (merchant instanceof AbstractVillager villager) {
            return villager.customName() == null
                    ? Component.text(villager.getUniqueId().toString().substring(0, 9) + "...")
                    : villager.customName();
        } else {
            return Component.text("Custom");
        }
    }

    private static Component getProfession(Merchant merchant) {
        if (merchant instanceof Villager villager) {
            return Component.translatable(villager.getProfession().translationKey());
        } else if (merchant instanceof WanderingTrader wanderingTrader) {
            return Component.translatable(wanderingTrader.getType().translationKey());
        } else {
            return Component.text("Custom");
        }
    }
}
