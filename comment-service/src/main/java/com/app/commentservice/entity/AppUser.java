package com.app.commentservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "users")
@Data
public class AppUser {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(length = 80)
    private String username;

    @Column(length = 160)
    private String email;

    @Column(length = 120)
    private String fullName;

    @Column(name = "is_active")
    private boolean isActive;
}
