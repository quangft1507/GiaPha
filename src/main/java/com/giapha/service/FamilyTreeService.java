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
        List<Person> rootPersons = personRepository.findRootPersons(treeId);
        if (rootPersons.isEmpty()) {
            return null;
        }

        if (rootPersons.size() == 1) {
            return buildNode(rootPersons.get(0));
        } else {
            TreeNodeDTO virtualRoot = new TreeNodeDTO();
            virtualRoot.setName("Root");
            virtualRoot.setChildren(rootPersons.stream().map(this::buildNode).collect(Collectors.toList()));
            return virtualRoot;
        }
    }

    public TreeNodeDTO buildBranchData(Long personId) {
        Person person = personRepository.findById(personId).orElse(null);
        if (person == null) {
            return null;
        }
        return buildNode(person);
    }

    private TreeNodeDTO buildNode(Person person) {
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
        List<Relationship> spouseRels = relationshipRepository.findSpouseRelationships(person.getId());
        if (!spouseRels.isEmpty()) {
            List<TreeNodeDTO> spouses = new ArrayList<>();
            for (Relationship spouseRel : spouseRels) {
                // Pick the OTHER person as spouse
                Person spousePerson = spouseRel.getPerson().getId().equals(person.getId())
                        ? spouseRel.getRelatedPerson()
                        : spouseRel.getPerson();
                spouses.add(TreeNodeDTO.builder()
                        .id(spousePerson.getId())
                        .name(spousePerson.getFullName())
                        .ho(spousePerson.getHo())
                        .tenDem(spousePerson.getTenDem())
                        .ten(spousePerson.getTen())
                        .aliasName(spousePerson.getAliasName())
                        .gender(spousePerson.getGender())
                        .birthDate(spousePerson.getBirthDate())
                        .deathDate(spousePerson.getDeathDate())
                        .isDeceased(spousePerson.getIsDeceased())
                        .birthPlace(spousePerson.getBirthPlace())
                        .avatarUrl(spousePerson.getAvatarUrl())
                        .generation(spousePerson.getGeneration())
                        .birthOrder(spousePerson.getBirthOrder())
                        .occupation(spousePerson.getOccupation())
                        .build());
            }
            node.setSpouses(spouses);
        }

        // Children
        List<Person> children = relationshipRepository.findChildrenOfPerson(person.getId());
        if (!children.isEmpty()) {
            List<TreeNodeDTO> childrenNodes = new ArrayList<>();
            for (Person child : children) {
                TreeNodeDTO childNode = buildNode(child);
                
                // Find other parent
                List<Person> parentsOfChild = relationshipRepository.findParentsOfPerson(child.getId());
                for (Person p : parentsOfChild) {
                    if (!p.getId().equals(person.getId())) {
                        childNode.setOtherParentId(p.getId());
                        break;
                    }
                }
                
                childrenNodes.add(childNode);
            }
            node.setChildren(childrenNodes);
        }

        // Funeral Cares
        List<FuneralCare> cares = funeralCareRepository.findByDeceasedPersonId(person.getId());
        if (!cares.isEmpty()) {
            node.setFuneralCares(cares.stream().map(care -> FuneralCareDTO.builder()
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
                    .build()).collect(Collectors.toList()));
        }

        return node;
    }

    private FamilyTreeDTO toDTO(FamilyTree tree) {
        return FamilyTreeDTO.builder()
                .id(tree.getId())
                .name(tree.getName())
                .description(tree.getDescription())
                .memberCount(tree.getPersons() != null ? tree.getPersons().size() : 0)
                .createdAt(tree.getCreatedAt())
                .build();
    }
}
