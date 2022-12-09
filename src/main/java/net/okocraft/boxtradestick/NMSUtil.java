package net.okocraft.boxtradestick;

import io.papermc.paper.event.player.PlayerPurchaseEvent;
import java.util.Objects;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.trading.MerchantOffer;
import org.bukkit.Statistic;
import org.bukkit.craftbukkit.v1_19_R2.entity.CraftAbstractVillager;
import org.bukkit.craftbukkit.v1_19_R2.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_19_R2.entity.CraftVillager;
import org.bukkit.craftbukkit.v1_19_R2.inventory.CraftMerchant;
import org.bukkit.craftbukkit.v1_19_R2.inventory.CraftMerchantRecipe;
import org.bukkit.entity.AbstractVillager;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Merchant;
import org.bukkit.inventory.MerchantRecipe;
import org.jetbrains.annotations.Nullable;

public class NMSUtil {

    public static void processTrade(Player player, Merchant merchant, MerchantRecipe merchantOffer,
                                    PlayerPurchaseEvent event) {
        processTrade(merchant, merchantOffer, event);
        player.incrementStatistic(Statistic.TRADED_WITH_VILLAGER);
        if (merchant instanceof Villager villager) {
            villager.setVillagerExperience(villager.getVillagerExperience() + merchantOffer.getVillagerExperience());
        }
    }

    public static void startTrading(Merchant merchant, Player player) {
        if (merchant instanceof Villager villager) {
            updateSpecialPrices(player, villager);
        }
        net.minecraft.world.item.trading.Merchant merchantHandle = getMerchantHandle(merchant);
        if (merchantHandle == null) {
            return;
        }
        merchantHandle.setTradingPlayer(((CraftPlayer) player).getHandle());
    }

    public static boolean simulateMobInteract(Player player, AbstractVillager abstractVillager, EquipmentSlot hand) {
        if (abstractVillager.isDead() || abstractVillager.isTrading() || abstractVillager.isSleeping()) {
            return false;
        }

        if (!(abstractVillager instanceof Villager villager)) {
            return true;
        }

        if (!villager.isAdult()) {
            setUnhappy(villager);
        } else {

            boolean noRecipe = villager.getRecipeCount() == 0;

            if (hand == EquipmentSlot.HAND) {
                if (noRecipe) {
                    setUnhappy(villager);
                }

                player.incrementStatistic(Statistic.TALKED_TO_VILLAGER);
            }

            if (!noRecipe) {
                startTrading(villager, player);
                return true;
            }
        }
        return false;
    }

    public static void stopTrading(Merchant merchant) {
        net.minecraft.world.item.trading.Merchant merchantHandle = getMerchantHandle(merchant);
        if (merchantHandle != null) {
            merchantHandle.setTradingPlayer(null);
        }
    }

    private static void processTrade(Merchant merchant, MerchantRecipe merchantOffer, PlayerPurchaseEvent event) {
        net.minecraft.world.item.trading.Merchant minecraftMerchant = getMerchantHandle(merchant);
        if (minecraftMerchant == null) {
            return;
        }
        MerchantOffer minecraftMerchantOffer = CraftMerchantRecipe.fromBukkit(merchantOffer).toMinecraft();

        minecraftMerchant.processTrade(minecraftMerchantOffer, event);
    }

    @Nullable
    private static net.minecraft.world.item.trading.Merchant getMerchantHandle(Merchant merchant) {
        if (merchant instanceof CraftAbstractVillager villager) {
            return villager.getHandle();
        } else if (merchant instanceof CraftMerchant craftMerchant) {
            return craftMerchant.getMerchant();
        } else {
            return null;
        }
    }

    private static void updateSpecialPrices(Player player, Villager villager) {
        var villagerHandle = ((CraftVillager) villager).getHandle();
        var playerHandle = ((CraftPlayer) player).getHandle();

        int i = villagerHandle.getPlayerReputation(playerHandle);

        if (i != 0) {
            for (MerchantOffer merchantrecipe : villagerHandle.getOffers()) {
                if (!merchantrecipe.ignoreDiscounts) {
                    merchantrecipe.addToSpecialPriceDiff(-Mth.floor((float) i * merchantrecipe.getPriceMultiplier()));
                }
            }
        }

        if (playerHandle.hasEffect(MobEffects.HERO_OF_THE_VILLAGE)) {
            MobEffectInstance mobeffect = playerHandle.getEffect(MobEffects.HERO_OF_THE_VILLAGE);
            int j = Objects.requireNonNull(mobeffect).getAmplifier();
            for (MerchantOffer merchantrecipe1 : villagerHandle.getOffers()) {
                if (merchantrecipe1.ignoreDiscounts) continue; // Paper
                double d0 = 0.3D + 0.0625D * (double) j;
                int k = (int) Math.floor(d0 * (double) merchantrecipe1.getBaseCostA().getCount());

                merchantrecipe1.addToSpecialPriceDiff(-Math.max(k, 1));
            }
        }
    }

    private static void setUnhappy(Villager villager) {
        ((CraftVillager) villager).getHandle().setUnhappy();
    }
}
