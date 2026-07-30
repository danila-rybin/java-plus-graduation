package ru.practicum.statsclient;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import ru.practicum.dto.EndpointHitDto;
import ru.practicum.dto.ViewStatsDto;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Реализация StatsClient на основе RestTemplate.
 * Важно: параметры дат должны быть URL-кодированы, формат строго "yyyy-MM-dd HH:mm:ss".
 */
@Slf4j
public class RestStatsClient implements StatsClient {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public RestStatsClient(RestTemplate restTemplate, String baseUrl) {
        Assert.notNull(restTemplate, "restTemplate must not be null");
        Assert.hasText(baseUrl, "baseUrl must not be blank");
        this.restTemplate = restTemplate;
        // Удаляем завершающий слэш, чтобы избежать двойных слэшей при сборке URL
        if (baseUrl.endsWith("/")) {
            this.baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        } else {
            this.baseUrl = baseUrl;
        }
    }

    @Override
    public void hit(EndpointHitDto dto) {
        Assert.notNull(dto, "EndpointHitDto must not be null");
        URI uri = UriComponentsBuilder.fromHttpUrl(baseUrl).path("/hit").build().toUri();

        log.info("Sending hit to: {}", uri);
        log.info("Hit data: app={}, uri={}, ip={}", dto.getApp(), dto.getUri(), dto.getIp());

        try {
            restTemplate.postForLocation(uri, dto);
            log.info("Hit sent successfully");
        } catch (RestClientException ex) {
            log.error("Error sending hit to {}: {}", uri, ex.getMessage());
            throw new StatsClientException("Ошибка вызова POST /hit: " + ex.getMessage(), ex);
        }
    }

    @Override
    public List<ViewStatsDto> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, boolean unique) {
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl(baseUrl)
                .path("/stats")
                .queryParam("start", start.format(FORMATTER))
                .queryParam("end", end.format(FORMATTER))
                .queryParam("unique", unique);

        if (uris != null) {
            uris.forEach(uri -> builder.queryParam("uris", uri));
        }

        URI uri = builder.build()
                .encode()
                .toUri();

        ResponseEntity<List<ViewStatsDto>> response =
                restTemplate.exchange(
                        uri,
                        HttpMethod.GET,
                        HttpEntity.EMPTY,
                        new ParameterizedTypeReference<List<ViewStatsDto>>() {
                        });

        return response.getBody() == null
                ? List.of()
                : response.getBody();
    }
}
