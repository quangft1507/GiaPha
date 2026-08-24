package com.giapha.dto;

import com.giapha.enums.Gender;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersonDTO {
    private Long id;
    private String ho;
    private String tenDem;
    private String ten;
    private String fullName;
    private String aliasName;
    private Gender gender;
    private LocalDate birthDate;
    private LocalDate deathDate;
    private Boolean isDeceased;
    private String birthPlace;
    private String avatarUrl;
    private String biography;
    private String phone;
    private String occupation;
    private Integer generation;
    private Integer birthOrder;
    private Long familyTreeId;
    
    // New fields for table view and caretaker
    private Long parentId;
    private Long otherParentId;
    private String fatherName;
    private String motherName;
    private Long caretakerId;
    private String caretakerName;
    private LocalDateTime createdAt;
}
