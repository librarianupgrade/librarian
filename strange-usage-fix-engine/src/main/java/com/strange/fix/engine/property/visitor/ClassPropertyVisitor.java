package com.strange.fix.engine.property.visitor;

import com.strange.common.utils.ClassUtil;
import com.strange.fix.engine.formatter.CodeFormatter;
import com.strange.fix.engine.property.AnnotationProperty;
import com.strange.fix.engine.property.ClassProperty;
import com.strange.fix.engine.property.FieldProperty;
import com.strange.fix.engine.property.MethodProperty;
import org.eclipse.jdt.core.dom.*;

import java.util.ArrayList;
import java.util.List;

public class ClassPropertyVisitor extends ASTVisitor {

    private final CompilationUnit compilationUnit;

    private final List<ClassProperty> classPropertyList;

    public ClassPropertyVisitor(CompilationUnit compilationUnit) {
        this.compilationUnit = compilationUnit;
        classPropertyList = new ArrayList<>();
    }

    @Override
    public boolean visit(TypeDeclaration node) {
        ClassProperty classProperty = new ClassProperty();
        ITypeBinding typeBinding = node.resolveBinding();
        int nestedLevel = getNestedLevel(node);
        String qualifiedName;
        if (typeBinding != null) {
            qualifiedName = ClassUtil.removeGenericType(typeBinding.getQualifiedName());
            if (!node.isPackageMemberTypeDeclaration()) {
                qualifiedName = replaceDots(qualifiedName, nestedLevel);
            }
            classProperty.setClassName(qualifiedName);
            ITypeBinding superBinding = typeBinding.getSuperclass();
            if (superBinding != null) {
                String qualifiedSuperClassName = ClassUtil.removeGenericType(superBinding.getQualifiedName());
                classProperty.setSuperClassName(qualifiedSuperClassName);
            }
            ITypeBinding[] interfaceBindings = typeBinding.getInterfaces();
            List<String> interfaceClassName = new ArrayList<>();
            if (interfaceBindings != null) {
                for (ITypeBinding interfaceBinding : interfaceBindings) {
                    String qualifiedInterfaceName = ClassUtil.removeGenericType(interfaceBinding.getQualifiedName());
                    interfaceClassName.add(qualifiedInterfaceName);
                }
            }
            classProperty.setInterfaces(interfaceClassName);
        } else {
            qualifiedName = ClassUtil.removeGenericType(node.getName().getFullyQualifiedName());
            if (!node.isPackageMemberTypeDeclaration()) {
                qualifiedName = replaceDots(qualifiedName, nestedLevel);
            }
            classProperty.setClassName(qualifiedName);
            Type superclassType = node.getSuperclassType();
            classProperty.setSuperClassName(getClassName(superclassType));
            List<Type> interfaceTypes = node.superInterfaceTypes();
            List<String> interfaceClassNameList = new ArrayList<>();
            for (Type interfaceType : interfaceTypes) {
                interfaceClassNameList.add(getClassName(interfaceType));
            }
            classProperty.setInterfaces(interfaceClassNameList);
        }
        // class declaration and source code
        classProperty.setClassDeclaration(node);
        classProperty.setSourceCode(new CodeFormatter(node.toString()).startFormat());

        // annotation in the class declaration
        List<?> modifiers = node.modifiers();
        for (Object mod : modifiers) {
            if (mod instanceof Annotation anno) {
                AnnotationProperty annotationProperty = new AnnotationProperty();
                annotationProperty.setAnnotation(anno);
                annotationProperty.setBelongedClassName(qualifiedName);
                classProperty.addAnnotationProperty(annotationProperty);
            }
        }

        // method declarations
        MethodDeclaration[] methods = node.getMethods();
        for (MethodDeclaration methodDeclaration : methods) {
            int startLine = compilationUnit.getLineNumber(methodDeclaration.getStartPosition());
            int endLine = compilationUnit.getLineNumber(methodDeclaration.getStartPosition() + methodDeclaration.getLength());
            IMethodBinding methodBinding = methodDeclaration.resolveBinding();
            MethodProperty methodProperty = new MethodProperty();
            methodProperty.setBelongedClassName(qualifiedName);
            methodProperty.setStartLineNumber(startLine);
            methodProperty.setEndLineNumber(endLine);
            methodProperty.setConstructor(methodDeclaration.isConstructor());

            ITypeBinding[] parameterTypes = null;
            ITypeBinding returnType = null;
            if (methodBinding != null) {
                methodProperty.setMethodName(methodBinding.getName());
                parameterTypes = methodBinding.getParameterTypes();
                returnType = methodBinding.getReturnType();
            } else {
                methodProperty.setMethodName(methodDeclaration.getName().getIdentifier());
            }

            // return type list
            if (returnType != null) {
                methodProperty.setReturnTypeName(ClassUtil.removeGenericType(returnType.getQualifiedName()));
            } else {
                methodProperty.setReturnTypeName(getTypeName(methodDeclaration.getReturnType2()));
            }

            // param name list
            if (parameterTypes != null) {
                List<String> paramList = new ArrayList<>();
                for (ITypeBinding parameterType : parameterTypes) {
                    String qualifiedParamName = ClassUtil.removeGenericType(parameterType.getQualifiedName());
                    paramList.add(qualifiedParamName);
                }
                methodProperty.setParameters(paramList);
            } else {
                List<String> paramList = new ArrayList<>();
                List<?> parameters = methodDeclaration.parameters();
                for (Object paramObj : parameters) {
                    SingleVariableDeclaration parameter = (SingleVariableDeclaration) paramObj;
                    Type parameterType = parameter.getType();
                    String className = getClassName(parameterType);
                    paramList.add(className);
                }
                methodProperty.setParameters(paramList);
            }

            // method declaration and source code
            methodProperty.setMethodDeclaration(methodDeclaration);
            methodProperty.setSourceCode(new CodeFormatter(methodDeclaration.toString()).startFormat());

            classProperty.addMethodProperty(methodProperty);
        }

        FieldDeclaration[] fields = node.getFields();
        for (FieldDeclaration fieldDeclaration : fields) {
            FieldProperty fieldProperty = new FieldProperty();
            fieldProperty.setBelongedClassName(qualifiedName);
            List<?> fragments = fieldDeclaration.fragments();
            for (Object fragmentObj : fragments) {
                VariableDeclarationFragment fragment = (VariableDeclarationFragment) fragmentObj;
                String fieldName = fragment.getName().getIdentifier();
                fieldProperty.setFieldName(fieldName);
            }

            Type type = fieldDeclaration.getType();
            ITypeBinding fieldTypeBinding = type.resolveBinding();
            if (fieldTypeBinding != null) {
                fieldProperty.setFieldType(ClassUtil.removeGenericType(fieldTypeBinding.getQualifiedName()));
            } else {
                fieldProperty.setFieldType(ClassUtil.removeGenericType(getClassName(type)));
            }

            // field declaration and source code
            fieldProperty.setFieldDeclaration(fieldDeclaration);
            fieldProperty.setSourceCode(new CodeFormatter(fieldDeclaration.toString()).startFormat());

            classProperty.addFieldProperty(fieldProperty);
        }
        classPropertyList.add(classProperty);
        return super.visit(node);
    }

