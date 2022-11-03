package net.okocraft.boxtradestick;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Sound;
import org.bukkit.entity.AbstractVillager;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.Merchant;

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

        if (!isTrading(villager)) {
            NMSUtil.stopTrading(villager);
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

        if (!NMSUtil.simulateMobInteract(player, villager, EquipmentSlot.HAND)) {
            return;
        }

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

        NMSUtil.stopTrading(villager);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (onEntityDamageByEntityEvent || !(event.getRightClicked() instanceof AbstractVillager abstractVillager)) {
            return;
        }

        if (!isTrading(abstractVillager)) {
            NMSUtil.stopTrading(abstractVillager);
        }

        Player player = event.getPlayer();
        if (!BoxUtil.checkPlayerCondition(player, "boxtradestick.trade")) {
            return;
        }

        event.setCancelled(true);

        if (abstractVillager instanceof Villager villager
                && !NMSUtil.simulateMobInteract(player, villager, event.getHand())) {
            return;
        }

        player.openInventory(new MerchantRecipesGUI(player, abstractVillager).getInventory());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof MerchantRecipesGUI gui
                && event.getClickedInventory() != null) {
            event.setCancelled(true);
            gui.onClick(event.getSlot());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof MerchantRecipesGUI gui) {
            gui.onClose();
        }
    }

    private boolean isTrading(Merchant merchant) {
        if (!(merchant instanceof AbstractVillager villager)) {
            return false;
        }

        HumanEntity trader = merchant.getTrader();
        if (trader == null) {
            return false;
        }

        Inventory inv = trader.getOpenInventory().getTopInventory();
        if (inv.getHolder() instanceof MerchantRecipesGUI gui) {
            return gui.getMerchant().equals(merchant);
        } else if (inv.getHolder() instanceof AbstractVillager villager1) {
            return villager.equals(villager1);
        }

        return false;
    }

}
