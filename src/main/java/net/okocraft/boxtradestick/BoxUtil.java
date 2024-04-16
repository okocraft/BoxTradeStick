package net.okocraft.boxtradestick;

import java.util.Optional;
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
