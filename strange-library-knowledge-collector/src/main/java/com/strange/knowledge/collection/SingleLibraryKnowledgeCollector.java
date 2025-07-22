package com.strange.knowledge.collection;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ClassPathResource;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@Slf4j
public class SingleLibraryKnowledgeCollector {

    private static final String PROPERTY_FILE_NAME = "application.properties";

    private static final String KNOWLEDGE_DIRECTORY_PATH;

    private String groupId;

    private String artifactId;

    static {
        InputStream stream = new ClassPathResource(PROPERTY_FILE_NAME).getStream();
        try {
            Properties properties = new Properties();
            properties.load(stream);
            KNOWLEDGE_DIRECTORY_PATH = (String) properties.get("config.knowledge-root-path");
        } catch (IOException e) {
            log.error("ConfigPropertyInitError: ", e);
            throw new RuntimeException(e);
        }
    }

    public SingleLibraryKnowledgeCollector(String groupId, String artifactId) {
        this.groupId = groupId;
        this.artifactId = artifactId;
    }

    public void collect() {
        KnowledgeCollectionTask collectionTask = new KnowledgeCollectionTask(FileUtil.file(KNOWLEDGE_DIRECTORY_PATH), groupId, artifactId);
        collectionTask.collect();
    }


    public static void main(String[] args) {
        SingleLibraryKnowledgeCollector collector = new SingleLibraryKnowledgeCollector("org.locationtech.jts", "jts-core");
        collector.collect();
        log.info("✅ Collection Finish");
    }
}
