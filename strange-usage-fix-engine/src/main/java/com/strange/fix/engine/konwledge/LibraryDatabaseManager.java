package com.strange.fix.engine.konwledge;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.file.FileReader;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Library Database Schema:
 * - Library Database Directory
 * - jar
 * - {groupId}__gt__{artifactId}
 * - version_list.json
 * - 1.0.0
 * - {artifactId}-{version}.jar
 * - 1.0.1
 * - {artifactId}-{version}.jar
 * - 2.0.1
 * - {artifactId}-{version}.jar
 * - javadoc
 * - {groupId}__gt__{artifactId}
 * - version_list.json
 * - 1.0.0
 * - {artifactId}-{version}-javadoc.jar
 * - java_doc
 * - 1.0.1
 * - {artifactId}-{version}-javadoc.jar
 * - java_doc
 * - 2.0.1
 * - {artifactId}-{version}-javadoc.jar
 * - java_doc
 * - pom
 * - {groupId}__gt__{artifactId}
 * - version_list.json
 * - 1.0.0
 * - {artifactId}-{version}.pom
 * - 1.0.1
 * - {artifactId}-{version}.pom
 * - 2.0.1
 * - {artifactId}-{version}.pom
 * - source
 * - {groupId}__gt__{artifactId}
 * - version_list.json
 * - 1.0.0
 * - {artifactId}-{version}-sources.jar
 * - source_code
 * - 1.0.1
 * - {artifactId}-{version}-sources.jar
 * - source_code
 * - 2.0.1
 * - {artifactId}-{version}-sources.jar
 * - source_code
 * - testcase
 * - {groupId}__gt__{artifactId}
 * - version_list.json
 * - 1.0.0
 * - {artifactId}-{version}-test-sources.jar
 * - testcase_source_code
 * - 1.0.1
 * - {artifactId}-{version}-test-sources.jar
 * - testcase_source_code
 * - 2.0.1
 * - {artifactId}-{version}-test-sources.jar
 * - testcase_source_code
 */
public class LibraryDatabaseManager {

    private static final String POM_DIRECTOR_NAME = "pom";
    private static final String JAR_DIRECTOR_NAME = "jar";
    private static final String SOURCE_DIRECTOR_NAME = "source";
    private static final String JAVADOC_DIRECTOR_NAME = "javadoc";
    private static final String TESTCASE_DIRECTOR_NAME = "testcase";
    private static final String TESTCASE_JAR_DIRECTORY_NAME = "test-jar";
    private static final String VERSION_LIST_FILE_NAME = "version_list.json";
    private static final String DIR_NAME_SCHEMA = "{}__gt__{}";
    private static final String JAR_FILE_NAME_SCHEMA = "{}-{}.jar";
    private static final String TESTCASE_JAR_FILE_NAME_SCHEMA = "{}-{}-tests.jar";
    private static final String SOURCE_CODE_DIR_NAME = "source_code";
    private static final String TESTCASE_CODE_DIR_NAME = "testcase_source_code";

    private final String libraryDatabasePath;

    private final File libraryDatabaseDir;

    public LibraryDatabaseManager(String libraryDatabasePath) {
        this(FileUtil.file(libraryDatabasePath));
    }

    public LibraryDatabaseManager(File libraryDatabaseDir) {
        if (libraryDatabaseDir != null && libraryDatabaseDir.isDirectory()) {
            this.libraryDatabasePath = libraryDatabaseDir.getAbsolutePath();
            this.libraryDatabaseDir = libraryDatabaseDir;
        } else {
            String message;
            if (libraryDatabaseDir == null) {
                message = "Library database directory must not be null.";
            } else {
                message = String.format(
                        "Invalid library database directory: '%s'. It either does not exist or is not a directory.",
                        libraryDatabaseDir.getAbsolutePath()
                );
            }
            throw new RuntimeException(message);
        }
    }

    public LibraryKnowledgeDirectory getLibraryKnowledgeDir(String groupId, String artifactId) {
        LibraryKnowledgeDirectory libraryKnowledgeDirectory = new LibraryKnowledgeDirectory();
        libraryKnowledgeDirectory.setJarDir(getLibraryJarDir(groupId, artifactId));
        libraryKnowledgeDirectory.setPomDir(getLibraryPomDir(groupId, artifactId));
        libraryKnowledgeDirectory.setJavaDocDir(getLibraryJavaDocDir(groupId, artifactId));
        libraryKnowledgeDirectory.setSourceDir(getLibrarySourceDir(groupId, artifactId));
        libraryKnowledgeDirectory.setTestcaseDir(getLibraryTestcaseDir(groupId, artifactId));
        return libraryKnowledgeDirectory;
    }

