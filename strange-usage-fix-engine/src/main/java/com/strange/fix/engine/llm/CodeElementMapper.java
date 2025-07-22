package com.strange.fix.engine.llm;

import cn.hutool.core.util.StrUtil;
import com.strange.common.utils.ClassUtil;
import com.strange.common.utils.JDTUtil;
import com.strange.fix.engine.llm.entity.ClassModificationMapping;
import com.strange.fix.engine.llm.entity.MethodModificationMapping;
import com.strange.fix.engine.property.*;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.eclipse.jdt.core.dom.ASTMatcher;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ImportDeclaration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
public class CodeElementMapper {

    private final File promptCodeFile;

    private final File llmCodeFile;


    @Getter
    private final List<AnnotationProperty> addedAnnotationPropertyList;

    @Getter
    private final List<AnnotationProperty> deletedAnnotationPropertyList;

    @Getter
    private final List<ClassModificationMapping> modifiedClassPropertyList;

    @Getter
    private final List<ImportDeclaration> addedImportStatementList;

    @Getter
    private final List<ImportDeclaration> deletedImportStatementList;

    @Getter
    private final List<MethodProperty> addedMethodPropertyList;

    @Getter
    private final List<MethodModificationMapping> modifiedMethodPropertyList;

    @Getter
    private final List<MethodProperty> deletedMethodPropertyList;

    @Getter
    private final List<FieldProperty> addedFieldPropertyList;

    @Getter
    private final List<FieldProperty> deletedFieldPropertyList;

    public CodeElementMapper( File promptCodeFile,  File llmCodeFile) {
        this.promptCodeFile = promptCodeFile;
        this.llmCodeFile = llmCodeFile;

        // init the diff list
        addedAnnotationPropertyList = new ArrayList<>();
        deletedAnnotationPropertyList = new ArrayList<>();
        deletedImportStatementList = new ArrayList<>();
        addedImportStatementList = new ArrayList<>();
        modifiedClassPropertyList = new ArrayList<>();
        addedMethodPropertyList = new ArrayList<>();
        deletedMethodPropertyList = new ArrayList<>();
        modifiedMethodPropertyList = new ArrayList<>();
        addedFieldPropertyList = new ArrayList<>();
        deletedFieldPropertyList = new ArrayList<>();

        startImportStatementMapping();
        startClassElementMapping();
    }

    private void startImportStatementMapping() {
        List<ImportDeclaration> promptCodeImportStatements = JDTUtil.getImportDeclarations(promptCodeFile);
        List<ImportDeclaration> llmCodeImportStatements = JDTUtil.getImportDeclarations(llmCodeFile);

        for (ImportDeclaration importStatement : promptCodeImportStatements) {
            boolean deleted = llmCodeImportStatements.stream()
                    .noneMatch(llmCodeImportStatement -> isEqualImportNode(importStatement, llmCodeImportStatement));
            if (deleted) {
                // if the llm code don't contain this statement, it indicates this statement is deleted
                deletedImportStatementList.add(importStatement);
            }
        }

        for (ImportDeclaration importStatement : llmCodeImportStatements) {
            boolean added = promptCodeImportStatements.stream()
                    .noneMatch(promptCodeImportStatement -> isEqualImportNode(importStatement, promptCodeImportStatement));
            if (added) {
                // if the prompt code don't contain this statement, it indicates this statement is newly added
                addedImportStatementList.add(importStatement);
            }
        }
    }

    private void startClassElementMapping() {
        Map<String, ClassProperty> promptCodeMap = PropertyExtractor.getFilePropertyMap(promptCodeFile);
        Map<String, ClassProperty> llmCodeMap = PropertyExtractor.getFilePropertyMap(llmCodeFile);

        for (String className : promptCodeMap.keySet()) {
            if (promptCodeMap.containsKey(className) && llmCodeMap.containsKey(className)) {
                ClassProperty promptCodeClassProperty = promptCodeMap.get(className);
                ClassProperty llmCodeClassProperty = llmCodeMap.get(className);
                // First check the modified class pair (if not equal indicates that the class is modified)
                if (!isEqualClassDefinition(promptCodeClassProperty, llmCodeClassProperty)) {
                    ClassModificationMapping classModificationMapping = new ClassModificationMapping(promptCodeClassProperty, llmCodeClassProperty);
                    modifiedClassPropertyList.add(classModificationMapping);
                }
                // First check the deleted, added annotation in the class declaration
                startAnnotationMapping(promptCodeClassProperty, llmCodeClassProperty);
                // Then check the deleted, added, or modified method declaration
                startMethodMapping(promptCodeClassProperty, llmCodeClassProperty);
                // Lastly check the deleted, added field declaration
                startFieldMapping(promptCodeClassProperty, llmCodeClassProperty);
            } else {
                log.warn("The property‐map keys for Prompt Code and LLM Code are in-consistent.");
            }
        }
    }

