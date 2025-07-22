package com.strange.brokenapi.analysis.dependency;

import java.io.File;

public interface DependencyAction {

    Integer COPY_THREAD_NUMBER = 50;

    void cleanProject();

    boolean packageProject();

    boolean installProject();

    void goOfflineProjectDependency();

    File copyProjectDependency();

    File generateDependencyTree();

    File generateCompleteDependencyTree();

}
