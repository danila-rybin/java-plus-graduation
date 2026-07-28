//package ru.practicum.stats.config;
//
//import org.apache.catalina.connector.Connector;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
//import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//@Configuration
//public class AdditionalConnectorConfig {
//
//    @Value("${server.port:9090}")
//    private int serverPort;
//
//    @Value("${additional.connector.port:8081}")
//    private int additionalPort;
//
//    @Bean
//    public ServletWebServerFactory servletContainer() {
//        TomcatServletWebServerFactory tomcat = new TomcatServletWebServerFactory();
//        tomcat.setPort(serverPort);
//        tomcat.addAdditionalTomcatConnectors(additionalHttpConnector());
//        return tomcat;
//    }
//
//    private Connector additionalHttpConnector() {
//        Connector connector = new Connector(TomcatServletWebServerFactory.DEFAULT_PROTOCOL);
//        connector.setPort(additionalPort);
//        return connector;
//    }
//}