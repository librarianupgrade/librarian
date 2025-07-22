package com.strange.common.utils;

import lombok.NonNull;
import soot.G;
import soot.Scene;
import soot.SootClass;
import soot.options.Options;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SootUtil {
    public static void initializeSoot( List<File> jarFileList) {
        G.reset();

        List<String> argsList = new ArrayList<>(Arrays.asList("-allow-phantom-refs",
                "-w",
                "-keep-line-number", "enabled"
        ));

        for (File file : jarFileList) {
            argsList.add("-process-dir");
            argsList.add(file.getAbsolutePath());
        }

        argsList.addAll(Arrays.asList("-p", "jb", "use-original-names:true"));
        String[] args;
        args = argsList.toArray(new String[0]);

        Options.v().parse(args);
        Options.v().set_src_prec(Options.src_prec_java);
        Options.v().set_whole_program(true);
        Options.v().set_debug(false);
        Options.v().set_allow_phantom_refs(true);
        Options.v().set_verbose(true);
        Options.v().set_keep_line_number(true);
        Options.v().setPhaseOption("cg", "all-reachable:true");
        Options.v().set_no_bodies_for_excluded(true);
        Options.v().set_app(true);
        // load necessary class and method
        Scene.v().loadNecessaryClasses();
    }

    public static void initializeSootWithSpecificClass( List<File> jarFileList,  String className) {
        G.reset();

        List<String> argsList = new ArrayList<>(Arrays.asList("-allow-phantom-refs",
                "-w",
                "-keep-line-number", "enabled"
        ));

        for (File file : jarFileList) {
            argsList.add("-process-dir");
            argsList.add(file.getAbsolutePath());
        }

        argsList.addAll(Arrays.asList("-p", "jb", "use-original-names:true"));
        String[] args;
        args = argsList.toArray(new String[0]);

        Options.v().parse(args);
        Options.v().set_src_prec(Options.src_prec_java);
        Options.v().set_prepend_classpath(true);
        Options.v().set_output_format(Options.output_format_none);
        Options.v().set_allow_phantom_refs(true);
        Options.v().set_whole_program(true);
        Options.v().set_keep_line_number(true);
        Options.v().set_app(true);

        SootClass sc = Scene.v().forceResolve(className, SootClass.BODIES);
        sc.setApplicationClass();

        Scene.v().loadNecessaryClasses();
    }
}
