package com.strange.brokenapi.analysis.dependency.property;

import com.strange.common.utils.ClassUtil;
import lombok.Data;

@Data
public class DependencyClass {

    private String signature; // The signature of class is the complete class name

    private String packageName; // The package name of a complete class name

    private Class<?> clazz; // The Class Object

    public DependencyClass(String packageName, String signature) {
        this.packageName = packageName;
        this.signature = signature;
    }

    public DependencyClass(String packageName, String signature, Class<?> clazz) {
        this.packageName = packageName;
        this.signature = signature;
        this.clazz = clazz;
    }

    public boolean equals(String className) {
        if (className == null || signature == null) return false;

        if (className.equals(this.signature)) {
            return true;
        }

        if (className.equals(ClassUtil.getSimpleClassName(this.signature))) {
            return true;
        }

        if (ClassUtil.isPrefix(className, this.signature)) {
            return true;
        }
        return false;
//         // to solve inner class
//         List<String> split = StrUtil.split(className, '.');
//         List<String> rest = new ArrayList<>();
//         for (String s : split) {
//             rest.add(s);
//             if (isFirstLetterUpperCase(s)) break;
//         }
//         String processedClassName = StrUtil.join(".", rest);
// //        return processedClassName.equals(signature) || processedClassName.equals(ClassUtil.getSimpleClassName(this.signature)) || signature.startsWith(processedClassName);
//         return processedClassName.equals(signature) || processedClassName.equals(ClassUtil.getSimpleClassName(this.signature));
    }

    
}
