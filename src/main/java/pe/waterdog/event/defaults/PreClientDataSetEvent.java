package pe.waterdog.event.defaults;

import com.nukkitx.protocol.bedrock.BedrockServerSession;
import net.minidev.json.JSONObject;
import pe.waterdog.event.Event;

/**
 * Called right when we decoded the player's LoginPacket data in the handshake(HandshakeUpstreamHandler).
 * Can be used to modify or filter (for) certain data, for example skin data.
 */
public class PreClientDataSetEvent extends Event {

    private final JSONObject clientData;

    private final JSONObject extraData;

    private final BedrockServerSession playerSession;

    public PreClientDataSetEvent(JSONObject clientData, JSONObject extraData, BedrockServerSession playerSession) {
        this.clientData = clientData;
        this.extraData = extraData;
        this.playerSession = playerSession;
    }

    public JSONObject getClientData() {
        return clientData;
    }

    public BedrockServerSession getPlayerSession() {
        return playerSession;
    }

    public JSONObject getExtraData() {
        return extraData;
    }
}
