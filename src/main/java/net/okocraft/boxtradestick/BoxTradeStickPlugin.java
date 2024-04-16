package net.okocraft.boxtradestick;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Locale;
import java.util.jar.JarFile;
import java.util.logging.Level;
import net.kyori.adventure.key.Key;
import com.github.siroshun09.configapi.api.Configuration;
import com.github.siroshun09.configapi.api.util.ResourceUtils;
import com.github.siroshun09.configapi.yaml.YamlConfiguration;
import com.github.siroshun09.translationloader.ConfigurationLoader;
import com.github.siroshun09.translationloader.TranslationLoader;
import com.github.siroshun09.translationloader.directory.TranslationDirectory;
import net.okocraft.box.api.BoxAPI;
import net.okocraft.box.feature.stick.StickFeature;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class BoxTradeStickPlugin extends JavaPlugin {

    private final TranslationDirectory translationDirectory;

    public BoxTradeStickPlugin() {
        Path pluginDirectory = getDataFolder().toPath();

        this.translationDirectory =
                TranslationDirectory.newBuilder()
                        .setDirectory(pluginDirectory.resolve("languages"))
                        .setKey(Key.key("boxtradestick", "languages"))
                        .setDefaultLocale(Locale.ENGLISH)
                        .onDirectoryCreated(this::saveDefaultLanguages)
                        .setVersion(getPluginMeta().getVersion()) // getPluginMeta never returns null
                        .setTranslationLoaderCreator(this::getBundledTranslation)
                        .build();
    }

    private void saveDefaultLanguages(@NotNull Path directory) throws IOException {
        var english = "en.yml";
        ResourceUtils.copyFromClassLoader(this.getClassLoader(), english, directory.resolve(english));

        var japanese = "ja_JP.yml";
        ResourceUtils.copyFromClassLoader(this.getClassLoader(), japanese, directory.resolve(japanese));
    }

    private @Nullable TranslationLoader getBundledTranslation(@NotNull Locale locale) throws IOException {
        var strLocale = locale.toString();

        if (!(strLocale.equals("en") || strLocale.equals("ja_JP"))) {
            return null;
        }

        Configuration source;

        try (var jar = new JarFile(getFile());
             var input = ResourceUtils.getInputStreamFromJar(jar, strLocale + ".yml")) {
            source = YamlConfiguration.loadFromInputStream(input);
        }

        var loader = ConfigurationLoader.create(locale, source);
        loader.load();

        return loader;
    }

    @Override
    public void onLoad() {

        try {
            translationDirectory.load();
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Could not load languages", e);
        }
    }

    @Override
    public void onEnable() {
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
        translationDirectory.unload();
    }

}
