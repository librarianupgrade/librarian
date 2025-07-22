package com.strange.fix.engine.llm.visitor;

import com.strange.common.utils.ClassUtil;
import com.strange.fix.engine.llm.CodeElementMapper;
import com.strange.fix.engine.llm.entity.ClassModificationMapping;
import com.strange.fix.engine.llm.entity.MethodModificationMapping;
import com.strange.fix.engine.property.AnnotationProperty;
import com.strange.fix.engine.property.ClassProperty;
import com.strange.fix.engine.property.FieldProperty;
import com.strange.fix.engine.property.MethodProperty;
import lombok.Getter;
import lombok.NonNull;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class CodeMergeVisitor extends ASTVisitor {
    private final CompilationUnit previousCodeUnit;

    private final CodeElementMapper codeElementMapper;

    @Getter
    private final ASTRewrite mergeRewrite;

    public CodeMergeVisitor( CompilationUnit previousCodeUnit,  CodeElementMapper codeElementMapper) {
        this.previousCodeUnit = previousCodeUnit;
        this.codeElementMapper = codeElementMapper;
        this.mergeRewrite = ASTRewrite.create(previousCodeUnit.getAST());
    }

    @Override
    public void endVisit(CompilationUnit node) {
        ListRewrite importListRewrite =
                mergeRewrite.getListRewrite(node, CompilationUnit.IMPORTS_PROPERTY);

        // first, delete the deleted import statement
        List<ImportDeclaration> deletedImportStatementList = codeElementMapper.getDeletedImportStatementList();
        List<ImportDeclaration> importNodeList = node.imports();
        for (ImportDeclaration importDeclaration : importNodeList) {
            if (containImportStatement(importDeclaration, deletedImportStatementList)) {
                importListRewrite.remove(importDeclaration, null);
            }
        }

        // second, add the added import statement
        List<ImportDeclaration> addedImportStatementList = codeElementMapper.getAddedImportStatementList();
        for (ImportDeclaration importDeclaration : addedImportStatementList) {
            ASTNode copyNode = ASTNode.copySubtree(previousCodeUnit.getAST(), importDeclaration);
            importListRewrite.insertLast(copyNode, null);
        }
    }

    private boolean containImportStatement(ImportDeclaration targetImportDeclaration,
                                           List<ImportDeclaration> importDeclarationList) {
        for (ImportDeclaration importDeclaration : importDeclarationList) {
            boolean isMatched = targetImportDeclaration.subtreeMatch(new ASTMatcher(false), importDeclaration);
            if (isMatched) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean visit(TypeDeclaration node) {
        String className = node.getName().getIdentifier();

        ClassModificationMapping modifiedClassMapping = codeElementMapper.getModifiedClassByClassName(className);
        List<MethodModificationMapping> modifiedMethodList = codeElementMapper.getModifiedMethodByClassName(className);
        List<MethodProperty> addedMethodList = codeElementMapper.getAddedMethodByClassName(className);
        List<MethodProperty> deletedMethodList = codeElementMapper.getDeletedMethodByClassName(className);
        List<FieldProperty> addedFieldList = codeElementMapper.getAddedFieldByClassName(className);
        List<FieldProperty> deletedFieldList = codeElementMapper.getDeletedFieldByClassName(className);
        List<AnnotationProperty> addedAnnotationList = codeElementMapper.getAddedAnnotationByClassName(className);
        List<AnnotationProperty> deletedAnnotationList = codeElementMapper.getDeletedAnnotationByClassName(className);

        if (modifiedClassMapping != null) {
            // modified the class signature (including the extends super class name and implemented interfaces)
            ClassProperty newClassProperty = modifiedClassMapping.getNewClassProperty();
            TypeDeclaration newClassDeclaration = (TypeDeclaration) newClassProperty.getClassDeclaration();
            Type newSuperclassType = newClassDeclaration.getSuperclassType();
            List<Type> newInterfaceList = newClassDeclaration.superInterfaceTypes();

            // first resolve the extends super class
            if (newSuperclassType != null) {
                Type copySuperType = (Type) ASTNode.copySubtree(previousCodeUnit.getAST(), newSuperclassType);
                if (node.getSuperclassType() != null) {
                    mergeRewrite.replace(node, copySuperType, null);
                } else {
                    mergeRewrite.set(node, TypeDeclaration.SUPERCLASS_TYPE_PROPERTY, copySuperType, null);
                }
            } else {
                if (node.getSuperclassType() != null) {
                    mergeRewrite.remove(node.getSuperclassType(), null);
                }
            }

            // then resolve the implements interfaces
            ListRewrite listRW = mergeRewrite.getListRewrite(
                    node, TypeDeclaration.SUPER_INTERFACE_TYPES_PROPERTY);
            // deleted all existed interfaces
            @SuppressWarnings("unchecked")
            List<Type> oldInterfaceList = node.superInterfaceTypes();
            for (Type oldInterface : oldInterfaceList) {
                listRW.remove(oldInterface, null);
            }
            // added all the new interfaces
            for (Type newInterface : newInterfaceList) {
                Type newIf = (Type) ASTNode.copySubtree(previousCodeUnit.getAST(), newInterface);
                listRW.insertLast(newIf, null);
            }
        }

        // resolve the added annotation
        for (AnnotationProperty annotationProperty : addedAnnotationList) {
            Annotation annotation = annotationProperty.getAnnotation();
            ASTNode copiedAnnotation = ASTNode.copySubtree(previousCodeUnit.getAST(), annotation);
            ListRewrite listRewrite = mergeRewrite.getListRewrite(node, TypeDeclaration.MODIFIERS2_PROPERTY);
            listRewrite.insertFirst(copiedAnnotation, null);
        }

        // resolve the deleted annotation
        for (AnnotationProperty annotationProperty : deletedAnnotationList) {
            Annotation annotation = findAnnotation(annotationProperty.getAnnotation(), node);
            if (annotation != null) {
                mergeRewrite.remove(annotation, null);
            }
        }

        // resolve the deleted method declaration
        for (MethodProperty methodProperty : deletedMethodList) {
            List<MethodDeclaration> methodDeclarationList = Arrays.asList(node.getMethods());
            MethodDeclaration deletedMethodDeclaration = findMethodDeclaration(methodProperty, methodDeclarationList);
            if (deletedMethodDeclaration != null) {
                ListRewrite bodyRewrite = mergeRewrite.getListRewrite(node, TypeDeclaration.BODY_DECLARATIONS_PROPERTY);
                bodyRewrite.remove(deletedMethodDeclaration, null);
            }
        }

        // modified the method declaration
        for (MethodModificationMapping methodModificationMapping : modifiedMethodList) {
            List<MethodDeclaration> methodDeclarationList = Arrays.asList(node.getMethods());
            MethodProperty oldMethodProperty = methodModificationMapping.getOldMethodProperty();
            MethodProperty newMethodProperty = methodModificationMapping.getNewMethodProperty();
            MethodDeclaration targetMethodDeclaration = findMethodDeclaration(oldMethodProperty, methodDeclarationList);
            if (targetMethodDeclaration != null) {
                // replace the target method declaration with new method declaration
                ASTNode parent = targetMethodDeclaration.getParent();
                MethodDeclaration fixMethodDeclaration = newMethodProperty.getMethodDeclaration();
                MethodDeclaration copyMethodDeclaration = (MethodDeclaration) ASTNode.copySubtree(previousCodeUnit.getAST(), fixMethodDeclaration);
                ListRewrite listRewrite = mergeRewrite.getListRewrite(parent,
                        (ChildListPropertyDescriptor) targetMethodDeclaration.getLocationInParent());
                listRewrite.replace(targetMethodDeclaration, copyMethodDeclaration, null);
            }
        }

        // resolve the added method declaration
        for (MethodProperty methodProperty : addedMethodList) {
            ListRewrite bodyRewrite = mergeRewrite.getListRewrite(node, TypeDeclaration.BODY_DECLARATIONS_PROPERTY);
            MethodDeclaration addedMethodDeclaration = methodProperty.getMethodDeclaration();
            MethodDeclaration copyMethodDeclaration = (MethodDeclaration) ASTNode.copySubtree(previousCodeUnit.getAST(), addedMethodDeclaration);
            bodyRewrite.insertLast(copyMethodDeclaration, null);
        }

        // resolve the deleted field declaration
        for (FieldProperty fieldProperty : deletedFieldList) {
            List<FieldDeclaration> fieldDeclarationList = Arrays.asList(node.getFields());
            FieldDeclaration deletedFieldDeclaration = findFieldDeclaration(fieldProperty, fieldDeclarationList);
            if (deletedFieldDeclaration != null) {
                ListRewrite bodyRewrite = mergeRewrite.getListRewrite(node, TypeDeclaration.BODY_DECLARATIONS_PROPERTY);
                bodyRewrite.remove(deletedFieldDeclaration, null);
            }
        }

        // resolve the added field declaration
        for (FieldProperty fieldProperty : addedFieldList) {
            ListRewrite bodyRewrite = mergeRewrite.getListRewrite(node, TypeDeclaration.BODY_DECLARATIONS_PROPERTY);
            FieldDeclaration addedFieldDeclaration = fieldProperty.getFieldDeclaration();
            FieldDeclaration copyMethodDeclaration = (FieldDeclaration) ASTNode.copySubtree(previousCodeUnit.getAST(), addedFieldDeclaration);
            bodyRewrite.insertFirst(copyMethodDeclaration, null);
        }

        return super.visit(node);
    }

    @Override
    public boolean visit(EnumDeclaration node) {
        String className = node.getName().getIdentifier();

        List<MethodDeclaration> methodDeclarationList = new ArrayList<>();
        List<FieldDeclaration> fieldDeclarationList = new ArrayList<>();

        List<Object> bodyDeclarationList = node.bodyDeclarations();
        for (Object o : bodyDeclarationList) {
            if (o instanceof MethodDeclaration methodDeclaration) {
                methodDeclarationList.add(methodDeclaration);
            } else if (o instanceof FieldDeclaration fieldDeclaration) {
                fieldDeclarationList.add(fieldDeclaration);
            }
        }

        List<MethodModificationMapping> modifiedMethodList = codeElementMapper.getModifiedMethodByClassName(className);
        List<MethodProperty> addedMethodList = codeElementMapper.getAddedMethodByClassName(className);
        List<MethodProperty> deletedMethodList = codeElementMapper.getDeletedMethodByClassName(className);
        List<FieldProperty> addedFieldList = codeElementMapper.getAddedFieldByClassName(className);
        List<FieldProperty> deletedFieldList = codeElementMapper.getDeletedFieldByClassName(className);
        List<AnnotationProperty> addedAnnotationList = codeElementMapper.getAddedAnnotationByClassName(className);
        List<AnnotationProperty> deletedAnnotationList = codeElementMapper.getDeletedAnnotationByClassName(className);

        // resolve the added annotation
        for (AnnotationProperty annotationProperty : addedAnnotationList) {
            Annotation annotation = annotationProperty.getAnnotation();
            ASTNode copiedAnnotation = ASTNode.copySubtree(previousCodeUnit.getAST(), annotation);
            ListRewrite listRewrite = mergeRewrite.getListRewrite(node, TypeDeclaration.MODIFIERS2_PROPERTY);
            listRewrite.insertFirst(copiedAnnotation, null);
        }

        // resolve the deleted annotation
        for (AnnotationProperty annotationProperty : deletedAnnotationList) {
            Annotation annotation = findAnnotation(annotationProperty.getAnnotation(), node);
            if (annotation != null) {
                mergeRewrite.remove(annotation, null);
            }
        }

        // resolve the deleted method declaration
        for (MethodProperty methodProperty : deletedMethodList) {
            MethodDeclaration deletedMethodDeclaration = findMethodDeclaration(methodProperty, methodDeclarationList);
            if (deletedMethodDeclaration != null) {
                ListRewrite bodyRewrite = mergeRewrite.getListRewrite(node, TypeDeclaration.BODY_DECLARATIONS_PROPERTY);
                bodyRewrite.remove(deletedMethodDeclaration, null);
            }
        }

        // modified the method declaration
        for (MethodModificationMapping methodModificationMapping : modifiedMethodList) {
            MethodProperty oldMethodProperty = methodModificationMapping.getOldMethodProperty();
            MethodProperty newMethodProperty = methodModificationMapping.getNewMethodProperty();
            MethodDeclaration targetMethodDeclaration = findMethodDeclaration(oldMethodProperty, methodDeclarationList);
            if (targetMethodDeclaration != null) {
                // replace the target method declaration with new method declaration
                ASTNode parent = targetMethodDeclaration.getParent();
                MethodDeclaration fixMethodDeclaration = newMethodProperty.getMethodDeclaration();
                MethodDeclaration copyMethodDeclaration = (MethodDeclaration) ASTNode.copySubtree(previousCodeUnit.getAST(), fixMethodDeclaration);
                ListRewrite listRewrite = mergeRewrite.getListRewrite(parent,
                        (ChildListPropertyDescriptor) targetMethodDeclaration.getLocationInParent());
                listRewrite.replace(targetMethodDeclaration, copyMethodDeclaration, null);
            }
        }

        // resolve the added method declaration
        for (MethodProperty methodProperty : addedMethodList) {
            ListRewrite bodyRewrite = mergeRewrite.getListRewrite(node, TypeDeclaration.BODY_DECLARATIONS_PROPERTY);
            MethodDeclaration addedMethodDeclaration = methodProperty.getMethodDeclaration();
            MethodDeclaration copyMethodDeclaration = (MethodDeclaration) ASTNode.copySubtree(previousCodeUnit.getAST(), addedMethodDeclaration);
            bodyRewrite.insertLast(copyMethodDeclaration, null);
        }

        // resolve the deleted field declaration
        for (FieldProperty fieldProperty : deletedFieldList) {
            FieldDeclaration deletedFieldDeclaration = findFieldDeclaration(fieldProperty, fieldDeclarationList);
            if (deletedFieldDeclaration != null) {
                ListRewrite bodyRewrite = mergeRewrite.getListRewrite(node, TypeDeclaration.BODY_DECLARATIONS_PROPERTY);
                bodyRewrite.remove(deletedFieldDeclaration, null);
            }
        }

        // resolve the added field declaration
        for (FieldProperty fieldProperty : addedFieldList) {
            ListRewrite bodyRewrite = mergeRewrite.getListRewrite(node, TypeDeclaration.BODY_DECLARATIONS_PROPERTY);
            FieldDeclaration addedFieldDeclaration = fieldProperty.getFieldDeclaration();
            FieldDeclaration copyMethodDeclaration = (FieldDeclaration) ASTNode.copySubtree(previousCodeUnit.getAST(), addedFieldDeclaration);
            bodyRewrite.insertFirst(copyMethodDeclaration, null);
        }

        return super.visit(node);
    }

    private Annotation findAnnotation(Annotation annotation, AbstractTypeDeclaration typeDeclaration) {
        List<?> modifiers = typeDeclaration.modifiers();
        for (Object mod : modifiers) {
            if (mod instanceof Annotation anno) {
                if (annotation.subtreeMatch(new ASTMatcher(false), anno)) {
                    return anno;
                }
            }
        }
        return null;
    }

    private MethodDeclaration findMethodDeclaration(MethodProperty targetMethodProperty, List<MethodDeclaration> methodPropertyList) {
        for (MethodDeclaration methodProperty : methodPropertyList) {
            if (isEqualMethod(methodProperty, targetMethodProperty)) {
                return methodProperty;
            }
        }
        return null;
    }

    private boolean isEqualMethod(MethodDeclaration targetMethodDeclaration, MethodProperty methodProperty) {
        String targetMethodName = targetMethodDeclaration.getName().getIdentifier();
        if (!Objects.equals(methodProperty.getMethodName(), targetMethodName)) return false;

        List<String> targetParamList = new ArrayList<>();
        List<?> parameters = targetMethodDeclaration.parameters();
        for (Object paramObj : parameters) {
            SingleVariableDeclaration parameter = (SingleVariableDeclaration) paramObj;
            Type parameterType = parameter.getType();
            String className = getClassName(parameterType);
            if (parameter.isVarargs()) className += "[]";
            targetParamList.add(className);
        }

        List<String> actualParamList = methodProperty.getParameters();
        if (targetParamList.size() != actualParamList.size()) return false;

        for (int i = 0; i < actualParamList.size(); i++) {
            String targetSimpleClassName = ClassUtil.getSimpleClassName(ClassUtil.removeGenericType(targetParamList.get(i)));
            String actualSimpleClassName = ClassUtil.getSimpleClassName(ClassUtil.removeGenericType(actualParamList.get(i)));
            if (!Objects.equals(targetSimpleClassName, actualSimpleClassName)) {
                return false;
            }
        }

        return true;
    }

    private FieldDeclaration findFieldDeclaration(FieldProperty fieldProperty, List<FieldDeclaration> fieldDeclarationList) {
        for (FieldDeclaration fieldDeclaration : fieldDeclarationList) {
            if (isEqualField(fieldDeclaration, fieldProperty)) {
                return fieldDeclaration;
            }
        }
        return null;
    }

    private boolean isEqualField(FieldDeclaration fieldDeclaration, FieldProperty fieldProperty) {
        String targetFieldName = null;
        List<?> fragments = fieldDeclaration.fragments();
        for (Object fragmentObj : fragments) {
            VariableDeclarationFragment fragment = (VariableDeclarationFragment) fragmentObj;
            targetFieldName = fragment.getName().getIdentifier();
        }
        if (!Objects.equals(targetFieldName, fieldProperty.getFieldName())) return false;
        Type type = fieldDeclaration.getType();
        String targetTypeName = getClassName(type);

        String targetSimpleClassName = ClassUtil.getSimpleClassName(targetTypeName);
        String actualSimpleClassName = ClassUtil.getSimpleClassName(fieldProperty.getFieldType());
        return Objects.equals(targetSimpleClassName, actualSimpleClassName);
    }

    private static String getClassName(Type type) {
        if (type == null) return null;
        if (type instanceof SimpleType) {
            return ((SimpleType) type).getName().getFullyQualifiedName();
        } else if (type instanceof ParameterizedType) {
            return ((ParameterizedType) type).getType().toString();
        } else if (type instanceof ArrayType) {
            return ((ArrayType) type).getElementType().toString() + "[]";
        } else if (type instanceof WildcardType) {
            return "";
        }
        return type.toString();
    }
}
