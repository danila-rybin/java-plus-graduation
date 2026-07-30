package ru.practicum.statsclient.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;
import ru.practicum.statsclient.RestStatsClient;
import ru.practicum.statsclient.StatsClient;

import java.time.Duration;

@AutoConfiguration
@ConditionalOnClass(RestTemplate.class)
@EnableConfigurationProperties(StatsClientProperties.class)
public class StatsClientAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public RestTemplate statsRestTemplate(RestTemplateBuilder builder, StatsClientProperties props) {
        return builder
                .setConnectTimeout(Duration.ofMillis(props.getConnectTimeoutMillis()))
                .setReadTimeout(Duration.ofMillis(props.getReadTimeoutMillis()))
                .build();
    }

    @Bean
    @ConditionalOnMissingBean(StatsClient.class)
    public StatsClient statsClient(RestTemplate statsRestTemplate, StatsClientProperties props) {
        return new RestStatsClient(statsRestTemplate, props.getBaseUrl());
    }
}
