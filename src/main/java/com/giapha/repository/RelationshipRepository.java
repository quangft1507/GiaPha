package com.giapha.repository;

import com.giapha.entity.Relationship;
import com.giapha.enums.RelationshipType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import com.giapha.entity.Person;

@Repository
public interface RelationshipRepository extends JpaRepository<Relationship, Long> {
    List<Relationship> findByPersonId(Long personId);
    List<Relationship> findByRelatedPersonId(Long relatedPersonId);
    List<Relationship> findByPersonIdAndType(Long personId, RelationshipType type);

    @Query("SELECT r.relatedPerson FROM Relationship r WHERE r.person.id = :parentId AND r.type = 'PARENT_CHILD' ORDER BY r.childOrder ASC")
    List<Person> findChildrenOfPerson(@Param("parentId") Long parentId);

    @Query("SELECT r FROM Relationship r WHERE (r.person.id = :personId OR r.relatedPerson.id = :personId) AND r.type = 'SPOUSE'")
    List<Relationship> findSpouseRelationships(@Param("personId") Long personId);

    @Query("SELECT r.person FROM Relationship r WHERE r.relatedPerson.id = :childId AND r.type = 'PARENT_CHILD'")
    List<Person> findParentsOfPerson(@Param("childId") Long childId);

    Optional<Relationship> findByPersonIdAndRelatedPersonIdAndType(Long personId, Long relatedPersonId, RelationshipType type);

    @Query("SELECT r FROM Relationship r WHERE r.person.familyTree.id = :treeId OR r.relatedPerson.familyTree.id = :treeId")
    List<Relationship> findAllByTreeId(@Param("treeId") Long treeId);
}