    private void startAnnotationMapping(ClassProperty promptCodeClassProperty, ClassProperty llmCodeClassProperty) {
        List<AnnotationProperty> promptCodeAnnotationPropertyList = promptCodeClassProperty.getAnnotationPropertyList();
        List<AnnotationProperty> llmCodeAnnotationPropertyList = llmCodeClassProperty.getAnnotationPropertyList();

        for (AnnotationProperty annotationProperty : promptCodeAnnotationPropertyList) {
            AnnotationProperty anno = findAnnotationProperty(annotationProperty, llmCodeAnnotationPropertyList);
            if (anno == null) {
                deletedAnnotationPropertyList.add(annotationProperty);
            }
        }

        for (AnnotationProperty annotationProperty : llmCodeAnnotationPropertyList) {
            AnnotationProperty anno = findAnnotationProperty(annotationProperty, promptCodeAnnotationPropertyList);
            if (anno == null) {
                addedAnnotationPropertyList.add(annotationProperty);
            }
        }
    }

    private void startMethodMapping(ClassProperty promptCodeClassProperty, ClassProperty llmCodeClassProperty) {
        List<MethodProperty> promptCodeMethodPropertyList = promptCodeClassProperty.getMethodPropertyList();
        List<MethodProperty> llmCodeMethodPropertyList = llmCodeClassProperty.getMethodPropertyList();
        for (MethodProperty methodProperty : promptCodeMethodPropertyList) {
            MethodProperty m = findMethodProperty(methodProperty, llmCodeMethodPropertyList);
            if (m == null) {
                // it indicates this method is deleted
                deletedMethodPropertyList.add(methodProperty);
            } else {
                if (!Objects.equals(methodProperty.getSourceCode(), m.getSourceCode())) {
                    // it indicates this method is modified
                    MethodModificationMapping methodModificationMapping = new MethodModificationMapping(methodProperty, m);
                    modifiedMethodPropertyList.add(methodModificationMapping);
                }
            }
        }

        for (MethodProperty methodProperty : llmCodeMethodPropertyList) {
            MethodProperty m = findMethodProperty(methodProperty, promptCodeMethodPropertyList);
            if (m == null) {
                // it indicates this method is added
                addedMethodPropertyList.add(methodProperty);
            }
        }
    }

    private void startFieldMapping(ClassProperty promptCodeClassProperty, ClassProperty llmCodeClassProperty) {
        List<FieldProperty> promptCodeFieldPropertyList = promptCodeClassProperty.getFieldPropertyList();
        List<FieldProperty> llmCodeFieldPropertyList = llmCodeClassProperty.getFieldPropertyList();
        for (FieldProperty fieldProperty : promptCodeFieldPropertyList) {
            FieldProperty f = findFieldProperty(fieldProperty, llmCodeFieldPropertyList);
            if (f == null) {
                // it indicates this field is deleted
                deletedFieldPropertyList.add(fieldProperty);
            }
        }

        for (FieldProperty fieldProperty : llmCodeFieldPropertyList) {
            FieldProperty f = findFieldProperty(fieldProperty, promptCodeFieldPropertyList);
            if (f == null) {
                // it indicates this field is added
                addedFieldPropertyList.add(fieldProperty);
            }
        }
    }

    private boolean isEqualImportNode(ImportDeclaration node1, ImportDeclaration node2) {
        return node1.subtreeMatch(new ASTMatcher(false), node2);
    }

    // Compare the super class name and the interface name list whether is equal
    private boolean isEqualClassDefinition(ClassProperty promptCodeClassProperty, ClassProperty llmCodeClassProperty) {
        String promptCodeSuperClassName = promptCodeClassProperty.getSuperClassName();
        String llmCodeSuperClassName = llmCodeClassProperty.getSuperClassName();
        if (!Objects.equals(promptCodeSuperClassName, llmCodeSuperClassName)) return false;

        List<String> promptCodeInterfaces = promptCodeClassProperty.getInterfaces();
        List<String> llmCodeInterfaces = llmCodeClassProperty.getInterfaces();
        if (promptCodeInterfaces.size() != llmCodeInterfaces.size()) return false;

        return CollectionUtils.isEqualCollection(promptCodeInterfaces, llmCodeInterfaces);
    }

