package com.strange.brokenapi.analysis.jdt.locate;


import com.strange.brokenapi.analysis.dependency.DependencyNode;
import com.strange.brokenapi.analysis.dependency.DependencyReLocator;
import com.strange.brokenapi.analysis.dependency.DependencyTreeResolver;
import com.strange.brokenapi.analysis.dependency.maven.MavenDependencyModuleResolver;
import com.strange.brokenapi.analysis.dependency.maven.MavenDependencyNode;
import com.strange.brokenapi.analysis.jdt.DeprecationResult;
import com.strange.brokenapi.analysis.jdt.ErrorResult;
import com.strange.brokenapi.analysis.jdt.MavenModuleCompilationAnalyzer;
import com.strange.brokenapi.analysis.jdt.locate.deprecation.DeprecationHandler;
import com.strange.brokenapi.analysis.jdt.locate.deprecation.DeprecationProblemHandlerFactory;
import com.strange.brokenapi.analysis.jdt.locate.error.ErrorHandler;
import com.strange.brokenapi.analysis.jdt.locate.error.ErrorProblemHandlerFactory;
import com.strange.common.enums.FixTypeEnum;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class MavenModuleProblemAnalyzer {

    private final MavenModuleCompilationAnalyzer analyzer;

    private final FixTypeEnum fixTypeEnum;

    private final List<ErrorProblemLocation> errorProblemList;

    private final List<DeprecationProblemLocation> deprecationProblemList;

    private final MavenDependencyModuleResolver oldModuleAnalyzer;

    private final MavenDependencyModuleResolver newModuleAnalyzer;


    public MavenModuleProblemAnalyzer(MavenModuleCompilationAnalyzer moduleCompilationAnalyzer, FixTypeEnum fixTypeEnum) {
        this.analyzer = moduleCompilationAnalyzer;
        this.fixTypeEnum = fixTypeEnum;
        this.errorProblemList = new ArrayList<>();
        this.deprecationProblemList = new ArrayList<>();
        this.oldModuleAnalyzer = moduleCompilationAnalyzer.getOldModuleResolver();
        this.newModuleAnalyzer = moduleCompilationAnalyzer.getNewModuleResolver();
        locatingDependencies();
    }

    private void locatingDependencies() {
        switch (fixTypeEnum) {
            case FixCompilationError:
                locateErrorDependencies(analyzer.getErrorResults());
                break;
            case FixDeprecation:
                locateDeprecationDependencies(analyzer.getDeprecationResults());
                break;
            case Both:
                locateErrorDependencies(analyzer.getErrorResults());
                locateDeprecationDependencies(analyzer.getDeprecationResults());
                break;
        }
    }

    private boolean isSNAPSHOTVersion(DependencyNode node) {
        return node.getVersion().contains("SNAPSHOT");
    }

    private void locateErrorDependencies(List<ErrorResult> results) {
        for (ErrorResult result : results) {
            DependencyNode dependencyNode = processError(result);
            if (dependencyNode != null && !isSNAPSHOTVersion(dependencyNode) && dependencyNode != MavenDependencyNode.SELF) {
                ErrorProblemLocation errorProblemLocation = new ErrorProblemLocation();
                errorProblemLocation.setModuleName(dependencyNode.getModuleName());
                errorProblemLocation.setErrorResult(result);
                errorProblemLocation.setGroupId(dependencyNode.getGroupId());
                errorProblemLocation.setArtifactId(dependencyNode.getArtifactId());

                DependencyTreeResolver oldDependencyTreeResolver = oldModuleAnalyzer.getModuleDependencyMap().get(dependencyNode.getModuleName());
                DependencyTreeResolver newDependencyTreeResolver = newModuleAnalyzer.getModuleDependencyMap().get(dependencyNode.getModuleName());
                String oldVersion = oldDependencyTreeResolver.getDependencyVersionMap().get(dependencyNode.getShortSignature());
                if (oldVersion == null) {
                    // maybe relocate the dependency position
                    String relocateDependencyShortSignature = DependencyReLocator.relocate(dependencyNode);
                    oldVersion = oldDependencyTreeResolver.getDependencyVersionMap().get(relocateDependencyShortSignature);
                    if (oldVersion != null) {
                        DependencyNode.ShortSignaturePair shortSignaturePair = DependencyNode.toShortSignaturePair(relocateDependencyShortSignature);
                        if (shortSignaturePair != null) {
                            errorProblemLocation.setGroupId(shortSignaturePair.getGroupId());
                            errorProblemLocation.setArtifactId(shortSignaturePair.getArtifactId());
                        }
                    }
                }

                String newVersion = newDependencyTreeResolver.getDependencyVersionMap().get(dependencyNode.getShortSignature());
                if (newVersion == null) {
                    // maybe relocate the dependency position
                    String relocateDependencyShortSignature = DependencyReLocator.relocate(dependencyNode);
                    newVersion = newDependencyTreeResolver.getDependencyVersionMap().get(relocateDependencyShortSignature);
                }

                errorProblemLocation.setOldVersion(oldVersion);
                errorProblemLocation.setNewVersion(newVersion);

                if (oldVersion != null && newVersion == null) {
                    // indicates that the transitive dependency has been removed in the new version.
                    errorProblemLocation.setDueToTransitiveDependency(true);
                    errorProblemLocation.setTransitiveDependency(dependencyNode);
                    dependencyNode = oldDependencyTreeResolver.getFatherDependency(dependencyNode);

                    errorProblemLocation.setGroupId(dependencyNode.getGroupId());
                    errorProblemLocation.setArtifactId(dependencyNode.getArtifactId());
                    oldVersion = oldDependencyTreeResolver.getDependencyVersionMap().get(dependencyNode.getShortSignature());
                    if (oldVersion == null) {
                        // maybe relocate the dependency position
                        String relocateDependencyShortSignature = DependencyReLocator.relocate(dependencyNode);
                        oldVersion = oldDependencyTreeResolver.getDependencyVersionMap().get(relocateDependencyShortSignature);
                    }

                    newVersion = newDependencyTreeResolver.getDependencyVersionMap().get(dependencyNode.getShortSignature());
                    if (newVersion == null) {
                        // maybe relocate the dependency position
                        String relocateDependencyShortSignature = DependencyReLocator.relocate(dependencyNode);
                        newVersion = newDependencyTreeResolver.getDependencyVersionMap().get(relocateDependencyShortSignature);
                    }
                    errorProblemLocation.setOldVersion(oldVersion);
                    errorProblemLocation.setNewVersion(newVersion);
                }
                result.setErrorProblemLocation(errorProblemLocation);
                this.errorProblemList.add(errorProblemLocation);
            }
        }
    }

    private DependencyNode processError(ErrorResult errorResult) {
        ErrorHandler handler = ErrorProblemHandlerFactory.getHandler(errorResult);
        if (handler == null) {
            log.warn("No handler found for error problem: {}", errorResult);
            return null;
        }
        return handler.handle(errorResult);
    }

    private void locateDeprecationDependencies(List<DeprecationResult> results) {
        for (DeprecationResult result : results) {
            DependencyNode dependencyNode = processDeprecation(result);
            if (dependencyNode != null && dependencyNode != MavenDependencyNode.SELF) {
                DeprecationProblemLocation deprecationProblemLocation = new DeprecationProblemLocation();
                deprecationProblemLocation.setGroupId(dependencyNode.getGroupId());
                deprecationProblemLocation.setArtifactId(dependencyNode.getArtifactId());
                deprecationProblemLocation.setModuleName(dependencyNode.getModuleName());
                deprecationProblemLocation.setDeprecationResult(result);
                DependencyTreeResolver oldDependencyTreeResolver = oldModuleAnalyzer.getModuleDependencyMap().get(dependencyNode.getModuleName());
                DependencyTreeResolver newDependencyTreeResolver = newModuleAnalyzer.getModuleDependencyMap().get(dependencyNode.getModuleName());
                deprecationProblemLocation.setOldVersion(oldDependencyTreeResolver.getDependencyVersionMap().get(dependencyNode.getShortSignature()));
                deprecationProblemLocation.setNewVersion(newDependencyTreeResolver.getDependencyVersionMap().get(dependencyNode.getShortSignature()));
                result.setDeprecationProblemLocation(deprecationProblemLocation);
                this.deprecationProblemList.add(deprecationProblemLocation);
            }
        }
    }

    private DependencyNode processDeprecation(DeprecationResult deprecationResult) {
        DeprecationHandler handler = DeprecationProblemHandlerFactory.getHandler(deprecationResult);
        if (handler == null) {
            log.warn("No handler found for deprecation problem: {}", deprecationResult);
            return null;
        }
        return handler.handle(deprecationResult);
    }

    public FixTypeEnum getFixTypeEnum() {
        return fixTypeEnum;
    }

    public List<ErrorProblemLocation> getErrorProblemList() {
        return errorProblemList;
    }

    public List<DeprecationProblemLocation> getDeprecationProblemList() {
        return deprecationProblemList;
    }
}
