package com.strange.brokenapi.analysis.jdt.locate;


import com.strange.brokenapi.analysis.dependency.DependencyNode;
import com.strange.brokenapi.analysis.jdt.ErrorResult;
import lombok.Data;

@Data
public class ErrorProblemLocation {

    private String groupId;

    private String artifactId;

    private String moduleName;

    private String oldVersion;

    private String newVersion;

    private ErrorResult errorResult;

    private boolean isDueToTransitiveDependency;

    private DependencyNode transitiveDependency;

    public ErrorProblemLocation() {
        this.isDueToTransitiveDependency = false;
        this.transitiveDependency = null;
    }

    public String getSignature() {
        return groupId + ":" + artifactId + ":" + oldVersion + ":" + newVersion;
    }
}
