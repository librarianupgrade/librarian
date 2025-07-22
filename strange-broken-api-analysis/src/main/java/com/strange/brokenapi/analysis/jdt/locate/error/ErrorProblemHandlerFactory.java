package com.strange.brokenapi.analysis.jdt.locate.error;


import com.strange.brokenapi.analysis.jdt.ErrorResult;
import com.strange.brokenapi.analysis.jdt.locate.error.handler.*;

public class ErrorProblemHandlerFactory {

    public static ErrorHandler getHandler(ErrorResult errorResult) {
        String errorType = errorResult.getErrorType();

        return switch (errorType) {
            case "TypeMismatch" -> new TypeMismatchHandler();
            case "FinalMethodCannotBeOverridden" -> new FinalMethodCannotBeOverriddenHandler();
            case "NotVisibleField" -> new NotVisibleFieldHandler();
            case "IncompatibleMethodReference" -> new IncompatibleMethodReferenceHandler();
            case "CannotThrowType" -> new CannotThrowTypeHandler();
            case "InvalidClassInstantiation" -> new InvalidClassInstantiationHandler();
            case "UndefinedConstructor" -> new UndefinedConstructorHandler();
            case "IncompatibleReturnType" -> new IncompatibleReturnTypeHandler();
            case "ParameterMismatch" -> new ParameterMismatchHandler();
            case "UndefinedMethod" -> new UndefinedMethodHandler();
            case "ImportNotFound" -> new ImportNotFoundHandler();
            case "UnhandledException" -> new UnhandledExceptionHandler();
            case "ReturnTypeMismatch" -> new ReturnTypeMismatchHandler();
            case "AmbiguousMethod" -> new AmbiguousMethodHandler();
            case "AbstractMethodMustBeImplemented" -> new AbstractMethodMustBeImplementedHandler();
            case "MissingTypeInMethod" -> new MissingTypeInMethodHandler();
            case "MethodMustOverrideOrImplement" -> new MethodMustOverrideOrImplementHandler();
            case "NotVisibleType" -> new NotVisibleTypeHandler();
            case "IsClassPathCorrectWithReferencingType" -> new IsClassPathCorrectWithReferencingTypeHandler();
            case "UnreachableCatch" -> new UnreachableCatchHandler();
            case "IllegalCast" -> new IllegalCastHandler();
            case "IncorrectArityForParameterizedType" -> new IncorrectArityForParameterizedTypeHandler();
            case "MethodNameClash" -> new MethodNameClashHandler();
            case "IncorrectSwitchType17" -> new IncorrectSwitchType17Handler();
            case "StaticMethodRequested" -> new StaticMethodRequestedHandler();
            case "UnresolvedVariable" -> new UnresolvedVariableHandler();
            case "UndefinedField" -> new UndefinedFieldHandler();
            case "UndefinedConstructorInDefaultConstructor" -> new UndefinedConstructorInDefaultConstructorHandler();
            case "UndefinedType" -> new UndefinedTypeHandler();
            case "UndefinedName" -> new UndefinedNameHandler();
            case "MethodReturnsVoid" -> new MethodReturnsVoidHandler();
            case "NonGenericType" -> new NonGenericTypeHandler();
            case "NotVisibleConstructor" -> new NotVisibleConstructorHandler();
            default -> null;
        };
    }
}
