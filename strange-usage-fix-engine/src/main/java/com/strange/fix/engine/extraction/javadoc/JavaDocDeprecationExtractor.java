package com.strange.fix.engine.extraction.javadoc;


import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.file.FileNameUtil;
import com.strange.common.utils.ClassUtil;
import com.strange.fix.engine.extraction.javadoc.entity.ProjectDeprecation;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FileASTRequestor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class JavaDocDeprecationExtractor {

    public static ProjectDeprecation extractDocInFile(String javaFilePath) {
        return extractDocInFileList(List.of(javaFilePath));
    }

    public static ProjectDeprecation extractDocInFile(File javaCodeFile) {
        if (javaCodeFile != null && javaCodeFile.isFile()) {
            return extractDocInFile(javaCodeFile.getAbsolutePath());
        } else {
            return null;
        }
    }

    public static ProjectDeprecation extractDocInDirectory(String projectPath) {
        List<String> sourceFileList = new ArrayList<>();
        try {
            Files.walk(Path.of(projectPath))
                    .filter(p -> p.toString().endsWith(".java"))
                    .forEach(path -> sourceFileList.add(path.toFile().getAbsolutePath()));
        } catch (IOException ignored) {
        }
        return extractDocInFileList(sourceFileList);
    }

    public static ProjectDeprecation extractDocInDirectory(String projectPath, String className) {
        String simpleClassName = ClassUtil.getSimpleClassName(className);
        List<String> sourceFileList = new ArrayList<>();
        try {
            Files.walk(Path.of(projectPath))
                    .filter(p -> p.toString().endsWith(".java") && Objects.equals(simpleClassName, FileNameUtil.mainName(p.toFile())))
                    .forEach(path -> sourceFileList.add(path.toFile().getAbsolutePath()));
        } catch (IOException ignored) {
        }
        return extractDocInFileList(sourceFileList);
    }


    public static ProjectDeprecation extractDocInFileList(List<String> sourceFileList) {
        if (CollUtil.isEmpty(sourceFileList)) {
            return null;
        }
        ProjectDeprecation projectDeprecation = new ProjectDeprecation();
        FileASTRequestor requester = new FileASTRequestor() {
            @Override
            public void acceptAST(String sourceFilePath, CompilationUnit ast) {
                ast.accept(new JavaDocDeprecationVisitor(projectDeprecation));
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

        return projectDeprecation;
    }

    public static ProjectDeprecation extractDocInDirectory(File projectDir) {
        if (projectDir.isDirectory()) {
            return extractDocInDirectory(projectDir.getAbsolutePath());
        } else {
            return null;
        }
    }
}
