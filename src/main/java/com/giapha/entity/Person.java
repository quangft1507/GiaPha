package com.giapha.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.giapha.enums.Gender;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "person")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Person {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "NVARCHAR(255)")
    private String ho;

    @Column(columnDefinition = "NVARCHAR(255)")
    private String tenDem;

    @Column(columnDefinition = "NVARCHAR(255)")
    private String ten;

    @Column(columnDefinition = "NVARCHAR(255)")
    private String aliasName;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    private LocalDate birthDate;
    private LocalDate deathDate;

    @Builder.Default
    private Boolean isDeceased = false;

    @Column(columnDefinition = "NVARCHAR(255)")
    private String birthPlace;

    private String avatarUrl;

    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String biography;

    private String phone;

    @Column(columnDefinition = "NVARCHAR(255)")
    private String occupation;

    private Integer generation;
    private Integer birthOrder;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "family_tree_id")
    @JsonIgnore
    private FamilyTree familyTree;

    @OneToMany(mappedBy = "person", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @JsonIgnore
    private List<Relationship> relationshipsAsPerson = new ArrayList<>();

    @OneToMany(mappedBy = "relatedPerson", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @JsonIgnore
    private List<Relationship> relationshipsAsRelatedPerson = new ArrayList<>();

    @OneToMany(mappedBy = "deceasedPerson", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @JsonIgnore
    private List<FuneralCare> funeralCaresAsDeceased = new ArrayList<>();

    @OneToMany(mappedBy = "caretakerPerson", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @JsonIgnore
    private List<FuneralCare> funeralCaresAsCaretaker = new ArrayList<>();

    @OneToMany(mappedBy = "person", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @JsonIgnore
    private List<Media> mediaList = new ArrayList<>();

    @Transient
    public String getFullName() {
        StringBuilder sb = new StringBuilder();
        if (ho != null && !ho.trim().isEmpty()) {
            sb.append(ho.trim()).append(" ");
        }
        if (tenDem != null && !tenDem.trim().isEmpty()) {
            sb.append(tenDem.trim()).append(" ");
        }
        if (ten != null && !ten.trim().isEmpty()) {
            sb.append(ten.trim());
        }
        return sb.toString().trim();
    }
}
