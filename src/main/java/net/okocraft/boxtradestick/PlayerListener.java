package net.okocraft.boxtradestick;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Sound;
import org.bukkit.entity.AbstractVillager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {

    private final Map<UUID, Long> hitTradeCooldown = new HashMap<>();
    private boolean onEntityDamageByEntityEvent = false;

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        hitTradeCooldown.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onPlayerAttackVillager(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) {
            return;
        }

        if (!(event.getEntity() instanceof AbstractVillager villager)) {
            return;
        }

        if (!BoxUtil.checkPlayerCondition(player, "boxtradestick.trade")) {
            return;
        }

        event.setCancelled(true);

        long cooldown = hitTradeCooldown.getOrDefault(player.getUniqueId(), 0L) + 1000L - System.currentTimeMillis();
        if (0 < cooldown) {
            player.sendActionBar(Translatables.HIT_TRADING_COOLDOWN.apply(cooldown));
            return;
        }

        onEntityDamageByEntityEvent = true;
        if (!new PlayerInteractEntityEvent(player, villager).callEvent()) {
            return;
        }
        onEntityDamageByEntityEvent = false;

        MerchantRecipesGUI gui = new MerchantRecipesGUI(player, villager);
        int selectedOfferIndex = gui.getCurrentSelected();
        if (selectedOfferIndex == -1) {
            player.playSound(villager, Sound.ENTITY_VILLAGER_NO, 1, 1);
            return;
        }

        int traded = gui.tradeForMaxUses(gui.getCurrentSelected());
        if (traded > 0) {
            player.playSound(villager, Sound.ENTITY_VILLAGER_TRADE, 1, 1);
            hitTradeCooldown.put(player.getUniqueId(), System.currentTimeMillis());
        } else {
            player.playSound(villager, Sound.ENTITY_VILLAGER_NO, 1, 1);
            player.sendActionBar(Translatables.OUT_OF_STOCK);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (onEntityDamageByEntityEvent) {
            return;
        }
        if (event.getRightClicked() instanceof AbstractVillager villager) {
            Player player = event.getPlayer();
            if (BoxUtil.checkPlayerCondition(player, "boxtradestick.trade")) {
                player.openInventory(new MerchantRecipesGUI(player, villager).getInventory());
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof MerchantRecipesGUI gui
                && event.getClickedInventory() != null) {
            event.setCancelled(true);
            gui.onClick(event.getSlot());
        }
    }

}
