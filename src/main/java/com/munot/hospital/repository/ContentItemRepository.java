package com.munot.hospital.repository;

import com.munot.hospital.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface ContentItemRepository extends JpaRepository<ContentItem, Long> {
    List<ContentItem> findByTypeAndActiveTrueOrderBySortOrderAscIdAsc(ContentType type);
    List<ContentItem> findByTypeOrderBySortOrderAscIdAsc(ContentType type);
    Optional<ContentItem> findByTypeAndSlug(ContentType type, String slug);
}