    private AnnotationProperty findAnnotationProperty(AnnotationProperty targetAnnotationProperty, List<AnnotationProperty> annotationPropertyList) {
        for (AnnotationProperty annotationProperty : annotationPropertyList) {
            Annotation annotation = annotationProperty.getAnnotation();
            Annotation targetAnnotation = targetAnnotationProperty.getAnnotation();
            if (targetAnnotation.subtreeMatch(new ASTMatcher(false), annotation)) {
                return annotationProperty;
            }
        }
        return null;
    }

    private MethodProperty findMethodProperty(MethodProperty targetMethodProperty, List<MethodProperty> methodPropertyList) {
        for (MethodProperty methodProperty : methodPropertyList) {
            if (isEqualMethod(targetMethodProperty, methodProperty)) {
                return methodProperty;
            }
        }
        return null;
    }

    private FieldProperty findFieldProperty(FieldProperty targetFieldProperty, List<FieldProperty> fieldPropertyList) {
        for (FieldProperty fieldProperty : fieldPropertyList) {
            if (isEqualField(targetFieldProperty, fieldProperty)) {
                return fieldProperty;
            }
        }
        return null;
    }

    private boolean isEqualMethod( MethodProperty targetMethodProperty,  MethodProperty actualmethodProperty) {
        String targetMethodName = targetMethodProperty.getMethodName();
        String actualMethodName = actualmethodProperty.getMethodName();
        if (!Objects.equals(actualMethodName, targetMethodName)) return false;

        List<String> targetParameters = targetMethodProperty.getParameters();
        List<String> actualParameters = actualmethodProperty.getParameters();

        return CollectionUtils.isEqualCollection(targetParameters, actualParameters);
    }

    private boolean isEqualField( FieldProperty targetFieldProperty,  FieldProperty actualFieldProperty) {
        FieldDeclaration targetFieldDeclaration = targetFieldProperty.getFieldDeclaration();
        FieldDeclaration actualFieldDeclaration = actualFieldProperty.getFieldDeclaration();
        return targetFieldDeclaration.subtreeMatch(new ASTMatcher(false), actualFieldDeclaration);
    }

    public ClassModificationMapping getModifiedClassByClassName(String className) {
        String simpleClassName = ClassUtil.getSimpleClassName(ClassUtil.removeGenericType(className));
        for (ClassModificationMapping classModificationMapping : this.modifiedClassPropertyList) {
            String actualClassName = classModificationMapping.getOldClassProperty().getClassName();
            String actualSimpleClassName = ClassUtil.getSimpleClassName(ClassUtil.removeGenericType(actualClassName));
            if (isEqualClassName(simpleClassName, actualSimpleClassName)) {
                return classModificationMapping;
            }
        }
        return null;
    }

    public List<AnnotationProperty> getAddedAnnotationByClassName(String className) {
        List<AnnotationProperty> selectedAnnotationList = new ArrayList<>();
        String simpleClassName = ClassUtil.getSimpleClassName(ClassUtil.removeGenericType(className));
        for (AnnotationProperty annotationProperty : addedAnnotationPropertyList) {
            String belongedClassName = annotationProperty.getBelongedClassName();
            String actualSimpleClassName = ClassUtil.getSimpleClassName(ClassUtil.removeGenericType(belongedClassName));
            if (isEqualClassName(simpleClassName, actualSimpleClassName)) {
                selectedAnnotationList.add(annotationProperty);
            }
        }
        return selectedAnnotationList;
    }

    public List<AnnotationProperty> getDeletedAnnotationByClassName(String className) {
        List<AnnotationProperty> selectedAnnotationList = new ArrayList<>();
        String simpleClassName = ClassUtil.getSimpleClassName(ClassUtil.removeGenericType(className));
        for (AnnotationProperty annotationProperty : deletedAnnotationPropertyList) {
            String belongedClassName = annotationProperty.getBelongedClassName();
            String actualSimpleClassName = ClassUtil.getSimpleClassName(ClassUtil.removeGenericType(belongedClassName));
            if (isEqualClassName(simpleClassName, actualSimpleClassName)) {
                selectedAnnotationList.add(annotationProperty);
            }
        }
        return selectedAnnotationList;
    }

