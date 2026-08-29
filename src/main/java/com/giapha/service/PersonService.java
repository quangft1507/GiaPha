package com.giapha.service;

import com.giapha.dto.PersonCreateRequest;
import com.giapha.dto.PersonDTO;
import com.giapha.entity.FamilyTree;
import com.giapha.entity.Person;
import com.giapha.entity.Relationship;
import com.giapha.enums.RelationshipType;
import com.giapha.repository.FamilyTreeRepository;
import com.giapha.repository.FuneralCareRepository;
import com.giapha.repository.PersonRepository;
import com.giapha.repository.RelationshipRepository;
import com.giapha.entity.FuneralCare;
import com.giapha.enums.CareType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class PersonService {
    private final PersonRepository personRepository;
    private final FamilyTreeRepository familyTreeRepository;
    private final RelationshipRepository relationshipRepository;
    private final FuneralCareRepository funeralCareRepository;

    public PersonDTO createPerson(Long treeId, PersonCreateRequest request) {
        FamilyTree tree = familyTreeRepository.findById(treeId)
                .orElseThrow(() -> new RuntimeException("Tree not found"));
        Person person = fromRequest(request);
        person.setFamilyTree(tree);
        if (person.getGeneration() == null) {
            person.setGeneration(1);
        }
        person = personRepository.save(person);
        
        // Handle caretaker (người cúng dường)
        if (request.getCaretakerId() != null) {
            updateCaretaker(person, request.getCaretakerId());
        }
        
        return toDTO(person);
    }

    public PersonDTO updatePerson(Long personId, PersonCreateRequest request) {
        Person person = personRepository.findById(personId)
                .orElseThrow(() -> new RuntimeException("Person not found"));
        person.setHo(request.getHo());
        person.setTenDem(request.getTenDem());
        person.setTen(request.getTen());
        person.setAliasName(request.getAliasName());
        person.setGender(request.getGender() != null ? request.getGender() : com.giapha.enums.Gender.NAM);
        person.setBirthDate(request.getBirthDate());
        person.setDeathDate(request.getDeathDate());
        person.setIsDeceased(Boolean.TRUE.equals(request.getIsDeceased()));
        person.setBirthPlace(request.getBirthPlace());
        person.setBiography(request.getBiography());
        person.setPhone(request.getPhone());
        person.setOccupation(request.getOccupation());
        person.setBirthOrder(request.getBirthOrder() != null ? request.getBirthOrder() : 1);
        
        person = personRepository.save(person);
        
        if (request.getCaretakerId() != null) {
            updateCaretaker(person, request.getCaretakerId());
        } else {
            // Remove caretaker if null
            List<FuneralCare> cares = funeralCareRepository.findByDeceasedPersonId(personId);
            cares.removeIf(c -> c.getCareType() != CareType.CUNG_DUONG);
            if (!cares.isEmpty()) {
                funeralCareRepository.deleteAll(cares);
            }
        }
        
        // Update parent relationships if requested
        List<Relationship> parentRels = relationshipRepository.findByRelatedPersonId(personId).stream()
                .filter(r -> r.getType() == RelationshipType.PARENT_CHILD)
                .collect(Collectors.toList());
        
        if (request.getParentId() != null) {
            Person newParent = personRepository.findById(request.getParentId()).orElse(null);
            if (newParent != null) {
                if (!parentRels.isEmpty()) {
                    Relationship primaryRel = parentRels.get(0);
                    if (!primaryRel.getPerson().getId().equals(newParent.getId())) {
                        primaryRel.setPerson(newParent);
                        relationshipRepository.save(primaryRel);
                    }
                } else {
                    Relationship rel = new Relationship();
                    rel.setPerson(newParent);
                    rel.setRelatedPerson(person);
                    rel.setType(RelationshipType.PARENT_CHILD);
                    rel.setChildOrder(person.getBirthOrder() != null ? person.getBirthOrder() : 1);
                    relationshipRepository.save(rel);
                }
                if (newParent.getGeneration() != null) {
                    person.setGeneration(newParent.getGeneration() + 1);
                    personRepository.save(person);
                }
            }
        }
        
        if (request.getOtherParentId() != null) {
            Person newOtherParent = personRepository.findById(request.getOtherParentId()).orElse(null);
            if (newOtherParent != null) {
                Relationship secondaryParentRel = parentRels.size() > 1 ? parentRels.get(1) : null;
                if (secondaryParentRel != null) {
                    secondaryParentRel.setPerson(newOtherParent);
                    relationshipRepository.save(secondaryParentRel);
                } else {
                    Relationship rel2 = new Relationship();
                    rel2.setPerson(newOtherParent);
                    rel2.setRelatedPerson(person);
                    rel2.setType(RelationshipType.PARENT_CHILD);
                    rel2.setChildOrder(person.getBirthOrder());
                    relationshipRepository.save(rel2);
                }
            }
        } else if (parentRels.size() > 1) {
            relationshipRepository.delete(parentRels.get(1));
        }
        
        return toDTO(person);
    }
    
    private void updateCaretaker(Person person, Long caretakerId) {
        Person caretaker = personRepository.findById(caretakerId).orElse(null);
        if (caretaker != null) {
            List<FuneralCare> existingCares = funeralCareRepository.findByDeceasedPersonId(person.getId());
            FuneralCare care = existingCares.stream()
                .filter(c -> c.getCareType() == CareType.CUNG_DUONG)
                .findFirst()
                .orElse(new FuneralCare());
                
            care.setDeceasedPerson(person);
            care.setCaretakerPerson(caretaker);
            care.setCareType(CareType.CUNG_DUONG);
            care.setNotes("Người cúng dường (Thiết lập từ form)");
            funeralCareRepository.save(care);
        }
    }

    public void deletePerson(Long personId) {
        Person person = personRepository.findById(personId)
                .orElseThrow(() -> new RuntimeException("Person not found"));
        
        List<Relationship> rels = relationshipRepository.findByPersonId(personId);
        rels.addAll(relationshipRepository.findByRelatedPersonId(personId));
        relationshipRepository.deleteAll(rels);

        funeralCareRepository.deleteAll(funeralCareRepository.findByDeceasedPersonId(personId));
        funeralCareRepository.deleteAll(funeralCareRepository.findByCaretakerPersonId(personId));

        personRepository.delete(person);
    }

    public PersonDTO getPersonById(Long personId) {
        Person person = personRepository.findById(personId)
                .orElseThrow(() -> new RuntimeException("Person not found"));
        return toDTO(person);
    }

    public List<PersonDTO> getPersonsByTreeId(Long treeId) {
        return personRepository.findByFamilyTreeIdOrderByGenerationAscBirthOrderAsc(treeId)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    public PersonDTO addChild(Long parentId, PersonCreateRequest request) {
        Person parent = personRepository.findById(parentId)
                .orElseThrow(() -> new RuntimeException("Parent not found"));
        Person child = fromRequest(request);
        child.setFamilyTree(parent.getFamilyTree());
        child.setGeneration((parent.getGeneration() != null ? parent.getGeneration() : 1) + 1);
        
        List<Person> existingChildren = relationshipRepository.findChildrenOfPerson(parentId);
        child.setBirthOrder(existingChildren.size() + 1);
        
        personRepository.save(child);
        
        Relationship rel = new Relationship();
        rel.setPerson(parent);
        rel.setRelatedPerson(child);
        rel.setType(RelationshipType.PARENT_CHILD);
        rel.setChildOrder(child.getBirthOrder());
        relationshipRepository.save(rel);
        
        if (request.getOtherParentId() != null) {
            Person otherParent = personRepository.findById(request.getOtherParentId())
                    .orElseThrow(() -> new RuntimeException("Other parent not found"));
            Relationship rel2 = new Relationship();
            rel2.setPerson(otherParent);
            rel2.setRelatedPerson(child);
            rel2.setType(RelationshipType.PARENT_CHILD);
            rel2.setChildOrder(child.getBirthOrder());
            relationshipRepository.save(rel2);
        }
        
        return toDTO(child);
    }

    public PersonDTO addParent(Long childId, PersonCreateRequest request) {
        Person child = personRepository.findById(childId)
                .orElseThrow(() -> new RuntimeException("Child not found"));
        Person parent = fromRequest(request);
        parent.setFamilyTree(child.getFamilyTree());

        Integer childGen = child.getGeneration();
        if (childGen == null || childGen <= 1) {
            // Shift all persons in the tree down by 1 generation
            List<Person> allInTree = personRepository.findByFamilyTreeId(child.getFamilyTree().getId());
            for (Person p : allInTree) {
                p.setGeneration((p.getGeneration() != null ? p.getGeneration() : 1) + 1);
            }
            personRepository.saveAll(allInTree);
            parent.setGeneration(1);
        } else {
            parent.setGeneration(childGen - 1);
        }

        parent = personRepository.save(parent);

        // Relationship parent -> child
        Relationship rel = new Relationship();
        rel.setPerson(parent);
        rel.setRelatedPerson(child);
        rel.setType(RelationshipType.PARENT_CHILD);
        rel.setChildOrder(child.getBirthOrder() != null ? child.getBirthOrder() : 1);
        relationshipRepository.save(rel);

        // Check if child has other existing parents, link as spouse
        List<Person> existingParents = relationshipRepository.findParentsOfPerson(childId);
        for (Person existingParent : existingParents) {
            if (!existingParent.getId().equals(parent.getId())) {
                existingParent.setGeneration(parent.getGeneration());
                personRepository.save(existingParent);

                List<Relationship> spouseRels = relationshipRepository.findSpouseRelationships(parent.getId());
                boolean alreadySpouse = spouseRels.stream().anyMatch(r ->
                    (r.getPerson().getId().equals(existingParent.getId()) && r.getRelatedPerson().getId().equals(parent.getId())) ||
                    (r.getPerson().getId().equals(parent.getId()) && r.getRelatedPerson().getId().equals(existingParent.getId()))
                );
                if (!alreadySpouse) {
                    Relationship spouseRel = new Relationship();
                    spouseRel.setPerson(parent);
                    spouseRel.setRelatedPerson(existingParent);
                    spouseRel.setType(RelationshipType.SPOUSE);
                    relationshipRepository.save(spouseRel);
                }
            }
        }

        if (request.getCaretakerId() != null) {
            updateCaretaker(parent, request.getCaretakerId());
        }

        return toDTO(parent);
    }

    public PersonDTO addSpouse(Long personId, PersonCreateRequest request) {
        Person person = personRepository.findById(personId)
                .orElseThrow(() -> new RuntimeException("Person not found"));
        Person spouse = fromRequest(request);
        spouse.setFamilyTree(person.getFamilyTree());
        spouse.setGeneration(person.getGeneration());
        personRepository.save(spouse);
        
        Relationship rel = new Relationship();
        rel.setPerson(person);
        rel.setRelatedPerson(spouse);
        rel.setType(RelationshipType.SPOUSE);
        relationshipRepository.save(rel);
        
        return toDTO(spouse);
    }

    public void reorderChildren(Long parentId, List<Long> orderedChildIds) {
        for (int i = 0; i < orderedChildIds.size(); i++) {
            Long childId = orderedChildIds.get(i);
            Relationship rel = relationshipRepository.findByPersonIdAndRelatedPersonIdAndType(parentId, childId, RelationshipType.PARENT_CHILD)
                    .orElseThrow(() -> new RuntimeException("Relationship not found"));
            rel.setChildOrder(i + 1);
            relationshipRepository.save(rel);
            
            Person child = personRepository.findById(childId).orElse(null);
            if(child != null) {
                child.setBirthOrder(i + 1);
                personRepository.save(child);
            }
        }
    }
    
    public void deleteBranch(Long personId) {
        // Recursive deletion
        List<Person> children = relationshipRepository.findChildrenOfPerson(personId);
        for (Person child : children) {
            deleteBranch(child.getId());
        }
        deletePerson(personId);
    }
    
    public void pasteBranch(Long sourceId, Long targetId) {
        Person source = personRepository.findById(sourceId).orElseThrow(() -> new RuntimeException("Source not found"));
        Person target = personRepository.findById(targetId).orElseThrow(() -> new RuntimeException("Target not found"));
        
        clonePersonRecursive(source, target, target.getFamilyTree());
    }
    
    private void clonePersonRecursive(Person source, Person newParent, FamilyTree targetTree) {
        // Create new person based on source
        Person clone = new Person();
        clone.setHo(source.getHo());
        clone.setTenDem(source.getTenDem());
        clone.setTen(source.getTen());
        clone.setAliasName(source.getAliasName());
        clone.setGender(source.getGender());
        clone.setBirthDate(source.getBirthDate());
        clone.setDeathDate(source.getDeathDate());
        clone.setIsDeceased(source.getIsDeceased());
        clone.setBirthPlace(source.getBirthPlace());
        clone.setBiography(source.getBiography());
        clone.setPhone(source.getPhone());
        clone.setOccupation(source.getOccupation());
        clone.setFamilyTree(targetTree);
        clone.setGeneration((newParent != null && newParent.getGeneration() != null ? newParent.getGeneration() : 1) + 1);
        
        List<Person> existingChildren = relationshipRepository.findChildrenOfPerson(newParent.getId());
        clone.setBirthOrder(existingChildren.size() + 1);
        
        personRepository.save(clone);
        
        // Add relationship to new parent
        Relationship rel = new Relationship();
        rel.setPerson(newParent);
        rel.setRelatedPerson(clone);
        rel.setType(RelationshipType.PARENT_CHILD);
        rel.setChildOrder(clone.getBirthOrder());
        relationshipRepository.save(rel);
        
        // Recursively copy children
        List<Person> sourceChildren = relationshipRepository.findChildrenOfPerson(source.getId());
        for (Person child : sourceChildren) {
            clonePersonRecursive(child, clone, targetTree);
        }
        
        // Optionally clone spouses... but that can get complicated. Let's just clone descendants for now.
    }

    private PersonDTO toDTO(Person person) {
        PersonDTO dto = PersonDTO.builder()
                .id(person.getId())
                .ho(person.getHo())
                .tenDem(person.getTenDem())
                .ten(person.getTen())
                .fullName(person.getFullName())
                .aliasName(person.getAliasName())
                .gender(person.getGender())
                .birthDate(person.getBirthDate())
                .deathDate(person.getDeathDate())
                .isDeceased(person.getIsDeceased())
                .birthPlace(person.getBirthPlace())
                .avatarUrl(person.getAvatarUrl())
                .biography(person.getBiography())
                .phone(person.getPhone())
                .occupation(person.getOccupation())
                .generation(person.getGeneration())
                .birthOrder(person.getBirthOrder())
                .familyTreeId(person.getFamilyTree() != null ? person.getFamilyTree().getId() : null)
                .createdAt(person.getCreatedAt())
                .build();
                
        // Fetch Father, Mother
        List<Relationship> parentRels = relationshipRepository.findByRelatedPersonId(person.getId());
        for (Relationship rel : parentRels) {
            if (rel.getType() == RelationshipType.PARENT_CHILD && rel.getPerson() != null) {
                Person parent = rel.getPerson();
                if (dto.getParentId() == null) {
                    dto.setParentId(parent.getId());
                } else if (dto.getOtherParentId() == null) {
                    dto.setOtherParentId(parent.getId());
                }
                
                if (parent.getGender() == com.giapha.enums.Gender.NAM) {
                    dto.setFatherName(parent.getFullName());
                } else if (parent.getGender() == com.giapha.enums.Gender.NU) {
                    dto.setMotherName(parent.getFullName());
                }
            }
        }
        
        // Fetch Caretaker
        List<FuneralCare> cares = funeralCareRepository.findByDeceasedPersonId(person.getId());
        cares.stream().filter(c -> c.getCareType() == CareType.CUNG_DUONG && c.getCaretakerPerson() != null).findFirst().ifPresent(c -> {
            dto.setCaretakerId(c.getCaretakerPerson().getId());
            dto.setCaretakerName(c.getCaretakerPerson().getFullName());
        });
        
        return dto;
    }

    private Person fromRequest(PersonCreateRequest request) {
        Person p = new Person();
        p.setHo(request.getHo());
        p.setTenDem(request.getTenDem());
        p.setTen(request.getTen());
        p.setAliasName(request.getAliasName());
        p.setGender(request.getGender() != null ? request.getGender() : com.giapha.enums.Gender.NAM);
        p.setBirthDate(request.getBirthDate());
        p.setDeathDate(request.getDeathDate());
        p.setIsDeceased(Boolean.TRUE.equals(request.getIsDeceased()));
        p.setBirthPlace(request.getBirthPlace());
        p.setBiography(request.getBiography());
        p.setPhone(request.getPhone());
        p.setOccupation(request.getOccupation());
        p.setBirthOrder(request.getBirthOrder() != null ? request.getBirthOrder() : 1);
        return p;
    }
}
