package com.strange.fix.engine.extraction.sourcecode.localization.visitor;

import com.strange.brokenapi.analysis.ApiSignature;
import com.strange.brokenapi.analysis.enums.ApiTypeEnum;
import com.strange.common.utils.ClassUtil;
import com.strange.fix.engine.extraction.sourcecode.localization.entity.ApiLocation;
import lombok.NonNull;
import org.eclipse.jdt.core.dom.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public abstract class ApiLocationVisitor extends ASTVisitor {

    protected final CompilationUnit compilationUnit;

    protected final File codeFile;

    protected final ApiSignature apiSignature;

    public ApiLocationVisitor( CompilationUnit compilationUnit,  File codeFile,  ApiSignature apiSignature) {
        this.compilationUnit = compilationUnit;
        this.codeFile = codeFile;
        this.apiSignature = apiSignature;
    }

    public abstract List<ApiLocation> getApiLocations();

    protected ApiSignature nodeToApiSignatureDef(MethodDeclaration methodDeclaration) {
        ApiSignature newApiSignature = new ApiSignature();
        newApiSignature.setBrokenApiType(ApiTypeEnum.METHOD_DEF);
        IMethodBinding methodBinding = methodDeclaration.resolveBinding();
        ITypeBinding[] parameterTypes = null;
        ITypeBinding returnType = null;

        if (methodBinding != null) {
            newApiSignature.setMethodName(methodBinding.getName());
            ITypeBinding typeBinding = methodBinding.getDeclaringClass();
            newApiSignature.setClassName(ClassUtil.removeGenericType(typeBinding.getQualifiedName()));

            parameterTypes = methodBinding.getParameterTypes();
            returnType = methodBinding.getReturnType();
        } else {
            newApiSignature.setMethodName(methodDeclaration.getName().getIdentifier());
        }

        // return type list
        if (returnType != null) {
            newApiSignature.setMethodReturnType(ClassUtil.removeGenericType(returnType.getQualifiedName()));
        } else {
            newApiSignature.setMethodReturnType(getTypeName(methodDeclaration.getReturnType2()));
        }

        // param name list
        if (parameterTypes != null) {
            List<String> paramList = new ArrayList<>();
            for (ITypeBinding parameterType : parameterTypes) {
                String qualifiedParamName = ClassUtil.removeGenericType(parameterType.getQualifiedName());
                paramList.add(qualifiedParamName);
            }
            newApiSignature.setMethodParamList(paramList);
        } else {
            List<String> paramList = new ArrayList<>();
            List<?> parameters = methodDeclaration.parameters();
            for (Object paramObj : parameters) {
                SingleVariableDeclaration parameter = (SingleVariableDeclaration) paramObj;
                Type parameterType = parameter.getType();
                String className = getClassName(parameterType);
                paramList.add(className);
            }
            newApiSignature.setMethodParamList(paramList);
        }
        return newApiSignature;
    }

    protected ApiSignature nodeToApiSignatureDef(TypeDeclaration typeDeclaration) {
        ApiSignature newApiSignature = new ApiSignature();
        newApiSignature.setBrokenApiType(ApiTypeEnum.CLASS_DEF);
        ITypeBinding typeBinding = typeDeclaration.resolveBinding();
        if (typeBinding != null) {
            newApiSignature.setClassName(ClassUtil.removeGenericType(typeBinding.getQualifiedName()));
        } else {
            newApiSignature.setClassName(ClassUtil.removeGenericType(typeDeclaration.getName().getFullyQualifiedName()));
        }
        return newApiSignature;
    }

    protected ApiSignature nodeToApiSignatureDef(FieldDeclaration fieldDeclaration) {
        // TODO 添加field def转换
        ApiSignature newApiSignature = new ApiSignature();
        newApiSignature.setBrokenApiType(ApiTypeEnum.FIELD_DEF);
        VariableDeclarationFragment frag = (VariableDeclarationFragment) fieldDeclaration.fragments().get(0);
        String fieldName = frag.getName().getIdentifier();
        newApiSignature.setFieldName(fieldName);
        Type fieldTypeNode = fieldDeclaration.getType();
        ITypeBinding typeBinding = fieldTypeNode.resolveBinding();
        if (typeBinding != null) {
            String fieldClassName = ClassUtil.removeGenericType(typeBinding.getQualifiedName());
            newApiSignature.setClassName(fieldClassName);
        } else {
            String fieldClassName = ClassUtil.removeGenericType(getClassName(fieldTypeNode));
            newApiSignature.setClassName(fieldClassName);
        }

        IVariableBinding varBinding = frag.resolveBinding();
        ITypeBinding declaringClassBinding = varBinding.getDeclaringClass();
        String fieldBelongedClassName = ClassUtil.removeGenericType(declaringClassBinding.getQualifiedName());
        newApiSignature.setFieldBelongedClassName(fieldBelongedClassName);

        return newApiSignature;
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

    private static String getTypeName(Type type) {
        if (type instanceof SimpleType) {
            SimpleType simpleType = (SimpleType) type;
            return simpleType.getName().getFullyQualifiedName();
        } else if (type instanceof ArrayType) {
            ArrayType arrayType = (ArrayType) type;
            return getTypeName(arrayType.getElementType()) + "[]";
        } else if (type instanceof ParameterizedType) {
            ParameterizedType paramType = (ParameterizedType) type;
            return getTypeName(paramType.getType()) + "<" + paramType.typeArguments() + ">";
        }
        return type.toString();
    }

    protected TypeDeclaration findAncestorTypeDeclaration(ASTNode node) {
        ASTNode cur = node;
        while (cur != null) {
            if (cur instanceof TypeDeclaration) {
                return (TypeDeclaration) cur;
            }
            cur = cur.getParent();
        }
        return null;
    }

    protected MethodDeclaration findAncestorMethodDeclaration(ASTNode node) {
        ASTNode cur = node;
        while (cur != null) {
            if (cur instanceof MethodDeclaration) {
                return (MethodDeclaration) cur;
            }
            cur = cur.getParent();
        }
        return null;
    }

    protected FieldDeclaration findAncestorFieldDeclaration(ASTNode node) {
        ASTNode cur = node;
        while (cur != null) {
            if (cur instanceof FieldDeclaration) {
                return (FieldDeclaration) cur;
            }
            cur = cur.getParent();
        }
        return null;
    }
}
