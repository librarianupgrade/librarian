package com.strange.code.format.formatter;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RuntimeUtil;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.stream.Stream;

@Slf4j
public class ProjectCodeFormatter {

    private final File projectDir;

    public ProjectCodeFormatter( File projectDir) {
        if (!projectDir.isDirectory()) {
            throw new IllegalArgumentException(
                    String.format(
                            "Invalid project directory: '%s'. It must point to an existing directory.",
                            projectDir.getAbsolutePath()
                    )
            );
        }
        this.projectDir = projectDir;
    }

    public void startFormat() {
        List<Path> javaFilePathList = getAllJavaFiles();
        if (CollUtil.isNotEmpty(javaFilePathList)) {
            int coreNumber = RuntimeUtil.getProcessorCount();
            ThreadPoolExecutor pool = new ThreadPoolExecutor(
                    coreNumber,
                    coreNumber * 2,
                    coreNumber * 60L, TimeUnit.SECONDS,
                    new LinkedBlockingQueue<>()
            );
            try {
                List<? extends Future<?>> futures = javaFilePathList.stream()
                        .map(p -> pool.submit(() -> new FileCodeFormatter(p).startFormat(p)))
                        .toList();

                for (Future<?> f : futures) {
                    f.get();
                }
            } catch (ExecutionException | InterruptedException e) {
                log.error("CodeFormatError: ", e);
            } finally {
                pool.shutdown();
            }
        }
    }

    private List<Path> getAllJavaFiles() {
        Set<String> IGNORED_FILE_NAMES = Set.of("module-info.java", "package-info.java");
        List<Path> javaFiles = null;
        try (Stream<Path> s = Files.walk(projectDir.toPath())) {
            javaFiles = s.filter(p -> {
                        if (p.toString().endsWith(".java")) {
                            String fileName = FileUtil.getName(p.toFile());
                            return !IGNORED_FILE_NAMES.contains(fileName);
                        } else {
                            return false;
                        }
                    })
                    .toList();
        } catch (IOException e) {
            log.error("CodeFormatError: ", e);
        }
        return javaFiles;
    }
}
