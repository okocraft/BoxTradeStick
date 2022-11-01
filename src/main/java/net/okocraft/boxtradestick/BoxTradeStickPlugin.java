package net.okocraft.boxtradestick;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.logging.Level;
import net.kyori.adventure.key.Key;
import net.okocraft.box.lib.configapi.api.Configuration;
import net.okocraft.box.lib.configapi.api.util.ResourceUtils;
import net.okocraft.box.lib.configapi.yaml.YamlConfiguration;
import net.okocraft.box.lib.translationloader.ConfigurationLoader;
import net.okocraft.box.lib.translationloader.TranslationLoader;
import net.okocraft.box.lib.translationloader.directory.TranslationDirectory;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class BoxTradeStickPlugin extends JavaPlugin {

    private final Path jarFile;

    private final TranslationDirectory translationDirectory;

    public BoxTradeStickPlugin() {
        this.jarFile = Paths.get(getClass().getProtectionDomain().getCodeSource().getLocation().getPath());

        Path pluginDirectory = getDataFolder().toPath();

        this.translationDirectory =
                TranslationDirectory.newBuilder()
                        .setDirectory(pluginDirectory.resolve("languages"))
                        .setKey(Key.key("boxtradestick", "languages"))
                        .setDefaultLocale(Locale.ENGLISH)
                        .onDirectoryCreated(this::saveDefaultLanguages)
                        .setVersion(getDescription().getVersion())
                        .setTranslationLoaderCreator(this::getBundledTranslation)
                        .build();
    }

    private void saveDefaultLanguages(@NotNull Path directory) throws IOException {
        var english = "en.yml";
        ResourceUtils.copyFromJarIfNotExists(jarFile, english, directory.resolve(english));

        var japanese = "ja_JP.yml";
        ResourceUtils.copyFromJarIfNotExists(jarFile, japanese, directory.resolve(japanese));
    }

    private @Nullable TranslationLoader getBundledTranslation(@NotNull Locale locale) throws IOException {
        var strLocale = locale.toString();

        if (!(strLocale.equals("en") || strLocale.equals("ja_JP"))) {
            return null;
        }

        Configuration source;

        try (var input = ResourceUtils.getInputStreamFromJar(jarFile, strLocale + ".yml")) {
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
        getServer().getPluginManager().registerEvents(new PlayerListener(), this);
    }

    @Override
    public void onDisable() {
        translationDirectory.unload();
    }

}
