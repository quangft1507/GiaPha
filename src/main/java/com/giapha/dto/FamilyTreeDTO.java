package com.giapha.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FamilyTreeDTO {
    private Long id;
    private String name;
    private String description;

    private Integer memberCount;
    private LocalDateTime createdAt;
}
