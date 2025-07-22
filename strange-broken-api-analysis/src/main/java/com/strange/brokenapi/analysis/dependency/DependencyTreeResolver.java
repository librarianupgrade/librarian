package com.strange.brokenapi.analysis.dependency;


import com.strange.brokenapi.analysis.dependency.property.DependencyProperty;

import java.io.File;
import java.util.List;
import java.util.Map;

public abstract class DependencyTreeResolver {

    private final File dependencyConfigFile;
    private final DependencyAction dependencyAction;

    protected DependencyTreeResolver(File dependencyConfigFile, DependencyAction dependencyAction) {
        if (dependencyConfigFile == null || !dependencyConfigFile.isFile()) {
            throw new IllegalArgumentException("");
        }
        this.dependencyConfigFile = dependencyConfigFile;
        this.dependencyAction = dependencyAction;
    }

    protected DependencyAction getDependencyAction() {
        return dependencyAction;
    }

    public File getDependencyConfigFile() {
        return dependencyConfigFile;
    }

    public abstract Map<String, String> getDependencyVersionMap();

    public abstract DependencyNode getFatherDependency(DependencyNode dependencyNode);

    public abstract List<File> getDependencyJars();

    public abstract String getArtifactId();

    public abstract String getGroupId();

    public abstract String getVersion();

    public abstract DependencyNode getDependencyNode();

    public abstract boolean packageProject();

    public abstract DependencyProperty getDependencyProperty();

    public abstract String getModuleName();

}
