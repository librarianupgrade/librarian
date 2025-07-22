package com.strange.fix.engine.llm.entity;

import com.strange.fix.engine.property.ClassProperty;
import lombok.Data;

@Data
public class ClassModificationMapping {
    private ClassProperty oldClassProperty;

    private ClassProperty newClassProperty;

    public ClassModificationMapping(ClassProperty oldClassProperty, ClassProperty newClassProperty) {
        this.oldClassProperty = oldClassProperty;
        this.newClassProperty = newClassProperty;
    }
}
