package com.strange.brokenapi.analysis.jdt.locate.deprecation;


import com.strange.brokenapi.analysis.jdt.DeprecationResult;
import com.strange.brokenapi.analysis.jdt.locate.deprecation.handler.UsingDeprecatedConstructorHandler;
import com.strange.brokenapi.analysis.jdt.locate.deprecation.handler.UsingDeprecatedFieldHandler;
import com.strange.brokenapi.analysis.jdt.locate.deprecation.handler.UsingDeprecatedMethodHandler;
import com.strange.brokenapi.analysis.jdt.locate.deprecation.handler.UsingDeprecatedTypeHandler;

public class DeprecationProblemHandlerFactory {

    public static DeprecationHandler getHandler(DeprecationResult deprecationResult) {
        String type = deprecationResult.getDeprecatedType();

        return switch (type) {
            case "UsingDeprecatedMethod" -> new UsingDeprecatedMethodHandler();
            case "UsingDeprecatedConstructor" -> new UsingDeprecatedConstructorHandler();
            case "UsingDeprecatedType" -> new UsingDeprecatedTypeHandler();
            case "UsingDeprecatedField" -> new UsingDeprecatedFieldHandler();
            default -> null;
        };
    }
}
