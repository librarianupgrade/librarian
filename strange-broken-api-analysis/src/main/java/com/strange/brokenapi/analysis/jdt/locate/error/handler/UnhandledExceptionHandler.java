package com.strange.brokenapi.analysis.jdt.locate.error.handler;

import com.strange.brokenapi.analysis.dependency.DependencyNode;
import com.strange.brokenapi.analysis.enums.ApiTypeEnum;
import com.strange.brokenapi.analysis.jdt.ErrorResult;
import com.strange.brokenapi.analysis.jdt.locate.error.ErrorHandler;
import com.strange.brokenapi.analysis.ApiSignature;
import com.strange.brokenapi.analysis.jdt.visitor.MethodBodyInvocationVisitor;
import com.strange.brokenapi.analysis.jdt.visitor.MethodBodyVisitor;
import com.strange.brokenapi.analysis.jdt.visitor.context.MethodInvocationContext;
import org.eclipse.jdt.core.dom.CompilationUnit;

import java.util.List;

public class UnhandledExceptionHandler implements ErrorHandler {
    @Override
    public DependencyNode handle(ErrorResult errorResult) {
        List<String> arguments = errorResult.getArguments();
        // arguments[0] is the class name of the exception
        String needHandledException = arguments.get(0);

        Integer lineNumber = errorResult.getErrorLineNumber();
        MethodBodyInvocationVisitor invocationVisitor = new MethodBodyInvocationVisitor();
        MethodBodyVisitor methodBodyVisitor = new MethodBodyVisitor(lineNumber, invocationVisitor);

        CompilationUnit newAST = errorResult.getNewAST();
        newAST.accept(methodBodyVisitor);
        List<MethodInvocationContext> invocationContextList = invocationVisitor.getInvocationContextList();

        for (MethodInvocationContext context : invocationContextList) {
            if (context.getStartLineNumber() >= lineNumber && lineNumber <= context.getEndLineNumber()) {
                List<String> methodExceptions = context.getMethodExceptions();
                if (isMatchedException(needHandledException, methodExceptions)) {
                    String belongedClassName = context.getBelongedClassName();
                    DependencyNode dependencyNode = errorResult.getOldTreeResolver().getDependencyProperty().getDependencyNodeByClassName(belongedClassName);
                    if (dependencyNode != null) {
                        ApiSignature apiSignature = new ApiSignature();
                        apiSignature.setClassName(belongedClassName);
                        apiSignature.setMethodName(context.getMethodName());
                        apiSignature.setMethodParamList(context.getParameterList());
                        apiSignature.setBrokenApiType(ApiTypeEnum.METHOD);
                        errorResult.setApiSignature(apiSignature);
                    }
                }
            }
        }

        return null;
    }

    private boolean isMatchedException(String needHandledException, List<String> exceptionList) {
        for (String exceptionName : exceptionList) {
            if (needHandledException.contains(exceptionName)) return true;
        }
        return false;
    }
}
