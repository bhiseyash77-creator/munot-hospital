package com.munot.hospital.notification;

import com.munot.hospital.config.AppProperties;
import com.munot.hospital.entity.Channel;
import com.munot.hospital.util.Phones;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import java.util.Map;

/** Generic JSON SMS gateway sender (SMS_API_URL / SMS_API_TOKEN). Adapt the body to your SMS provider. */
@Component
public class SmsSender implements ChannelSender {
    private final AppProperties props;
    private final RestClient http = RestClient.create();

    public SmsSender(AppProperties props) { this.props = props; }

    @Override public Channel channel() { return Channel.SMS; }

    @Override
    public boolean configured() {
        var n = props.messaging();
        return n.smsApiUrl() != null && !n.smsApiUrl().isBlank() && n.smsApiToken() != null && !n.smsApiToken().isBlank();
    }

    @Override
    public void send(String to, String title, String message) {
        http.post().uri(props.messaging().smsApiUrl())
                .header("Authorization", "Bearer " + props.messaging().smsApiToken())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("to", Phones.international(to), "message", message))
                .retrieve().toBodilessEntity();
    }
}
