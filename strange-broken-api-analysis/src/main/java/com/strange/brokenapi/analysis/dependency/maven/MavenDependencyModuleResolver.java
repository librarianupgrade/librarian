package com.strange.brokenapi.analysis.dependency.maven;

import cn.hutool.core.io.FileUtil;

import com.strange.brokenapi.analysis.dependency.DependencyTreeResolver;
import com.strange.common.enums.JDKVersionEnum;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// to resolve the module tree
public class MavenDependencyModuleResolver {
    private static final String POM_FILE_NAME = "pom.xml";
    private static final String DELIMITER = File.separator;
    private final File projectDir;
    private final File rootPomFile;
    private final List<String> moduleNameList;
    private final Map<String, File> moduleMap; // Module Name ---> module File
    private final Map<String, DependencyTreeResolver> moduleDependencyMap; // Module Name ---> Dependency Tree Resolver
    private final MavenModule rootModule; // Module Tree Structure
    private final List<String> parsePath; // parse path
    private Boolean isRootModule;
    private final Boolean isNeedInstall;
    private final JDKVersionEnum jdkVersionEnum;
    private final String dependencyPath;

    public MavenDependencyModuleResolver(File projectDir) throws Exception {
        this(projectDir, JDKVersionEnum.JDK_1_8, false, "dependency");
    }

    public MavenDependencyModuleResolver(File projectDir, JDKVersionEnum jdkVersionEnum, Boolean isNeedInstall, String dependencyPath) throws Exception {
        this.projectDir = projectDir;
        this.jdkVersionEnum = jdkVersionEnum;
        this.isNeedInstall = isNeedInstall;

        if (dependencyPath == null || dependencyPath.isEmpty()) {
            dependencyPath = "dependency";
        }
        this.dependencyPath = dependencyPath;

        if (projectDir == null || !projectDir.isDirectory()) {
            throw new IllegalArgumentException("Invalid argument: 'projectDir' should be a non-null directory.");
        }

        this.rootPomFile = FileUtil.file(projectDir, POM_FILE_NAME);
        if (!rootPomFile.exists() || !rootPomFile.exists()) {
            throw new IllegalArgumentException("POM file does not exist: " + rootPomFile.getAbsolutePath());
        }
        this.moduleNameList = new ArrayList<>();
        this.moduleMap = new HashMap<>();
        this.moduleDependencyMap = new HashMap<>();
        this.parsePath = new ArrayList<>();
        this.isRootModule = true;
        this.rootModule = parseProject(rootPomFile);
    }

    protected MavenModule parseProject(File pomFile) throws Exception {
        MavenXpp3Reader reader = new MavenXpp3Reader();
        Model model = reader.read(new FileReader(pomFile));
        String groupId = model.getGroupId();
        String artifactId = model.getArtifactId();
        String version = model.getVersion();
        File projectFile = pomFile.getParentFile();

        String currentModuleName = String.join(DELIMITER, parsePath);
        MavenDependencyTreeResolver resolver = new MavenDependencyTreeResolver(pomFile, currentModuleName, jdkVersionEnum, isRootModule, isNeedInstall, dependencyPath);
        moduleDependencyMap.put(currentModuleName, resolver);
        isRootModule = false;

        MavenModule rootNode = new MavenModule(groupId, artifactId, version, projectFile, model, resolver);

        if (model.getModules() != null && !model.getModules().isEmpty()) {
            for (String moduleName : model.getModules()) {
                File moduleFile = FileUtil.file(projectFile.getAbsolutePath(), moduleName);
                File modulePomFile = FileUtil.file(moduleFile, POM_FILE_NAME);
                parsePath.add(moduleName);
                rootNode.addSubModule(parseProject(modulePomFile));
                parsePath.remove(parsePath.size() - 1);
            }
        } else {
            // it is a jar module
            rootNode.setModulePom(false);
        }
        return rootNode;
    }

    public File getProjectDir() {
        return projectDir;
    }

    public File getRootPomFile() {
        return rootPomFile;
    }

    public MavenModule getRootModule() {
        return rootModule;
    }

    public List<String> getModuleNameList() {
        return moduleNameList;
    }

    public Map<String, File> getModuleMap() {
        return moduleMap;
    }

    public JDKVersionEnum getJdkVersionEnum() {
        return jdkVersionEnum;
    }

    public Map<String, DependencyTreeResolver> getModuleDependencyMap() {
        return moduleDependencyMap;
    }

    public void printModuleTree() {
        printModuleTree(rootModule, "");
    }

    private void printModuleTree(MavenModule node, String prefix) {
        System.out.println(prefix + "- " + node.getModuleName() + " " + node.getModulePom());
        for (MavenModule subModule : node.getSubModules()) {
            printModuleTree(subModule, prefix + "  ");
        }
    }

}
