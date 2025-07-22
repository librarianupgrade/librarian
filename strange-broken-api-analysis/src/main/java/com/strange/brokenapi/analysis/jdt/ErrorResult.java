package com.strange.brokenapi.analysis.jdt;

import cn.hutool.core.annotation.PropIgnore;
import com.strange.brokenapi.analysis.ApiSignature;
import com.strange.brokenapi.analysis.dependency.DependencyTreeResolver;
import com.strange.brokenapi.analysis.jdt.locate.ErrorProblemLocation;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.internal.compiler.problem.DefaultProblem;

import java.io.File;
import java.util.List;
import java.util.Objects;

@Getter
@Setter
public class ErrorResult {
    private String moduleName;

    private String errorMessage;

    private String filePath;

    private Integer errorId;

    private String errorType;

    private Integer errorLineNumber;

    private String originatingFileName;

    private List<String> arguments;

    @PropIgnore
    private File codeFile;

    @PropIgnore
    private CompilationUnit oldAST;

    @PropIgnore
    private CompilationUnit newAST;

    @PropIgnore
    private DependencyTreeResolver oldTreeResolver;

    @PropIgnore
    private DependencyTreeResolver newTreeResolver;

    @PropIgnore
    private DefaultProblem problem;

    @PropIgnore
    private ErrorProblemLocation errorProblemLocation;

    @PropIgnore
    private ApiSignature apiSignature;

    @Override
    public String toString() {
        return "ErrorResult{" + "filePath='" + filePath + '\'' +
                ", errorType='" + errorType + '\'' +
                ", arguments=" + arguments +
                ", apiSignature=" + apiSignature +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ErrorResult that = (ErrorResult) o;
        return Objects.equals(problem, that.problem);
    }

    @Override
    public int hashCode() {
        return problem.hashCode();
    }
}