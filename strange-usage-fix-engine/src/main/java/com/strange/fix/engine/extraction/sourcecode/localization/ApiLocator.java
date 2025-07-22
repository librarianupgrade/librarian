package com.strange.fix.engine.extraction.sourcecode.localization;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import com.strange.brokenapi.analysis.ApiSignature;
import com.strange.fix.engine.extraction.sourcecode.localization.entity.ApiLocation;
import lombok.NonNull;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FileASTRequestor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class ApiLocator {

    protected File projectDir;

    protected ApiSignature apiSignature;

    private List<ApiLocation> apiLocationList;

    public ApiLocator( File projectDir, ApiSignature apiSignature) {
        this.projectDir = projectDir;
        this.apiSignature = apiSignature;
        this.apiLocationList = null;
    }

    /**
     * @param sourceFile   the source code file
     * @param ast          the CompilationUnit of the source code file
     * @param apiSignature the target Api Signature
     * @return list of ApiLocation
     */
    protected abstract List<ApiLocation> locateInFile(File sourceFile, CompilationUnit ast, ApiSignature apiSignature);

    private List<String> getAllJavaFilePaths() {
        List<String> sourceFileList = new ArrayList<>();
        try {
            Files.walk(projectDir.toPath())
                    .filter(p -> p.toString().endsWith(".java"))
                    .forEach(path -> sourceFileList.add(path.toFile().getAbsolutePath()));
        } catch (IOException ignored) {
        }
        return sourceFileList;
    }

    private void beginLocate() {
        apiLocationList = new ArrayList<>();
        List<String> sourceFileList = getAllJavaFilePaths();
        FileASTRequestor requester = new FileASTRequestor() {
            @Override
            public void acceptAST(String sourceFilePath, CompilationUnit ast) {
                List<ApiLocation> apiLocations = locateInFile(FileUtil.file(sourceFilePath), ast, apiSignature);
                if (CollUtil.isNotEmpty(apiLocations)) {
                    apiLocationList.addAll(apiLocations);
                }
            }
        };

        ASTParser p = ASTParser.newParser(AST.getJLSLatest());
        p.setKind(ASTParser.K_COMPILATION_UNIT);
        p.setResolveBindings(true);
        p.setBindingsRecovery(true);
        p.setStatementsRecovery(true);
        String[] sourceCodePathArray = sourceFileList.toArray(new String[0]);

        String[] encodings = new String[sourceCodePathArray.length];
        Arrays.fill(encodings, "UTF-8");
        p.setEnvironment(
                null,
                null,
                null,
                true
        );

        encodings = new String[sourceCodePathArray.length];
        Arrays.fill(encodings, "UTF-8");
        p.createASTs(sourceCodePathArray, encodings, new String[]{}, requester, new NullProgressMonitor());
    }

    public List<ApiLocation> retrieveApiLocation() {
        if (apiLocationList == null) {
            beginLocate();
        }
        return apiLocationList;
    }

    private void beginLocate(List<String> sourcePathList) {
        apiLocationList = new ArrayList<>();
        List<String> sourceFileList = getAllJavaFilePaths();
        FileASTRequestor requester = new FileASTRequestor() {
            @Override
            public void acceptAST(String sourceFilePath, CompilationUnit ast) {
                List<ApiLocation> apiLocations = locateInFile(FileUtil.file(sourceFilePath), ast, apiSignature);
                if (CollUtil.isNotEmpty(apiLocations)) {
                    apiLocationList.addAll(apiLocations);
                }
            }
        };

        ASTParser p = ASTParser.newParser(AST.getJLSLatest());
        p.setKind(ASTParser.K_COMPILATION_UNIT);
        p.setResolveBindings(true);
        p.setBindingsRecovery(true);
        p.setStatementsRecovery(true);


        String[] encodings = new String[sourcePathList.size()];
        Arrays.fill(encodings, "UTF-8");
        p.setEnvironment(
                null,
                sourcePathList.toArray(new String[0]),
                encodings,
                true
        );

        String[] sourceCodePathArray = sourceFileList.toArray(new String[0]);
        encodings = new String[sourceCodePathArray.length];
        Arrays.fill(encodings, "UTF-8");
        p.createASTs(sourceCodePathArray, encodings, new String[]{}, requester, new NullProgressMonitor());
    }
    public List<ApiLocation> retrieveApiLocation(List<String> sourcePathList) {
        if (apiLocationList == null) {
            beginLocate(sourcePathList);
        }
        return apiLocationList;
    }
}
