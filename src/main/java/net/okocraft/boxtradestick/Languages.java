package net.okocraft.boxtradestick;

import dev.siroshun.mcmsgdef.MessageKey;
import dev.siroshun.mcmsgdef.Placeholder;
import io.papermc.paper.datacomponent.DataComponentTypes;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.translation.Argument;
import org.bukkit.entity.AbstractVillager;
import org.bukkit.entity.Villager;
import org.bukkit.entity.WanderingTrader;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class Languages {

    private static final Map<String, String> DEFAULT_MESSAGES = new LinkedHashMap<>();
    private static final Placeholder<AbstractVillager> PROFESSION = abstractVillager -> Argument.component(
        "profession",
        switch (abstractVillager) {
            case Villager villager -> Component.translatable(villager.getProfession());
            case WanderingTrader trader -> Component.translatable(trader.getType());
            default -> Component.text("Custom");
        }
    );

    private static final Placeholder<AbstractVillager> VILLAGER_NAME = villager -> Argument.component(
        "name",
        () -> {
            var customName = villager.customName();
            return customName != null ? customName : Component.text(villager.getUniqueId().toString().substring(0, 9) + "...");
        }
    );

    private static final Placeholder<ItemStack> ITEM_NAME = item -> Argument.component(
        "item",
        () -> {
            Component itemName = item.getData(DataComponentTypes.ITEM_NAME);
            return itemName != null ? itemName : Component.translatable(item);
        }
    );

    private static final Placeholder<Integer> AMOUNT = amount -> Argument.numeric("amount", amount);
    private static final Placeholder<Long> COOLDOWN_MILLIS_TO_SECONDS = cooldown -> Argument.numeric("cooldown", ((double) cooldown) / 1000);
    private static final Placeholder<Integer> PRICE = price -> Argument.numeric("price", price);
    private static final Placeholder<Integer> NEW_PRICE = newPrice -> Argument.numeric("new_price", newPrice);
    private static final Placeholder<Integer> TIMES = times -> Argument.numeric("times", times);
    private static final Placeholder<Integer> KIND = kind -> Argument.numeric("kind", kind);

    public static final MessageKey.Arg2<AbstractVillager, AbstractVillager> GUI_TITLE = MessageKey.arg2(def("gui.title", "<black><profession> (<name>)"), PROFESSION, VILLAGER_NAME);
    public static final MessageKey.Arg1<Integer> GUI_CURRENT_STOCK = MessageKey.arg1(def("gui.current-stock", "<gray>Stock: <aqua><amount>"), AMOUNT);
    public static final MessageKey.Arg2<Integer, Integer> GUI_PRICE_DIFF = MessageKey.arg2(def("gui.price-diff", "<aqua><price><gray> → <aqua><new_price>"), PRICE, NEW_PRICE);
    public static final MessageKey RECIPE_SELECTED = MessageKey.key(def("gui.recipe.selected", "Selected"));
    public static final MessageKey RECIPE_NOT_SELECTED = MessageKey.key(def("gui.recipe.not-selected", "Not selected"));
    public static final MessageKey RECIPE_LORE = MessageKey.key(def("gui.recipe.lore", "<gray>You can trade item for max uses by hitting villagers<newline><gray>with holding box stick after select offer."));
    public static final MessageKey RECIPE_OUT_OF_STOCK = MessageKey.key(def("gui.recipe.out-of-stock", "<yellow>Out of stock!"));
    public static final MessageKey.Arg1<ItemStack> GUI_RESULT_NAME_AND_OUT_OF_STOCK = MessageKey.arg1(def("gui.result.name-and-out-of-stock", "<yellow><item> (Out of stock!)"), ITEM_NAME);
    public static final MessageKey.Arg1<ItemStack> GUI_RESULT_BULK_TRADE = MessageKey.arg1(def("gui.result.bulk-trade", "<gold>Bulk trade <aqua><item>"), ITEM_NAME);
    public static final MessageKey SCROLL_UP = MessageKey.key(def("gui.scroll.up-arrow", "<gold><bold>↑"));
    public static final MessageKey SCROLL_DOWN = MessageKey.key(def("gui.scroll.down-arrow", "<gold><bold>↓"));

    public static final MessageKey.Arg3<Integer, ItemStack, Integer> RESULT_TIMES = MessageKey.arg3(def("message.result-times.single", "<yellow>Traded <aqua><times></aqua> times and got <aqua><item></aqua>x<aqua><amount></aqua>."), TIMES, ITEM_NAME, AMOUNT);
    public static final MessageKey.Arg2<Integer, Integer> MULTIPLE_RESULT_TIMES = MessageKey.arg2(def("message.result-times.multiple", "<yellow>Total of <aqua><times></aqua> traded the <aqua><kind></aqua> kind of items."), TIMES, KIND);
    public static final MessageKey.Arg1<Long> HIT_TRADING_COOLDOWN = MessageKey.arg1(def("message.hit-trading-cooldown", "<yellow>Hit trading is now in cooldown for <aqua><cooldown></aqua> second(s)."), COOLDOWN_MILLIS_TO_SECONDS);

    @Contract("_, _ -> param1")
    private static @NotNull String def(@NotNull String key, @NotNull String msg) {
        DEFAULT_MESSAGES.put(key, msg);
        return key;
    }

    @Contract(pure = true)
    public static @NotNull @UnmodifiableView Map<String, String> defaultMessages() {
        return Collections.unmodifiableMap(DEFAULT_MESSAGES);
    }

    private Languages() {
        throw new UnsupportedOperationException();
    }
}
