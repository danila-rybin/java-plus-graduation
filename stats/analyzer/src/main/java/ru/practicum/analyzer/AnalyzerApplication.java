package ru.practicum.analyzer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableDiscoveryClient
@ComponentScan(basePackages = {
        "ru.practicum.analyzer",
        "ru.practicum.analyzer.config",
        "ru.practicum.analyzer.consumer",
        "ru.practicum.analyzer.service",
        "ru.practicum.analyzer.repository",
        "ru.practicum.analyzer.model",
        "ru.practicum.analyzer.grpc",
        "ru.practicum.analyzer.serialization"
})
@EnableJpaRepositories(basePackages = "ru.practicum.analyzer.repository")
@EntityScan(basePackages = "ru.practicum.analyzer.model")
public class AnalyzerApplication {

    public static void main(String[] args) {
        SpringApplication.run(AnalyzerApplication.class, args);
    }
}