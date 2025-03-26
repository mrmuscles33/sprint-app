package com.spring.application.services.interfaces;

import java.util.Map;

public interface JWTService {

    String createToken(String username);

    String createToken(String username, Map<String, ?> params);

    String getUsername(String token);

    Map<String, Object> getParams(String token);

    boolean validateToken(String token);
}
