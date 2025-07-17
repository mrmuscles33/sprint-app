package com.spring.application.utils;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.NonUniqueResultException;
import jakarta.persistence.Table;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class LocalDatabase {

    @Value("${spring.datasource.url}")
    private String databaseFolder;
    private static final ConcurrentHashMap<String, ReadWriteLock> tableLocks = new ConcurrentHashMap<>();

    public LocalDatabase(String databaseFolder) {
        this.databaseFolder = databaseFolder;
    }

    public <T> void create(Class<T> entity) throws IOException {
        String tableName = getTableName(entity);
        List<String> columns = getColumns(entity);

        validateDatabaseFolder();
        validateTableName(tableName);
        if (columns.isEmpty()) {
            throw new IllegalArgumentException("Columns cannot be null or empty");
        }

        Path filePath = getTablePath(tableName);
        if (Files.exists(filePath)) {
            throw new IOException("Table " + tableName + " already exists in the database folder: " + databaseFolder);
        }

        try (BufferedWriter writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW)) {
            writer.write(String.join(";", columns));
            writer.newLine();
        }
    }

    public <T> List<T> query(Class<T> entity) throws IOException {
        return query(entity, null);
    }

    public <T> List<T> query(Class<T> entity, Predicate<T> where) throws IOException {
        return query(entity, where, null);
    }

    public <T> List<T> query(Class<T> entity, Predicate<T> where, Comparator<T> order) throws IOException {
        String tableName = getTableName(entity);

        validateDatabaseFolder();
        validateTable(tableName);

        ReadWriteLock lock = tableLocks.computeIfAbsent(tableName, k -> new ReentrantReadWriteLock());
        lock.readLock().lock();
        List<T> result = new ArrayList<>();
        try {
            Path filePath = getTablePath(tableName);
            List<T> lines = getLines(filePath, entity);
            if (lines.isEmpty()) return List.of();

            result = lines.stream()
                    .parallel()
                    .filter(obj -> where == null || where.test(obj))
                    .sorted((a, b) -> {
                        if (order == null) {
                            return 0;
                        }
                        return order.compare(a, b);
                    }).collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
        return result;
    }

    public <T> void insert(T entity) throws IOException {
        insert(List.of(entity));
    }

    public <T> void insert(List<T> entities) throws IOException {
        if (entities == null || entities.isEmpty()) {
            return;
        }
        T first = entities.getFirst();
        String tableName = getTableName(first);

        validateDatabaseFolder();
        validateTable(tableName);

        ReadWriteLock lock = tableLocks.computeIfAbsent(tableName, k -> new ReentrantReadWriteLock());
        lock.writeLock().lock();
        try {
            // Check unity constraint based on ID columns
            List<String> idColumns = getIdColumns(first.getClass());
            if (!idColumns.isEmpty()) {
                Set<String> existingIdValues = query(first.getClass(), null).stream().map(entity -> getHashId(entity, idColumns)).collect(Collectors.toSet());
                List<String> idToInsert = entities.stream().map(entity -> getHashId(entity, idColumns)).toList();
                if (idToInsert.stream().distinct().count() != idToInsert.size() || existingIdValues.stream().anyMatch(idToInsert::contains)) {
                    throw new NonUniqueResultException("Some entities already exist in the table " + tableName + " and were not inserted.");
                }
            }

            // Transform entity to CSV line
            Path filePath = getTablePath(tableName);
            List<String> header = getHeader(filePath);
            List<String> linesToInsert = entities.stream().map(entity -> {
                LinkedHashMap<String, Object> toWrite;
                toWrite = new LinkedHashMap<>();
                for (String col : header) {
                    toWrite.put(col, getValue(entity, col));
                }
                return StringUtils.encodeCSVLine(toWrite, ";");
            }).toList();

            // Write lines into table
            Files.write(filePath, linesToInsert, StandardCharsets.UTF_8, StandardOpenOption.WRITE, StandardOpenOption.APPEND);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public <T> void delete(T entity) throws IOException {
        delete(List.of(entity));
    }

    public <T> void delete(List<T> entities) throws IOException {
        if (entities == null || entities.isEmpty()) {
            return;
        }
        T first = entities.getFirst();
        List<String> idColumns = getIdColumns(first.getClass());
        List<String> hashIds = idColumns.isEmpty() ? List.of() :
                entities.stream()
                        .map(entity -> getHashId(entity, idColumns))
                        .distinct()
                        .toList();
        delete(first.getClass(), obj -> {
            if (idColumns.isEmpty()) {
                return entities.stream().anyMatch(e -> e.equals(obj));
            }
            return hashIds.contains(getHashId(obj, idColumns));
        });
    }

    public <T> void delete(Class<T> entity, Predicate<T> where) throws IOException {
        String tableName = getTableName(entity);

        validateDatabaseFolder();
        validateTable(tableName);

        ReadWriteLock lock = tableLocks.computeIfAbsent(tableName, k -> new ReentrantReadWriteLock());
        lock.writeLock().lock();
        try {
            Path filePath = getTablePath(tableName);
            List<T> lines = getLines(filePath, entity);

            // If no lines, nothing to delete
            if (lines.isEmpty()) return;

            List<T> linesToKeep = where == null ? List.of() : lines.stream().parallel().filter(obj -> !where.test(obj)).toList();

            // No lines to delete
            if (linesToKeep.size() == lines.size()) {
                return;
            }

            // Truncate the file except the header
            List<String> header = getHeader(filePath);
            Files.write(filePath, List.of(String.join(";", header)), StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING);

            // Write remaining lines
            insert(linesToKeep);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public <T> void update(T entity) throws IOException {
        update(List.of(entity));
    }

    public <T> void update(T entity, Predicate<T> where) throws IOException {
        update(List.of(entity), where);
    }

    public <T> void update(List<T> entities) throws IOException {
        if (entities == null || entities.isEmpty()) {
            return;
        }
        T first = entities.getFirst();
        List<String> idColumns = getIdColumns(first.getClass());
        if (idColumns.isEmpty()) {
            throw new IllegalArgumentException("Entity must have at least one ID column to perform an update");
        }
        ReadWriteLock lock = tableLocks.computeIfAbsent(getTableName(first), k -> new ReentrantReadWriteLock());
        lock.writeLock().lock();
        try {
            delete(entities);
            insert(entities);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public <T> void update(List<T> entities, Predicate<T> where) throws IOException {
        if (entities == null || entities.isEmpty()) {
            return;
        }
        delete(safeCast(entities.getFirst().getClass()), where);
        insert(entities);
    }

    @SuppressWarnings("unchecked")
    private <T> Class<T> safeCast(Class<?> clazz) {
        try {
            return (Class<T>) clazz;
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("Invalid class type provided: " + clazz.getName(), e);
        }
    }

    private static List<String> getHeader(Path filePath) throws IOException {
        List<String> header;
        try (BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
            header = List.of(reader.readLine().split(";"));
        }
        return header;
    }

    private static <T> List<T> getLines(Path filePath, Class<T> entity) throws IOException {
        String tableName = getTableName(entity);
        List<String> lines = Files.readAllLines(filePath, StandardCharsets.UTF_8);

        if (lines.isEmpty()) return List.of();

        List<String> header = List.of(lines.getFirst().split(";"));
        return lines.stream()
                .parallel()
                .skip(1)
                .map(line -> StringUtils.parseCSVLine(line, header, ";"))
                .map(map -> ObjectUtils.mapToObject(map, entity))
                .toList();
    }

    private static <T> String getHashId(T entity, List<String> idColumns) {
        return idColumns.stream()
                .map(col -> Objects.requireNonNullElse(getValue(entity, col), "").toString())
                .collect(Collectors.joining(";"));
    }

    private static <T> Object getValue(T entity, String col) {
        Field field = Stream.of(entity.getClass().getDeclaredFields())
                .filter(f -> f.isAnnotationPresent(Column.class) && f.getAnnotation(Column.class).name().equals(col)).findFirst()
                .orElse(null);
        if (field == null) return null;
        field.setAccessible(true);
        try {
            return field.get(entity);
        } catch (IllegalAccessException e) {
            return null;
        }
    }

    private static List<String> getColumns(Object entity) {
        return getColumns(entity.getClass());
    }

    private static <T> List<String> getColumns(Class<T> entity) {
        return Arrays.stream(entity.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(Column.class))
                .map(field -> field.getAnnotation(Column.class).name())
                .collect(Collectors.toList());
    }

    private static <T> List<String> getIdColumns(Class<T> entity) {
        return Arrays.stream(entity.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(Column.class) && field.isAnnotationPresent(Id.class))
                .map(field -> field.getAnnotation(Column.class).name())
                .collect(Collectors.toList());
    }

    private static String getTableName(Object entity) {
        return getTableName(entity.getClass());
    }

    private static <T> String getTableName(Class<T> entity) {
        Table tableAnnotation = entity.getAnnotation(Table.class);
        if (tableAnnotation == null || tableAnnotation.name().isEmpty()) {
            throw new IllegalArgumentException("Entity class must have a @Table annotation with a valid name");
        }
        return tableAnnotation.name();
    }

    private Path getTablePath(String tableName) {
        return Paths.get(databaseFolder, tableName.trim() + ".csv");
    }

    private void validateDatabaseFolder() {
        if (StringUtils.isEmpty(this.databaseFolder)) {
            throw new IllegalArgumentException("Database folder cannot be null or empty");
        }
    }

    private void validateTableName(String tableName) {
        if (StringUtils.isEmpty(tableName)) {
            throw new IllegalArgumentException("Table name cannot be null or empty");
        }
    }

    private void validateTable(String tableName) throws IOException {
        if (!exists(tableName)) {
            throw new IllegalArgumentException("Table " + tableName + " does not exist in the database folder: " + databaseFolder);
        }
    }

    public <T> boolean exists(Class<T> entity) throws IOException {
        return exists(getTableName(entity));
    }

    public boolean exists(String tableName) throws IOException {
        validateDatabaseFolder();
        validateTableName(tableName);
        return Files.exists(getTablePath(tableName));
    }
}