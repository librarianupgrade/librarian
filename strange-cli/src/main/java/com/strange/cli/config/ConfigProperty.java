package com.strange.cli.config;

import cn.hutool.core.io.resource.ClassPathResource;
import com.strange.common.config.GlobalConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@Slf4j
@Component
public class ConfigProperty {

    private String gptAccessToken;

    private String fixSpaceDirPath;

    private String libraryDatabasePath;

    private Integer maxRetryCount;

    public ConfigProperty() {
        InputStream configInputStream = new ClassPathResource(GlobalConfig.CONFIGURATION_FILE_NAME).getStream();
        Properties properties = new Properties();
        try {
            properties.load(configInputStream);
            gptAccessToken = (String) properties.get("config.gpt-access-token");
            fixSpaceDirPath = (String) properties.get("config.fix-space-dir");
            libraryDatabasePath = (String) properties.get("config.library-database-path");
            maxRetryCount = Integer.parseInt((String) properties.get("config.llm.max-retry-count"));
        } catch (IOException e) {
            log.error("ConfigPropertyInitError: ", e);
        }
    }

    public void setGptAccessToken(String gptAccessToken) {
        this.gptAccessToken = gptAccessToken;
    }

    public void setFixSpaceDirPath(String fixSpaceDirPath) {
        this.fixSpaceDirPath = fixSpaceDirPath;
    }

    public void setLibraryDatabasePath(String libraryDatabasePath) {
        this.libraryDatabasePath = libraryDatabasePath;
    }

    public void setMaxRetryCount(Integer maxRetryCount) {
        this.maxRetryCount = maxRetryCount;
    }

    public String getGptAccessToken() {
        return gptAccessToken;
    }

    public String getFixSpaceDirPath() {
        return fixSpaceDirPath;
    }

    public String getLibraryDatabasePath() {
        return libraryDatabasePath;
    }

    public Integer getMaxRetryCount() {
        return maxRetryCount;
    }
}
