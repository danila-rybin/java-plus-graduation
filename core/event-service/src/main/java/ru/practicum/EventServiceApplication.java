package ru.practicum;

import net.devh.boot.grpc.client.autoconfigure.GrpcClientAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import ru.practicum.validation.CategoryValidator;

@SpringBootApplication
@ComponentScan(
        basePackages = {
                "ru.practicum.web",
                "ru.practicum.feign",
                "ru.practicum.exception",
                "ru.practicum.dto",
                "ru.practicum.validation"
        },
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = CategoryValidator.class)
        }
)
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "ru.practicum.feign")
@Import(GrpcClientAutoConfiguration.class)
@EntityScan(basePackages = {
        "ru.practicum.web.category.entity",
        "ru.practicum.web.event.entity",
        "ru.practicum.web.admin.entity",
        "ru.practicum.web.user.entity"
})
@EnableJpaRepositories(basePackages = {
        "ru.practicum.web.category.repository",
        "ru.practicum.web.event.repository",
        "ru.practicum.web.admin.repository",
        "ru.practicum.web.user.repository"
})
public class EventServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(EventServiceApplication.class, args);
    }
}