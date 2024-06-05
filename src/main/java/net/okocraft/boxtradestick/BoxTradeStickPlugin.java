package net.okocraft.boxtradestick;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import com.github.siroshun09.messages.api.directory.DirectorySource;
import com.github.siroshun09.messages.api.directory.MessageProcessors;
import com.github.siroshun09.messages.api.source.StringMessageMap;
import com.github.siroshun09.messages.api.util.PropertiesFile;
import com.github.siroshun09.messages.minimessage.localization.MiniMessageLocalization;
import com.github.siroshun09.messages.minimessage.source.MiniMessageSource;
import net.okocraft.box.api.BoxAPI;
import net.okocraft.box.feature.stick.StickFeature;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class BoxTradeStickPlugin extends JavaPlugin {

    private MiniMessageLocalization localization;

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
            .map(stick -> new PlayerListener(stick, this.localization))
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
        if (this.localization == null) { // on startup
            this.localization = new MiniMessageLocalization(MiniMessageSource.create(StringMessageMap.create(Languages.defaultMessages())), Languages::getLocaleFrom);
        } else { // on reload
            this.localization.clearSources();
        }

        DirectorySource.propertiesFiles(this.getDataFolder().toPath().resolve("languages"))
            .defaultLocale(Locale.ENGLISH, Locale.JAPANESE)
            .messageProcessor(MessageProcessors.appendMissingMessagesToPropertiesFile(this::loadDefaultMessageMap))
            .load(loaded -> this.localization.addSource(loaded.locale(), MiniMessageSource.create(loaded.messageSource())));
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
