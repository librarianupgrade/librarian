package com.strange.knowledge;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@EnableConfigurationProperties
@ConfigurationPropertiesScan
@SpringBootApplication
public class LibraryKnowledgeApplication {

    private static LibraryKnowledgeServer server;

    @Autowired
    public void setServer(LibraryKnowledgeServer server) {
        LibraryKnowledgeApplication.server = server;
    }

    public static void main(String[] args) throws IOException {
        SpringApplication.run(LibraryKnowledgeApplication.class, args);
        server.start();
    }
}
