package com.munot.hospital.service;

import com.munot.hospital.common.ApiException;
import com.munot.hospital.dto.ContentDtos.*;
import com.munot.hospital.entity.*;
import com.munot.hospital.repository.ContentItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

/** Admin-editable website content: facilities, why-choose-us points, testimonials and health tips. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContentService {
    private final ContentItemRepository repo;
    private final AuditService audit;

    private ContentView view(ContentItem c) {
        return new ContentView(c.getId(), c.getType(), c.getSlug(), c.getTitle(), c.getSubtitle(), c.getBody(), c.getIcon(), c.getSortOrder(), c.isActive());
    }

    public List<ContentView> publicList(ContentType type) {
        return repo.findByTypeAndActiveTrueOrderBySortOrderAscIdAsc(type).stream().map(this::view).toList();
    }

    public List<ContentView> adminList(ContentType type) {
        return repo.findByTypeOrderBySortOrderAscIdAsc(type).stream().map(this::view).toList();
    }

    static String slugify(String s) {
        String slug = s.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-+|-+$)", "");
        if (slug.isEmpty()) slug = "article";
        return slug.length() > 100 ? slug.substring(0, 100) : slug;
    }

    @Transactional
    public ContentView save(Long id, ContentRequest r) {
        ContentItem c = id == null ? new ContentItem() : repo.findById(id).orElseThrow(() -> ApiException.notFound("Content not found"));
        if (id != null && c.getType() != r.type()) throw ApiException.badRequest("Content type cannot be changed");
        c.setType(r.type());
        c.setTitle(r.title().trim());
        c.setSubtitle(r.subtitle());
        c.setBody(r.body());
        c.setIcon(r.icon() == null || r.icon().isBlank() ? null : r.icon());
        c.setSortOrder(r.sortOrder());
        c.setActive(r.active());
        if (r.type() == ContentType.HEALTH_TIP) {
            String base = r.slug() == null || r.slug().isBlank() ? slugify(r.title()) : slugify(r.slug());
            String slug = base;
            int n = 2;
            while (true) {
                Optional<ContentItem> other = repo.findByTypeAndSlug(ContentType.HEALTH_TIP, slug);
                if (other.isEmpty() || other.get().getId().equals(c.getId())) break;
                slug = base + "-" + n++;
            }
            c.setSlug(slug);
        } else {
            c.setSlug(null);
        }
        c = repo.save(c);
        audit.log("CONTENT_SAVED", "ContentItem", c.getId(), c.getType() + ": " + c.getTitle());
        return view(c);
    }

    @Transactional
    public void delete(Long id) {
        repo.deleteById(id);
        audit.log("CONTENT_DELETED", "ContentItem", id, null);
    }
}