    public File getLibraryJarDir(String groupId, String artifactId) {
        String dirName = StrUtil.format(DIR_NAME_SCHEMA, groupId, artifactId);
        return FileUtil.file(libraryDatabasePath, JAR_DIRECTOR_NAME, dirName);
    }

    public File getLibraryPomDir(String groupId, String artifactId) {
        String dirName = StrUtil.format(DIR_NAME_SCHEMA, groupId, artifactId);
        return FileUtil.file(libraryDatabasePath, POM_DIRECTOR_NAME, dirName);
    }

    public File getLibraryJavaDocDir(String groupId, String artifactId) {
        String dirName = StrUtil.format(DIR_NAME_SCHEMA, groupId, artifactId);
        return FileUtil.file(libraryDatabasePath, JAVADOC_DIRECTOR_NAME, dirName);
    }

    public File getLibrarySourceDir(String groupId, String artifactId) {
        String dirName = StrUtil.format(DIR_NAME_SCHEMA, groupId, artifactId);
        return FileUtil.file(libraryDatabasePath, SOURCE_DIRECTOR_NAME, dirName);
    }

    public File getLibraryTestcaseDir(String groupId, String artifactId) {
        String dirName = StrUtil.format(DIR_NAME_SCHEMA, groupId, artifactId);
        return FileUtil.file(libraryDatabasePath, TESTCASE_DIRECTOR_NAME, dirName);
    }

    public List<String> getLibraryVersionList(String groupId, String artifactId) {
        // get version list from the jar director
        String dirName = StrUtil.format(DIR_NAME_SCHEMA, groupId, artifactId);
        File versionListFile = FileUtil.file(libraryDatabasePath, JAR_DIRECTOR_NAME, dirName, VERSION_LIST_FILE_NAME);
        String versionListContent = new FileReader(versionListFile).readString();
        return JSONUtil.parseArray(versionListContent).toList(String.class);
    }

    public File getLibraryJarFile(String groupId, String artifactId, String version) {
        String dirName = StrUtil.format(DIR_NAME_SCHEMA, groupId, artifactId);
        String jarName = StrUtil.format(JAR_FILE_NAME_SCHEMA, artifactId, version);
        return FileUtil.file(libraryDatabasePath, JAR_DIRECTOR_NAME, dirName, version, jarName);
    }

    public File getLibraryTestcaseJarFile(String groupId, String artifactId, String version) {
        String dirName = StrUtil.format(DIR_NAME_SCHEMA, groupId, artifactId);
        String jarName = StrUtil.format(TESTCASE_JAR_FILE_NAME_SCHEMA, artifactId, version);
        return FileUtil.file(libraryDatabasePath, TESTCASE_JAR_DIRECTORY_NAME, dirName, version, jarName);
    }

    public File getLibrarySourceDir(String groupId, String artifactId, String version) {
        String dirName = StrUtil.format(DIR_NAME_SCHEMA, groupId, artifactId);
        return FileUtil.file(libraryDatabasePath, SOURCE_DIRECTOR_NAME, dirName, version, SOURCE_CODE_DIR_NAME);
    }

    public File getLibraryTestcaseDir(String groupId, String artifactId, String version) {
        String dirName = StrUtil.format(DIR_NAME_SCHEMA, groupId, artifactId);
        return FileUtil.file(libraryDatabasePath, TESTCASE_DIRECTOR_NAME, dirName, version, TESTCASE_CODE_DIR_NAME);
    }

    public List<String> getVersionRange(String groupId, String artifactId, String oldVersion, String newVersion) {
        List<String> libraryVersionList = getLibraryVersionList(groupId, artifactId);
        int startIndex = libraryVersionList.indexOf(oldVersion);
        int endIndex = libraryVersionList.indexOf(newVersion);
        if (startIndex == -1 || endIndex == -1) return null;

        List<String> versionList = new ArrayList<>();
        for (int i = startIndex; i <= endIndex; i++) {
            versionList.add(libraryVersionList.get(i));
        }
        return versionList;
    }
}
