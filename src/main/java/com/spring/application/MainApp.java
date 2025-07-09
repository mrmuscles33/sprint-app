package com.spring.application;

import com.spring.application.annotations.LogExecutionTime;
import com.spring.application.utils.DateUtils;
import com.spring.application.utils.LocalDatabase;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@SpringBootApplication
@RequiredArgsConstructor
@Log4j2
public class MainApp implements CommandLineRunner {

	private final LocalDatabase localDatabase;

	public static void main(String[] args) {
		SpringApplication.run(MainApp.class, args);
	}

	@Override
	@LogExecutionTime
	public void run(String... args) {
        try {
			if(!localDatabase.exists("TEST")) {
				localDatabase.create("TEST", List.of("ID", "NAME", "AGE"));
			}
			List<Map<String, Object>> result = localDatabase.query(
					"TEST",
					List.of("ID", "NAME", "AGE"),
					row -> row.get("ID") != null && row.get("ID") instanceof Integer id && id == 1
			);
			if(result.isEmpty()) {
				localDatabase.insert("TEST", Map.of("ID", 1, "NAME", "John Doe", "AGE", DateUtils.now()));
			}
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

}
