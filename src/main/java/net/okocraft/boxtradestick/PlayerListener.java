package net.okocraft.boxtradestick;

import io.papermc.paper.event.entity.EntityMoveEvent;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.entity.AbstractVillager;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPortalEnterEvent;
import org.bukkit.event.entity.EntityTeleportEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.Merchant;
import org.bukkit.inventory.MerchantInventory;

public class PlayerListener implements Listener {

    private static final long TRADE_COOLDOWN_TIME = 1000L;
    private static final long TRADE_COOLDOWN_TIME_AFTER_THE_2ND = 500L;

    private final BoxTradeStickPlugin plugin;
    private final Map<UUID, Long> tradeCooldownEndTimeMap = new ConcurrentHashMap<>();
    private volatile boolean onEntityDamageByEntityEvent = false;

    PlayerListener(BoxTradeStickPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        tradeCooldownEndTimeMap.remove(event.getPlayer().getUniqueId());
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

        if (!TradeProcessor.canTradeByStick(villager)) {
            return;
        }

        long cooldownTime = calcCooldownTime(player);
        if (cooldownTime > 0) {
            player.sendActionBar(Translatables.HIT_TRADING_COOLDOWN.apply(cooldownTime));
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

        int succeededCount = TradeProcessor.processSelectedOffersForMaxUses(player, villager);
        if (succeededCount > 0) {
            long cooldownEndTime = calcCooldownEndTime(succeededCount);
            tradeCooldownEndTimeMap.put(player.getUniqueId(), cooldownEndTime);
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

        if (TradeProcessor.canTradeByStick(abstractVillager)) {
            player.openInventory(new MerchantRecipesGUI(player, abstractVillager).getInventory());
        }
    }

    @EventHandler
    public void onEntityTeleport(EntityTeleportEvent event) {
        if (event.getEntity() instanceof Merchant merchant) {
            HumanEntity trader = merchant.getTrader();
            if (trader != null && MerchantRecipesGUI.isGUI(trader.getOpenInventory().getTopInventory())) {
                trader.closeInventory();
            }
        }
    }

    @EventHandler
    public void onVillagerDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof AbstractVillager villager)) {
            return;
        }

        for (Player p : plugin.getServer().getOnlinePlayers()) {
            MerchantRecipesGUI gui = MerchantRecipesGUI.fromTopInventory(p.getOpenInventory().getTopInventory());
            if (gui != null && villager.equals(gui.getVillager())) {
                p.closeInventory();
            }
        }
    }

    @EventHandler
    public void onEntityTeleport(EntityPortalEnterEvent event) {
        if (event.getEntity() instanceof Merchant merchant) {
            HumanEntity trader = merchant.getTrader();
            if (trader != null && MerchantRecipesGUI.isGUI(trader.getOpenInventory().getTopInventory())) {
                trader.closeInventory();
            }
        }
    }

    @EventHandler
    public void onEntityMove(EntityMoveEvent event) {
        if (event.getEntity() instanceof Merchant merchant) {
            HumanEntity trader = merchant.getTrader();
            if (trader != null && MerchantRecipesGUI.isGUI(trader.getOpenInventory().getTopInventory())
                    && trader.getLocation().distanceSquared(event.getEntity().getLocation()) > 100) {
                trader.closeInventory();
            }
        }
    }

    @EventHandler
    public void onEntityMove(VehicleMoveEvent event) {
        for (Entity passenger : event.getVehicle().getPassengers()) {
            if (passenger instanceof Merchant merchant) {
                HumanEntity trader = merchant.getTrader();
                if (trader != null && MerchantRecipesGUI.isGUI(trader.getOpenInventory().getTopInventory())
                        && trader.getLocation().distanceSquared(passenger.getLocation()) > 100) {
                    trader.closeInventory();
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getClickedInventory() == null) {
            return;
        }
        MerchantRecipesGUI gui = MerchantRecipesGUI.fromTopInventory(event.getView().getTopInventory());
        if (gui != null) {
            event.setCancelled(true);
            gui.onClick(event.getSlot());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClose(InventoryCloseEvent event) {
        MerchantRecipesGUI gui = MerchantRecipesGUI.fromTopInventory(event.getView().getTopInventory());

        if (gui != null) {
            gui.onClose();
        }
    }

    private long calcCooldownEndTime(int tradeCount) {
        return System.currentTimeMillis() + TRADE_COOLDOWN_TIME + TRADE_COOLDOWN_TIME_AFTER_THE_2ND * (tradeCount - 1);
    }

    private long calcCooldownTime(Player player) {
        long cooldownTime = tradeCooldownEndTimeMap.getOrDefault(player.getUniqueId(), 0L);
        return cooldownTime - System.currentTimeMillis();
    }

    private boolean isTrading(Merchant merchant) {
        HumanEntity trader = merchant.getTrader();

        if (trader == null) {
            return false;
        }

        Inventory inv = trader.getOpenInventory().getTopInventory();
        MerchantRecipesGUI gui = MerchantRecipesGUI.fromTopInventory(inv);

        if (gui != null) {
            return merchant.equals(gui.getVillager());
        } else if (inv instanceof MerchantInventory merchantInventory) {
            return merchantInventory.getMerchant() instanceof AbstractVillager villager &&
                    Bukkit.isOwnedByCurrentRegion(villager) &&
                    merchant.equals(villager);
        } else {
            return false;
        }
    }
}
