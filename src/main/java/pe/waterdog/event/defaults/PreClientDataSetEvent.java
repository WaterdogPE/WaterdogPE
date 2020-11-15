package pe.waterdog.event.defaults;

import com.google.gson.JsonObject;
import com.nukkitx.protocol.bedrock.BedrockServerSession;
import net.minidev.json.JSONObject;
import pe.waterdog.event.Event;

/**
 * Called right when we decoded the player's LoginPacket data in the handshake(HandshakeUpstreamHandler).
 * Can be used to modify or filter (for) certain data, for example skin data.
 */
public class PreClientDataSetEvent extends Event {

    private final BedrockServerSession playerSession;
    private final JsonObject clientData;
    private final JsonObject extraData;

    public PreClientDataSetEvent(JsonObject clientData, JsonObject extraData, BedrockServerSession playerSession) {
        this.clientData = clientData;
        this.extraData = extraData;
        this.playerSession = playerSession;
    }

    public BedrockServerSession getPlayerSession() {
        return this.playerSession;
    }

    public JsonObject getClientData() {
        return this.clientData;
    }

    public JsonObject getExtraData() {
        return this.extraData;
    }
}
