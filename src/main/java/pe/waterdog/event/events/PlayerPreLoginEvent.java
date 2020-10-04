package pe.waterdog.event.events;

import pe.waterdog.event.CancellableEvent;
import pe.waterdog.network.session.LoginData;

public class PlayerPreLoginEvent extends CancellableEvent {

    private LoginData loginData;
    private String cancelReason = "Login was cancelled";

    public PlayerPreLoginEvent(LoginData loginData) {
        this.loginData = loginData;
    }

    public LoginData getLoginData() {
        return loginData;
    }

    public String getCancelReason() {
        return cancelReason;
    }

    public void setCancelReason(String cancelReason) {
        this.cancelReason = cancelReason;
    }
}
