package com.strange.brokenapi.analysis.jdt;

import cn.hutool.core.map.BiMap;
import org.eclipse.jdt.core.compiler.IProblem;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;

public class ProblemIdMapping {

    private static final Class<IProblem> PROBLEM_CLASS = IProblem.class;

    private static final BiMap<Integer, String> MAPPING = new BiMap<>(new HashMap<>());

    static {
        for (Field field : PROBLEM_CLASS.getDeclaredFields()) {
            field.setAccessible(true);
            if(Modifier.isStatic(field.getModifiers()) && Modifier.isFinal(field.getModifiers())) {
                try {
                    String name = field.getName();
                    int value = field.getInt(null);
                    MAPPING.put(value, name);
                } catch (IllegalAccessException ignored) {
                }
            }
        }
    }

    public static String getErrorType(Integer errorId) {
        return MAPPING.get(errorId);
    }

    public static Integer getErrorId(String errorType) {
        return MAPPING.getKey(errorType);
    }
}
