package com.giapha.repository;

import com.giapha.entity.Person;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PersonRepository extends JpaRepository<Person, Long> {
    List<Person> findByFamilyTreeId(Long treeId);
    List<Person> findByFamilyTreeIdOrderByGenerationAscBirthOrderAsc(Long treeId);

    @Query("SELECT p FROM Person p WHERE p.familyTree.id = :treeId AND p.id NOT IN (SELECT r.relatedPerson.id FROM Relationship r WHERE r.person.familyTree.id = :treeId)")
    List<Person> findRootPersons(@Param("treeId") Long treeId);
}
