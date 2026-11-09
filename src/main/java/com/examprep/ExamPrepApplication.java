package com.examprep;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * ExamPrep — an AI-assisted examination preparation workspace.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableAsync
public class ExamPrepApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExamPrepApplication.class, args);
    }
}
