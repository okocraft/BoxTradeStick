package net.okocraft.boxtradestick;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import net.okocraft.box.api.BoxAPI;
import net.okocraft.box.api.event.stockholder.stock.StockEvent;
import net.okocraft.box.api.model.item.BoxItem;
import net.okocraft.box.api.model.stock.StockHolder;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.jetbrains.annotations.NotNull;

public final class BoxUtil {

    private BoxUtil() {}

    public static Optional<StockHolder> getStock(Player player) {
        var playerMap = BoxAPI.api().getBoxPlayerMap();

        if (!playerMap.isLoaded(player)) {
            return Optional.empty();
        }

        return Optional.of(playerMap.get(player).getCurrentStockHolder());
    }

    public static boolean tryConsumingStockMulti(Player player, List<ItemStack> items, Trade cause) {
        var stock = getStock(player);
        if (stock.isEmpty()) {
            return false;
        }

        items = new ArrayList<>(items);
        items.removeIf(i -> i.getType().isAir());

        var requiredItems = new ConcurrentHashMap<BoxItem, Integer>();
        for (ItemStack ingredient: items) {
            var boxItem = Optional.ofNullable(ingredient).flatMap(BoxUtil::getBoxItem);
            if (boxItem.isEmpty()) {
                return false;
            }
            if (stock.get().getAmount(boxItem.get()) < ingredient.getAmount()) {
                return false;
            }
            requiredItems.put(boxItem.get(), ingredient.getAmount());
        }

        for (var entry : requiredItems.entrySet()) {
            stock.get().decrease(entry.getKey(), entry.getValue(), cause);
        }

        return true;
    }

    public static int calcConsumedAmount(Player player, List<ItemStack> items) {
        var stock = getStock(player);
        if (stock.isEmpty()) {
            return 0;
        }

        items = new ArrayList<>(items);
        items.removeIf(i -> i.getType().isAir());

        int consumingAmount = Integer.MAX_VALUE;
        for (ItemStack ingredient: items) {
            var boxItem = Optional.ofNullable(ingredient).flatMap(BoxUtil::getBoxItem);
            if (boxItem.isEmpty()) {
                return 0;
            }
            consumingAmount = Math.min(consumingAmount, stock.get().getAmount(boxItem.get()) / ingredient.getAmount());
        }

        return consumingAmount;
    }

    public static Optional<BoxItem> getBoxItem(ItemStack item) {
        return BoxAPI.api().getItemManager().getBoxItem(item);
    }

    public record Trade(@NotNull Player trader, @NotNull MerchantRecipe merchantRecipe) implements StockEvent.Cause {
        @Override
        public @NotNull String name() {
            return "trade";
        }
    }
}
