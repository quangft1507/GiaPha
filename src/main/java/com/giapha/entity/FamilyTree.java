package com.giapha.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "family_tree")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FamilyTree {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String backgroundImage;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();



    @OneToMany(mappedBy = "familyTree", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Person> persons = new ArrayList<>();
}
