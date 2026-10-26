package com.munot.hospital.service;

import com.munot.hospital.common.ApiException;
import com.munot.hospital.dto.ClinicalDtos.*;
import com.munot.hospital.entity.Medicine;
import com.munot.hospital.repository.MedicineRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

/** Remembers every medicine the doctor prescribes (name + composition only) for auto-suggest. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MedicineService {
    private final MedicineRepository repo;
    private final AuditService audit;

    private MedicineView view(Medicine m) {
        return new MedicineView(m.getId(), m.getName(), m.getComposition(), m.getUseCount());
    }

    /** Matches on medicine name or composition (so typing "paracetamol" also finds a brand containing it). */
    public List<MedicineView> search(String q) {
        String term = q == null ? "" : q.trim();
        return (term.isEmpty() ? repo.findTop10ByOrderByUseCountDescNameAsc()
                : repo.findTop10ByNameContainingIgnoreCaseOrCompositionContainingIgnoreCaseOrderByUseCountDescNameAsc(term, term))
                .stream().map(this::view).toList();
    }

    /** Remembers ONLY the medicine name and its composition. Dose, frequency and duration are never stored here. */
    @Transactional
    public void remember(PrescriptionItemDto d, boolean countUse) {
        String name = d.medicineName() == null ? "" : d.medicineName().trim();
        if (name.isEmpty()) return;
        if (name.length() > 200) name = name.substring(0, 200);
        Medicine m = repo.findByNameIgnoreCase(name).orElseGet(Medicine::new);
        if (m.getName() == null) m.setName(name);
        String comp = d.composition() == null ? "" : d.composition().trim();
        if (!comp.isEmpty()) m.setComposition(comp.length() > 300 ? comp.substring(0, 300) : comp);
        if (countUse) { m.setUseCount(m.getUseCount() + 1); m.setLastUsedAt(LocalDateTime.now()); }
        repo.save(m);
    }

    @Transactional
    public void delete(Long id) {
        if (!repo.existsById(id)) throw ApiException.notFound("Medicine not found");
        repo.deleteById(id);
        audit.log("MEDICINE_REMOVED", "Medicine", id, null);
    }
}
