package com.giapha.controller;

import com.giapha.dto.FamilyTreeDTO;
import com.giapha.dto.TreeNodeDTO;
import com.giapha.service.FamilyTreeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/trees")
@RequiredArgsConstructor
public class FamilyTreeController {

    private final FamilyTreeService familyTreeService;

    @GetMapping
    public ResponseEntity<List<FamilyTreeDTO>> getAllTrees() {
        return ResponseEntity.ok(familyTreeService.getAllTrees());
    }

    @PostMapping
    public ResponseEntity<FamilyTreeDTO> createTree(@Valid @RequestBody FamilyTreeDTO request) {
        return ResponseEntity.ok(familyTreeService.createTree(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FamilyTreeDTO> getTreeById(@PathVariable Long id) {
        return ResponseEntity.ok(familyTreeService.getTreeById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<FamilyTreeDTO> updateTree(@PathVariable Long id, @Valid @RequestBody FamilyTreeDTO request) {
        return ResponseEntity.ok(familyTreeService.updateTree(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTree(@PathVariable Long id) {
        familyTreeService.deleteTree(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/data")
    public ResponseEntity<TreeNodeDTO> getTreeData(@PathVariable Long id) {
        return ResponseEntity.ok(familyTreeService.buildTreeData(id));
    }

    @GetMapping("/persons/{personId}/branch-data")
    public ResponseEntity<TreeNodeDTO> getBranchData(@PathVariable Long personId) {
        return ResponseEntity.ok(familyTreeService.buildBranchData(personId));
    }
}
