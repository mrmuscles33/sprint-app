package com.spring.application.model;

import com.spring.application.annotations.Mail;
import com.spring.application.annotations.Phone;
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
    private int id;

    @Past
    private LocalDateTime naissance;

    @NotEmpty
    private String nom;

    @Mail
    private String email;

    @Phone
    private String phone;
}
