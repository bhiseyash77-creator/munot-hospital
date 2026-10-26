package com.munot.hospital.service;

import com.munot.hospital.common.ApiException;
import com.munot.hospital.dto.AuthDtos.*;
import com.munot.hospital.entity.*;
import com.munot.hospital.repository.*;
import com.munot.hospital.security.JwtService;
import com.munot.hospital.security.LoginAttemptService;
import com.munot.hospital.util.Codes;
import com.munot.hospital.util.Phones;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AuthService {
    private final UserRepository users;
    private final PatientRepository patients;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final LoginAttemptService attempts;
    private final ViewMapper mapper;
    private final AuditService audit;
    private final String dummyHash;

    public AuthService(UserRepository users, PatientRepository patients, PasswordEncoder encoder, JwtService jwt,
                       LoginAttemptService attempts, ViewMapper mapper, AuditService audit) {
        this.users = users;
        this.patients = patients;
        this.encoder = encoder;
        this.jwt = jwt;
        this.attempts = attempts;
        this.mapper = mapper;
        this.audit = audit;
        this.dummyHash = encoder.encode("not-a-real-password-1");
    }

    public AuthResponse login(LoginRequest req) {
        String email = req.email().trim().toLowerCase();
        if (attempts.isLocked(email)) throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "Too many failed attempts. Try again in 15 minutes.");
        User u = users.findByEmailIgnoreCase(email).orElse(null);
        boolean ok = encoder.matches(req.password(), u == null ? dummyHash : u.getPasswordHash()) && u != null && u.isEnabled();
        if (!ok) {
            attempts.failed(email);
            audit.logAs(email, "LOGIN_FAILED", null);
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }
        attempts.succeeded(email);
        audit.logAs(email, "LOGIN_SUCCESS", u.getRole().name());
        return new AuthResponse(jwt.generate(u), mapper.user(u));
    }

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        String email = req.email().trim().toLowerCase();
        if (users.existsByEmailIgnoreCase(email)) throw ApiException.conflict("An account with this email already exists.");
        String phone = Phones.normalize(req.phone());
        User u = new User();
        u.setEmail(email);
        u.setPhone(phone);
        u.setFullName(req.fullName().trim());
        u.setPasswordHash(encoder.encode(req.password()));
        u.setRole(Role.PATIENT);
        u = users.save(u);

        // Link to a record created earlier by reception (same phone + name) instead of creating a duplicate patient.
        Patient existing = patients.findFirstByPhoneAndFullNameIgnoreCase(phone, u.getFullName()).orElse(null);
        if (existing != null && existing.getUser() != null) throw ApiException.conflict("A patient account already exists for this name and phone. Please log in or contact reception.");
        Patient p = existing != null ? existing : new Patient();
        if (existing == null) {
            p.setPatientCode("TMP-" + java.util.UUID.randomUUID().toString().substring(0, 12));
            p.setFullName(u.getFullName());
            p.setPhone(phone);
            p.setDateOfBirth(req.dateOfBirth());
            p.setGender(req.gender());
        }
        p.setUser(u);
        if (p.getEmail() == null) p.setEmail(email);
        p = patients.saveAndFlush(p);
        if (existing == null) p.setPatientCode(Codes.of("PAT", p.getId()));
        audit.logAs(email, "PATIENT_SELF_REGISTERED", p.getPatientCode());
        return new AuthResponse(jwt.generate(u), mapper.user(u));
    }

    public UserView me(User actor) { return mapper.user(actor); }

    @Transactional
    public void changePassword(User actor, ChangePasswordRequest req) {
        User u = users.findById(actor.getId()).orElseThrow();
        if (!encoder.matches(req.currentPassword(), u.getPasswordHash())) throw ApiException.badRequest("Current password is incorrect");
        u.setPasswordHash(encoder.encode(req.newPassword()));
        audit.log("PASSWORD_CHANGED", "User", u.getId(), null);
    }
}
