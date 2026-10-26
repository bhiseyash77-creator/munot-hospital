package com.munot.hospital.service;

import com.munot.hospital.entity.AuditLog;
import com.munot.hospital.entity.User;
import com.munot.hospital.repository.AuditLogRepository;
import com.munot.hospital.security.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class AuditService {
    private final AuditLogRepository repo;

    public AuditService(AuditLogRepository repo) { this.repo = repo; }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String action, String entityType, Object entityId, String details) {
        AuditLog l = new AuditLog();
        User u = CurrentUser.find().orElse(null);
        if (u != null) { l.setUserId(u.getId()); l.setUsername(u.getEmail()); }
        l.setAction(action);
        l.setEntityType(entityType);
        l.setEntityId(entityId == null ? null : String.valueOf(entityId));
        l.setDetails(details != null && details.length() > 480 ? details.substring(0, 480) : details);
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes a) {
            String fwd = a.getRequest().getHeader("X-Forwarded-For");
            l.setIpAddress(fwd != null ? fwd.split(",")[0].trim() : a.getRequest().getRemoteAddr());
        }
        repo.save(l);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAs(String username, String action, String details) {
        AuditLog l = new AuditLog();
        l.setUsername(username);
        l.setAction(action);
        l.setDetails(details);
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes a) l.setIpAddress(a.getRequest().getRemoteAddr());
        repo.save(l);
    }
}
