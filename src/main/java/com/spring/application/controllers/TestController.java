package com.spring.application.controllers;

import com.spring.application.annotations.ConditionalProperty;
import com.spring.application.annotations.LogExecutionTime;
import com.spring.application.dto.UserDto;
import com.spring.application.model.Test;
import com.spring.application.services.impl.JWTServiceImpl;
import com.spring.application.services.interfaces.TestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Log4j2
@RestController
@RequiredArgsConstructor

public class TestController {

    private final TestService testService;
    private final JWTServiceImpl jwtService;

    @GetMapping("/test/{id}")
    public Test get(@PathVariable Integer id) {
        return testService.getTest(id).orElse(new Test());
    }

    @PostMapping("/test")
    @ConditionalProperty(property = "environnement", value = "dev")
    @LogExecutionTime
    public Test post(@Valid @RequestBody Test test) {
        log.info("POST /test");
        return test;
    }

    @GetMapping("/token")
    public String getToken(@RequestBody UserDto user) {
        return jwtService.createToken(user.username(), Map.of("password", user.password()));
    }

    @PostMapping("/token")
    public Map<String, ?> verifyToken(@RequestBody String token) {
        return jwtService.getParams(token);
    }
}
