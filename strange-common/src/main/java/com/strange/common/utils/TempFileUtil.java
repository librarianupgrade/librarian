package com.strange.common.utils;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.file.FileNameUtil;

import java.io.File;

public class TempFileUtil {
    public static File createTempFile(File storageDir, File baseFile) {
        String prefixFileName = FileNameUtil.getPrefix(baseFile);
        prefixFileName = prefixFileName.replaceAll("(?:Temp|_|\\d|\\.java)+", "");
        return FileUtil.createTempFile("Temp_" + prefixFileName + "_", ".java.cache", storageDir, true);
    }
}
