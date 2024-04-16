package net.okocraft.boxtradestick;

import net.okocraft.box.api.event.stockholder.stock.StockEvent;
import org.bukkit.entity.Player;
import org.bukkit.inventory.MerchantRecipe;
import org.jetbrains.annotations.NotNull;

public record TradeCause(@NotNull Player trader, @NotNull MerchantRecipe merchantRecipe) implements StockEvent.Cause {

    @Override
    public @NotNull String name() {
        return "trade";
    }

}
