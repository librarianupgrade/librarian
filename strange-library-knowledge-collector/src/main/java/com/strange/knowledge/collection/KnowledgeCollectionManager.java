package com.strange.knowledge.collection;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ClassPathResource;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.json.JSONUtil;
import com.strange.common.utils.IOUtil;
import lombok.NonNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

public class KnowledgeCollectionManager {

    private File libraryRootDir;

    private Integer threadNumber;

    private List<LibraryEntity> libraryEntityList;

    public KnowledgeCollectionManager( String libraryRootPath,  String libraryListFileName,  Integer threadNumber) throws IOException {
        this.libraryRootDir = FileUtil.file(libraryRootPath);
        this.threadNumber = threadNumber;

        InputStream stream = new ClassPathResource(libraryListFileName).getStream();
        String libraryListContent = IOUtil.readString(stream);
        libraryEntityList = JSONUtil.parseArray(libraryListContent).toList(LibraryEntity.class);
    }

    public void startCollect() {
        ExecutorService pool = ThreadUtil.newExecutor(threadNumber);
        try {
            List<CompletableFuture<Void>> futures = libraryEntityList.stream()
                    .map(entity -> CompletableFuture.runAsync(() -> new KnowledgeCollectionTask(
                            libraryRootDir,
                            entity.getGroupId(),
                            entity.getArtifactId())
                            .collect(), pool))
                    .toList();

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        } finally {
            pool.shutdown();
        }
    }
}
