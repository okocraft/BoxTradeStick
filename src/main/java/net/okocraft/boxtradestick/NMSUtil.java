package net.okocraft.boxtradestick;

import io.papermc.paper.event.player.PlayerPurchaseEvent;
import net.minecraft.world.item.trading.MerchantOffer;
import org.bukkit.Statistic;
import org.bukkit.craftbukkit.v1_19_R1.entity.CraftAbstractVillager;
import org.bukkit.craftbukkit.v1_19_R1.inventory.CraftMerchant;
import org.bukkit.craftbukkit.v1_19_R1.inventory.CraftMerchantRecipe;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.Merchant;
import org.bukkit.inventory.MerchantRecipe;

public class NMSUtil {

    public static void processTrade(Player player, Merchant merchant, MerchantRecipe merchantOffer,
                                    PlayerPurchaseEvent event) {
        processTrade(merchant, merchantOffer, event);
        player.incrementStatistic(Statistic.TRADED_WITH_VILLAGER);
        if (merchant instanceof Villager villager) {
            villager.setVillagerExperience(villager.getVillagerExperience() + merchantOffer.getVillagerExperience());
        }
    }

    private static void processTrade(Merchant merchant, MerchantRecipe merchantOffer, PlayerPurchaseEvent event) {
        net.minecraft.world.item.trading.Merchant minecraftMerchant;
        MerchantOffer minecraftMerchantOffer = CraftMerchantRecipe.fromBukkit(merchantOffer).toMinecraft();
        if (merchant instanceof CraftAbstractVillager villager) {
            minecraftMerchant = villager.getHandle();
        } else if (merchant instanceof CraftMerchant craftMerchant) {
            minecraftMerchant = craftMerchant.getMerchant();
        } else {
            return;
        }

        minecraftMerchant.processTrade(minecraftMerchantOffer, event);
    }
}
