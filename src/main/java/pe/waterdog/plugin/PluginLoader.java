package pe.waterdog.plugin;

import pe.waterdog.ProxyServer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

public class PluginLoader {


    public static void loadPluginsIn(Path folderPath, PluginManager pluginManager, boolean directStartup) {
        try (Stream<Path> pluginPaths = Files.walk(folderPath)) {
            pluginPaths
                    .filter(Files::isRegularFile)
                    .filter(PluginLoader::isJarFile)
                    .forEach(jarPath -> {
                        pluginManager.loadPlugin(jarPath, directStartup);
                    });
        } catch (IOException e) {
            ProxyServer.getInstance().getLogger().error("Error while filtering plugin files", e);
        }
    }

    public static void loadPluginsIn(Path folderPath, PluginManager pluginManager) {
        loadPluginsIn(folderPath, pluginManager, false);
    }


    public static boolean isJarFile(Path file) {
        return file.getFileName().toString().endsWith(".jar");
    }
}
