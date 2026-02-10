package dev.waterdog.waterdogpe.command.defaults;

import dev.waterdog.waterdogpe.ProxyServer;
import dev.waterdog.waterdogpe.command.Command;
import dev.waterdog.waterdogpe.command.CommandSender;
import dev.waterdog.waterdogpe.command.CommandSettings;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;

public class StatusCommand extends Command {

    public StatusCommand() {
        super("wdstatus", CommandSettings.builder()
                .setDescription("waterdog.command.status.description")
                .setUsageMessage("waterdog.command.status.usage")
                .setPermission("waterdog.command.status.permission")
                .build());
    }

    @Override
    public boolean onExecute(CommandSender sender, String alias, String[] args) {
        ProxyServer proxy = sender.getProxy();
        StringBuilder sb = new StringBuilder();

        sb.append("§b--- §3WaterdogPE Status §b---\n");

        long uptimeMs = ManagementFactory.getRuntimeMXBean().getUptime();
        sb.append("§3Uptime: §b").append(formatUptime(uptimeMs)).append("\n");

        int onlinePlayers = proxy.getPlayers().size();
        int maxPlayers = proxy.getConfiguration().getMaxPlayerCount();
        sb.append("§3Players: §b").append(onlinePlayers).append(" / ").append(maxPlayers).append("\n");

        sb.append("§3Servers: §b").append(proxy.getServers().size()).append("\n");

        Runtime runtime = Runtime.getRuntime();
        long usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1048576;
        long totalMemory = runtime.totalMemory() / 1048576;
        long maxMemory = runtime.maxMemory() / 1048576;
        long usagePercent = totalMemory > 0 ? (usedMemory * 100 / totalMemory) : 0;
        sb.append("§3Memory: §b").append(usedMemory).append(" MB / ").append(totalMemory).append(" MB (Max: ").append(maxMemory).append(" MB) §e").append(usagePercent).append("%\n");

        sb.append("§3CPU Usage: §b").append(getProcessCpuLoad()).append("\n");

        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        sb.append("§3Threads: §b").append(threadMXBean.getThreadCount()).append("\n");

        sb.append("§3OS: §b").append(System.getProperty("os.name"))
                .append(" ").append(System.getProperty("os.version"))
                .append(" (").append(System.getProperty("os.arch")).append(")\n");

        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        sb.append("§3JVM: §b").append(runtimeMXBean.getVmName())
                .append(" ").append(runtimeMXBean.getVmVersion());

        sender.sendMessage(sb.toString());
        return true;
    }

    private String formatUptime(long uptimeMs) {
        long totalSeconds = uptimeMs / 1000;
        long days = totalSeconds / 86400;
        long hours = (totalSeconds % 86400) / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        StringBuilder sb = new StringBuilder();
        if (days > 0) {
            sb.append(days).append("d ");
        }
        sb.append(hours).append("h ");
        sb.append(minutes).append("m ");
        sb.append(seconds).append("s");
        return sb.toString();
    }

    private String getProcessCpuLoad() {
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
            double cpuLoad = ((com.sun.management.OperatingSystemMXBean) osBean).getProcessCpuLoad();
            if (cpuLoad < 0) {
                return "N/A";
            }
            return String.format("%.1f%%", cpuLoad * 100);
        }
        return "N/A";
    }
}
