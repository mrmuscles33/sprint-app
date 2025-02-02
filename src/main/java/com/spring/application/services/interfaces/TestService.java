package com.spring.application.services.interfaces;

import com.spring.application.model.Test;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public interface TestService {

    Optional<Test> getTest(Integer id);
}
