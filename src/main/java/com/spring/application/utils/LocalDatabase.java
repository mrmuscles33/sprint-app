package com.spring.application.utils;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LocalDatabase {

    @Value("${spring.datasource.url}")
    private String databaseFolder;

    public LocalDatabase(String databaseFolder) {
        this.databaseFolder = databaseFolder;
    }

    public void create(String tableName, List<String> columns) throws IOException {
        validateDatabaseFolder();
        validateTableName(tableName);
        if (columns == null || columns.isEmpty()) {
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

    public List<Map<String, Object>> query(String tableName, List<String> columns, Predicate<Map<String, Object>> where) throws IOException {
        validateDatabaseFolder();
        validateTable(tableName);

        Path filePath = getTablePath(tableName);
        try (BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                return List.of();
            }

            List<String> header = List.of(headerLine.split(";"));
            return reader.lines()
                    .map(line -> StringUtils.parseCSVLine(line, header, ";"))
                    .filter(row -> where == null || where.test(row))
                    .map(row -> filterColumns(row, columns == null || columns.isEmpty() ? header : columns))
                    .collect(Collectors.toList());
        }
    }

    public void insert(String tableName, Map<String, Object> row) throws IOException {
        insert(tableName, List.of(row));
    }

    public void insert(String tableName, List<Map<String, Object>> rows) throws IOException {
        validateDatabaseFolder();
        validateTable(tableName);

        if (rows == null || rows.isEmpty()) {
            return;
        }

        Path filePath = getTablePath(tableName);
        List<String> header;
        try (BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
            header = List.of(reader.readLine().split(";"));
        }

//        List<String> lines = rows.parallelStream()
//                .map(row -> {
//                    Map<String, Object> toWrite = header.stream().collect(Collectors.toMap(Function.identity(), row::get));
//                    return StringUtils.encodeCSVLine(toWrite, ";");
//                })
//                .toList();

        try (BufferedWriter writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8, StandardOpenOption.APPEND)) {
            for (Map<String, Object> row : rows) {
                Map<String, Object> toWrite = header.stream().collect(Collectors.toMap(Function.identity(), row::get));
                String line = StringUtils.encodeCSVLine(toWrite, ";");
                writer.write(line);
                writer.newLine();
            }
        }
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

    public boolean exists(String tableName) throws IOException {
        validateDatabaseFolder();
        validateTableName(tableName);
        return Files.exists(getTablePath(tableName));
    }

    private Map<String, Object> filterColumns(Map<String, Object> row, List<String> columns) {
        return row.entrySet().stream()
                .filter(entry -> columns.contains(entry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}