package com.strange.common.config;

import cn.hutool.core.io.FileUtil;
import com.strange.common.enums.JDKVersionEnum;

import java.io.File;

public class GlobalConfig {
    public static final JDKVersionEnum GlobalJDKVersion = JDKVersionEnum.JDK_17;

    public static final File DefaultFixSpace = FileUtil.file(System.getProperty("user.dir") + File.separator + "fixspace");

    public static final String CacheDirName = "__cache";

    public static final String CONFIGURATION_FILE_NAME = "application.properties";
}
