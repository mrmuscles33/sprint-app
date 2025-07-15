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
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class LocalDatabase {

    @Value("${spring.datasource.url}")
    private String databaseFolder;

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

        Path filePath = getTablePath(tableName);
        List<T> lines = getLines(filePath, entity);
        if (lines.isEmpty()) return List.of();

        return lines.stream()
                .parallel()
                .filter(obj -> where == null || where.test(obj))
                .sorted((a, b) -> {
                    if (order == null) {
                        return 0;
                    }
                    return order.compare(a, b);
                }).collect(Collectors.toList());
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

        // Check unity constraint based on ID columns
        List<String> idColumns = getIdColumns(first.getClass());
        if(!idColumns.isEmpty()) {
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
        try (FileChannel channel = FileChannel.open(filePath, StandardOpenOption.WRITE, StandardOpenOption.APPEND)) {
            long position = channel.size();
            try (FileLock ignored = channel.lock(position, Long.MAX_VALUE - position, false)) {
                channel.position(position);
                channel.write(StandardCharsets.UTF_8.encode(String.join(System.lineSeparator(), linesToInsert) + System.lineSeparator()));
            }
        }
    }

    public <T> void delete(Class<T> entity, Predicate<T> where) throws IOException {
        String tableName = getTableName(entity);

        validateDatabaseFolder();
        validateTable(tableName);

        Path filePath = getTablePath(tableName);
        List<T> lines = getLines(filePath, entity);

        // If no lines, nothing to delete
        if (lines.isEmpty()) return;

        List<T> linesToKeep = lines.stream().parallel().filter(obj -> where != null && !where.test(obj)).toList();

        // No lines to delete
        if (linesToKeep.size() == lines.size()) {
            return;
        }

        // Truncate the file except the header
        List<String> header = getHeader(filePath);
        try (FileChannel channel = FileChannel.open(filePath, StandardOpenOption.WRITE)) {
            try (FileLock ignored = channel.lock(0, Long.MAX_VALUE, false)) {
                channel.truncate(0);
                channel.position(0);
                channel.write(StandardCharsets.UTF_8.encode(String.join(";", header) + System.lineSeparator()));
            }
        }

        // Write remaining lines
        insert(linesToKeep);
    }

    private static List<String> getHeader(Path filePath) throws IOException {
        List<String> header;
        try (BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
            header = List.of(reader.readLine().split(";"));
        }
        return header;
    }

    private static <T> List<T> getLines(Path filePath, Class<T> entity) throws IOException {
        List<String> lines;
        try (FileChannel channel = FileChannel.open(filePath, StandardOpenOption.READ)) {
            try (FileLock ignored = channel.lock(0, Long.MAX_VALUE, true)) {
                ByteBuffer buffer = ByteBuffer.allocate((int) channel.size());
                channel.read(buffer);
                buffer.flip();

                String content = StandardCharsets.UTF_8.decode(buffer).toString();
                lines = Arrays.asList(content.split(System.lineSeparator()));

            }
        }

        if(lines.isEmpty()) return List.of();

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