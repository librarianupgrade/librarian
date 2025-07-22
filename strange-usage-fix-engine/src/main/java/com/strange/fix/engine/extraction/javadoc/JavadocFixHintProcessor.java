package com.strange.fix.engine.extraction.javadoc;

import com.strange.brokenapi.analysis.ApiSignature;
import com.strange.brokenapi.analysis.BrokenApiUsage;
import com.strange.brokenapi.analysis.jdt.locate.ErrorProblemLocation;
import com.strange.fix.engine.extraction.hint.JavaDocHint;
import com.strange.fix.engine.extraction.javadoc.entity.ClassDeprecation;
import com.strange.fix.engine.extraction.javadoc.entity.FieldDeprecation;
import com.strange.fix.engine.extraction.javadoc.entity.MethodDeprecation;
import com.strange.fix.engine.extraction.javadoc.entity.ProjectDeprecation;
import com.strange.fix.engine.konwledge.LibraryDatabaseManager;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.Collections;
import java.util.List;

@Slf4j
public class JavadocFixHintProcessor {

    private final BrokenApiUsage brokenApiUsage;

    private final File libraryDatabaseDir;

    private final LibraryDatabaseManager libraryDatabaseManager;

    public JavadocFixHintProcessor(BrokenApiUsage brokenApiUsage, File libraryDatabaseDir) {
        this.brokenApiUsage = brokenApiUsage;
        this.libraryDatabaseDir = libraryDatabaseDir;
        this.libraryDatabaseManager = new LibraryDatabaseManager(libraryDatabaseDir);
    }

    private List<String> getVersionRange() {
        ErrorProblemLocation errorProblemLocation = brokenApiUsage.getErrorResult().getErrorProblemLocation();
        String groupId = errorProblemLocation.getGroupId();
        String artifactId = errorProblemLocation.getArtifactId();
        String oldVersion = errorProblemLocation.getOldVersion();
        String newVersion = errorProblemLocation.getNewVersion();
        return libraryDatabaseManager.getVersionRange(groupId, artifactId, oldVersion, newVersion);
    }

    public JavaDocHint extractHint() {
        ErrorProblemLocation errorProblemLocation = brokenApiUsage.getErrorResult().getErrorProblemLocation();
        if (errorProblemLocation == null) {
            return null;
        }
        ApiSignature apiSignature = brokenApiUsage.getApiSignature();
        String groupId = errorProblemLocation.getGroupId();
        String artifactId = errorProblemLocation.getArtifactId();

        List<String> versionRange = getVersionRange();
        String className = apiSignature.getClassName();
        JavaDocHint javaDocHint = new JavaDocHint();
        for (String version : versionRange) {
            File librarySourceDir = libraryDatabaseManager.getLibrarySourceDir(groupId, artifactId, version);
            ProjectDeprecation projectDeprecation = JavaDocDeprecationExtractor.extractDocInDirectory(librarySourceDir.getAbsolutePath(), className);
            if (projectDeprecation != null) {
                boolean hasFind = false;
                switch (brokenApiUsage.getBrokenApiType()) {
                    case METHOD -> {
                        MethodDeprecation methodDeprecation = projectDeprecation.findMethodDeprecation(apiSignature.getClassName(), apiSignature.getMethodName(), apiSignature.getMethodParamList());
                        if (methodDeprecation != null) {
                            javaDocHint.setJavaDocContent(methodDeprecation.getJavaDocContent());
                            hasFind = true;
                        }
                    }
                    case CONSTRUCTOR -> {
                        MethodDeprecation methodDeprecation = projectDeprecation.findMethodDeprecation(apiSignature.getClassName(), apiSignature.getMethodName(), apiSignature.getMethodParamList());
                        if (methodDeprecation != null) {
                            javaDocHint.setJavaDocContent(methodDeprecation.getJavaDocContent());
                            hasFind = true;
                        }
                    }
                    case CLASS -> {
                        ClassDeprecation classDeprecation = projectDeprecation.findClassDeprecation(apiSignature.getClassName());
                        if (classDeprecation != null) {
                            javaDocHint.setJavaDocContent(classDeprecation.getJavaDocContent());
                            hasFind = true;
                        }
                    }
                    case FIELD -> {
                        FieldDeprecation fieldDeprecation = projectDeprecation.findFieldDeprecation(apiSignature.getClassName(), apiSignature.getFieldName());
                        if (fieldDeprecation != null) {
                            javaDocHint.setJavaDocContent(fieldDeprecation.getJavaDocContent());
                            hasFind = true;
                        }
                    }
                }
                if (hasFind) {
                    break;
                }
            }
        }
        return javaDocHint;
    }
}
