package com.strange.brokenapi.analysis;

import cn.hutool.core.annotation.Alias;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.file.FileReader;
import cn.hutool.json.JSONUtil;
import com.strange.common.enums.FixTypeEnum;
import com.strange.common.enums.JDKVersionEnum;
import lombok.Data;

import java.io.File;
import java.util.Optional;

@Data
public class InputProjectContext {

    private final String METADATA_FILE_NAME = "GT_Metadata.json";

    @Data
    static class Metadata {
        @Alias("jdk_version")
        private String jdkVersion;

        @Alias("change_pom_path")
        private String changePomPath;

        @Alias("root_path")
        private String rootPath;

        @Alias("old_version")
        private String oldVersion;

        @Alias("new_version")
        private String newVersion;

        @Alias("fix_type")
        private Integer fixTypeId;
    }

    private JDKVersionEnum jdkVersion;

    private FixTypeEnum fixType;

    private File changePomFile;

    private File projectRootDir;

    private File oldPomFile;

    private File newPomFile;

    public InputProjectContext(File projectDir) {
        File metadataFile = FileUtil.file(projectDir, METADATA_FILE_NAME);
        if (metadataFile.isFile()) {
            String content = new FileReader(metadataFile).readString();
            Metadata metadata = JSONUtil.toBean(content, Metadata.class);
            this.jdkVersion = JDKVersionEnum.getJDKVersion(metadata.getJdkVersion());

            this.projectRootDir = Optional.ofNullable(metadata.getRootPath())
                    .map(path -> {
                                if (path.isEmpty()) {
                                    return projectDir;
                                } else {
                                    return FileUtil.file(projectDir, path);
                                }
                            }
                    )
                    .orElse(projectDir);

            this.changePomFile = Optional.ofNullable(metadata.getChangePomPath())
                    .map(path -> {
                        if (path.isEmpty()) {
                            return FileUtil.file(projectRootDir, "pom.xml");
                        } else {
                            return FileUtil.file(projectRootDir, path, "pom.xml");
                        }
                    })
                    .orElseGet(() -> FileUtil.file(projectRootDir, "pom.xml"));

            this.oldPomFile = FileUtil.file(projectDir,
                    Optional.ofNullable(metadata.getOldVersion()).orElse("pom.xml.old"));

            this.newPomFile = FileUtil.file(projectDir,
                    Optional.ofNullable(metadata.getNewVersion()).orElse("pom.xml.new"));

            if(!oldPomFile.isFile() || !newPomFile.isFile()) {
                throw new RuntimeException("Failed to read input context: old or new POM files are missing.");
            }

            Integer fixTypeId = metadata.getFixTypeId();
            this.fixType = FixTypeEnum.getTypeById(fixTypeId);
            if(this.fixType == null) {
                throw new RuntimeException("Invalid fixTypeId: " + fixTypeId + " provided. No matching FixType found.");
            }
        } else {
            this.jdkVersion = JDKVersionEnum.JDK_1_8;
            this.changePomFile = FileUtil.file(projectDir, "pom.xml");
            this.projectRootDir = projectDir;
            this.oldPomFile = FileUtil.file(projectDir, "pom.xml.old");
            this.newPomFile = FileUtil.file(projectDir, "pom.xml.new");

            if(!oldPomFile.isFile() || !newPomFile.isFile()) {
                throw new RuntimeException("Failed to read input context: old or new POM files are missing.");
            }
            this.fixType = FixTypeEnum.FixCompilationError;
        }
    }
}
