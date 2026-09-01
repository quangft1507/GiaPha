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

    @Query("SELECT f FROM FuneralCare f JOIN f.deceasedPerson d WHERE d.familyTree.id = :treeId")
    List<FuneralCare> findAllByTreeId(@Param("treeId") Long treeId);
}
