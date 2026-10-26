package com.munot.hospital.notification;

import com.munot.hospital.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Polls for due notifications (follow-up / appointment reminders etc.) and dispatches them. */
@Component
public class ReminderScheduler {
    private static final Logger log = LoggerFactory.getLogger(ReminderScheduler.class);
    private final NotificationService notifications;

    public ReminderScheduler(NotificationService notifications) { this.notifications = notifications; }

    @Scheduled(fixedDelayString = "${app.reminder.poll-ms:60000}", initialDelay = 15000)
    public void run() {
        try {
            int n = notifications.dispatchDue();
            if (n > 0) log.info("Processed {} due notifications", n);
        } catch (Exception e) {
            log.error("Reminder dispatch failed", e);
        }
    }
}
