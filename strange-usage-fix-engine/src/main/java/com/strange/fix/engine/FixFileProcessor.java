package com.strange.fix.engine;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.file.FileReader;
import cn.hutool.core.io.file.FileWriter;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * During the fix process, code slicing is driven by line-number criteria.
 * Therefore, until the fix is fully applied, the original source files
 * must remain unchanged.
 */
@Getter
@Setter
public class FixFileProcessor implements Cloneable {

    private Map<String, File> tempFixFileMap; // absolute path of the broken code file ---> temp fix code file

    public FixFileProcessor() {
        this.tempFixFileMap = new HashMap<>();
    }

    public FixFileProcessor( Map<String, File> tempFixFileMap) {
        this.tempFixFileMap = tempFixFileMap;
    }

    public void addFixFile(String filePath, File fixFile) {
        tempFixFileMap.put(filePath, fixFile);
    }

    public File getFile(String filePath) {
        if (tempFixFileMap.containsKey(filePath)) return tempFixFileMap.get(filePath);
        else return FileUtil.file(filePath);
    }

    public File getFile(File file) {
        return getFile(file.getAbsolutePath());
    }

    public void applyFixChange() {
        for (Map.Entry<String, File> entry : tempFixFileMap.entrySet()) {
            String filePath = entry.getKey();
            File fixFile = entry.getValue();
            String fixFileContent = new FileReader(fixFile).readString();
            new FileWriter(filePath).write(fixFileContent);
        }
    }

    @Override
    public FixFileProcessor clone() {
        try {
            super.clone();
        } catch (CloneNotSupportedException ignored) {
        }
        return new FixFileProcessor(this.tempFixFileMap);
    }
}
