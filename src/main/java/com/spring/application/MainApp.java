package com.spring.application;

import com.spring.application.annotations.LogExecutionTime;
import com.spring.application.model.Test;
import com.spring.application.utils.DateUtils;
import com.spring.application.utils.LocalDatabase;
import com.spring.application.utils.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
            if (!localDatabase.exists(Test.class)) {
                localDatabase.create(Test.class);
            }

            localDatabase.delete(Test.class, test -> true);

			List<Test> testList = new ArrayList<>();
			for (int i = 1; i <= 10000; i++) {
				Test test = new Test();
				test.setId(i);
				test.setNom("Nom " + i);
				test.setNaissance(DateUtils.now());
				test.setActive(i % 2 == 0);
				testList.add(test);
			}

			localDatabase.insert(testList);

            List<Test> tests = localDatabase.query(
                    Test.class,
                    test -> test.getId() % 2 == 0
            );
            log.info(tests.stream().map(test -> StringUtils.toString(test.getId())).collect(Collectors.joining(", ")));
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

}
