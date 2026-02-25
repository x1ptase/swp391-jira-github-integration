package com.swp391.backend.entity;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@NoArgsConstructor
@Data
@Entity
@Table(name="StudentGroup")
public class StudentGroup {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="group_id" , nullable = false , updatable = false)
    private Long groupId;

    @NotBlank
    @JsonAlias("class_code")
    @Column(name="class_code" , nullable = false , length = 50 )
    private String classCode;

    @NotBlank
    @JsonAlias("group_name")
    @Column(name="group_name" ,  nullable = false , length = 120 )
    private String groupName;

    @NotBlank
    @JsonAlias("course_code")
    @Column(name="course_code" , nullable = false , length = 30)
    private String courseCode;

    @NotBlank
    @Column(name="semester" , nullable = false , length = 30)
    private String semester;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Column(name="created_at" , nullable = false  , updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }


}
