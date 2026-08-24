package com.giapha.dto;

import com.giapha.enums.Gender;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TreeNodeDTO {
    private Long id;
    private String name;
    private String ho;
    private String tenDem;
    private String ten;
    private String aliasName;
    private Gender gender;
    private LocalDate birthDate;
    private LocalDate deathDate;
    private Boolean isDeceased;
    private String birthPlace;
    private String avatarUrl;
    private Integer generation;
    private Integer birthOrder;
    private String occupation;
    private List<TreeNodeDTO> spouses;
    private List<TreeNodeDTO> children;
    private List<FuneralCareDTO> funeralCares;
    private Long otherParentId;
}
