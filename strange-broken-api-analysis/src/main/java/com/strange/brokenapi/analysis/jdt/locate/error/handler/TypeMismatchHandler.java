package com.strange.brokenapi.analysis.jdt.locate.error.handler;

import com.strange.brokenapi.analysis.dependency.DependencyNode;
import com.strange.brokenapi.analysis.dependency.property.DependencyProperty;
import com.strange.brokenapi.analysis.enums.ApiTypeEnum;
import com.strange.brokenapi.analysis.jdt.ErrorResult;
import com.strange.brokenapi.analysis.jdt.locate.error.ErrorHandler;
import com.strange.brokenapi.analysis.ApiSignature;
import com.strange.brokenapi.analysis.jdt.visitor.MethodBodyAssignmentVisitor;
import com.strange.brokenapi.analysis.jdt.visitor.MethodBodyInvocationVisitor;
import com.strange.brokenapi.analysis.jdt.visitor.MethodBodyVisitor;
import com.strange.brokenapi.analysis.jdt.visitor.context.FieldUsageContext;
import com.strange.brokenapi.analysis.jdt.visitor.context.MethodInvocationContext;

import java.util.List;

public class TypeMismatchHandler implements ErrorHandler {
    @Override
    public DependencyNode handle(ErrorResult errorResult) {
        Integer errorLineNumber = errorResult.getErrorLineNumber();
        List<String> arguments = errorResult.getArguments();
        // arguments[0] is the expected provided class name
        // arguments[1] is the actual theoretical class name
        String exceptedClassName = arguments.get(0);
        String actualClassName = arguments.get(1);

        DependencyProperty dependencyProperty = errorResult.getOldTreeResolver().getDependencyProperty();

        // if the type mismatch occur in return value of the method
        MethodBodyInvocationVisitor oldInvocationVisitor = new MethodBodyInvocationVisitor();
        MethodBodyInvocationVisitor newInvocationVisitor = new MethodBodyInvocationVisitor();

        errorResult.getOldAST().accept(new MethodBodyVisitor(errorLineNumber, oldInvocationVisitor));
        errorResult.getNewAST().accept(new MethodBodyVisitor(errorLineNumber, newInvocationVisitor));

        List<MethodInvocationContext> oldInvocationContextList = oldInvocationVisitor.getInvocationContextList();
        List<MethodInvocationContext> newInvocationContextList = newInvocationVisitor.getInvocationContextList();

        if (oldInvocationContextList.size() == newInvocationContextList.size()) {
            for (int i = 0; i < oldInvocationContextList.size(); i++) {
                MethodInvocationContext oldInvocationContext = oldInvocationContextList.get(i);
                if (oldInvocationContext.getStartLineNumber() <= errorLineNumber && errorLineNumber <= oldInvocationContext.getEndLineNumber()) {
                    MethodInvocationContext newInvocationContext = newInvocationContextList.get(i);
                    if (actualClassName.equals(oldInvocationContext.getReturnTypeClassName()) && exceptedClassName.equals(newInvocationContext.getReturnTypeClassName())) {
                        String belongedClassName = oldInvocationContext.getBelongedClassName();
                        DependencyNode dependencyNode = dependencyProperty.getDependencyNodeByClassName(belongedClassName);

                        ApiSignature apiSignature = new ApiSignature();
                        apiSignature.setClassName(belongedClassName);
                        apiSignature.setMethodName(oldInvocationContext.getMethodName());
                        apiSignature.setMethodParamList(oldInvocationContext.getParameterList());
                        apiSignature.setBrokenApiType(ApiTypeEnum.METHOD);
                        errorResult.setApiSignature(apiSignature);
                        return dependencyNode;
                    }
                }
            }
        }

        // if the type mismatch occur in assignment
        MethodBodyAssignmentVisitor methodBodyAssignmentVisitor = new MethodBodyAssignmentVisitor(errorLineNumber);
        errorResult.getOldAST().accept(new MethodBodyVisitor(errorLineNumber, methodBodyAssignmentVisitor));
        FieldUsageContext fieldUsageContext = methodBodyAssignmentVisitor.getFieldUsageContext();
        String belongedClassName = fieldUsageContext.getBelongedClassName();
        DependencyNode dependencyNode = dependencyProperty.getDependencyNodeByClassName(belongedClassName);
        ApiSignature apiSignature = new ApiSignature();
        apiSignature.setClassName(belongedClassName);
        // TODO
        apiSignature.setBrokenApiType(ApiTypeEnum.METHOD);
        errorResult.setApiSignature(apiSignature);
        return dependencyNode;
    }
}
