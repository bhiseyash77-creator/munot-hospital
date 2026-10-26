package com.munot.hospital.repository;

import com.munot.hospital.entity.ChatRead;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ChatReadRepository extends JpaRepository<ChatRead, Long> {
    Optional<ChatRead> findByUserId(Long userId);
}
