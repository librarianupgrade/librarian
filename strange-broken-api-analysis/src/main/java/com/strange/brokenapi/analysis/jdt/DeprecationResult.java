package com.strange.brokenapi.analysis.jdt;

import cn.hutool.core.annotation.PropIgnore;
import com.strange.brokenapi.analysis.dependency.DependencyTreeResolver;
import com.strange.brokenapi.analysis.jdt.locate.DeprecationProblemLocation;
import com.strange.brokenapi.analysis.ApiSignature;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.internal.compiler.problem.DefaultProblem;

import java.io.File;
import java.util.List;

@Getter
@Setter
public class DeprecationResult {
    private String moduleName;

    private String deprecatedMessage;

    private String filePath;

    private Integer deprecatedId;

    private String deprecatedType;

    private Integer deprecatedLineNumber;

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
    private DeprecationProblemLocation deprecationProblemLocation;

    @PropIgnore
    private ApiSignature apiSignature;

    @Override
    public String toString() {
        return String.format("DeprecationResult: { deprecatedType='%s', arguments=%s }", deprecatedType, arguments);
    }
}