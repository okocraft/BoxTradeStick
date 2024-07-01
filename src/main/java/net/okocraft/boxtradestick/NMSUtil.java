package net.okocraft.boxtradestick;

import io.papermc.paper.event.player.PlayerPurchaseEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.trading.MerchantOffer;
import org.bukkit.Statistic;
import org.bukkit.craftbukkit.entity.CraftAbstractVillager;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.entity.CraftVillager;
import org.bukkit.craftbukkit.entity.CraftWanderingTrader;
import org.bukkit.craftbukkit.inventory.CraftMerchant;
import org.bukkit.craftbukkit.inventory.CraftMerchantRecipe;
import org.bukkit.entity.AbstractVillager;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.entity.WanderingTrader;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Merchant;
import org.bukkit.inventory.MerchantRecipe;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NMSUtil {

    public static void processTrade(Player player, AbstractVillager abstractVillager, MerchantRecipe merchantOffer,
                                    PlayerPurchaseEvent event) {
        processTrade(abstractVillager, merchantOffer, event);
        player.incrementStatistic(Statistic.TRADED_WITH_VILLAGER);

        if (abstractVillager instanceof Villager villager) {
            villager.setVillagerExperience(villager.getVillagerExperience() + merchantOffer.getVillagerExperience());
        }
    }

    public static @Nullable AbstractVillager getVillagerFromMerchant(@NotNull Merchant merchant) {
        if (merchant instanceof AbstractVillager abstractVillager) {
            return abstractVillager;
        } else if (merchant instanceof CraftMerchant craftMerchant &&
                   craftMerchant.getMerchant() instanceof net.minecraft.world.entity.npc.AbstractVillager abstractVillager &&
                   abstractVillager.getBukkitEntity() instanceof AbstractVillager bukkitEntity) {
            return bukkitEntity;
        } else {
            return null;
        }
    }

    public static void startTrading(Player player, AbstractVillager abstractVillager) {
        if (!(player instanceof CraftPlayer craftPlayer)) {
            return;
        }

        if (abstractVillager instanceof CraftVillager villager) {
            updateSpecialPrices(craftPlayer, villager);
            villager.getHandle().setTradingPlayer(craftPlayer.getHandle());
        } else if (abstractVillager instanceof CraftWanderingTrader wanderingTrader) {
            wanderingTrader.getHandle().setTradingPlayer(craftPlayer.getHandle());
        }
    }

    public static boolean simulateMobInteract(Player player, AbstractVillager abstractVillager, EquipmentSlot hand) {
        if (abstractVillager.isDead() || abstractVillager.isTrading() || abstractVillager.isSleeping()) {
            return false;
        }

        if (abstractVillager instanceof Villager villager) {
            return simulateMobInteract(player, villager, hand);
        } else if (abstractVillager instanceof WanderingTrader wanderingTrader) {
            return simulateMobInteract(player, wanderingTrader, hand);
        } else {
            return false; // unsupported villager type
        }
    }

    // See: net.minecraft.world.entity.npc.Villager#mobInteract
    private static boolean simulateMobInteract(Player player, Villager villager, EquipmentSlot hand) {
        if (!villager.isAdult()) {
            setUnhappy(villager);
            return false;
        } else {
            boolean noRecipe = villager.getRecipeCount() == 0;

            if (hand == EquipmentSlot.HAND) {
                if (noRecipe) {
                    setUnhappy(villager);
                }

                player.incrementStatistic(Statistic.TALKED_TO_VILLAGER);
            }

            return !noRecipe;
        }
    }

    // See: net.minecraft.world.entity.npc.WanderingTrader#mobInteract
    private static boolean simulateMobInteract(Player player, WanderingTrader wanderingTrader, EquipmentSlot hand) {
        if (hand == EquipmentSlot.HAND) {
            player.incrementStatistic(Statistic.TALKED_TO_VILLAGER);
        }

        return wanderingTrader.getRecipeCount() != 0;
    }

    // See: net.minecraft.world.entity.npc.Villager#stopTrading
    public static void stopTrading(AbstractVillager villager) {
        if (villager instanceof CraftAbstractVillager craftAbstractVillager) {
            craftAbstractVillager.getHandle().setTradingPlayer(null);

            if (villager instanceof CraftVillager craftVillager) {
                for (var merchantOffer : craftVillager.getHandle().getOffers()) {
                    merchantOffer.resetSpecialPriceDiff();
                }
            }
        }
    }

    private static void processTrade(AbstractVillager villager, MerchantRecipe merchantOffer, PlayerPurchaseEvent event) {
        if (villager instanceof CraftAbstractVillager craftAbstractVillager) {
            MerchantOffer minecraftMerchantOffer = CraftMerchantRecipe.fromBukkit(merchantOffer).toMinecraft();
            craftAbstractVillager.getHandle().processTrade(minecraftMerchantOffer, event);
        }
    }

    // net.minecraft.world.entity.npc.Villager#updateSpecialPrices
    private static void updateSpecialPrices(CraftPlayer player, CraftVillager villager) {
        var playerHandle = player.getHandle();
        var villagerHandle = villager.getHandle();

        int playerReputation = villagerHandle.getPlayerReputation(playerHandle);

        if (playerReputation != 0) {
            for (MerchantOffer merchantrecipe : villagerHandle.getOffers()) {
                if (!merchantrecipe.ignoreDiscounts) {
                    merchantrecipe.addToSpecialPriceDiff(-Mth.floor((float) playerReputation * merchantrecipe.getPriceMultiplier()));
                }
            }
        }

        MobEffectInstance mobeffect = playerHandle.getEffect(MobEffects.HERO_OF_THE_VILLAGE);

        if (mobeffect == null) {
            return;
        }

        int amplifier = mobeffect.getAmplifier();

        for (MerchantOffer merchantrecipe1 : villagerHandle.getOffers()) {
            if (merchantrecipe1.ignoreDiscounts) continue; // Paper
            double d = 0.3D + 0.0625D * (double) amplifier;
            int i = (int) Math.floor(d * (double) merchantrecipe1.getBaseCostA().getCount());

            merchantrecipe1.addToSpecialPriceDiff(-Math.max(i, 1));
        }
    }

    private static void setUnhappy(Villager villager) {
        ((CraftVillager) villager).getHandle().setUnhappy();
    }
}
