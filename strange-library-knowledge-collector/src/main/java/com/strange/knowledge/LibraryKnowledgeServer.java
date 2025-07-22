package com.strange.knowledge;

import cn.hutool.extra.spring.SpringUtil;
import com.strange.knowledge.collection.KnowledgeCollectionManager;
import com.strange.knowledge.config.ServerConfig;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Import(cn.hutool.extra.spring.SpringUtil.class)
@ComponentScan(basePackages = {"cn.hutool.extra.spring"})
public class LibraryKnowledgeServer {
    public LibraryKnowledgeServer() {}

    public void start() throws IOException {
        ServerConfig serverConfig = SpringUtil.getBean(ServerConfig.class);

        new KnowledgeCollectionManager(serverConfig.getKnowledgeRootPath(),
                serverConfig.getLibraryListFileName(),
                serverConfig.getCollectThreadNumber())
                .startCollect();
    }
}
