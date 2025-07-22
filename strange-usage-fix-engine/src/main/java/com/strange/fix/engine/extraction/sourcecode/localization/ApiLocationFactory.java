package com.strange.fix.engine.extraction.sourcecode.localization;

import com.strange.brokenapi.analysis.ApiSignature;
import com.strange.brokenapi.analysis.enums.ApiTypeEnum;

import java.io.File;

public class ApiLocationFactory {
    public static ApiLocator getApiLocator(File projectDir, ApiTypeEnum apiTypeEnum, ApiSignature apiSignature) {
        switch (apiTypeEnum) {
            case METHOD -> {
                return new MethodApiInvocationLocator(projectDir, apiSignature);
            }
            case METHOD_DEF -> {
                return new MethodApiDefinitionLocator(projectDir, apiSignature);
            }
            case ABSTRACT_METHOD -> {
                return new AbstractMethodApiLocator(projectDir, apiSignature);
            }
            case CLASS -> {
                return new ClassApiUsageLocator(projectDir, apiSignature);
            }
            case CLASS_DEF -> {
                return new ClassApiDefinitionLocator(projectDir, apiSignature);
            }
            case CONSTRUCTOR -> {
                return new ConstructorApiInvocationLocator(projectDir, apiSignature);
            }
            case FIELD -> {
                return new FieldApiUsageLocator(projectDir, apiSignature);
            }
            case FIELD_DEF -> {
                return new FieldApiDefinitionLocator(projectDir, apiSignature);
            }
            // TODO add field locator in factory
            default -> {
                return null;
            }
        }
    }
}
