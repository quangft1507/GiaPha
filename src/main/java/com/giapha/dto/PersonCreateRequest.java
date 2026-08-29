package com.giapha.dto;

import com.giapha.enums.Gender;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.time.LocalDate;

@Data
public class PersonCreateRequest {
    private String ho;
    private String tenDem;
    @NotBlank(message = "Tên không được để trống")
    private String ten;
    private String aliasName;
    private Gender gender;
    private LocalDate birthDate;
    private LocalDate deathDate;
    private Boolean isDeceased;
    private String birthPlace;
    private String biography;
    private String phone;
    private String occupation;
    private Integer birthOrder;
    private Long caretakerId;
    private Long parentId;
    private Long otherParentId;
    private Long spouseId;
    private Long treeId;
}
