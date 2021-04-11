package dev.waterdog.command.defaults;

import dev.waterdog.ProxyServer;
import dev.waterdog.command.Command;
import dev.waterdog.command.CommandSender;
import dev.waterdog.command.CommandSettings;
import dev.waterdog.plugin.Plugin;

import java.util.ArrayList;

public class PluginsCommand extends Command {

    public PluginsCommand() {
        super("wdplugins", CommandSettings.builder()
                .setDescription("waterdog.command.plugins.description")
                .setUsageMessage("waterdog.command.plugins.usage")
                .setPermission("waterdog.command.plugins.permission")
                .build());
    }


    @Override
    public boolean onExecute(CommandSender sender, String alias, String[] args) {

        ArrayList<String> PluginsListArr = new ArrayList<>();
        for (Plugin p : ProxyServer.getInstance().getPluginManager().getPlugins()) {
            PluginsListArr.add(p.getName());
        }
        String pluginsformat = PluginsListArr.toString().replace("[", "").replace("]", "");
        sender.sendMessage("§aWaterdogPE Plugins: §e" + pluginsformat);
        return true;
    }


}
