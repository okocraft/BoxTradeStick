package net.okocraft.boxtradestick;

import io.papermc.paper.event.player.PlayerPurchaseEvent;
import net.minecraft.stats.Stats;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import org.bukkit.craftbukkit.v1_19_R1.entity.CraftAbstractVillager;
import org.bukkit.craftbukkit.v1_19_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_19_R1.inventory.CraftMerchant;
import org.bukkit.craftbukkit.v1_19_R1.inventory.CraftMerchantRecipe;
import org.bukkit.entity.Player;
import org.bukkit.inventory.MerchantRecipe;

public class NMSUtil {

    public static void processTrade(Player player, org.bukkit.inventory.Merchant merchant, MerchantRecipe merchantOffer,
                                    PlayerPurchaseEvent event) {
        var minecraftMerchant = toMinecraftMerchant(merchant);
        var minecraftMerchantOffer = toMinecraftMerchantOffer(merchantOffer);
        if (minecraftMerchant == null || minecraftMerchantOffer == null) {
            return;
        }
        minecraftMerchant.processTrade(minecraftMerchantOffer, event);
        if (player instanceof CraftPlayer craftPlayer) {
            craftPlayer.getHandle().awardStat(Stats.TRADED_WITH_VILLAGER);
        }
        minecraftMerchant.overrideXp(minecraftMerchant.getVillagerXp() + minecraftMerchantOffer.getXp());
    }

    private static Merchant toMinecraftMerchant(org.bukkit.inventory.Merchant merchant) {
        if (merchant instanceof CraftAbstractVillager) {
            return ((CraftAbstractVillager) merchant).getHandle();
        } else if (merchant instanceof CraftMerchant) {
            return ((CraftMerchant) merchant).getMerchant();
        } else {
            return null;
        }
    }

    private static MerchantOffer toMinecraftMerchantOffer(MerchantRecipe merchantOffer) {
        return CraftMerchantRecipe.fromBukkit(merchantOffer).toMinecraft();
    }
}
