package ru.practicum.statsclient.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "stats.client")
public class StatsClientProperties {

    /**
     * Базовый URL сервиса статистики, без завершающего слэша: http://host:port
     */
    private String baseUrl = "http://localhost:9090";

    /**
     * Таймаут установки соединения (мс).
     */
    private Integer connectTimeoutMillis = 5000;

    /**
     * Таймаут чтения данных (мс).
     */
    private Integer readTimeoutMillis = 5000;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public Integer getConnectTimeoutMillis() {
        return connectTimeoutMillis;
    }

    public void setConnectTimeoutMillis(Integer connectTimeoutMillis) {
        this.connectTimeoutMillis = connectTimeoutMillis;
    }

    public Integer getReadTimeoutMillis() {
        return readTimeoutMillis;
    }

    public void setReadTimeoutMillis(Integer readTimeoutMillis) {
        this.readTimeoutMillis = readTimeoutMillis;
    }
}
