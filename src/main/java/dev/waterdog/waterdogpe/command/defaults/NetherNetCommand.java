/*
 * Copyright 2026 WaterdogTEAM
 * Licensed under the GNU General Public License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.waterdog.waterdogpe.command.defaults;

import org.cloudburstmc.netty.channel.nethernet.signaling.JoinRefusal;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.waterdog.waterdogpe.ProxyServer;
import dev.waterdog.waterdogpe.command.Command;
import dev.waterdog.waterdogpe.command.CommandSender;
import dev.waterdog.waterdogpe.command.CommandSettings;
import dev.waterdog.waterdogpe.network.connection.TransportProfile;
import dev.waterdog.waterdogpe.network.nethernet.NetherNetInterface;
import dev.waterdog.waterdogpe.network.nethernet.NetherNetProperties;
import dev.waterdog.waterdogpe.network.nethernet.NetherNetProvider;
import dev.waterdog.waterdogpe.network.nethernet.ProxyIdentity;
import dev.waterdog.waterdogpe.player.ProxiedPlayer;
import dev.waterdog.waterdogpe.utils.config.proxy.NetherNetSettings;
import io.netty.channel.Channel;
import org.cloudburstmc.netty.signaling.ProviderClient;
import org.cloudburstmc.netty.signaling.ServerStatus;
import org.cloudburstmc.netty.signaling.admission.NativeAdmissionServerChannel;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Console-only view of the NetherNet transport: what is bound, what the provider is told, and the
 * counters behind admission.
 */
public class NetherNetCommand extends Command {

    private static final Gson GSON = new Gson();
    private static final int MAX_STATUS_LENGTH = 4096;

    public NetherNetCommand() {
        super("wdnethernet", CommandSettings.builder()
                .setDescription("waterdog.command.nethernet.description")
                .setUsageMessage("waterdog.command.nethernet.usage")
                .setPermission("waterdog.command.nethernet.permission")
                .build());
    }

    @Override
    public boolean onExecute(CommandSender sender, String alias, String[] args) {
        if (sender.isPlayer()) {
            sender.sendMessage("§cThis command requires the proxy console.");
            return true;
        }

        ProxyServer proxy = sender.getProxy();
        if (!proxy.getNetherNetSettings().enabled()) {
            sender.sendMessage("NetherNet is disabled in the configuration.");
            return true;
        }

        NetherNetInterface nethernet = proxy.getNetherNetInterface();
        if (args.length == 0) {
            this.overview(sender, proxy, nethernet);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "status" -> this.status(sender, nethernet, args);
            case "diagnostics" -> this.diagnostics(sender, nethernet);
            default -> {
                return false;
            }
        }
        return true;
    }

    private void overview(CommandSender sender, ProxyServer proxy, NetherNetInterface nethernet) {
        NetherNetSettings settings = proxy.getNetherNetSettings();
        StringBuilder sb = new StringBuilder("§b--- §3NetherNet §b---\n");
        sb.append("§3Running: ").append(yesNo(nethernet.isRunning())).append('\n');
        sb.append("§3Signaling mode: §b").append(settings.signalingMode().name().toLowerCase()).append('\n');
        sb.append("§3ICE port: §b").append(settings.getUdpPort() > 0 ? "udp/" + settings.getUdpPort() : "ephemeral").append('\n');

        for (NetherNetInterface.SignalingInfo info : nethernet.signalingInfo()) {
            sb.append("§3Signaling: §b").append(info.bind())
                    .append(info.active() ? " §aactive" : " §cinactive")
                    .append("§3, pending joins: §b").append(info.pendingJoins()).append('\n');
        }

        NetherNetProvider provider = nethernet.provider();
        if (settings.signalingMode().nxs()) {
            String host = URI.create(settings.getNxs().getEndpoint()).getHost();
            sb.append("§3Provider: §b").append(host).append(' ')
                    .append(provider != null && provider.isRunning() ? "§aregistered" : "§cnot registered").append('\n');
        }

        sb.append("§3Accepting connections: ").append(refusal(nethernet.acceptsConnections()));
        if (NetherNetProperties.MAX_CONNECTIONS > 0) {
            sb.append(" §3(limit §b").append(NetherNetProperties.MAX_CONNECTIONS).append("§3)");
        }
        sb.append('\n');

        long transported = proxy.getPlayers().values().stream().filter(NetherNetCommand::overNetherNet).count();
        sb.append("§3Players over NetherNet: §b").append(transported).append(" / ").append(proxy.getPlayers().size()).append('\n');

        sb.append("§3Identity: §b").append(ProxyIdentity.domain(proxy));
        String fingerprint = this.identityFingerprint(proxy);
        if (fingerprint != null) {
            sb.append(" §3key §b").append(fingerprint);
        }
        sender.sendMessage(sb.toString());
    }

