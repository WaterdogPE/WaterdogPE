package pe.waterdog.command.defaults;

import pe.waterdog.ProxyServer;
import pe.waterdog.command.Command;
import pe.waterdog.command.CommandSender;
import pe.waterdog.command.CommandSettings;

public class EndCommand extends Command {

    public EndCommand() {
        super("end", CommandSettings.builder()
                .setDescription("waterdog.command.end.description")
                .setPermission("waterdog.command.end.permission")
                .setUsageMessage("waterdog.command.end.usage").build());
    }

    @Override
    public boolean onExecute(CommandSender sender, String alias, String[] args) {
        sender.sendMessage("§aShutting down the proxy instance..");
        ProxyServer.getInstance().shutdown();
        return true;
    }
}
