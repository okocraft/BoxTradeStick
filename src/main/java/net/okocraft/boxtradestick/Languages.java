package net.okocraft.boxtradestick;

import com.github.siroshun09.messages.minimessage.arg.Arg1;
import com.github.siroshun09.messages.minimessage.arg.Arg2;
import com.github.siroshun09.messages.minimessage.arg.Arg3;
import com.github.siroshun09.messages.minimessage.base.MiniMessageBase;
import com.github.siroshun09.messages.minimessage.base.Placeholder;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.AbstractVillager;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.entity.WanderingTrader;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import static com.github.siroshun09.messages.minimessage.arg.Arg1.arg1;
import static com.github.siroshun09.messages.minimessage.arg.Arg2.arg2;
import static com.github.siroshun09.messages.minimessage.arg.Arg3.arg3;
import static com.github.siroshun09.messages.minimessage.base.MiniMessageBase.messageKey;

public final class Languages {

    private static final Map<String, String> DEFAULT_MESSAGES = new LinkedHashMap<>();
    private static final Placeholder<AbstractVillager> PROFESSION = Placeholder.component(
        "profession",
        abstractVillager -> switch (abstractVillager) {
            case Villager villager -> Component.translatable(villager.getProfession());
            case WanderingTrader trader -> Component.translatable(trader.getType());
            default -> Component.text("Custom");
        }
    );
    private static final Placeholder<AbstractVillager> VILLAGER_NAME = Placeholder.component(
        "name",
        abstractVillager -> {
            var customName = abstractVillager.customName();
            return customName != null ? customName : Component.text(abstractVillager.getUniqueId().toString().substring(0, 9) + "...");
        }
    );
    private static final Placeholder<ItemStack> ITEM_NAME = Placeholder.component("item", item -> {
        var customName = item.hasItemMeta() ? item.getItemMeta().displayName() : null;
        return customName != null ? customName : Component.translatable(item);
    });
    private static final Placeholder<Integer> AMOUNT = Placeholder.component("amount", Component::text);
    private static final Placeholder<Long> COOLDOWN_MILLIS_TO_SECONDS = Placeholder.component("cooldown", cooldown -> Component.text(((double) cooldown) / 1000));
    private static final Placeholder<Integer> PRICE = Placeholder.component("price", Component::text);
    private static final Placeholder<Integer> NEW_PRICE = Placeholder.component("new_price", Component::text);
    private static final Placeholder<Integer> TIMES = Placeholder.component("times", Component::text);
    private static final Placeholder<Integer> KIND = Placeholder.component("kind", Component::text);

    public static final Arg2<AbstractVillager, AbstractVillager> GUI_TITLE = arg2(def("gui.title", "<black><profession> (<name>)"), PROFESSION, VILLAGER_NAME);
    public static final Arg1<Integer> GUI_CURRENT_STOCK = arg1(def("gui.current-stock", "<gray>Stock: <aqua><amount>"), AMOUNT);
    public static final Arg2<Integer, Integer> GUI_PRICE_DIFF = arg2(def("gui.price-diff", "<aqua><price><gray> → <aqua><new_price>"), PRICE, NEW_PRICE);
    public static final MiniMessageBase RECIPE_SELECTED = messageKey(def("gui.recipe.selected", "Selected"));
    public static final MiniMessageBase RECIPE_NOT_SELECTED = messageKey(def("gui.recipe.not-selected", "Not selected"));
    public static final MiniMessageBase RECIPE_LORE = messageKey(def("gui.recipe.lore", "<gray>You can trade item for max uses by hitting villagers<newline><gray>with holding box stick after select offer."));
    public static final MiniMessageBase RECIPE_OUT_OF_STOCK = messageKey(def("gui.recipe.out-of-stock", "<yellow>Out of stock!"));
    public static final Arg1<ItemStack> GUI_RESULT_NAME_AND_OUT_OF_STOCK = arg1(def("gui.result.name-and-out-of-stock", "<yellow><item> (Out of stock!)"), ITEM_NAME);
    public static final Arg1<ItemStack> GUI_RESULT_BULK_TRADE = arg1(def("gui.result.bulk-trade", "<gold>Bulk trade <aqua><item>"), ITEM_NAME);
    public static final MiniMessageBase SCROLL_UP = messageKey(def("gui.scroll.up-arrow", "<gold><bold>↑"));
    public static final MiniMessageBase SCROLL_DOWN = messageKey(def("gui.scroll.down-arrow", "<gold><bold>↓"));

    public static final Arg3<Integer, ItemStack, Integer> RESULT_TIMES = arg3(def("message.result-times.single", "<yellow>Traded <aqua><times></aqua> times and got <aqua><item></aqua>x<aqua><amount></aqua>."), TIMES, ITEM_NAME, AMOUNT);
    public static final Arg2<Integer, Integer> MULTIPLE_RESULT_TIMES = arg2(def("message.result-times.multiple", "<yellow>Total of <aqua><times></aqua> traded the <aqua><kind></aqua> kind of items."), TIMES, KIND);
    public static final Arg1<Long> HIT_TRADING_COOLDOWN = arg1(def("message.hit-trading-cooldown", "<yellow>Hit trading is now in cooldown for <aqua><cooldown></aqua> second(s)."), COOLDOWN_MILLIS_TO_SECONDS);

    @Contract("_, _ -> param1")
    private static @NotNull String def(@NotNull String key, @NotNull String msg) {
        DEFAULT_MESSAGES.put(key, msg);
        return key;
    }

    @Contract(pure = true)
    public static @NotNull @UnmodifiableView Map<String, String> defaultMessages() {
        return Collections.unmodifiableMap(DEFAULT_MESSAGES);
    }

    public static @NotNull Locale getLocaleFrom(@Nullable Object obj) {
        if (obj instanceof Locale locale) {
            return locale;
        } else if (obj instanceof Player player) {
            return player.locale();
        } else {
            return Locale.getDefault();
        }
    }

    private Languages() {
        throw new UnsupportedOperationException();
    }
}
