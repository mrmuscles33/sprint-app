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

            // Insertion initiale
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

            int threadCount = 8;
            List<Thread> threads = new ArrayList<>();
            long start = System.currentTimeMillis();

            for (int t = 0; t < threadCount; t++) {
                Thread thread = new Thread(() -> {
                    try {
                        // Query
                        List<Test> evenTests = localDatabase.query(Test.class, test -> test.getId() % 2 == 0);
                        Thread.sleep(10 + (int) (Math.random() * 91)); // Pause 10-100ms

                        // Insert
                        Test newTest = new Test();
                        newTest.setId((int) (Math.random() * 100000) + 10001);
                        newTest.setNom("Threaded");
                        newTest.setNaissance(DateUtils.now());
                        newTest.setActive(true);
                        localDatabase.insert(newTest);
                        Thread.sleep(10 + (int) (Math.random() * 91));

                        // Update
                        if (!evenTests.isEmpty()) {
                            Test toUpdate = evenTests.getFirst();
                            toUpdate.setNom("Updated by thread");
                            localDatabase.update(toUpdate);
                        }
                        Thread.sleep(10 + (int) (Math.random() * 91));

                        // Delete
                        localDatabase.delete(Test.class, test -> test.getId() % 100 == 0);
                    } catch (IOException | InterruptedException e) {
                        log.error("Erreur dans le thread: " + e.getMessage());
                        Thread.currentThread().interrupt();
                    }
                });
                threads.add(thread);
                thread.start();
            }

            for (Thread thread : threads) {
                thread.join();
            }

            long end = System.currentTimeMillis();
            log.info("Test concurrent terminé en " + (end - start) + " ms");

            // Vérification finale
            long count = localDatabase.query(Test.class, t -> true).size();
            log.info("Nombre d'enregistrements finaux: " + count);

        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

}
