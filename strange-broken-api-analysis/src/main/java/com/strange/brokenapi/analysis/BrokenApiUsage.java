package com.strange.brokenapi.analysis;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.file.FileReader;
import com.strange.brokenapi.analysis.enums.ApiTypeEnum;
import com.strange.brokenapi.analysis.jdt.ErrorResult;
import com.strange.common.utils.JDTUtil;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.jface.text.BadLocationException;

import java.util.Objects;
import java.util.Set;

@Getter
@Setter
public class BrokenApiUsage {
    private Set<String> SPECIAL_ERROR_TYPE = Set.of("MethodMustOverrideOrImplement", "AbstractMethodMustBeImplemented");

    private ErrorResult errorResult;

    private String brokenStatement;

    private String trimmedBrokenStatement;

    private String brokenContent; // code with syntax correct structure

    private String slicedBrokenCode; // sliced code with compilation error

    private String fingerprint; // to check whether two broken api usage is equal

    private ApiTypeEnum brokenApiType;

    private ApiSignature apiSignature;

    public static BrokenApiUsage extractFromErrorResult(ErrorResult errorResult) throws BadLocationException {
        BrokenApiUsage brokenApiUsage = new BrokenApiUsage();
        brokenApiUsage.setErrorResult(errorResult);
        ApiSignature apiSignature = errorResult.getApiSignature();
        brokenApiUsage.setApiSignature(apiSignature);
        if (apiSignature != null) {
            brokenApiUsage.setBrokenApiType(apiSignature.getBrokenApiType());
        }

        String sourceCode = new FileReader(errorResult.getFilePath()).readString();

        // retrieve the statement contained the broken api usage
        Integer errorLineNumber = errorResult.getErrorLineNumber();
        String brokenStatement = JDTUtil.getStatementByLineNumber(sourceCode, errorLineNumber);
        brokenApiUsage.setBrokenStatement(brokenStatement);

        if (brokenStatement != null) {
            brokenApiUsage.setTrimmedBrokenStatement(brokenStatement.replaceAll("\\s+", ""));
        }

        // retrieve the content contained the broken api usage is done in the `FixEngine`

        // generate the fingerprint for the broken api usage
        String fingerprint = BrokenApiUsageSignatureGenerator.generate(brokenApiUsage);
        brokenApiUsage.setFingerprint(fingerprint);
        return brokenApiUsage;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BrokenApiUsage that = (BrokenApiUsage) o;
        if (SPECIAL_ERROR_TYPE.contains(this.errorResult.getErrorType())) {
            return Objects.equals(this.fingerprint, that.fingerprint) &&
                    Objects.equals(this.trimmedBrokenStatement, that.trimmedBrokenStatement) &&
                    Objects.equals(this.errorResult.getFilePath(), that.errorResult.getFilePath()) &&
                    CollUtil.isEqualList(this.errorResult.getArguments(), that.errorResult.getArguments());
        } else {
            return Objects.equals(this.fingerprint, that.fingerprint) &&
                    Objects.equals(this.trimmedBrokenStatement, that.trimmedBrokenStatement) &&
                    Objects.equals(this.errorResult.getFilePath(), that.errorResult.getFilePath());
        }
    }

    @Override
    public int hashCode() {
        int hashCode = 0;
        hashCode = 31 * hashCode + Objects.hashCode(fingerprint);
        hashCode = 31 * hashCode + Objects.hashCode(trimmedBrokenStatement);
        hashCode = 31 * hashCode + Objects.hashCode(errorResult.getFilePath());
        if (SPECIAL_ERROR_TYPE.contains(this.errorResult.getErrorType())) {
            hashCode = 31 * hashCode + errorResult.getArguments().hashCode();
        }
        return hashCode;
    }

    @Override
    public String toString() {
        return "BrokenApiUsage{" +
                "errorResult=" + errorResult +
                ", apiSignature=" + apiSignature +
                '}';
    }
}
