package com.munot.hospital.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(String timezone, String corsAllowedOrigins, String uploadDir, boolean seedDemo,
                            Jwt jwt, Messaging messaging, Reminder reminder, Admin admin, Translate translate) {
    public record Translate(String provider, String url, String apiKey) {}
    public record Admin(String email, String password, String name) {}
    public record Jwt(String secret, long expirationMinutes) {}
    public record Messaging(String mailFrom, String whatsappApiUrl, String whatsappApiToken, String smsApiUrl, String smsApiToken) {}
    public record Reminder(int dayBeforeHour, int sameDayLeadMinutes, long pollMs) {}
}
