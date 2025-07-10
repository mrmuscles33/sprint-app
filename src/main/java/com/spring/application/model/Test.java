package com.spring.application.model;

import com.spring.application.annotations.Mail;
import com.spring.application.annotations.Phone;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Past;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "TEST")
public class Test {

    @Id
    @Column(name = "ID")
    private int id;

    @Past
    @Column(name = "NAISSANCE")
    private LocalDateTime naissance;

    @NotEmpty
    @Column(name = "NOM")
    private String nom;

    @Mail
    @Column(name = "EMAIL")
    private String email;

    @Phone
    @Column(name = "PHONE")
    private String phone;

    @Column(name = "ACTIVE")
    private boolean active;
}
