package com.strange.fix.engine.extraction.sourcecode.mapping;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.strange.fix.engine.konwledge.LibraryDatabaseManager;
import lombok.NonNull;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RefactoringAnalyzerFactory {


    private static final Map<String, RefactoringAnalyzer> refactorAnalyzerCacheMap = new ConcurrentHashMap<>();

    public synchronized static RefactoringAnalyzer getRefactoringAnalyzer( LibraryDatabaseManager libraryDatabaseManager,  String groupId,
                                                                           String artifactId,  String oldVersion,  String newVersion,
                                                                           MappingSourceEnum mappingSource) {
        File oldLibraryCodeDir = null;
        File newLibraryCodeDir = null;
        switch (mappingSource) {
            case SOURCE_CODE -> {
                oldLibraryCodeDir = libraryDatabaseManager.getLibrarySourceDir(groupId, artifactId, oldVersion);
                newLibraryCodeDir = libraryDatabaseManager.getLibrarySourceDir(groupId, artifactId, newVersion);
            }
            case TESTCASE_CODE -> {
                oldLibraryCodeDir = libraryDatabaseManager.getLibraryTestcaseDir(groupId, artifactId, oldVersion);
                newLibraryCodeDir = libraryDatabaseManager.getLibraryTestcaseDir(groupId, artifactId, newVersion);
            }
            default -> {
                throw new RuntimeException(
                        String.format(
                                "Unsupported mappingSource '%s' for library %s:%s (versions %s → %s)",
                                mappingSource, groupId, artifactId, oldVersion, newVersion
                        )
                );
            }
        }

        String hashValue = hash(oldLibraryCodeDir.getAbsolutePath(), newLibraryCodeDir.getAbsolutePath());
        if (refactorAnalyzerCacheMap.containsKey(hashValue)) return refactorAnalyzerCacheMap.get(hashValue);
        else {
            RefactoringAnalyzer refactoringAnalyzer = new RefactoringAnalyzer(oldLibraryCodeDir, newLibraryCodeDir);
            refactorAnalyzerCacheMap.put(hashValue, refactoringAnalyzer);
            return refactoringAnalyzer;
        }
    }

    private static String hash(String oldLibraryCodePath, String newLibraryCodePath) {
        String combinedString = StrUtil.join("|", oldLibraryCodePath, newLibraryCodePath);
        return DigestUtil.sha256Hex(combinedString);
    }
}
