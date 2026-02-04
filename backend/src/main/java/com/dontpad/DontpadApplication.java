package com.dontpad;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main Spring Boot application for dontpad backend.
 * Uses Java 25 virtual threads for high concurrency.
 */
@SpringBootApplication
public class DontpadApplication {
    private static final Logger logger = LoggerFactory.getLogger(DontpadApplication.class);

    public static void main(String[] args) {
        logger.info("Starting Dontpad application with Java {}", System.getProperty("java.version"));
        SpringApplication.run(DontpadApplication.class, args);
    }
}