    @Override
    public boolean visit(EnumDeclaration node) {
        ClassProperty classProperty = new ClassProperty();
        ITypeBinding typeBinding = node.resolveBinding();
        int nestedLevel = getNestedLevel(node);
        String qualifiedName;
        if (typeBinding != null) {
            qualifiedName = ClassUtil.removeGenericType(typeBinding.getQualifiedName());
            if (!node.isPackageMemberTypeDeclaration()) {
                qualifiedName = replaceDots(qualifiedName, nestedLevel);
            }
            classProperty.setClassName(qualifiedName);
            ITypeBinding superBinding = typeBinding.getSuperclass();
            if (superBinding != null) {
                String qualifiedSuperClassName = ClassUtil.removeGenericType(superBinding.getQualifiedName());
                classProperty.setSuperClassName(qualifiedSuperClassName);
            }
            ITypeBinding[] interfaceBindings = typeBinding.getInterfaces();
            List<String> interfaceClassName = new ArrayList<>();
            if (interfaceBindings != null) {
                for (ITypeBinding interfaceBinding : interfaceBindings) {
                    String qualifiedInterfaceName = ClassUtil.removeGenericType(interfaceBinding.getQualifiedName());
                    interfaceClassName.add(qualifiedInterfaceName);
                }
            }
            classProperty.setInterfaces(interfaceClassName);
        } else {
            qualifiedName = ClassUtil.removeGenericType(node.getName().getFullyQualifiedName());
            if (!node.isPackageMemberTypeDeclaration()) {
                qualifiedName = replaceDots(qualifiedName, nestedLevel);
            }
            classProperty.setClassName(qualifiedName);
            List<Type> interfaceTypes = node.superInterfaceTypes();
            List<String> interfaceClassNameList = new ArrayList<>();
            if (interfaceTypes != null) {
                for (Type interfaceType : interfaceTypes) {
                    interfaceClassNameList.add(getClassName(interfaceType));
                }
            }
            classProperty.setInterfaces(interfaceClassNameList);
        }
        // class declaration and source code
        classProperty.setClassDeclaration(node);
        classProperty.setSourceCode(new CodeFormatter(node.toString()).startFormat());

        List<?> bodyDeclarationList = node.bodyDeclarations();
        List<MethodDeclaration> methodDeclarationList = new ArrayList<>();
        List<FieldDeclaration> fieldDeclarationList = new ArrayList<>();

        for (Object o : bodyDeclarationList) {
            if (o instanceof MethodDeclaration methodDeclaration) {
                methodDeclarationList.add(methodDeclaration);
            } else if (o instanceof FieldDeclaration fieldDeclaration) {
                fieldDeclarationList.add(fieldDeclaration);
            }
        }

        for (MethodDeclaration methodDeclaration : methodDeclarationList) {
            int startLine = compilationUnit.getLineNumber(methodDeclaration.getStartPosition());
            int endLine = compilationUnit.getLineNumber(methodDeclaration.getStartPosition() + methodDeclaration.getLength());
            IMethodBinding methodBinding = methodDeclaration.resolveBinding();
            MethodProperty methodProperty = new MethodProperty();
            methodProperty.setBelongedClassName(qualifiedName);
            methodProperty.setStartLineNumber(startLine);
            methodProperty.setEndLineNumber(endLine);
            methodProperty.setConstructor(methodDeclaration.isConstructor());

            ITypeBinding[] parameterTypes = null;
            ITypeBinding returnType = null;
            if (methodBinding != null) {
                methodProperty.setMethodName(methodBinding.getName());
                parameterTypes = methodBinding.getParameterTypes();
                returnType = methodBinding.getReturnType();
            } else {
                methodProperty.setMethodName(methodDeclaration.getName().getIdentifier());
            }

            // return type list
            if (returnType != null) {
                methodProperty.setReturnTypeName(ClassUtil.removeGenericType(returnType.getQualifiedName()));
            } else {
                methodProperty.setReturnTypeName(getTypeName(methodDeclaration.getReturnType2()));
            }

            // param name list
            if (parameterTypes != null) {
                List<String> paramList = new ArrayList<>();
                for (ITypeBinding parameterType : parameterTypes) {
                    String qualifiedParamName = ClassUtil.removeGenericType(parameterType.getQualifiedName());
                    paramList.add(qualifiedParamName);
                }
                methodProperty.setParameters(paramList);
            } else {
                List<String> paramList = new ArrayList<>();
                List<?> parameters = methodDeclaration.parameters();
                for (Object paramObj : parameters) {
                    SingleVariableDeclaration parameter = (SingleVariableDeclaration) paramObj;
                    Type parameterType = parameter.getType();
                    String className = getClassName(parameterType);
                    paramList.add(className);
                }
                methodProperty.setParameters(paramList);
            }

            // method declaration and source code
            methodProperty.setMethodDeclaration(methodDeclaration);
            methodProperty.setSourceCode(new CodeFormatter(methodDeclaration.toString()).startFormat());

            classProperty.addMethodProperty(methodProperty);
        }

        for (FieldDeclaration fieldDeclaration : fieldDeclarationList) {
            FieldProperty fieldProperty = new FieldProperty();
            fieldProperty.setBelongedClassName(qualifiedName);
            List<?> fragments = fieldDeclaration.fragments();
            for (Object fragmentObj : fragments) {
                VariableDeclarationFragment fragment = (VariableDeclarationFragment) fragmentObj;
                String fieldName = fragment.getName().getIdentifier();
                fieldProperty.setFieldName(fieldName);
            }

            Type type = fieldDeclaration.getType();
            ITypeBinding fieldTypeBinding = type.resolveBinding();
            if (fieldTypeBinding != null) {
                fieldProperty.setFieldType(ClassUtil.removeGenericType(fieldTypeBinding.getQualifiedName()));
            } else {
                fieldProperty.setFieldType(ClassUtil.removeGenericType(getClassName(type)));
            }

            // field declaration and source code
            fieldProperty.setFieldDeclaration(fieldDeclaration);
            fieldProperty.setSourceCode(new CodeFormatter(fieldDeclaration.toString()).startFormat());

            classProperty.addFieldProperty(fieldProperty);
        }
        classPropertyList.add(classProperty);
        return super.visit(node);
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

    private static String replaceDots(String input, int level) {
        if (level == 0) {
            return input;
        }
        char[] chars = input.toCharArray();
        int count = 0;
        for (int i = chars.length - 1; i >= 0; i--) {
            if (chars[i] == '.') {
                count++;
                if (count <= level) {
                    chars[i] = '$';
                } else {
                    break;
                }
            }
        }
        return new String(chars);
    }

    private static int getNestedLevel(AbstractTypeDeclaration type) {
        int level = 0;
        AbstractTypeDeclaration parentType = getParentType(type);
        while (parentType != null) {
            level++;
            parentType = getParentType(parentType);
        }
        return level;
    }

    private static AbstractTypeDeclaration getParentType(AbstractTypeDeclaration type) {
        ASTNode parentNode = type.getParent();
        if (parentNode instanceof AbstractTypeDeclaration) {
            return (AbstractTypeDeclaration) parentNode;
        }
        return null;
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

    public List<ClassProperty> getClassPropertyList() {
        return classPropertyList;
    }
}
