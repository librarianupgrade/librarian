package com.strange.brokenapi.analysis.dependency.maven;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.file.FileReader;
import cn.hutool.core.lang.Pair;
import com.strange.brokenapi.analysis.dependency.DependencyAction;
import com.strange.brokenapi.analysis.dependency.DependencyNode;
import com.strange.common.enums.JDKVersionEnum;
import com.strange.common.utils.CommandUtil;
import com.strange.common.utils.OSUtil;
import fr.dutra.tools.maven.deptree.core.InputType;
import fr.dutra.tools.maven.deptree.core.Node;
import fr.dutra.tools.maven.deptree.core.Parser;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.model.Dependency;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public class MavenDependencyAction implements DependencyAction {

    @Deprecated
    private final List<String> JAR_NAME_SCHEMA = List.of("{}-{}.jar", "{}-{}-shaded.jar", "{}-{}-tests.jar", "{}-{}-jdk15.jar", "{}-{}-runtime.jar", "{}-{}-osx-x86_64.jar",
            "{}-{}-indy.jar", "{}-{}-win.jar", "{}-{}-linux.jar", "{}-{}-asl.jar");
    private final String CLEAN_DEPENDENCY_COMMAND = "mvn clean";
    private final String INSTALL_DEPENDENCY_COMMAND = "mvn install -DskipTests";
    private final String PACKAGE_DEPENDENCY_COMMAND = "mvn clean package -DskipTests";
    private final String GO_OFFLINE_DEPENDENCY_COMMAND = "mvn dependency:go-offline";
    private final String GENERATE_DEPENDENCY_TREE_COMMAND =
            OSUtil.isWindows() ? "mvn dependency:tree -DoutputFile=\"target/dependency-tree.txt\"" : "mvn dependency:tree -DoutputFile=target/dependency-tree.txt";
    private final String GENERATE_COMPLETE_DEPENDENCY_TREE_COMMAND =
            OSUtil.isWindows() ? "mvn dependency:tree -Dverbose -DoutputFile=\"target/complete-dependency-tree.txt\"" : "mvn dependency:tree -Dverbose -DoutputFile=target/complete-dependency-tree.txt";
    private final JDKVersionEnum jdkVersionEnum;
    private final File projectDir;
    private final File pomFile;
    private final Node treeNode;
    private String treeNodeText;
    private final Boolean isRootModule;
    private Map<String, File> jarFileMap = null; // short signature ---> jar file
    private File dependencyDir;

    public MavenDependencyAction(File pomFile, JDKVersionEnum jdkVersionEnum, Boolean isRootModule, Boolean isNeedInstall, String dependencyPath) {
        if (pomFile == null || !pomFile.isFile()) {
            throw new IllegalArgumentException("The provided pomFile does not exist or is not a file. Please ensure the file path is correct.");
        }
        this.jdkVersionEnum = jdkVersionEnum;
        this.isRootModule = isRootModule;
        this.projectDir = pomFile.getParentFile();
        this.pomFile = pomFile;
        this.dependencyDir = FileUtil.file(projectDir, dependencyPath);

//        if (dependencyDir.exists()) {
//            FileUtil.del(dependencyDir);
//        }

        if (isRootModule) {
//            cleanProject();
            if (isNeedInstall) {
//                jdkVersionEnum.runSetJDKEnvironmentCommand();
                boolean installProjectResult = installProject();
//                GlobalConfig.GlobalJDKVersion.runSetJDKEnvironmentCommand();

                if (!installProjectResult) {
                    throw new RuntimeException("Project installation failed, unable to proceed.");
                }
            }
        }
        this.treeNode = initTreeNode();
    }

    private Node initTreeNode() {
        File treeTextFile = generateDependencyTree();

        Node treeNode = null;
        try {
            this.treeNodeText = new FileReader(treeTextFile, Charset.defaultCharset()).readString();

            InputType type = InputType.TEXT;
            Reader r = new BufferedReader(new InputStreamReader(Files.newInputStream(treeTextFile.toPath()), StandardCharsets.UTF_8));
            Parser parser = type.newParser();
            treeNode = parser.parse(r);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize the dependency tree node. Reason: " + e.getMessage(), e);
        }
        return treeNode;
    }

    public Node getTreeNode() {
        return treeNode;
    }

    public String getTreeNodeText() {
        return treeNodeText;
    }

    public Map<String, File> getJarFileMap() {
        if (jarFileMap == null) {
            copyProjectDependency();
            jarFileMap = new HashMap<>();
            traverseDependencyRecursively();
        }
        return jarFileMap;
    }

    private void traverseDependencyRecursively() {
        if (treeNode != null) {
            for (Node node : treeNode.getChildNodes()) {
                traverseDependencyRecursively(node, dependencyDir);
            }
        }
    }

    private void traverseDependencyRecursively(Node node, File dependencyDir) {
        if (node == null) return;

        // copy operation
        if (node.getScope().equals("system")) {
            Dependency dependency = PomHelper.getDependency(pomFile, node.getGroupId(), node.getArtifactId());
            if (dependency == null) return;
            String systemPath = dependency.getSystemPath();
            systemPath = PomHelper.normalize(pomFile, systemPath);
            File jarFile = FileUtil.file(systemPath);
            if (jarFile.isFile()) {
                String jarName = FileUtil.getName(systemPath);
                File targetJarFile = FileUtil.file(dependencyDir, jarName);
                String dependencySignature = DependencyNode.getShortSignature(node.getGroupId(), node.getArtifactId());
                jarFileMap.put(dependencySignature, targetJarFile);
            }
        } else {
            File localRepository = FileUtil.file(FileUtil.getUserHomePath(), ".m2", "repository");
            String groupId = node.getGroupId().replace(".", "/");
            String artifactId = node.getArtifactId();
            String version = node.getVersion();
            File currentJarHome = FileUtil.file(localRepository, groupId, artifactId, version);

            List<File> jarFileList = Arrays.stream(FileUtil.ls(currentJarHome.getAbsolutePath()))
                    .filter(file -> "jar".equals(FileUtil.getSuffix(file)))
                    .toList();

            for (File currentJarFile : jarFileList) {
                if (currentJarFile.isFile() && !currentJarFile.getName().contains("javadoc") && !currentJarFile.getName().contains("sources")) {
                    String currentJarName = FileUtil.getName(currentJarFile);
                    File targetJarFile = FileUtil.file(dependencyDir, currentJarName);
                    String dependencySignature = DependencyNode.getShortSignature(node.getGroupId(), node.getArtifactId());
                    jarFileMap.put(dependencySignature, targetJarFile);
                }
            }

            for (Node childNode : node.getChildNodes()) {
                traverseDependencyRecursively(childNode, dependencyDir);
            }
        }
    }

    @Override
    public void cleanProject() {
        try {
            CommandUtil.execCommand(projectDir, CLEAN_DEPENDENCY_COMMAND);
        } catch (IOException | InterruptedException ignored) {
        }
    }

    @Override
    public boolean packageProject() {
        try {
            String output = CommandUtil.execCommand(projectDir, PACKAGE_DEPENDENCY_COMMAND);
            return output.contains("BUILD SUCCESS");
        } catch (IOException | InterruptedException e) {
            return false;
        }
    }

    @Override
    public boolean installProject() {
        try {
            String output = CommandUtil.execCommand(projectDir, INSTALL_DEPENDENCY_COMMAND);
            return output.contains("BUILD SUCCESS");
        } catch (IOException | InterruptedException e) {
            return false;
        }
    }

    @Data
    static class CopyTask {
        private List<Pair<File, File>> taskQueue;

        public CopyTask() {
            this.taskQueue = new ArrayList<>();
        }

        public void addTask(File sourceFile, File destFile) {
            this.taskQueue.add(new Pair<>(sourceFile, destFile));
        }

        public void startCopy() {
            ExecutorService executor = Executors.newFixedThreadPool(COPY_THREAD_NUMBER);
            for (Pair<File, File> task : taskQueue) {
                executor.submit(() -> {
                    File srcFile = task.getKey();
                    File destFile = task.getValue();
                    FileUtil.copy(srcFile, destFile, true);
                });
            }
            executor.shutdown();
        }
    }

    @Override
    public void goOfflineProjectDependency() {
        try {
            CommandUtil.execCommand(projectDir, GO_OFFLINE_DEPENDENCY_COMMAND);
        } catch (IOException | InterruptedException ignored) {
        }
    }

    @Override
    public File copyProjectDependency() {
        if (dependencyDir.isDirectory() && dependencyDir.exists()) {
            return dependencyDir;
        }

        // First, download all project dependencies to the local repository
        goOfflineProjectDependency();

        // Then copy from the local repository; if a dependency is not found in the local repository, simply skip it
        if (!dependencyDir.isDirectory()) {
            FileUtil.mkdir(dependencyDir);
        }

        CopyTask copyTask = new CopyTask();
        if (treeNode != null) {
            for (Node node : treeNode.getChildNodes()) {
                copyDependencyRecursively(node, dependencyDir, copyTask);
            }
        }

        copyTask.startCopy();
        return dependencyDir;
    }

    private void copyDependencyRecursively(Node node, File targetDir, CopyTask copyTask) {
        if (node == null) return;

        // copy operation
        if (node.getScope().equals("system")) {
            Dependency dependency = PomHelper.getDependency(pomFile, node.getGroupId(), node.getArtifactId());
            if (dependency == null) return;
            String systemPath = dependency.getSystemPath();
            systemPath = PomHelper.normalize(pomFile, systemPath);
            File jarFile = FileUtil.file(systemPath);
            if (jarFile.isFile()) {
                String jarName = FileUtil.getName(systemPath);
                File targetJarFile = FileUtil.file(targetDir, jarName);
//                FileUtil.copy(jarFile, targetJarFile, true);
                copyTask.addTask(jarFile, targetJarFile);
            }
        } else {
            File localRepository = FileUtil.file(FileUtil.getUserHomePath(), ".m2", "repository");
            String groupId = node.getGroupId().replace(".", "/");
            String artifactId = node.getArtifactId();
            String version = node.getVersion();
            File currentJarHome = FileUtil.file(localRepository, groupId, artifactId, version);

            List<File> jarFileList = Arrays.stream(FileUtil.ls(currentJarHome.getAbsolutePath()))
                    .filter(file -> "jar".equals(FileUtil.getSuffix(file)))
                    .toList();

            for (File currentJarFile : jarFileList) {
                if (currentJarFile.isFile() && !currentJarFile.getName().contains("javadoc") && !currentJarFile.getName().contains("sources")) {
                    String currentJarName = FileUtil.getName(currentJarFile);
                    File targetJarFile = FileUtil.file(targetDir, currentJarName);
//                    FileUtil.copy(currentJarFile, targetJarFile, true);
                    copyTask.addTask(currentJarFile, targetJarFile);
                }
            }

            for (Node childNode : node.getChildNodes()) {
                copyDependencyRecursively(childNode, targetDir, copyTask);
            }
        }
    }

    @Override
    public File generateDependencyTree() {
        File treeTextFile = FileUtil.file(projectDir, "target", "dependency-tree.txt");
        if (isRootModule || !treeTextFile.exists()) {
            try {
                CommandUtil.execCommand(projectDir, GENERATE_DEPENDENCY_TREE_COMMAND);
            } catch (IOException | InterruptedException ignored) {
            }
        }
        return treeTextFile;
    }

    @Override
    public File generateCompleteDependencyTree() {
        File completeTreeTextFile = FileUtil.file(projectDir, "target", "complete-dependency-tree.txt");
        if (isRootModule || !completeTreeTextFile.exists()) {
            try {
                CommandUtil.execCommand(projectDir, GENERATE_COMPLETE_DEPENDENCY_TREE_COMMAND);
            } catch (IOException | InterruptedException ignored) {
            }
        }
        return completeTreeTextFile;
    }
}
