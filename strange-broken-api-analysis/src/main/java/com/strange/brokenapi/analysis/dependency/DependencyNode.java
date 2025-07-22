package com.strange.brokenapi.analysis.dependency;

import cn.hutool.core.util.StrUtil;
import lombok.Data;
import lombok.NonNull;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public abstract class DependencyNode {

    private final String groupId;

    private final String artifactId;

    private final String version;

    private final String scope;

    private final String moduleName;

    private final Set<DependencyNode> childrenNodes;

    private final File jarFile;

    protected DependencyNode() {
        this.groupId = "null";
        this.artifactId = "null";
        this.version = "null";
        this.scope = "null";
        this.moduleName = "null";
        this.childrenNodes = null;
        this.jarFile = null;
    }

    public DependencyNode(String groupId, String artifactId, String version, String scope, File jarFile, String moduleName) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.scope = scope;
        this.jarFile = jarFile;
        this.childrenNodes = new HashSet<>();
        this.moduleName = moduleName;
    }

    public Set<DependencyNode> getChildrenNodes() {
        return childrenNodes;
    }

    public File getJarFile() {
        return jarFile;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getScope() {
        return scope;
    }

    public String getVersion() {
        return version;
    }

    public String getModuleName() {
        return moduleName;
    }

    public void addChildNode(DependencyNode node) {
        if (node != null)
            this.childrenNodes.add(node);
    }

    public String getSignature() {
        return getSignature(this.groupId, this.artifactId, this.version);
    }

    public String getShortSignature() {
        return getShortSignature(this.groupId, this.artifactId);
    }

    @Data
    public static class ShortSignaturePair {
        private String groupId;

        private String artifactId;

        public ShortSignaturePair(String groupId, String artifactId) {
            this.groupId = groupId;
            this.artifactId = artifactId;
        }
    }

    public static ShortSignaturePair toShortSignaturePair(String shortSignature) {
        List<String> split = StrUtil.split(shortSignature, ":");
        if (split.size() == 2) {
            return new ShortSignaturePair(split.get(0), split.get(1));
        } else {
            return null;
        }
    }

    public static String getSignature( String groupId,  String artifactId,  String version) {
        return groupId +
                ':' +
                artifactId +
                ':' +
                version;
    }

    public static String getShortSignature( String groupId,  String artifactId) {
        return groupId +
                ':' +
                artifactId;
    }

    public void printDependencyTree() {
        printDependencyTree(this, "");
    }

    private void printDependencyTree(DependencyNode node, String prefix) {
        System.out.println(prefix + "- " + node.getSignature());
        for (DependencyNode childNode : node.getChildrenNodes()) {
            printDependencyTree(childNode, prefix + "  ");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DependencyNode that = (DependencyNode) o;
        return Objects.equals(groupId, that.groupId) && Objects.equals(artifactId, that.artifactId)
                && Objects.equals(version, that.version) && Objects.equals(scope, that.scope);
    }

    @Override
    public int hashCode() {
        return Objects.hash(groupId, artifactId, version);
    }

    @Override
    public String toString() {
        return getSignature();
    }
}
