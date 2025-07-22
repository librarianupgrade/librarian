package com.strange.fix.engine.llm.entity;

import com.strange.fix.engine.property.MethodProperty;
import lombok.Data;

@Data
public class MethodModificationMapping {
    private MethodProperty oldMethodProperty;

    private MethodProperty newMethodProperty;

    public MethodModificationMapping(MethodProperty oldMethodProperty, MethodProperty newMethodProperty) {
        this.oldMethodProperty = oldMethodProperty;
        this.newMethodProperty = newMethodProperty;
    }
}
