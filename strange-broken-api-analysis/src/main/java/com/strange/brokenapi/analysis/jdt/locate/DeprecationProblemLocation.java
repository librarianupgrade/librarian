package com.strange.brokenapi.analysis.jdt.locate;


import com.strange.brokenapi.analysis.jdt.DeprecationResult;
import lombok.Data;

@Data
public class DeprecationProblemLocation {

    private String groupId;

    private String artifactId;

    private String oldVersion;

    private String newVersion;

    private String moduleName;

    private String modulePath;

    private DeprecationResult deprecationResult;

    public String getSignature() {
        return groupId + ":" + artifactId + ":" + oldVersion + ":" + newVersion;
    }
}