    /**
     * Inspect or override the status reported to the provider.
     */
    private void status(CommandSender sender, NetherNetInterface nethernet, String[] args) {
        NetherNetProvider provider = this.runningProvider(sender, nethernet);
        if (provider == null) {
            return;
        }

        try {
            if (args.length == 1) {
                sender.sendMessage(GSON.toJson(provider.serverStatus()));
            } else if (args.length == 2 && args[1].equalsIgnoreCase("automatic")) {
                provider.restoreAutomaticServerStatus();
                sender.sendMessage("Automatic provider status restored.");
            } else if (args.length == 3 && args[1].equalsIgnoreCase("set") && args[2].length() <= MAX_STATUS_LENGTH) {
                provider.setServerStatus(parseStatus(args[2]));
                sender.sendMessage("Provider status override queued.");
            } else {
                sender.sendMessage("Usage: wdnethernet status [automatic | set <base64url JSON snapshot>]");
            }
        } catch (RuntimeException invalid) {
            sender.sendMessage("Invalid server status snapshot: " + invalid.getMessage());
        }
    }

    /**
     * A full {@link ServerStatus} as base64url JSON, which keeps the snapshot a single argument.
     */
    static ServerStatus parseStatus(String encoded) {
        String json = new String(Base64.getUrlDecoder().decode(encoded), StandardCharsets.UTF_8);
        ServerStatus status = GSON.fromJson(json, ServerStatus.class);
        if (status == null) {
            throw new IllegalArgumentException("empty snapshot");
        }
        return status;
    }

    /**
     * Admission counters and the provider's own view of this proxy.
     */
    private void diagnostics(CommandSender sender, NetherNetInterface nethernet) {
        JsonObject report = new JsonObject();

        JsonArray signaling = new JsonArray();
        for (NetherNetInterface.SignalingInfo info : nethernet.signalingInfo()) {
            JsonObject entry = new JsonObject();
            entry.addProperty("bind", String.valueOf(info.bind()));
            entry.addProperty("active", info.active());
            entry.addProperty("pendingJoins", info.pendingJoins());
            entry.addProperty("networkId", info.networkId());
            signaling.add(entry);
        }
        report.add("signaling", signaling);

        NetherNetProvider provider = nethernet.provider();
        Channel channel = provider == null ? null : provider.channel();
        if (channel instanceof NativeAdmissionServerChannel nativeChannel && nativeChannel.isActive()) {
            JsonObject entry = new JsonObject();
            entry.addProperty("bind", String.valueOf(nativeChannel.localAddress()));
            entry.addProperty("nativeCreationAttempts", nativeChannel.creationAttempts());
            entry.addProperty("liveNativePeers", nativeChannel.liveNativePeers());
            entry.addProperty("droppedEvents", nativeChannel.droppedEvents());
            entry.add("admission", GSON.toJsonTree(nativeChannel.admissionStats()));
            entry.add("native", GSON.toJsonTree(nativeChannel.nativeStats()));
            report.add("provider", entry);
        }
        sender.sendMessage(GSON.toJson(report));

        ProviderClient client = provider == null ? null : provider.client();
        if (client == null) {
            if (sender.getProxy().getNetherNetSettings().signalingMode().nxs()) {
                sender.sendMessage("Provider is not running.");
            }
            return;
        }
        client.readiness().whenComplete((ready, failure) -> sender.sendMessage(failure == null
                ? "Provider readiness: " + ready
                : "Provider readiness unavailable: " + NetherNetProvider.failureMessage(failure)));
    }

    private NetherNetProvider runningProvider(CommandSender sender, NetherNetInterface nethernet) {
        NetherNetProvider provider = nethernet.provider();
        if (provider == null || !provider.isRunning()) {
            sender.sendMessage(sender.getProxy().getNetherNetSettings().signalingMode().nxs()
                    ? "Provider is not running."
                    : "No provider is configured, the signaling mode is not nxs or hybrid.");
            return null;
        }
        return provider;
    }

    private static boolean overNetherNet(ProxiedPlayer player) {
        return player.getConnection().getPeer().getTransportProfile() == TransportProfile.NETHERNET;
    }

    /**
     * SHA-256 of the public key, so a fleet can check its nodes share one identity.
     */
    private String identityFingerprint(ProxyServer proxy) {
        try {
            byte[] key = ProxyIdentity.identity(proxy).publicKey().getEncoded();
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(key));
        } catch (Exception e) {
            return null;
        }
    }

    private static String yesNo(boolean value) {
        return value ? "§ayes" : "§cno";
    }

    private static String refusal(JoinRefusal refusal) {
        if (refusal == null) {
            return "§ayes";
        }
        return refusal.toString();
    }
}
