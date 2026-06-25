package com.example.user.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@Entity
@Table(name = "patients")
public class Patient {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String name;
    @Column(nullable = false, unique = true)
    private String email;
    private String password;
    private String phoneNumber;
    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;
    @ManyToMany(mappedBy = "patients")
    @Builder.Default
    private List<Personal> doctors = new ArrayList<>();
}
