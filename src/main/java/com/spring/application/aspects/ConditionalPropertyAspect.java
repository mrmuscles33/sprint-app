package com.spring.application.aspects;

import com.spring.application.annotations.ConditionalProperty;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Log4j2
@AllArgsConstructor
public class ConditionalPropertyAspect {

    private final Environment environment;

    @Before(value = "@annotation(conditionalProperty)", argNames = "joinPoint,conditionalProperty")
    public void methodSecurity(JoinPoint joinPoint, ConditionalProperty conditionalProperty) throws IllegalAccessException {
        authorize(joinPoint, conditionalProperty);
    }

    @Before(value = "@within(conditionalProperty)", argNames = "joinPoint,conditionalProperty")
    public void classSecurity(JoinPoint joinPoint, ConditionalProperty conditionalProperty) throws IllegalAccessException {
        authorize(joinPoint, conditionalProperty);
    }

    private void authorize(JoinPoint joinPoint, ConditionalProperty conditionalProperty) throws IllegalAccessException {
        String propertyValue = environment.getProperty(conditionalProperty.property());
        if (!conditionalProperty.value().equals(propertyValue)) {
            log.warn("Access to {} is not allowed in this environment", joinPoint.getSignature().getName());
            throw new IllegalAccessException("Access is not allowed in this environment");
        }
    }
}