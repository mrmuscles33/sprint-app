package com.spring.application.interceptor;

import com.spring.application.annotations.Authenticated;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.log4j.Log4j2;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;
import java.util.List;

@Log4j2
@Component
public class AuthenticationInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) throws Exception {
        if (handler instanceof HandlerMethod handlerMethod) {
            String errorMessage;
            Authenticated authenticated = handlerMethod.getMethodAnnotation(Authenticated.class);
            if (authenticated == null) {
                authenticated = handlerMethod.getBeanType().getAnnotation(Authenticated.class);
            }
            if (authenticated != null) {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
                    errorMessage = String.format("User is not authenticated to access %s.%s", handlerMethod.getBeanType().getName(), handlerMethod.getMethod().getName());
                    log.error(errorMessage);
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, errorMessage);
                    return false;
                }

                List<String> roles = Arrays.asList(authenticated.roles());
                if (!roles.isEmpty()) {
                    List<String> userRoles = authentication.getAuthorities().stream()
                            .map(GrantedAuthority::getAuthority)
                            .toList();
                    boolean hasRole = roles.stream().anyMatch(userRoles::contains);
                    if (!hasRole) {
                        errorMessage = String.format("User does not have the required roles to access %s.%s", handlerMethod.getBeanType().getName(), handlerMethod.getMethod().getName());
                        log.error(errorMessage);
                        response.sendError(HttpServletResponse.SC_FORBIDDEN, errorMessage);
                        return false;
                    }
                }
            }
        }
        return true;
    }
}
