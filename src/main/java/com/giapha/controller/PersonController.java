package com.giapha.controller;

import com.giapha.dto.PersonCreateRequest;
import com.giapha.dto.PersonDTO;
import com.giapha.service.PersonService;
import com.giapha.service.ExcelImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PersonController {

    private final PersonService personService;
    private final ExcelImportService excelImportService;

    @GetMapping("/trees/excel-template")
    public ResponseEntity<byte[]> downloadExcelTemplate() {
        try {
            byte[] file = excelImportService.generateTemplate();
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"gia_pha_template.xlsx\"")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(file);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/trees/{treeId}/import")
    public ResponseEntity<String> importExcel(@PathVariable Long treeId, @RequestParam("file") MultipartFile file) {
        try {
            excelImportService.importExcel(treeId, file);
            return ResponseEntity.ok("Import thành công!");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi import: " + e.getMessage());
        }
    }

    @GetMapping("/trees/{treeId}/persons")
    public ResponseEntity<List<PersonDTO>> getPersonsByTreeId(@PathVariable Long treeId) {
        return ResponseEntity.ok(personService.getPersonsByTreeId(treeId));
    }

    @PostMapping("/trees/{treeId}/persons")
    public ResponseEntity<PersonDTO> createPerson(@PathVariable Long treeId, @Valid @RequestBody PersonCreateRequest request) {
        return ResponseEntity.ok(personService.createPerson(treeId, request));
    }

    @GetMapping("/persons/{id}")
    public ResponseEntity<PersonDTO> getPersonById(@PathVariable Long id) {
        return ResponseEntity.ok(personService.getPersonById(id));
    }

    @PutMapping("/persons/{id}")
    public ResponseEntity<PersonDTO> updatePerson(@PathVariable Long id, @Valid @RequestBody PersonCreateRequest request) {
        return ResponseEntity.ok(personService.updatePerson(id, request));
    }

    @DeleteMapping("/persons/{id}")
    public ResponseEntity<Void> deletePerson(@PathVariable Long id) {
        personService.deletePerson(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/persons/{parentId}/children")
    public ResponseEntity<PersonDTO> addChild(@PathVariable Long parentId, @Valid @RequestBody PersonCreateRequest request) {
        return ResponseEntity.ok(personService.addChild(parentId, request));
    }

    @PostMapping("/persons/{personId}/spouse")
    public ResponseEntity<PersonDTO> addSpouse(@PathVariable Long personId, @Valid @RequestBody PersonCreateRequest request) {
        return ResponseEntity.ok(personService.addSpouse(personId, request));
    }

    @PutMapping("/persons/{parentId}/reorder-children")
    public ResponseEntity<Void> reorderChildren(@PathVariable Long parentId, @RequestBody List<Long> orderedChildIds) {
        personService.reorderChildren(parentId, orderedChildIds);
        return ResponseEntity.ok().build();
    }
    
    @DeleteMapping("/persons/{id}/branch")
    public ResponseEntity<Void> deleteBranch(@PathVariable Long id) {
        personService.deleteBranch(id);
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/persons/{targetId}/paste-branch")
    public ResponseEntity<Void> pasteBranch(@PathVariable Long targetId, @RequestParam Long sourceId) {
        personService.pasteBranch(sourceId, targetId);
        return ResponseEntity.ok().build();
    }
}
