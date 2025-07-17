package com.spring.application.utils;

import jakarta.persistence.Column;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

public class ObjectUtils {

    public static <T> T mapToObject(Map<String, Object> map, Class<T> clazz) {
        if (map == null || clazz == null) {
            throw new IllegalArgumentException("Map and Class must not be null");
        }
        try {
            T instance = clazz.getDeclaredConstructor().newInstance();
            for (Field field : clazz.getDeclaredFields()) {
                field.setAccessible(true);
                String key = field.isAnnotationPresent(Column.class) ? field.getAnnotation(Column.class).name() : field.getName();
                if (map.containsKey(key)) {
                    Object value = map.get(key);
                    if (value != null && field.getType().isAssignableFrom(value.getClass())) {
                        field.set(instance, value);
                    } else if (value != null) {
                        field.set(instance, convertValue(value, field.getType()));
                    }
                }
            }
            return instance;
        } catch (Exception e) {
            throw new RuntimeException("Error while mapping to object: " + e.getMessage(), e);
        }
    }

    private static Object convertValue(Object value, Class<?> targetType) {
        if (targetType.isAssignableFrom(Integer.class) || targetType.isAssignableFrom(int.class)) {
            return NumberUtils.toInt(value);
        } else if (targetType.isAssignableFrom(Double.class) || targetType.isAssignableFrom(double.class)) {
            return NumberUtils.toDouble(value);
        } else if (targetType.isAssignableFrom(LocalDateTime.class)) {
            return DateUtils.toDateTime(StringUtils.toString(value), DateUtils.YMDHMS);
        } else if (targetType.isAssignableFrom(LocalDate.class)) {
            return DateUtils.toDate(StringUtils.toString(value), DateUtils.YMD);
        } else if (targetType.isAssignableFrom(Boolean.class) || targetType.isAssignableFrom(boolean.class)) {
            return Boolean.parseBoolean(StringUtils.toString(value));
        } else if (targetType.isAssignableFrom(String.class)) {
            return StringUtils.toString(value);
        }
        throw new IllegalArgumentException("Unsupported target type: " + targetType.getName());
    }
}
