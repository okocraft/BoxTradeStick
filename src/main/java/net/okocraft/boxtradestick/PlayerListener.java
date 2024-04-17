package net.okocraft.boxtradestick;

import java.util.EnumSet;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import com.github.siroshun09.messages.minimessage.localization.MiniMessageLocalization;
import net.okocraft.box.api.BoxAPI;
import net.okocraft.box.feature.stick.item.BoxStickItem;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.AbstractVillager;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
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
import org.bukkit.inventory.MerchantInventory;
import org.jetbrains.annotations.NotNull;

public class PlayerListener implements Listener {

    private static final long TRADE_COOLDOWN_TIME = 1000L;
    private static final long TRADE_COOLDOWN_TIME_AFTER_THE_2ND = 500L;
    private static final EnumSet<GameMode> IGNORING_GAME_MODES = EnumSet.of(GameMode.CREATIVE, GameMode.SPECTATOR);

    private final BoxStickItem boxStickItem;
    private final MiniMessageLocalization localization;

    private final Map<UUID, Long> tradeCooldownEndTimeMap = new ConcurrentHashMap<>();
    private final ThreadLocal<PlayerInteractEntityEvent> calledPlayerInteractEntityEvent = new ThreadLocal<>();

    public PlayerListener(BoxStickItem boxStickItem, MiniMessageLocalization localization) {
        this.boxStickItem = boxStickItem;
        this.localization = localization;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        tradeCooldownEndTimeMap.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onPlayerAttackVillager(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player) || !(event.getEntity() instanceof AbstractVillager villager)) {
            return;
        }

        if (!this.hasBoxStick(player)) {
            return;
        }

        event.setCancelled(true);

        if (shouldBlockEventProcess(player) || !TradeProcessor.canTradeByStick(villager)) {
            return;
        }

        var messageSource = this.localization.findSource(player);

        long cooldownTime = calcCooldownTime(player);
        if (cooldownTime > 0) {
            player.sendActionBar(Languages.HIT_TRADING_COOLDOWN.apply(cooldownTime).create(messageSource));
            return;
        }

        var playerInteractEvent = new PlayerInteractEntityEvent(player, villager);
        this.calledPlayerInteractEntityEvent.set(playerInteractEvent);
        if (!playerInteractEvent.callEvent()) {
            return;
        }
        this.calledPlayerInteractEntityEvent.remove();

        checkCurrentTrader(villager);

        if (!NMSUtil.simulateMobInteract(player, villager, EquipmentSlot.HAND)) {
            return;
        }

        NMSUtil.startTrading(player, villager);

        int succeededCount = TradeProcessor.processSelectedOffersForMaxUses(player, messageSource, villager);
        if (succeededCount > 0) {
            long cooldownEndTime = calcCooldownEndTime(succeededCount);
            tradeCooldownEndTimeMap.put(player.getUniqueId(), cooldownEndTime);
        }

        NMSUtil.stopTrading(villager);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (this.calledPlayerInteractEntityEvent.get() == event || !(event.getRightClicked() instanceof AbstractVillager villager)) {
            return;
        }

        Player player = event.getPlayer();
        if (TradeProcessor.canTradeByStick(villager) && this.hasBoxStick(player) && !shouldBlockEventProcess(player)) {
            event.setCancelled(true);
            checkCurrentTrader(villager);

            if (NMSUtil.simulateMobInteract(player, villager, event.getHand())) {
                NMSUtil.startTrading(player, villager);

                MerchantRecipesGUI gui = new MerchantRecipesGUI(player, this.localization.findSource(player), villager);
                player.openInventory(gui.getInventory());
                gui.scheduleWatchingTask();
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

    private boolean hasBoxStick(@NotNull Player player) {
        return this.boxStickItem.check(player.getInventory().getItemInMainHand()) ||
               this.boxStickItem.check(player.getInventory().getItemInOffHand());
    }

    private static void checkCurrentTrader(AbstractVillager villager) {
        HumanEntity trader = villager.getTrader();

        if (trader == null) {
            return;
        }

        Inventory inv = trader.getOpenInventory().getTopInventory();
        MerchantRecipesGUI gui = MerchantRecipesGUI.fromTopInventory(inv);

        if (gui != null && villager.equals(gui.getVillager())) {
            return;
        }

        if (inv instanceof MerchantInventory merchantInventory) {
            var merchant = NMSUtil.getVillagerFromMerchant(merchantInventory.getMerchant());

            if (merchant != null && Bukkit.isOwnedByCurrentRegion(merchant) && merchant.equals(villager)) {
                return;
            }
        }

        NMSUtil.stopTrading(villager); // cleanup current trader
    }

    private static boolean shouldBlockEventProcess(@NotNull Player player) {
        return IGNORING_GAME_MODES.contains(player.getGameMode()) || !BoxAPI.api().canUseBox(player) || !player.hasPermission("boxtradestick.trade");
    }
}
