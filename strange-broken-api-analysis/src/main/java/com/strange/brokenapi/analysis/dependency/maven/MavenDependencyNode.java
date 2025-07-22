package com.strange.brokenapi.analysis.dependency.maven;


import com.strange.brokenapi.analysis.dependency.DependencyNode;
import fr.dutra.tools.maven.deptree.core.Node;

import java.io.File;
import java.util.Map;

public class MavenDependencyNode extends DependencyNode {
    public static final MavenDependencyNode SELF = new MavenDependencyNode();

    private Map<String, File> jarFileMap; // dependency short signature ---> jar file

    private MavenDependencyNode() {
        super();
    }

    public MavenDependencyNode(Node treeNode, File jarFile, Map<String, File> jarFileMap, String moduleName) {
        super(treeNode.getGroupId(), treeNode.getArtifactId(), treeNode.getVersion()
                , treeNode.getScope(), jarFile, moduleName);

        this.jarFileMap = jarFileMap;
        for (Node childNode : treeNode.getChildNodes()) {
            super.addChildNode(traverseDependencyTree(childNode,
                    jarFileMap.getOrDefault(
                            getShortSignature(childNode.getGroupId(), childNode.getArtifactId()),
                            null), moduleName)
            );
        }
    }

    private DependencyNode traverseDependencyTree(Node node, File jarFile, String moduleName) {
        DependencyNode dependencyNode = new MavenDependencyNode(node, jarFile, jarFileMap, moduleName);
        for (Node childNode : node.getChildNodes()) {
            dependencyNode.addChildNode(
                    traverseDependencyTree(childNode,
                            jarFileMap.getOrDefault(
                                    getShortSignature(childNode.getGroupId(), childNode.getArtifactId()),
                                    null), moduleName
                    )
            );
        }
        return dependencyNode;
    }
}
