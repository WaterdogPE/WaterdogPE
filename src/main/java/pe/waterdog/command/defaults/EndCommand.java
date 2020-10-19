package pe.waterdog.command.defaults;

import pe.waterdog.ProxyServer;
import pe.waterdog.command.Command;
import pe.waterdog.command.CommandSender;
import pe.waterdog.command.CommandSettings;

public class EndCommand extends Command {

    public EndCommand() {
        super("end", CommandSettings.builder()
                .setDescription("Shut down the proxy instance")
                .setPermission("waterdog.command.end")
                .setUsageMessage("/end").build());
    }

    @Override
    public boolean onExecute(CommandSender sender, String alias, String[] args) {
        sender.sendMessage("§aShutting down the proxy instance..");
        ProxyServer.getInstance().shutdown();
        return true;
    }
}
