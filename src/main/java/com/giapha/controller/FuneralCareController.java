package com.giapha.controller;

import com.giapha.dto.FuneralCareDTO;
import com.giapha.dto.FuneralCareRequest;
import com.giapha.service.FuneralCareService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class FuneralCareController {

    private final FuneralCareService funeralCareService;

    @PostMapping("/persons/{personId}/funeral-care")
    public ResponseEntity<FuneralCareDTO> assignCare(@PathVariable Long personId, @Valid @RequestBody FuneralCareRequest request) {
        return ResponseEntity.ok(funeralCareService.assignCare(personId, request));
    }

    @GetMapping("/persons/{personId}/funeral-care")
    public ResponseEntity<List<FuneralCareDTO>> getCareForPerson(@PathVariable Long personId) {
        return ResponseEntity.ok(funeralCareService.getCareForPerson(personId));
    }

    @PutMapping("/funeral-care/{id}")
    public ResponseEntity<FuneralCareDTO> updateCare(@PathVariable Long id, @Valid @RequestBody FuneralCareRequest request) {
        return ResponseEntity.ok(funeralCareService.updateCare(id, request));
    }

    @DeleteMapping("/funeral-care/{id}")
    public ResponseEntity<Void> removeCare(@PathVariable Long id) {
        funeralCareService.removeCare(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/persons/{personId}/caretaking")
    public ResponseEntity<List<FuneralCareDTO>> getCareByCaretaker(@PathVariable Long personId) {
        return ResponseEntity.ok(funeralCareService.getCareByCaretaker(personId));
    }
}
