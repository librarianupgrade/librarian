package com.strange.knowledge.collection;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.file.FileWriter;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.ZipUtil;
import cn.hutool.http.Header;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@Slf4j
public class KnowledgeCollectionTask {
    private static final String MAVEN_CENTRAL_URL = "https://repo1.maven.org/maven2";
    private static final List<String> SUFFIX_LIST = Arrays.asList(".jar", ".bundle");
    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122 Safari/537.36";
    private static final String POM_DIRECTOR_NAME = "pom";
    private static final String JAR_DIRECTOR_NAME = "jar";
    private static final String SOURCE_DIRECTOR_NAME = "source";
    private static final String JAVADOC_DIRECTOR_NAME = "javadoc";
    private static final String TESTCASE_DIRECTOR_NAME = "testcase";
    private static final String VERSION_LIST_FILE_NAME = "version_list.json";
    private static final String DIR_NAME_SCHEMA = "{}__gt__{}";
    private static final String JAVADOC_CODE_DIR_NAME = "java_doc";
    private static final String SOURCE_CODE_DIR_NAME = "source_code";
    private static final String TESTCASE_CODE_DIR_NAME = "testcase_source_code";
    private Path rootDir;
    private Path jarDir;
    private Path javaDocDir;
    private Path sourceDir;
    private Path testcaseDir;
    private Path pomDir;

    private static final int TIMEOUT_MS = 90_000;
    private static final int RETRY_CNT = 3;

    private final String groupId;
    private final String artifactId;
    private final String groupUrlPath;
    private final String baseUrl;
    private final String folderName;
    private final List<String> versions;

    public KnowledgeCollectionTask( File libraryRootDir,  String groupId,  String artifactId) {
        this.rootDir = libraryRootDir.toPath();
        this.jarDir = rootDir.resolve(JAR_DIRECTOR_NAME);
        this.javaDocDir = rootDir.resolve(JAVADOC_DIRECTOR_NAME);
        this.pomDir = rootDir.resolve(POM_DIRECTOR_NAME);
        this.sourceDir = rootDir.resolve(SOURCE_DIRECTOR_NAME);
        this.testcaseDir = rootDir.resolve(TESTCASE_DIRECTOR_NAME);

        this.groupId = groupId;
        this.artifactId = artifactId;
        this.groupUrlPath = groupId.replace('.', '/');
        this.baseUrl = StrUtil.format("{}/{}/{}", MAVEN_CENTRAL_URL, groupUrlPath, artifactId);
        this.folderName = StrUtil.format(DIR_NAME_SCHEMA, groupId, artifactId);
        this.versions = fetchVersions();

        if (CollUtil.isEmpty(this.versions)) {
            log.warn("Cannot fetch any available versions from Maven Central: " + groupId + ":" + artifactId);
        }

        prepareDirectory();
    }

    private void prepareDirectory() {
        // prepare root directory
        if (!FileUtil.exist(rootDir.toFile())) {
            FileUtil.mkdir(rootDir);
        }

        // prepare pom directory
        if (!FileUtil.exist(pomDir.toFile())) {
            FileUtil.mkdir(pomDir);
        }
        for (String version : versions) {
            Path destDir = pomDir.resolve(folderName).resolve(version);
            if (!FileUtil.exist(destDir.toFile())) {
                FileUtil.mkdir(destDir);
            }
        }
        File versionListFile = FileUtil.file(pomDir.toFile().getAbsolutePath(), folderName, VERSION_LIST_FILE_NAME);
        String versionListContent = JSONUtil.toJsonPrettyStr(versions);
        new FileWriter(versionListFile).write(versionListContent);

        // prepare jar directory
        if (!FileUtil.exist(jarDir.toFile())) {
            FileUtil.mkdir(jarDir);
        }
        for (String version : versions) {
            Path destDir = jarDir.resolve(folderName).resolve(version);
            if (!FileUtil.exist(destDir.toFile())) {
                FileUtil.mkdir(destDir);
            }
        }
        versionListFile = FileUtil.file(jarDir.toFile().getAbsolutePath(), folderName, VERSION_LIST_FILE_NAME);
        versionListContent = JSONUtil.toJsonPrettyStr(versions);
        new FileWriter(versionListFile).write(versionListContent);

        // prepare javadoc directory
        if (!FileUtil.exist(javaDocDir.toFile())) {
            FileUtil.mkdir(javaDocDir);
        }
        for (String version : versions) {
            Path destDir = javaDocDir.resolve(folderName).resolve(version);
            if (!FileUtil.exist(destDir.toFile())) {
                FileUtil.mkdir(destDir);
            }
        }
        versionListFile = FileUtil.file(javaDocDir.toFile().getAbsolutePath(), folderName, VERSION_LIST_FILE_NAME);
        versionListContent = JSONUtil.toJsonPrettyStr(versions);
        new FileWriter(versionListFile).write(versionListContent);

        // prepare source directory
        if (!FileUtil.exist(sourceDir.toFile())) {
            FileUtil.mkdir(sourceDir);
        }
        for (String version : versions) {
            Path destDir = sourceDir.resolve(folderName).resolve(version);
            if (!FileUtil.exist(destDir.toFile())) {
                FileUtil.mkdir(destDir);
            }
        }
        versionListFile = FileUtil.file(sourceDir.toFile().getAbsolutePath(), folderName, VERSION_LIST_FILE_NAME);
        versionListContent = JSONUtil.toJsonPrettyStr(versions);
        new FileWriter(versionListFile).write(versionListContent);

        // prepare testcase directory
        if (!FileUtil.exist(testcaseDir.toFile())) {
            FileUtil.mkdir(testcaseDir);
        }
        for (String version : versions) {
            Path destDir = testcaseDir.resolve(folderName).resolve(version);
            if (!FileUtil.exist(destDir.toFile())) {
                FileUtil.mkdir(destDir);
            }
        }
        versionListFile = FileUtil.file(testcaseDir.toFile().getAbsolutePath(), folderName, VERSION_LIST_FILE_NAME);
        versionListContent = JSONUtil.toJsonPrettyStr(versions);
        new FileWriter(versionListFile).write(versionListContent);
    }

