package net.okocraft.boxtradestick;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import dev.siroshun.mcmsgdef.directory.DirectorySource;
import dev.siroshun.mcmsgdef.directory.MessageProcessors;
import dev.siroshun.mcmsgdef.file.PropertiesFile;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.minimessage.translation.MiniMessageTranslationStore;
import net.kyori.adventure.translation.GlobalTranslator;
import net.okocraft.box.api.BoxAPI;
import net.okocraft.box.feature.stick.StickFeature;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class BoxTradeStickPlugin extends JavaPlugin {

    private MiniMessageTranslationStore translationStore;

    @Override
    public void onLoad() {
        try {
            this.loadMessages();
        } catch (IOException e) {
            this.getSLF4JLogger().error("Could not load languages.", e);
        }
    }

    @Override
    public void onEnable() {
        if (!BoxAPI.isLoaded()) {
            this.getSLF4JLogger().error("Box is not loaded. All features of BoxTradeStick will not be working.");
            return;
        }

        BoxAPI.api().getFeatureProvider().getFeature(StickFeature.class)
            .map(StickFeature::getBoxStickItem)
            .map(PlayerListener::new)
            .ifPresentOrElse(
                listener -> this.getServer().getPluginManager().registerEvents(listener, this),
                () -> this.getSLF4JLogger().error("Failed to get the Box Stick from Box.")
            );
    }

    @Override
    public void onDisable() {
        this.getServer().getOnlinePlayers().forEach(player -> {
            if (MerchantRecipesGUI.fromInventory(player.getOpenInventory().getTopInventory()) != null) {
                player.closeInventory();
            }
        });
    }

    private void loadMessages() throws IOException {
        if (this.translationStore != null) {
            GlobalTranslator.translator().removeSource(this.translationStore);
        }

        this.translationStore = DirectorySource.propertiesFiles(this.getDataFolder().toPath().resolve("languages"))
            .defaultLocale(Locale.ENGLISH, Locale.JAPANESE)
            .messageProcessor(MessageProcessors.appendMissingMessagesToPropertiesFile(this::loadDefaultMessageMap))
            .loadAsMiniMessageTranslationStore(Key.key("boxtradestick", "messages"));
        GlobalTranslator.translator().addSource(this.translationStore);
    }

    private @Nullable Map<String, String> loadDefaultMessageMap(@NotNull Locale locale) throws IOException {
        if (locale.equals(Locale.ENGLISH)) {
            return Languages.defaultMessages();
        } else {
            try (var input = this.getResource(locale + ".properties")) {
                return input != null ? PropertiesFile.load(input) : null;
            }
        }
    }
}
