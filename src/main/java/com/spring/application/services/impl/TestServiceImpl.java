package com.spring.application.services.impl;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.spring.application.model.Test;
import com.spring.application.repository.TestRepository;
import com.spring.application.services.interfaces.TestService;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;

@Service
@AllArgsConstructor
@RequiredArgsConstructor
public class TestServiceImpl implements TestService {

    TestRepository testRepository;

    @Override
    public Optional<Test> getTest(Integer id) {
        return testRepository.findById(id);
    }
}
