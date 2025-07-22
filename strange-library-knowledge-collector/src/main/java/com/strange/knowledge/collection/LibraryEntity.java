package com.strange.knowledge.collection;

import cn.hutool.core.annotation.Alias;
import lombok.Data;

@Data
public class LibraryEntity {

    @Alias("group_id")
    private String groupId;

    @Alias("artifact_id")
    private String artifactId;
}
