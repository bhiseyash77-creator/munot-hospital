package com.munot.hospital.service;

import com.munot.hospital.common.ApiException;
import com.munot.hospital.config.AppProperties;
import com.munot.hospital.dto.AdminDtos.NotificationView;
import com.munot.hospital.entity.*;
import com.munot.hospital.notification.ChannelSender;
import com.munot.hospital.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class NotificationService {
    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("hh:mm a");
    private static final List<Channel> CHANNELS = List.of(Channel.IN_APP, Channel.EMAIL, Channel.SMS, Channel.WHATSAPP);

    private final NotificationRepository repo;
    private final SettingsService settings;
    private final AppProperties props;
    private final ViewMapper mapper;
    private final Map<Channel, ChannelSender> senders = new EnumMap<>(Channel.class);

    public NotificationService(NotificationRepository repo, SettingsService settings, AppProperties props, ViewMapper mapper,
                               List<ChannelSender> channelSenders) {
        this.repo = repo;
        this.settings = settings;
        this.props = props;
        this.mapper = mapper;
        channelSenders.forEach(s -> senders.put(s.channel(), s));
    }

    private String hospital() { return settings.get("hospital_name", "Dr. Munot's Orthopaedic Care Hospital"); }

    // ---------- scheduling ----------

    /** Confirmation (now) + previous-day and same-day reminders for an appointment or follow-up appointment. */
    @Transactional
    public void scheduleForAppointment(Appointment a) {
        boolean follow = a.getType() == AppointmentType.FOLLOW_UP;
        String doctor = "Dr. " + stripDr(a.getDoctor().getUser().getFullName());
        String when = a.getAppointmentDate().format(DATE) + " at " + a.getStartTime().format(TIME);
        String startKey = a.getAppointmentDate() + "T" + a.getStartTime();
        LocalDateTime now = LocalDateTime.now();

        if (a.getStatus() == AppointmentStatus.BOOKED) { // online request: NOT confirmed yet, no reminders until staff confirm
            createRows(a.getPatient(), a, NotificationType.GENERAL, "REQ:" + a.getId() + ":" + startKey, now, "Appointment request received",
                    "We have received your appointment request with " + doctor + " for " + when + " at " + hospital()
                            + ". It is NOT confirmed yet - you will get a message once the hospital confirms it. Ref: " + a.getAppointmentCode() + ".");
            return;
        }
        createRows(a.getPatient(), a, NotificationType.APPOINTMENT_CONFIRMATION, "CONF:" + a.getId() + ":" + startKey, now,
                follow ? "Follow-up appointment confirmed" : "Appointment confirmed",
                (follow ? "Your follow-up appointment with " : "Your appointment with ") + doctor + " at " + hospital() + " is confirmed for " + when
                        + ". Ref: " + a.getAppointmentCode() + ".");

        LocalDateTime dayBefore = LocalDateTime.of(a.getAppointmentDate().minusDays(1), LocalTime.of(props.reminder().dayBeforeHour(), 0));
        NotificationType rt = follow ? NotificationType.FOLLOWUP_REMINDER : NotificationType.APPOINTMENT_REMINDER;
        if (dayBefore.isAfter(now)) {
            createRows(a.getPatient(), a, rt, "D1:" + a.getId() + ":" + startKey, dayBefore, follow ? "Follow-up reminder" : "Appointment reminder",
                    "Reminder: Your " + (follow ? "follow-up " : "") + "appointment with " + doctor + " is scheduled for tomorrow, "
                            + when + " at " + hospital() + ".");
        }
        LocalDateTime start = LocalDateTime.of(a.getAppointmentDate(), a.getStartTime());
        LocalDateTime sameDay = start.minusMinutes(props.reminder().sameDayLeadMinutes());
        LocalDateTime earliest = LocalDateTime.of(a.getAppointmentDate(), LocalTime.of(7, 0));
        if (sameDay.isBefore(earliest)) sameDay = earliest;
        if (sameDay.isAfter(now) && sameDay.isBefore(start)) {
            createRows(a.getPatient(), a, rt, "D0:" + a.getId() + ":" + startKey, sameDay, follow ? "Follow-up today" : "Appointment today",
                    "Reminder: Your " + (follow ? "follow-up " : "") + "appointment with " + doctor + " is scheduled for today at "
                            + a.getStartTime().format(TIME) + " at " + hospital() + ".");
        }
    }

    /** For follow-ups where the doctor chose a date but no free slot could be booked automatically. */
    @Transactional
    public void scheduleForPendingFollowUp(FollowUp f) {
        String doctor = "Dr. " + stripDr(f.getDoctor().getUser().getFullName());
        String date = f.getFollowUpDate().format(DATE);
        LocalDateTime now = LocalDateTime.now();
        createRows(f.getPatient(), null, NotificationType.FOLLOWUP_REMINDER, "FUC:" + f.getId(), now, "Follow-up advised",
                doctor + " has advised a follow-up visit on " + date + ". Please contact " + hospital() + " to confirm a time.");
        LocalDateTime dayBefore = LocalDateTime.of(f.getFollowUpDate().minusDays(1), LocalTime.of(props.reminder().dayBeforeHour(), 0));
        if (dayBefore.isAfter(now)) createRows(f.getPatient(), null, NotificationType.FOLLOWUP_REMINDER, "FUD1:" + f.getId(), dayBefore,
                "Follow-up reminder", "Reminder: Your follow-up with " + doctor + " is due tomorrow (" + date + ") at " + hospital() + ".");
        LocalDateTime morning = LocalDateTime.of(f.getFollowUpDate(), LocalTime.of(8, 0));
        if (morning.isAfter(now)) createRows(f.getPatient(), null, NotificationType.FOLLOWUP_REMINDER, "FUD0:" + f.getId(), morning,
                "Follow-up today", "Reminder: Your follow-up appointment with " + doctor + " is scheduled for today at " + hospital() + ".");
    }

    @Transactional
    public void notifyCancelled(Appointment a) {
        String doctor = "Dr. " + stripDr(a.getDoctor().getUser().getFullName());
        String when = a.getAppointmentDate().format(DATE) + " at " + a.getStartTime().format(TIME);
        createRows(a.getPatient(), a, NotificationType.GENERAL, "CAN:" + a.getId() + ":" + a.getAppointmentDate() + "T" + a.getStartTime(), LocalDateTime.now(),
                "Appointment cancelled", "Your appointment with " + doctor + " on " + when + " at " + hospital()
                        + " could not be kept and has been cancelled by the hospital. Please book another time or call us. Ref: " + a.getAppointmentCode() + ".");
    }

    @Transactional
    public void notifyPrescriptionAvailable(Prescription rx) {
        createRows(rx.getPatient(), rx.getConsultation().getAppointment(), NotificationType.PRESCRIPTION_AVAILABLE, "RX:" + rx.getId(),
                LocalDateTime.now(), "Prescription available",
                "Your prescription (" + rx.getPrescriptionCode() + ") from " + hospital() + " is now available in your patient portal.");
    }

    /** Removes not-yet-sent reminders (used on cancel / reschedule). */
    @Transactional
    public void cancelPending(Long appointmentId) {
        repo.deleteAll(repo.findByAppointmentIdAndStatus(appointmentId, NotificationStatus.PENDING));
        repo.flush();
    }

    private void createRows(Patient p, Appointment a, NotificationType type, String keyBase, LocalDateTime at, String title, String msg) {
        for (Channel ch : CHANNELS) {
            boolean ok = switch (ch) {
                case IN_APP -> p.getUser() != null;
                case EMAIL -> p.getEmail() != null && !p.getEmail().isBlank();
                default -> p.getPhone() != null && !p.getPhone().isBlank();
            };
            if (!ok) continue;
            String key = type.name() + ":" + keyBase + ":" + ch.name();
            if (key.length() > 120 || repo.existsByDedupeKey(key)) continue;
            Notification n = new Notification();
            n.setRecipientUser(p.getUser());
            n.setPatient(p);
            n.setAppointment(a);
            n.setChannel(ch);
            n.setType(type);
            n.setTitle(title);
            n.setMessage(msg.length() > 1000 ? msg.substring(0, 1000) : msg);
            n.setScheduledAt(at);
            n.setDedupeKey(key);
            boolean instant = ch == Channel.IN_APP && !at.isAfter(LocalDateTime.now());
            n.setStatus(instant ? NotificationStatus.SENT : NotificationStatus.PENDING);
            if (instant) n.setSentAt(LocalDateTime.now());
            repo.save(n);
        }
    }

    private static String stripDr(String name) { return name.replaceFirst("(?i)^dr\\.?\\s*", ""); }

    // ---------- dispatch ----------

    /** Called by the scheduler. Row locks (SKIP LOCKED) make multi-instance deployments safe. */
    @Transactional
    public int dispatchDue() {
        LocalDateTime now = LocalDateTime.now();
        List<Notification> due = repo.findDue(NotificationStatus.PENDING, now, PageRequest.of(0, 100));
        for (Notification n : due) {
            if (n.getScheduledAt().isBefore(now.minusHours(24))) { n.setStatus(NotificationStatus.SKIPPED); n.setLastError("Stale (older than 24h)"); continue; }
            if (n.getChannel() == Channel.IN_APP) { n.setStatus(NotificationStatus.SENT); n.setSentAt(now); continue; }
            ChannelSender s = senders.get(n.getChannel());
            if (s == null || !s.configured()) { n.setStatus(NotificationStatus.SKIPPED); n.setLastError("Channel not configured"); continue; }
            String to = n.getChannel() == Channel.EMAIL ? n.getPatient().getEmail() : n.getPatient().getPhone();
            try {
                s.send(to, n.getTitle(), n.getMessage());
                n.setStatus(NotificationStatus.SENT);
                n.setSentAt(LocalDateTime.now());
                n.setLastError(null);
            } catch (Exception e) {
                n.setAttempts(n.getAttempts() + 1);
                String err = e.getClass().getSimpleName() + ": " + String.valueOf(e.getMessage());
                n.setLastError(err.length() > 290 ? err.substring(0, 290) : err);
                if (n.getAttempts() >= 3) n.setStatus(NotificationStatus.FAILED);
                else n.setScheduledAt(now.plusMinutes(5));
                log.warn("Notification {} via {} failed: {}", n.getId(), n.getChannel(), err);
            }
        }
        return due.size();
    }

    // ---------- in-app inbox ----------

    public List<NotificationView> inbox(User u) {
        return repo.findTop100ByRecipientUserIdAndChannelAndStatusOrderByScheduledAtDesc(u.getId(), Channel.IN_APP, NotificationStatus.SENT)
                .stream().map(mapper::notification).toList();
    }

    public long unreadCount(User u) {
        return repo.countByRecipientUserIdAndChannelAndStatusAndReadFlagFalse(u.getId(), Channel.IN_APP, NotificationStatus.SENT);
    }

    @Transactional
    public void markRead(User u, Long id) {
        Notification n = repo.findById(id).orElseThrow(() -> ApiException.notFound("Notification not found"));
        if (n.getRecipientUser() == null || !n.getRecipientUser().getId().equals(u.getId())) throw ApiException.forbidden("Access denied");
        n.setReadFlag(true);
    }

    @Transactional
    public void markAllRead(User u) {
        repo.findTop100ByRecipientUserIdAndChannelAndStatusOrderByScheduledAtDesc(u.getId(), Channel.IN_APP, NotificationStatus.SENT)
                .forEach(n -> n.setReadFlag(true));
    }
}
