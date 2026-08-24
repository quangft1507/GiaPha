package com.giapha.dto;

import com.giapha.enums.CareType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FuneralCareDTO {
    private Long id;
    private Long deceasedPersonId;
    private String deceasedPersonName;
    private Long caretakerPersonId;
    private String caretakerPersonName;
    private CareType careType;
    private String careTypeDisplayName;
    private String notes;
    private LocalDate assignedDate;
    private Boolean isActive;
}
