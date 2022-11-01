package net.okocraft.boxtradestick;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import net.okocraft.box.api.BoxAPI;
import net.okocraft.box.api.BoxProvider;
import net.okocraft.box.api.model.item.BoxItem;
import net.okocraft.box.api.model.stock.StockHolder;
import net.okocraft.box.feature.stick.StickFeature;
import net.okocraft.box.feature.stick.item.BoxStickItem;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public final class BoxUtil {

    private static final BoxAPI BOX = BoxProvider.get();

    private BoxUtil() {}

    public static Optional<StockHolder> getStock(Player player) {
        var playerMap = BoxProvider.get().getBoxPlayerMap();

        if (!playerMap.isLoaded(player)) {
            return Optional.empty();
        }

        return Optional.of(playerMap.get(player).getCurrentStockHolder());
    }

    public static boolean tryConsumingStockMulti(Player player, List<ItemStack> items) {
        var stock = getStock(player);
        if (stock.isEmpty()) {
            return false;
        }

        items = new ArrayList<>(items);
        items.removeIf(i -> i.getType() == Material.AIR);

        var requiredItems = new HashMap<BoxItem, Integer>();
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

        requiredItems.forEach(stock.get()::decrease);
        return true;
    }

    public static Optional<BoxItem> getBoxItem(ItemStack item) {
        return BoxProvider.get().getItemManager().getBoxItem(item);
    }

    public static BoxStickItem getBoxStickItem() {
        try {
            var stickFeature = BOX.getFeatures().stream()
                    .filter(f -> f instanceof StickFeature)
                    .map(f -> (StickFeature) f)
                    .findFirst()
                    .orElseThrow(() -> new Exception("Stick feature is not loaded"));

            // crying for the fact we need to reflect field;;
            var boxStickItemField = stickFeature.getClass().getDeclaredField("boxStickItem");
            boxStickItemField.setAccessible(true);
            return (BoxStickItem) boxStickItemField.get(stickFeature);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load boxStickItem.", e);
        }
    }

    public static boolean checkPlayerCondition(@NotNull Player player, @NotNull String permissionNode) {
        if (player.getGameMode() != GameMode.ADVENTURE &&
                player.getGameMode() != GameMode.SURVIVAL) {
            return false;
        }

        if (BoxProvider.get().isDisabledWorld(player)) {
            return false;
        }

        if (!player.hasPermission(permissionNode)) {
            return false;
        }

        return getBoxStickItem().check(player.getInventory().getItemInOffHand())
                || getBoxStickItem().check(player.getInventory().getItemInMainHand());
    }

}
