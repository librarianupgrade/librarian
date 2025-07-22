package com.strange.brokenapi.analysis.dependency.maven;

import cn.hutool.core.io.FileUtil;

import com.strange.brokenapi.analysis.dependency.DependencyNode;
import com.strange.brokenapi.analysis.dependency.DependencyTreeResolver;
import com.strange.brokenapi.analysis.dependency.property.DependencyProperty;
import com.strange.common.enums.JDKVersionEnum;
import lombok.extern.slf4j.Slf4j;


import java.io.File;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class MavenDependencyTreeResolver extends DependencyTreeResolver {

    private final File projectDir;

    private final Set<String> dependencySet; // dependency signature set

    private Map<String, String> dependencyVersinMap; // dependency short signature ---> dependency version

    private final DependencyNode treeNode;

    private final JDKVersionEnum jdkVersionEnum;

    private final Map<String, File> jarFileMap; // dependency short signature ---> dependency jar file

    private final Map<String, DependencyNode> fatherDependencyMap; // child dependency short signature  ---> father dependency

    private DependencyProperty dependencyProperty;

    private String moduleName; // its corresponding module name

    public MavenDependencyTreeResolver(File pomFile, String moduleName, JDKVersionEnum jdkVersionEnum, Boolean isRootModule, Boolean isNeedInstall, String dependencyPath) {
        super(pomFile, new MavenDependencyAction(pomFile, jdkVersionEnum, isRootModule, isNeedInstall, dependencyPath));
        this.jdkVersionEnum = jdkVersionEnum;
        this.moduleName = moduleName;

        this.projectDir = pomFile.getParentFile();
        MavenDependencyAction dependencyAction = (MavenDependencyAction) super.getDependencyAction();

        this.jarFileMap = dependencyAction.getJarFileMap();
        this.treeNode = new MavenDependencyNode(dependencyAction.getTreeNode(), null, jarFileMap, moduleName);

        this.dependencySet = new HashSet<>();
        this.dependencyVersinMap = new HashMap<>();
        this.fatherDependencyMap = new HashMap<>();
        traverseTree(treeNode, null, true);
    }

    private void traverseTree(DependencyNode node, DependencyNode fatherNode, boolean isRoot) {
        if (!isRoot) {
            dependencySet.add(node.getSignature());
            dependencyVersinMap.put(node.getShortSignature(), node.getVersion());
            if(fatherNode != null) {
                fatherDependencyMap.put(node.getShortSignature(), fatherNode);
            }
        }

        for (DependencyNode childNode : node.getChildrenNodes()) {
            traverseTree(childNode, node, false);
        }
    }

    @Override
    public Map<String, String> getDependencyVersionMap() {
        return dependencyVersinMap;
    }

    @Override
    public List<File> getDependencyJars() {
        File dependencyDir = getDependencyAction().copyProjectDependency();
        if (dependencyDir.isDirectory()) {
            return Stream.of(FileUtil.ls(dependencyDir.getAbsolutePath()))
                    .filter(file -> file.getName().endsWith(".jar"))
                    .collect(Collectors.toList());
        } else {
            return new ArrayList<>();
        }
    }

    @Override
    public String getArtifactId() {
        return treeNode.getArtifactId();
    }

    @Override
    public String getGroupId() {
        return treeNode.getGroupId();
    }

    @Override
    public String getVersion() {
        return treeNode.getVersion();
    }

    public File getProjectDir() {
        return projectDir;
    }

    public JDKVersionEnum getJdkVersionEnum() {
        return jdkVersionEnum;
    }

    public Map<String, File> getJarFileMap() {
        return jarFileMap;
    }

    @Override
    public DependencyNode getDependencyNode() {
        return treeNode;
    }

    @Override
    public boolean packageProject() {
        return getDependencyAction().packageProject();
    }

    @Override
    public DependencyProperty getDependencyProperty() {
        if (this.dependencyProperty == null) {
            this.dependencyProperty = new DependencyProperty(projectDir, treeNode, getDependencyJars());
        }
        return dependencyProperty;
    }

    @Override
    public String getModuleName() {
        return moduleName;
    }

    @Override
    public DependencyNode getFatherDependency(DependencyNode dependencyNode) {
        // DependencyNode currentDependencyNode = dependencyNode;
        // while(true) {
        //     DependencyNode fatherDependencyNode = fatherDependencyMap.get(currentDependencyNode.getShortSignature());
        //     if(fatherDependencyNode == null) return null;
        //     if(treeNode.getShortSignature().equals(fatherDependencyNode.getShortSignature())) return currentDependencyNode;
        //     currentDependencyNode = fatherDependencyNode;
        // }
        return fatherDependencyMap.get(dependencyNode.getShortSignature());
    }
}
