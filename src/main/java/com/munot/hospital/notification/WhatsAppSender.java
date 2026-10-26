package com.munot.hospital.notification;

import com.munot.hospital.config.AppProperties;
import com.munot.hospital.entity.Channel;
import com.munot.hospital.util.Phones;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import java.util.Map;

/**
 * WhatsApp Business Cloud API style sender. Endpoint and token come from WHATSAPP_API_URL / WHATSAPP_API_TOKEN.
 * NOTE: business-initiated WhatsApp messages normally require pre-approved templates; adapt buildBody() to your provider.
 */
@Component
public class WhatsAppSender implements ChannelSender {
    private final AppProperties props;
    private final RestClient http = RestClient.create();

    public WhatsAppSender(AppProperties props) { this.props = props; }

    @Override public Channel channel() { return Channel.WHATSAPP; }

    @Override
    public boolean configured() {
        var n = props.messaging();
        return n.whatsappApiUrl() != null && !n.whatsappApiUrl().isBlank() && n.whatsappApiToken() != null && !n.whatsappApiToken().isBlank();
    }

    Map<String, Object> buildBody(String to, String message) {
        return Map.of("messaging_product", "whatsapp", "to", Phones.international(to), "type", "text", "text", Map.of("body", message));
    }

    @Override
    public void send(String to, String title, String message) {
        http.post().uri(props.messaging().whatsappApiUrl())
                .header("Authorization", "Bearer " + props.messaging().whatsappApiToken())
                .contentType(MediaType.APPLICATION_JSON)
                .body(buildBody(to, message))
                .retrieve().toBodilessEntity();
    }
}