    public void collect() {
        ExecutorService executorService = ThreadUtil.newExecutor(5);
        try {
            List<CompletableFuture<Void>> futures = Arrays.asList(
                    CompletableFuture.runAsync(this::downloadPom, executorService),
                    CompletableFuture.runAsync(this::downloadJar, executorService),
                    CompletableFuture.runAsync(this::downloadSource, executorService),
                    CompletableFuture.runAsync(this::downloadJavadoc, executorService),
                    CompletableFuture.runAsync(this::downloadTestcase, executorService)
            );
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        } finally {
            executorService.shutdown();
        }
    }

    private void downloadPom() {
        for (String version : versions) {
            String pomUrl = StrUtil.format("{}/{}/{}-{}.pom", baseUrl, version, artifactId, version);
            Path destDir = pomDir.resolve(folderName).resolve(version);
            downloadWithRetry(pomUrl, destDir);
        }
    }

    private void downloadJar() {
        for (String version : versions) {
            String prefix = StrUtil.format("{}/{}/{}-{}", baseUrl, version, artifactId, version);
            Path destDir = jarDir.resolve(folderName).resolve(version);
            tryAlternatives(prefix, destDir, false, null);
        }
    }

    private void downloadSource() {
        for (String version : versions) {
            String prefix = StrUtil.format("{}/{}/{}-{}-sources", baseUrl, version, artifactId, version);
            Path destDir = sourceDir.resolve(folderName).resolve(version);
            tryAlternatives(prefix, destDir, true, SOURCE_CODE_DIR_NAME);
        }
    }

    private void downloadJavadoc() {
        for (String version : versions) {
            String prefix = StrUtil.format("{}/{}/{}-{}-javadoc", baseUrl, version, artifactId, version);
            Path destDir = javaDocDir.resolve(folderName).resolve(version);
            tryAlternatives(prefix, destDir, true, JAVADOC_CODE_DIR_NAME);
        }
    }

    private void downloadTestcase() {
        for (String version : versions) {
            String prefix = StrUtil.format("{}/{}/{}-{}-test-sources", baseUrl, version, artifactId, version);
            Path destDir = testcaseDir.resolve(folderName).resolve(version);
            tryAlternatives(prefix, destDir, true, TESTCASE_CODE_DIR_NAME);
        }
    }

    private List<String> fetchVersions() {
        String metaUrl = StrUtil.format("{}/maven-metadata.xml", baseUrl);
        try (HttpResponse resp = HttpRequest.get(metaUrl)
                .timeout(TIMEOUT_MS)
                .header(Header.USER_AGENT, USER_AGENT)
                .execute()) {

            if (!resp.isOk()) {
                return Collections.emptyList();
            }
            Document doc = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder()
                    .parse(new ByteArrayInputStream(resp.bodyBytes()));

            NodeList list = doc.getElementsByTagName("version");
            List<String> versions = new ArrayList<>(list.getLength());
            for (int i = 0; i < list.getLength(); i++) {
                versions.add(list.item(i).getTextContent());
            }
            return versions;
        } catch (Exception e) {
            log.error("ParseVersionListError:", e);
            return Collections.emptyList();
        }
    }

    private Path downloadWithRetry(String url, Path destDir) {
        FileUtil.mkdir(destDir);
        String fileName = StrUtil.subAfter(url, "/", true);
        Path destPath = destDir.resolve(fileName);

        for (int i = 1; i <= RETRY_CNT; i++) {
            try (HttpResponse resp = HttpRequest.get(url)
                    .timeout(TIMEOUT_MS)
                    .header(Header.USER_AGENT, USER_AGENT)
                    .execute()) {

                if (resp.isOk()) {
                    FileUtil.writeBytes(resp.bodyBytes(), destPath.toFile());
                    return destPath;
                }
            } catch (Exception e) {
                log.error("DownloadTimeout: ", e);
            }
            ThreadUtil.safeSleep(1_000L * i);
        }
        log.warn("Download FAILED after {} retries: {}", RETRY_CNT, url);
        return null;
    }

    private void tryAlternatives(String urlPrefix, Path destDir,
                                 boolean unzip, String unzipSubFolderName) {
        FileUtil.mkdir(destDir);
        for (String suffix : SUFFIX_LIST) {
            Path downloaded = downloadWithRetry(urlPrefix + suffix, destDir);
            if (downloaded != null && FileUtil.exist(downloaded.toFile())) {
                if (unzip) {
                    Path target = unzipSubFolderName == null
                            ? destDir
                            : destDir.resolve(unzipSubFolderName);
                    unzipJar(downloaded, target);
                }
                break;
            }
        }
    }

    private void unzipJar(Path jarPath, Path targetDir) {
        try {
            FileUtil.mkdir(targetDir);
            ZipUtil.unzip(jarPath.toFile().getAbsoluteFile(), targetDir.toFile().getAbsoluteFile());
        } catch (Exception e) {
            log.warn("UnzipFailed:", e);
        }
    }
}
