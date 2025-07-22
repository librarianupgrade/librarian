package com.strange.common.utils;

import org.apache.commons.lang3.SystemUtils;

public class OSUtil {

    public static boolean isWindows() {
        return SystemUtils.IS_OS_WINDOWS;
    }

    public static boolean isMac() {
        return SystemUtils.IS_OS_MAC;
    }

    public static boolean isLinux() {
        return SystemUtils.IS_OS_LINUX;
    }

}
