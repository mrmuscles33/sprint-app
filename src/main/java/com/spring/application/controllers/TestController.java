package com.spring.application.controllers;

import com.spring.application.annotations.Authenticated;
import com.spring.application.annotations.ConditionalProperty;
import com.spring.application.annotations.LogExecutionTime;
import com.spring.application.model.Test;
import com.spring.application.services.interfaces.TestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

@Log4j2
@RestController
@RequiredArgsConstructor
@ConditionalOnProperty(name = "environment", havingValue = "dev")
public class TestController {

    private final TestService testService;

    @GetMapping("/test/{id}")
    public Test get(@PathVariable Integer id) {
        return testService.getTest(id).orElse(new Test());
    }

    @PostMapping("/test")
    @LogExecutionTime
    public Test post(@Valid @RequestBody Test test) {
        log.info("POST /test");
        return test;
    }

    @GetMapping("/ping")
    @Authenticated(roles = {"ROLE_ADMIN"})
    @ConditionalProperty(property = "environment", value = "dev")
    public String ping() {
        return "pong";
    }
}
