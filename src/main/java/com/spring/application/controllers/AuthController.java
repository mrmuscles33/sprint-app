package com.spring.application.controllers;

import com.spring.application.dto.UserDto;
import com.spring.application.services.impl.JWTServiceImpl;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final JWTServiceImpl jwtService;

    @GetMapping("/token")
    public ResponseEntity<String> getToken(@RequestBody UserDto user, HttpServletResponse response) {
        // TODO : Validate the user credentials
        String token = jwtService.createToken(user.username());
        // Ajouter le JWT dans l'en-tête Authorization
        response.setHeader("Authorization", "Bearer " + token);
        return ResponseEntity.ok(token);
    }
}
