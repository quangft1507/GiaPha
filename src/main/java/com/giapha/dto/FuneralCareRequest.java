package com.giapha.dto;

import com.giapha.enums.CareType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;

@Data
public class FuneralCareRequest {
    @NotNull(message = "Caretaker ID cannot be null")
    private Long caretakerPersonId;
    @NotNull(message = "Care type cannot be null")
    private CareType careType;
    private String notes;
    private LocalDate assignedDate;
}
