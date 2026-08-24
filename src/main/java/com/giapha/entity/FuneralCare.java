package com.giapha.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.giapha.enums.CareType;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "funeral_care")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FuneralCare {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CareType careType;

    @Column(columnDefinition = "TEXT")
    private String notes;

    private LocalDate assignedDate;

    @Builder.Default
    private Boolean isActive = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deceased_person_id")
    @JsonIgnore
    private Person deceasedPerson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "caretaker_person_id")
    @JsonIgnore
    private Person caretakerPerson;
}
