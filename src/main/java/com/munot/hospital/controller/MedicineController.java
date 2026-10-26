package com.munot.hospital.controller;

import com.munot.hospital.dto.ClinicalDtos.*;
import com.munot.hospital.security.CurrentUser;
import com.munot.hospital.service.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/medicines")
@PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
public class MedicineController {
    private final MedicineService medicines;

    public MedicineController(MedicineService medicines) { this.medicines = medicines; }

    @GetMapping("/search")
    public List<MedicineView> search(@RequestParam(required = false, defaultValue = "") String q) { return medicines.search(q); }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) { medicines.delete(id); }
}
