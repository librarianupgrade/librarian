package com.strange.common.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CodeUtil {
    public static String removeJavaDoc(String sourceCode) {
        final Pattern JAVADOC = Pattern.compile(
                "/\\*\\*[\\s\\S]*?\\*/",
                Pattern.DOTALL
        );

        Matcher m = JAVADOC.matcher(sourceCode);
        return m.replaceAll("");
    }
}
