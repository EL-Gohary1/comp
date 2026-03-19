package com.contractdetector;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Entry point for the ContractDetector system.
 *
 * <p>ContractDetector monitors live API responses captured during RestAssured test
 * execution, infers their JSON schemas, detects schema version drift, and performs
 * automated impact analysis on affected POJOs and test classes.
 */
@Slf4j
@EnableAsync
@SpringBootApplication
public class ContractDetectorApplication {

    public static void main(String[] args) {
        SpringApplication.run(ContractDetectorApplication.class, args);
        log.info("ContractDetector started successfully.");
    }
}
