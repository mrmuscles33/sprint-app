package com.spring.application.services.impl;

import com.spring.application.model.Test;
import com.spring.application.repository.TestRepository;
import com.spring.application.services.interfaces.TestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class TestServiceImpl implements TestService {

    @Autowired
    TestRepository testRepository;

    @Override
    public Optional<Test> getTest(Integer id) {
        return testRepository.findById(id);
    }
}
