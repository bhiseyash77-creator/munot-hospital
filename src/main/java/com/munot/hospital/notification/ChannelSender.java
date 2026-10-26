package com.munot.hospital.notification;

import com.munot.hospital.entity.Channel;

public interface ChannelSender {
    Channel channel();

    /** True when credentials/endpoints are present in the environment. */
    boolean configured();

    void send(String to, String title, String message) throws Exception;
}
