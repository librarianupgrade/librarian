package com.strange.brokenapi.analysis.jdt;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Pair;
import com.strange.common.enums.JDKVersionEnum;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FileASTRequestor;

import java.io.File;
import java.util.*;
import java.util.stream.Stream;

public class CodeCompilationAnalyzer {

    final private String[] sourceCodePathArray;

    final private List<String> classpathList;

    final private List<String> sourcePathList;

    final JDKVersionEnum jdkVersionEnum;

    private final List<Pair<File, CompilationUnit>> compilationUnitList;

    private final Map<String, CompilationUnit> codeFileASTMap; // absolute code file path ---> AST Structure


    public CodeCompilationAnalyzer(String[] sourceCodePathArray, List<String> classpathList, List<String> sourcePathList, JDKVersionEnum jdkVersionEnum) {
        if (sourceCodePathArray == null) {
            throw new IllegalArgumentException("`sourceCodePathArray` cannot be null. Please provide valid source code.");
        }
        if (sourcePathList == null || sourcePathList.isEmpty()) {
            throw new IllegalArgumentException("The `sourcePathList` list is either null or empty. Please provide a valid sourcepath.");
        }

        this.sourceCodePathArray = sourceCodePathArray;
        this.classpathList = Optional.ofNullable(classpathList).orElse(new ArrayList<>());
        this.sourcePathList = sourcePathList;
        this.jdkVersionEnum = jdkVersionEnum;
        this.compilationUnitList = new ArrayList<>();
        this.codeFileASTMap = new HashMap<>();
        initCompilationUnits();
    }

    private String getCompilationVersion() {
        return switch (jdkVersionEnum) {
            case JDK_1_8 -> JavaCore.VERSION_1_8;
            case JDK_11 -> JavaCore.VERSION_11;
            case JDK_17 -> JavaCore.VERSION_17;
            case JDK_23 -> JavaCore.VERSION_23;
            default -> JavaCore.VERSION_1_8;
        };
    }

    private void initCompilationUnits() {
        Map<String, String> options = JavaCore.getOptions();
        options.put(JavaCore.COMPILER_COMPLIANCE, getCompilationVersion());
        options.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, getCompilationVersion());
        options.put(JavaCore.COMPILER_SOURCE, getCompilationVersion());

        ASTParser parser = ASTParser.newParser(AST.getJLSLatest());
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setCompilerOptions(options);

        parser.setResolveBindings(true);
        parser.setBindingsRecovery(true);
        parser.setStatementsRecovery(true);

        // set parser environment
        String[] encodings = new String[sourcePathList.size()];
        Arrays.fill(encodings, "UTF-8");
        parser.setEnvironment(
                this.classpathList.toArray(new String[0]),
                this.sourcePathList.toArray(new String[0]),
                encodings,
                true
        );

        FileASTRequestor requester = new FileASTRequestor() {
            @Override
            public void acceptAST(String sourceFilePath, CompilationUnit ast) {
                codeFileASTMap.put(sourceFilePath, ast);
                compilationUnitList.add(new Pair<>(FileUtil.file(sourceFilePath), ast));
            }
        };

        encodings = new String[sourceCodePathArray.length];
        Arrays.fill(encodings, "UTF-8");
        parser.createASTs(sourceCodePathArray, encodings, new String[]{}, requester, new NullProgressMonitor());
    }

    public List<Pair<File, IProblem>> analysisError() {
        List<Pair<File, IProblem>> errorProblems = new ArrayList<>();
        for (Pair<File, CompilationUnit> pair : compilationUnitList) {
            List<IProblem> errors = Stream.of(pair.getValue().getProblems())
                    .filter(IProblem::isError).toList();
            for (IProblem error : errors) {
                errorProblems.add(new Pair<>(pair.getKey(), error));
            }
        }

        return errorProblems;
    }

    public List<Pair<File, IProblem>> analysisWarning() {
        List<Pair<File, IProblem>> warningProblems = new ArrayList<>();
        for (Pair<File, CompilationUnit> pair : compilationUnitList) {
            List<IProblem> warnings = Stream.of(pair.getValue().getProblems())
                    .filter(IProblem::isWarning).toList();
            for (IProblem warning : warnings) {
                warningProblems.add(new Pair<>(pair.getKey(), warning));
            }
        }
        return warningProblems;
    }

    public Map<String, CompilationUnit> getCodeFileASTMap() {
        return this.codeFileASTMap;
    }
}
