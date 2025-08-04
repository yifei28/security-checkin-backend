package com.duhao.security.checkinapp.controller;

import com.duhao.security.checkinapp.entity.WorkSite;
import com.duhao.security.checkinapp.repository.WorkSiteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sites")
public class WorkSiteController {
    private final WorkSiteRepository workSiteRepository;

    @Autowired
    public WorkSiteController(WorkSiteRepository workSiteRepository) {
        this.workSiteRepository = workSiteRepository;
    }

    @PostMapping
    public ResponseEntity<?> addWorkSite(@RequestBody WorkSite workSite){
        return ResponseEntity.ok(workSiteRepository.save(workSite));
    }

    @PutMapping("/{id}")
    public ResponseEntity<WorkSite> updateSite(@PathVariable Long id, @RequestBody WorkSite updatedSite) {
        return workSiteRepository.findById(id).map(existing -> {
            existing.setName(updatedSite.getName());
            existing.setLatitude(updatedSite.getLatitude());
            existing.setLongitude(updatedSite.getLongitude());
            existing.setAllowedRadiusMeters(updatedSite.getAllowedRadiusMeters());
            return ResponseEntity.ok(workSiteRepository.save(existing));
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSite(@PathVariable Long id) {
        workSiteRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public List<WorkSite> getAllWorkSites() {
        return workSiteRepository.findAll();
    }
}
