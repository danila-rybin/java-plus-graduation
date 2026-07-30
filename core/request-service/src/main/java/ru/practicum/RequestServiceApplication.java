package ru.practicum;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = {
        "ru.practicum.web.request",
        "ru.practicum.web.event",
        "ru.practicum.web.user",
        "ru.practicum.category.entity",
        "ru.practicum.feign",
        "ru.practicum.exception",
        "ru.practicum.dto"
})
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "ru.practicum.feign")
public class RequestServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(RequestServiceApplication.class, args);
    }
}
