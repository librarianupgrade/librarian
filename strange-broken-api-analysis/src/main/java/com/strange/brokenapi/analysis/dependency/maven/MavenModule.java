package com.strange.brokenapi.analysis.dependency.maven;

import org.apache.maven.model.Model;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MavenModule {
    private final String groupId;
    private final String artifactId;
    private final String version;
    private final File moduleDir;
    private final Model mavenModel;
    private final List<MavenModule> subModules;
    private Boolean modulePom;
    private final MavenDependencyTreeResolver treeResolver;

    public MavenModule(String groupId, String artifactId, String version, File moduleDir, Model mavenModel, MavenDependencyTreeResolver treeResolver) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.moduleDir = moduleDir;
        this.subModules = new ArrayList<>();
        this.modulePom = true;
        this.mavenModel = mavenModel;
        this.treeResolver = treeResolver;
    }

    public void addSubModule(MavenModule node) {
        subModules.add(node);
    }

    public String getModuleName() {
        return artifactId;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public String getVersion() {
        return version;
    }

    public File getModuleDir() {
        return moduleDir;
    }

    public List<MavenModule> getSubModules() {
        return subModules;
    }

    public Boolean getModulePom() {
        return modulePom;
    }

    public void setModulePom(Boolean modulePom) {
        this.modulePom = modulePom;
    }

    public MavenDependencyTreeResolver getTreeResolver() {
        return treeResolver;
    }

    public Model getMavenModel() {
        return mavenModel;
    }

    @Override
    public String toString() {
        return "ModuleNode{" +
                "moduleName='" + artifactId + '\'' +
                ", modulePath='" + moduleDir.getAbsolutePath() + '\'' +
                ", subModules=" + subModules +
                '}';
    }
}
