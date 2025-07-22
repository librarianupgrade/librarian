package com.strange.fix.engine.llm;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.resource.ClassPathResource;
import cn.hutool.core.util.StrUtil;
import com.strange.brokenapi.analysis.ApiSignature;
import com.strange.brokenapi.analysis.BrokenApiUsage;
import com.strange.brokenapi.analysis.jdt.ErrorResult;
import com.strange.brokenapi.analysis.jdt.locate.ErrorProblemLocation;
import com.strange.common.utils.IOUtil;
import com.strange.fix.engine.extraction.hint.CodeMapping;
import com.strange.fix.engine.extraction.hint.FixHint;
import com.strange.fix.engine.extraction.hint.JavaDocHint;
import com.strange.fix.engine.extraction.hint.MigrationCaseHint;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jdt.internal.compiler.problem.DefaultProblem;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

@Slf4j
public class FixPromptGenerator {
    private final static String PROMPT_TEMPLATE_FILE_NAME = "fix_code_template.txt";

    private final static String PROMPT_TEMPLATE;

    static {
        try {
            PROMPT_TEMPLATE = IOUtil.readString(new ClassPathResource(PROMPT_TEMPLATE_FILE_NAME).getStream());
        } catch (IOException e) {
            log.error("ReadPromptTemplateError: ", e);
            throw new RuntimeException(e);
        }
    }

    private final BrokenApiUsage brokenApiUsage;

    private final FixHint fixHint;

    private Integer retryCount;

    public FixPromptGenerator( BrokenApiUsage brokenApiUsage,  FixHint fixHint) {
        this.brokenApiUsage = brokenApiUsage;
        this.fixHint = fixHint;
        this.retryCount = -1;
    }

    public String generate() {
        this.retryCount += 1;
        ErrorResult errorResult = brokenApiUsage.getErrorResult();
        String groupId = "None";
        String artifactId = "None";
        String oldVersion = "None";
        String newVersion = "None";
        ErrorProblemLocation errorProblemLocation = errorResult.getErrorProblemLocation();
        if (errorProblemLocation != null) {
            groupId = errorProblemLocation.getGroupId();
            artifactId = errorProblemLocation.getArtifactId();
            oldVersion = errorProblemLocation.getOldVersion();
            newVersion = errorProblemLocation.getNewVersion();
        }

        String apiSignature = brokenApiUsage.getApiSignature().toString();

        JavaDocHint javaDocHint = fixHint.getJavaDocHint();
        String javaDocContent = "None";
        if (javaDocHint != null) {
            javaDocContent = javaDocHint.getJavaDocContent() != null ? javaDocHint.getJavaDocContent() : "None";
        }

        MigrationCaseHint migrationCaseHint = fixHint.getMigrationCaseHint();
        ShadowFixCase shadowFixCase = chooseShadowFixCase();
        String mappedApiSignature = "None";
        if (migrationCaseHint != null) {
            mappedApiSignature = getMappedApiSignature(brokenApiUsage.getApiSignature(), migrationCaseHint.getMappedApiSignature());
        }

        String oldLibrarySlicedCode = shadowFixCase.getPreviousCode();
        String newLibrarySlicedCode = shadowFixCase.getRefactoredCode();

        return String.format(PROMPT_TEMPLATE, groupId, artifactId, oldVersion, newVersion, apiSignature, mappedApiSignature,
                brokenApiUsage.getBrokenContent(), brokenApiUsage.getBrokenStatement(), getCompilationErrorMessage(errorResult),
                oldLibrarySlicedCode, newLibrarySlicedCode, javaDocContent);
    }

    private String getCompilationErrorMessage( ErrorResult errorResult) {
        DefaultProblem problem = errorResult.getProblem();
        String errorMessage = problem.getMessage();
        String errorArguments = String.join(",", problem.getArguments());
        return StrUtil.format("Compilation Error Message: {}, Arguments: {}", errorMessage, errorArguments);
    }

    private String getMappedApiSignature(ApiSignature apiSignature, ApiSignature mappedApiSignature) {
        if (mappedApiSignature == null || Objects.equals(apiSignature, mappedApiSignature)) {
            return "";
        } else {
            return "You should now use **" + mappedApiSignature + "** in the new version.";
        }
    }

    private ShadowFixCase chooseShadowFixCase() {
        String previousCode = "None";
        String refactoredCode = "None";

        MigrationCaseHint migrationCaseHint = fixHint.getMigrationCaseHint();
        if (migrationCaseHint != null) {
            List<CodeMapping> codeMappingList = migrationCaseHint.getCodeMappingList();

            if (CollUtil.isNotEmpty(codeMappingList)) {
                // if code mapping list is not null, first use the code mapping to fix
                if (retryCount < migrationCaseHint.getCodeMappingList().size()) {
                    CodeMapping codeMapping = codeMappingList.get(retryCount);
                    return new ShadowFixCase(codeMapping.getPreviousCode(), codeMapping.getRefactoredCode());
                } else {
                    CodeMapping codeMapping = codeMappingList.get(0);
                    return new ShadowFixCase(codeMapping.getPreviousCode(), codeMapping.getRefactoredCode());
                }
            }
        }
        return new ShadowFixCase(previousCode, refactoredCode);
    }
}
