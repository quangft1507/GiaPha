package com.giapha.service;

import com.giapha.dto.FamilyTreeDTO;
import com.giapha.dto.FuneralCareDTO;
import com.giapha.dto.TreeNodeDTO;
import com.giapha.entity.FamilyTree;
import com.giapha.entity.FuneralCare;
import com.giapha.entity.Person;
import com.giapha.entity.Relationship;
import com.giapha.repository.FamilyTreeRepository;
import com.giapha.repository.FuneralCareRepository;
import com.giapha.repository.PersonRepository;
import com.giapha.repository.RelationshipRepository;
import com.giapha.enums.CareType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class FamilyTreeService {
    private final FamilyTreeRepository familyTreeRepository;
    private final PersonRepository personRepository;
    private final RelationshipRepository relationshipRepository;
    private final FuneralCareRepository funeralCareRepository;

    public FamilyTreeDTO createTree(FamilyTreeDTO request) {
        FamilyTree tree = new FamilyTree();
        tree.setName(request.getName());
        tree.setDescription(request.getDescription());
        return toDTO(familyTreeRepository.save(tree));
    }

    public FamilyTreeDTO updateTree(Long treeId, FamilyTreeDTO request) {
        FamilyTree tree = familyTreeRepository.findById(treeId)
                .orElseThrow(() -> new RuntimeException("Tree not found"));
        tree.setName(request.getName());
        tree.setDescription(request.getDescription());
        return toDTO(familyTreeRepository.save(tree));
    }

    public void deleteTree(Long treeId) {
        familyTreeRepository.deleteById(treeId);
    }

    public FamilyTreeDTO getTreeById(Long treeId) {
        FamilyTree tree = familyTreeRepository.findById(treeId)
                .orElseThrow(() -> new RuntimeException("Tree not found"));
        return toDTO(tree);
    }

    public List<FamilyTreeDTO> getAllTrees() {
        return familyTreeRepository.findAll()
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    public TreeNodeDTO buildTreeData(Long treeId) {
        return buildTreeFromFlat(treeId, null);
    }

    public TreeNodeDTO buildBranchData(Long personId) {
        Person person = personRepository.findById(personId).orElse(null);
        if (person == null) return null;
        Long treeId = person.getFamilyTree().getId();
        return buildTreeFromFlat(treeId, personId);
    }

    /**
     * Efficient flat-query tree builder.
     * Loads ALL persons and ALL relationships for a tree in 3 queries total,
     * then builds the hierarchy in memory — avoids N+1 entirely.
     */
    private TreeNodeDTO buildTreeFromFlat(Long treeId, Long rootPersonId) {
        // 1. Load all persons for this tree
        List<Person> allPersons = personRepository.findByFamilyTreeIdOrderByGenerationAscBirthOrderAsc(treeId);
        if (allPersons.isEmpty()) return null;

        // 2. Load all relationships for this tree in ONE query
        List<Relationship> allRels = relationshipRepository.findAllByTreeId(treeId);

        // 3. Load all funeral cares for this tree in ONE query
        List<FuneralCare> allCares = funeralCareRepository.findAllByTreeId(treeId);

        // Build lookup maps
        java.util.Map<Long, Person> personMap = new java.util.HashMap<>();
        for (Person p : allPersons) personMap.put(p.getId(), p);

        // parent → List<child IDs> (PARENT_CHILD rels)
        java.util.Map<Long, List<Long>> childrenMap = new java.util.HashMap<>();
        // person → List<spouse IDs>
        java.util.Map<Long, List<Long>> spouseMap = new java.util.HashMap<>();
        // child → other-parent ID
        java.util.Map<Long, Long> otherParentMap = new java.util.HashMap<>();
        // child → parent IDs list
        java.util.Map<Long, List<Long>> parentsMap = new java.util.HashMap<>();

        for (Relationship rel : allRels) {
            if (rel.getType() == com.giapha.enums.RelationshipType.PARENT_CHILD) {
                Long parentId = rel.getPerson().getId();
                Long childId = rel.getRelatedPerson().getId();
                childrenMap.computeIfAbsent(parentId, k -> new ArrayList<>()).add(childId);
                parentsMap.computeIfAbsent(childId, k -> new ArrayList<>()).add(parentId);
            } else if (rel.getType() == com.giapha.enums.RelationshipType.SPOUSE) {
                Long a = rel.getPerson().getId();
                Long b = rel.getRelatedPerson().getId();
                spouseMap.computeIfAbsent(a, k -> new ArrayList<>()).add(b);
                spouseMap.computeIfAbsent(b, k -> new ArrayList<>()).add(a);
            }
        }

        // Other parent map: for each child that has 2 parents, record which is "other"
        for (java.util.Map.Entry<Long, List<Long>> entry : parentsMap.entrySet()) {
            if (entry.getValue().size() >= 2) {
                otherParentMap.put(entry.getKey(), entry.getValue().get(1));
            }
        }

        // Funeral care map: deceasedId → caretaker
        java.util.Map<Long, FuneralCare> caretakerMap = new java.util.HashMap<>();
        for (FuneralCare care : allCares) {
            if (care.getCareType() == CareType.CUNG_DUONG && care.getCaretakerPerson() != null
                    && care.getDeceasedPerson() != null) {
                caretakerMap.putIfAbsent(care.getDeceasedPerson().getId(), care);
            }
        }

        // Find root(s): if rootPersonId given use it, else find persons with no parent
        java.util.Set<Long> visited = new java.util.HashSet<>();
        if (rootPersonId != null) {
            Person root = personMap.get(rootPersonId);
            if (root == null) return null;
            return buildNodeFromMap(root, personMap, childrenMap, spouseMap, otherParentMap, caretakerMap, visited);
        }

        // No root specified → find persons with no parents (root persons)
        java.util.Set<Long> hasParent = new java.util.HashSet<>(parentsMap.keySet());
        List<Person> roots = allPersons.stream()
                .filter(p -> !hasParent.contains(p.getId()))
                .collect(Collectors.toList());

        if (roots.isEmpty()) {
            roots = List.of(allPersons.get(0));
        }

        // Filter out spouses of other root persons.
        // Spouses will be rendered alongside their partner — no need to show them as separate roots.
        // BUG FIX: skip persons already in spousesOfRoots to avoid excluding BOTH spouses.
        if (roots.size() > 1) {
            java.util.Set<Long> rootIds = roots.stream()
                    .map(Person::getId)
                    .collect(java.util.stream.Collectors.toSet());
            java.util.Set<Long> spousesOfRoots = new java.util.HashSet<>();
            for (Person r : roots) {
                if (spousesOfRoots.contains(r.getId())) continue; // already excluded, skip
                List<Long> spouseIds = spouseMap.getOrDefault(r.getId(), new ArrayList<>());
                for (Long sid : spouseIds) {
                    if (rootIds.contains(sid)) {
                        spousesOfRoots.add(sid); // exclude the spouse, keep r
                    }
                }
            }
            if (!spousesOfRoots.isEmpty()) {
                List<Person> filtered = roots.stream()
                        .filter(r -> !spousesOfRoots.contains(r.getId()))
                        .collect(Collectors.toList());
                if (!filtered.isEmpty()) {
                    roots = filtered; // only apply if result is non-empty
                }
            }
        }

        if (roots.isEmpty()) {
            roots = List.of(allPersons.get(0));
        }

        if (roots.size() == 1) {
            return buildNodeFromMap(roots.get(0), personMap, childrenMap, spouseMap, otherParentMap, caretakerMap, visited);
        }

        // Still multiple roots (truly separate branches) → virtual root wrapper
        TreeNodeDTO virtualRoot = new TreeNodeDTO();
        virtualRoot.setName("Root");
        virtualRoot.setChildren(roots.stream()
                .map(r -> buildNodeFromMap(r, personMap, childrenMap, spouseMap, otherParentMap, caretakerMap, visited))
                .filter(n -> n != null)
                .collect(Collectors.toList()));
        return virtualRoot;
    }

    private TreeNodeDTO buildNodeFromMap(Person person,
            java.util.Map<Long, Person> personMap,
            java.util.Map<Long, List<Long>> childrenMap,
            java.util.Map<Long, List<Long>> spouseMap,
            java.util.Map<Long, Long> otherParentMap,
            java.util.Map<Long, FuneralCare> caretakerMap,
            java.util.Set<Long> visited) {

        if (visited.contains(person.getId())) return null;
        visited.add(person.getId());

        TreeNodeDTO node = TreeNodeDTO.builder()
                .id(person.getId())
                .name(person.getFullName())
                .ho(person.getHo())
                .tenDem(person.getTenDem())
                .ten(person.getTen())
                .aliasName(person.getAliasName())
                .gender(person.getGender())
                .birthDate(person.getBirthDate())
                .deathDate(person.getDeathDate())
                .isDeceased(person.getIsDeceased())
                .birthPlace(person.getBirthPlace())
                .avatarUrl(person.getAvatarUrl())
                .generation(person.getGeneration())
                .birthOrder(person.getBirthOrder())
                .occupation(person.getOccupation())
                .build();

        // Spouses
        List<Long> spouseIds = spouseMap.get(person.getId());
        if (spouseIds != null && !spouseIds.isEmpty()) {
            List<TreeNodeDTO> spouses = new ArrayList<>();
            for (Long sid : spouseIds) {
                Person sp = personMap.get(sid);
                if (sp != null) {
                    spouses.add(TreeNodeDTO.builder()
                            .id(sp.getId()).name(sp.getFullName())
                            .ho(sp.getHo()).tenDem(sp.getTenDem()).ten(sp.getTen())
                            .aliasName(sp.getAliasName()).gender(sp.getGender())
                            .birthDate(sp.getBirthDate()).deathDate(sp.getDeathDate())
                            .isDeceased(sp.getIsDeceased()).birthPlace(sp.getBirthPlace())
                            .avatarUrl(sp.getAvatarUrl()).generation(sp.getGeneration())
                            .birthOrder(sp.getBirthOrder()).occupation(sp.getOccupation())
                            .build());
                }
            }
            node.setSpouses(spouses);
        }

        // Children (recursive in-memory, no extra DB calls)
        List<Long> childIds = childrenMap.get(person.getId());
        if (childIds != null && !childIds.isEmpty()) {
            List<TreeNodeDTO> childNodes = new ArrayList<>();
            for (Long cid : childIds) {
                Person child = personMap.get(cid);
                if (child != null) {
                    TreeNodeDTO childNode = buildNodeFromMap(child, personMap, childrenMap, spouseMap, otherParentMap, caretakerMap, visited);
                    if (childNode != null) {
                        childNode.setOtherParentId(otherParentMap.get(cid));
                        childNodes.add(childNode);
                    }
                }
            }
            node.setChildren(childNodes);
        }

        // Funeral care
        FuneralCare care = caretakerMap.get(person.getId());
        if (care != null) {
            node.setFuneralCares(List.of(FuneralCareDTO.builder()
                    .id(care.getId())
                    .deceasedPersonId(care.getDeceasedPerson().getId())
                    .deceasedPersonName(care.getDeceasedPerson().getFullName())
                    .caretakerPersonId(care.getCaretakerPerson().getId())
                    .caretakerPersonName(care.getCaretakerPerson().getFullName())
                    .careType(care.getCareType())
                    .careTypeDisplayName(care.getCareType().getDisplayName())
                    .notes(care.getNotes())
                    .assignedDate(care.getAssignedDate())
                    .isActive(care.getIsActive())
                    .build()));
        }

        return node;
    }

    private FamilyTreeDTO toDTO(FamilyTree tree) {
        return FamilyTreeDTO.builder()
                .id(tree.getId())
                .name(tree.getName())
                .description(tree.getDescription())
                .memberCount((int) personRepository.countByFamilyTreeId(tree.getId()))
                .createdAt(tree.getCreatedAt())
                .build();
    }
}
