package com.munot.hospital.security;

import com.munot.hospital.common.ApiException;
import com.munot.hospital.entity.User;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.Optional;

public final class CurrentUser {
    private CurrentUser() {}

    public static Optional<User> find() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a != null && a.getPrincipal() instanceof User u) return Optional.of(u);
        return Optional.empty();
    }

    public static User get() {
        return find().orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Authentication required"));
    }
}
