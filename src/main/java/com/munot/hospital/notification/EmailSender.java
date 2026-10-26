package com.munot.hospital.notification;

import com.munot.hospital.config.AppProperties;
import com.munot.hospital.entity.Channel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class EmailSender implements ChannelSender {
    private final ObjectProvider<JavaMailSender> mail;
    private final AppProperties props;
    private final Environment env;

    public EmailSender(ObjectProvider<JavaMailSender> mail, AppProperties props, Environment env) {
        this.mail = mail;
        this.props = props;
        this.env = env;
    }

    @Override public Channel channel() { return Channel.EMAIL; }

    @Override
    public boolean configured() {
        String host = env.getProperty("spring.mail.host", "");
        return !host.isBlank() && mail.getIfAvailable() != null && props.messaging().mailFrom() != null && !props.messaging().mailFrom().isBlank();
    }

    @Override
    public void send(String to, String title, String message) {
        SimpleMailMessage m = new SimpleMailMessage();
        m.setFrom(props.messaging().mailFrom());
        m.setTo(to);
        m.setSubject(title);
        m.setText(message);
        mail.getObject().send(m);
    }
}