    public List<MethodProperty> getAddedMethodByClassName(String className) {
        String simpleClassName = ClassUtil.getSimpleClassName(ClassUtil.removeGenericType(className));
        List<MethodProperty> selectedMethodPropertyList = new ArrayList<>();
        for (MethodProperty methodProperty : this.addedMethodPropertyList) {
            String belongedClassName = methodProperty.getBelongedClassName();
            String belongedSimpleClassName = ClassUtil.getSimpleClassName(ClassUtil.removeGenericType(belongedClassName));
            if (isEqualClassName(simpleClassName, belongedSimpleClassName)) {
                selectedMethodPropertyList.add(methodProperty);
            }
        }
        return selectedMethodPropertyList;
    }

    public List<MethodProperty> getDeletedMethodByClassName(String className) {
        String simpleClassName = ClassUtil.getSimpleClassName(ClassUtil.removeGenericType(className));
        List<MethodProperty> selectedMethodPropertyList = new ArrayList<>();
        for (MethodProperty methodProperty : this.deletedMethodPropertyList) {
            String belongedClassName = methodProperty.getBelongedClassName();
            String belongedSimpleClassName = ClassUtil.getSimpleClassName(ClassUtil.removeGenericType(belongedClassName));
            if (isEqualClassName(simpleClassName, belongedSimpleClassName)) {
                selectedMethodPropertyList.add(methodProperty);
            }
        }
        return selectedMethodPropertyList;
    }


    public List<MethodModificationMapping> getModifiedMethodByClassName(String className) {
        String simpleClassName = ClassUtil.getSimpleClassName(ClassUtil.removeGenericType(className));
        List<MethodModificationMapping> selectedMethodMappingList = new ArrayList<>();
        for (MethodModificationMapping modificationMapping : this.modifiedMethodPropertyList) {
            String belongedClassName = modificationMapping.getOldMethodProperty().getBelongedClassName();
            String belongedSimpleClassName = ClassUtil.getSimpleClassName(ClassUtil.removeGenericType(belongedClassName));
            if (isEqualClassName(simpleClassName, belongedSimpleClassName)) {
                selectedMethodMappingList.add(modificationMapping);
            }
        }
        return selectedMethodMappingList;
    }

    public List<FieldProperty> getAddedFieldByClassName(String className) {
        String simpleClassName = ClassUtil.getSimpleClassName(ClassUtil.removeGenericType(className));
        List<FieldProperty> selectedFieldPropertyList = new ArrayList<>();
        for (FieldProperty fieldProperty : this.addedFieldPropertyList) {
            String belongedClassName = fieldProperty.getBelongedClassName();
            String belongedSimpleClassName = ClassUtil.getSimpleClassName(ClassUtil.removeGenericType(belongedClassName));
            if (isEqualClassName(simpleClassName, belongedSimpleClassName)) {
                selectedFieldPropertyList.add(fieldProperty);
            }
        }
        return selectedFieldPropertyList;
    }

    public List<FieldProperty> getDeletedFieldByClassName(String className) {
        String simpleClassName = ClassUtil.getSimpleClassName(ClassUtil.removeGenericType(className));
        List<FieldProperty> selectedFieldPropertyList = new ArrayList<>();
        for (FieldProperty fieldProperty : this.deletedFieldPropertyList) {
            String belongedClassName = fieldProperty.getBelongedClassName();
            String belongedSimpleClassName = ClassUtil.getSimpleClassName(ClassUtil.removeGenericType(belongedClassName));
            if (isEqualClassName(simpleClassName, belongedSimpleClassName)) {
                selectedFieldPropertyList.add(fieldProperty);
            }
        }
        return selectedFieldPropertyList;
    }

    private boolean isEqualClassName(String targetClassName, String actualClassName) {
        targetClassName = targetClassName.replaceAll("\\$", ".");
        List<String> split = StrUtil.split(targetClassName, ".");
        targetClassName = split.get(split.size() - 1);

        actualClassName = actualClassName.replaceAll("\\$", ".");
        split = StrUtil.split(actualClassName, ".");
        actualClassName = split.get(split.size() - 1);
        return Objects.equals(targetClassName, actualClassName);
    }
}
