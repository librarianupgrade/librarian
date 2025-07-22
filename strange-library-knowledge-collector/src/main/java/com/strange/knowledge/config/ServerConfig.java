package com.strange.knowledge.config;

import cn.hutool.core.io.resource.ClassPathResource;
import com.strange.common.config.GlobalConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@Slf4j
@Component
public class ServerConfig {

    private String knowledgeRootPath;

    private String libraryListFileName;

    private Integer collectThreadNumber;

    public ServerConfig() {
        InputStream configInputStream = new ClassPathResource(GlobalConfig.CONFIGURATION_FILE_NAME).getStream();
        Properties properties = new Properties();
        try {
            properties.load(configInputStream);
            knowledgeRootPath = (String) properties.get("config.knowledge-root-path");
            libraryListFileName = (String) properties.get("config.library-list-file-name");
            collectThreadNumber = Integer.parseInt((String) properties.get("config.collect-thread-number"));
        } catch (IOException e) {
            log.error("ServerConfigInitError: ", e);
        }
    }

    public String getKnowledgeRootPath() {
        return knowledgeRootPath;
    }

    public void setKnowledgeRootPath(String knowledgeRootPath) {
        this.knowledgeRootPath = knowledgeRootPath;
    }

    public String getLibraryListFileName() {
        return libraryListFileName;
    }

    public void setLibraryListFileName(String libraryListFileName) {
        this.libraryListFileName = libraryListFileName;
    }

    public Integer getCollectThreadNumber() {
        return collectThreadNumber;
    }

    public void setCollectThreadNumber(Integer collectThreadNumber) {
        this.collectThreadNumber = collectThreadNumber;
    }
}
