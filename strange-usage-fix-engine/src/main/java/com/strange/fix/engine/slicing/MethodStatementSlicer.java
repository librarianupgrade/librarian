package com.strange.fix.engine.slicing;

import cn.hutool.core.io.file.FileReader;
import com.strange.brokenapi.analysis.ApiSignature;
import com.strange.common.utils.ClassUtil;
import com.strange.common.utils.CodeUtil;
import com.strange.common.utils.JDTUtil;
import com.strange.fix.engine.property.ClassProperty;
import com.strange.fix.engine.property.MethodProperty;
import com.strange.fix.engine.property.PropertyExtractor;
import com.strange.fix.engine.taint.TaintFlowAnalyzer;
import lombok.extern.slf4j.Slf4j;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Type;

import java.io.File;
import java.util.*;

@Slf4j
public class MethodStatementSlicer extends CodeSlicer {
    private List<ClassProperty> classPropertyList;
    private List<Integer> slicedCodeLineList;
    private final Integer lineNumber;

    // lineNumbers need to keep all the number in lineNumbers should to belong to a same method
    public MethodStatementSlicer(File slicedFile, List<Integer> lineNumbers, File jarFile, ApiSignature apiSignature) {
        super(slicedFile, lineNumbers, jarFile, apiSignature);
        this.slicedCodeLineList = null;
        this.lineNumber = lineNumbers.get(0);
    }

    private MethodProperty getMethodPropertyContainTargetLine() {
        classPropertyList = PropertyExtractor.getFileProperties(slicedFile);
        for (ClassProperty classProperty : classPropertyList) {
            List<MethodProperty> methodPropertyList = classProperty.getMethodPropertyList();
            for (MethodProperty methodProperty : methodPropertyList) {
                if (methodProperty.getStartLineNumber() <= lineNumber && lineNumber <= methodProperty.getEndLineNumber()) {
                    return methodProperty;
                }
            }
        }
        return null;
    }

    private List<Integer> slicedCodeLine() {
        if (slicedCodeLineList == null) {
            MethodProperty methodProperty = getMethodPropertyContainTargetLine();
            if (methodProperty == null) {
                slicedCodeLineList = List.of(lineNumber);
                return slicedCodeLineList;
            }
            
            SootMethod sootMethod = getTargetSootMethod(methodProperty);
            if (sootMethod == null) {
//                log.warn("GetSootMethodIsNull");
                slicedCodeLineList = List.of(lineNumber);
                return slicedCodeLineList;
            }

            TaintFlowAnalyzer taintFlowAnalyzer = new TaintFlowAnalyzer(slicedFile, apiSignature, lineNumberList, sootMethod,
                    sootMethod.getDeclaringClass().getName());
            slicedCodeLineList = taintFlowAnalyzer.taintAnalysis();
        }
        return slicedCodeLineList;
    }

    @Deprecated
    @Override
    public SlicingResult slicedCodeWithoutSyntaxCompletion() {
        String sourceCode = new FileReader(slicedFile).readString();
        Set<String> codeStatementSet = new HashSet<>();
        List<Integer> lineNumberList = slicedCodeLine();
        List<String> statementList = new ArrayList<>();
        for (Integer lineNumber : lineNumberList) {
            String statement = JDTUtil.getStatementByLineNumber(sourceCode, lineNumber);
            if (!codeStatementSet.contains(statement)) {
                codeStatementSet.add(statement);
                statementList.add(statement);
            }
        }
        String slicedCode = String.join("\n", statementList).replaceAll("\n", " ")
                .replaceAll("\\s+", " ").strip();
        slicedCode = CodeUtil.removeJavaDoc(slicedCode);
        return new SlicingResult(lineNumberList, slicedCode);
    }

    @Override
    public SlicingResult slicedCodeWithSyntaxCompletion() {
        // Perform taint analysis to get all tainted line numbers
        List<Integer> lineNumberList = slicedCodeLine();
        String slicedCode = MethodSlicingHelper.trimWithLineNumbers(slicedFile, lineNumberList);
        slicedCode = CodeUtil.removeJavaDoc(slicedCode);
        return new SlicingResult(lineNumberList, slicedCode);
    }

    private SootMethod getTargetSootMethod(MethodProperty methodProperty) {
        String belongedClassName = methodProperty.getBelongedClassName();
        SootClass sootClass = Scene.v().getSootClass(belongedClassName);
        return getSootMethodByProperty(sootClass, methodProperty);
    }

    private SootMethod getSootMethodByProperty(SootClass sootClass, MethodProperty methodProperty) {
        List<SootMethod> methods = sootClass.getMethods();
        for (SootMethod sootMethod : methods) {
            if (isEqual(sootMethod, methodProperty)) {
                return sootMethod;
            }
        }
        return null;
    }

    private boolean isEqual(SootMethod sootMethod, MethodProperty methodProperty) {
        String targetMethodName = methodProperty.getMethodName();
        List<String> targetParameters = methodProperty.getParameters();

        if (methodProperty.isConstructor()) {
            String className = sootMethod.getDeclaringClass().getName().replaceAll("\\$", ".");
            if (!Objects.equals(targetMethodName, ClassUtil.getSimpleClassName(className))) {
                return false;
            }
        } else {
            if (!Objects.equals(targetMethodName, sootMethod.getName())) {
                return false;
            }
        }

        if (targetParameters.size() != sootMethod.getParameterCount()) return false;

        List<Type> parameterTypes = sootMethod.getParameterTypes();

        for (int i = 0; i < targetParameters.size(); i++) {
            String targetSimpleParamClassName = ClassUtil.getSimpleClassName(targetParameters.get(i));
            String actualClassName = parameterTypes.get(i).toQuotedString().replaceAll("\\$", ".");
            String actualSimpleClassName = ClassUtil.getSimpleClassName(actualClassName);
            if (!Objects.equals(targetSimpleParamClassName, actualSimpleClassName)) return false;
        }
        return true;
    }
}
