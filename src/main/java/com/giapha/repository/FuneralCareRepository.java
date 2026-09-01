package com.giapha.repository;

import com.giapha.entity.FuneralCare;
import com.giapha.enums.CareType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface FuneralCareRepository extends JpaRepository<FuneralCare, Long> {
    List<FuneralCare> findByDeceasedPersonId(Long personId);
    List<FuneralCare> findByCaretakerPersonId(Long personId);
    List<FuneralCare> findByDeceasedPersonIdAndCareType(Long personId, CareType careType);
    List<FuneralCare> findByDeceasedPersonIdAndIsActiveTrue(Long personId);

    @org.springframework.data.jpa.repository.Query("SELECT f FROM FuneralCare f WHERE f.deceasedPerson.familyTree.id = :treeId")
    List<FuneralCare> findAllByTreeId(@org.springframework.data.repository.query.Param("treeId") Long treeId);
}
