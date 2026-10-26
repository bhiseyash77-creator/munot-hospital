package com.munot.hospital.controller;

import com.munot.hospital.dto.*;
import com.munot.hospital.dto.AdminDtos.*;
import com.munot.hospital.dto.AppointmentDtos.*;
import com.munot.hospital.dto.AuthDtos.*;
import com.munot.hospital.dto.BillingDtos.*;
import com.munot.hospital.dto.ClinicalDtos.*;
import com.munot.hospital.dto.PatientDtos.*;
import com.munot.hospital.entity.*;
import com.munot.hospital.security.CurrentUser;
import com.munot.hospital.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.*;
import java.util.*;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notifications;

    @GetMapping("/my")
    public List<NotificationView> mine() { return notifications.inbox(CurrentUser.get()); }

    @GetMapping("/my/unread-count")
    public Map<String, Long> unread() { return Map.of("count", notifications.unreadCount(CurrentUser.get())); }

    @PutMapping("/{id}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void read(@PathVariable Long id) { notifications.markRead(CurrentUser.get(), id); }

    @PutMapping("/my/read-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void readAll() { notifications.markAllRead(CurrentUser.get()); }
}
