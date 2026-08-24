package com.giapha.config;

import com.giapha.entity.*;
import com.giapha.enums.CareType;
import com.giapha.enums.Gender;
import com.giapha.enums.RelationshipType;
import com.giapha.repository.*;
import org.springframework.boot.CommandLineRunner;

import org.springframework.stereotype.Component;
import java.time.LocalDate;

@Component
public class DataInitializer implements CommandLineRunner {

    private final FamilyTreeRepository treeRepository;
    private final PersonRepository personRepository;
    private final RelationshipRepository relationshipRepository;
    private final FuneralCareRepository funeralCareRepository;

    public DataInitializer(FamilyTreeRepository treeRepository, 
                           PersonRepository personRepository, RelationshipRepository relationshipRepository,
                           FuneralCareRepository funeralCareRepository) {
        this.treeRepository = treeRepository;
        this.personRepository = personRepository;
        this.relationshipRepository = relationshipRepository;
        this.funeralCareRepository = funeralCareRepository;
    }

    @Override
    public void run(String... args) {
        if (treeRepository.count() == 0) {
            FamilyTree tree = FamilyTree.builder()
                .name("Gia tộc họ Nguyễn")
                .description("Gia phả mẫu họ Nguyễn")
                .build();
            treeRepository.save(tree);

            Person grandFather = Person.builder()
                .ho("Nguyễn").tenDem("Văn").ten("A")
                .gender(Gender.NAM).generation(1)
                .familyTree(tree)
                .build();
            personRepository.save(grandFather);

            Person grandMother = Person.builder()
                .ho("Trần").tenDem("Thị").ten("B")
                .gender(Gender.NU).generation(1)
                .familyTree(tree)
                .build();
            personRepository.save(grandMother);

            Relationship marriage = Relationship.builder()
                .person(grandFather)
                .relatedPerson(grandMother)
                .type(RelationshipType.SPOUSE)
                .build();
            relationshipRepository.save(marriage);

            Person father = Person.builder()
                .ho("Nguyễn").tenDem("Văn").ten("C")
                .gender(Gender.NAM).generation(2).birthOrder(1)
                .familyTree(tree)
                .build();
            personRepository.save(father);
            
            Relationship parentChild = Relationship.builder()
                .person(grandFather)
                .relatedPerson(father)
                .type(RelationshipType.PARENT_CHILD)
                .childOrder(1)
                .build();
            relationshipRepository.save(parentChild);
            
            FuneralCare care = FuneralCare.builder()
                .deceasedPerson(grandFather)
                .caretakerPerson(father)
                .careType(CareType.CUNG_DUONG)
                .assignedDate(LocalDate.now())
                .build();
            funeralCareRepository.save(care);
        }
    }
}
