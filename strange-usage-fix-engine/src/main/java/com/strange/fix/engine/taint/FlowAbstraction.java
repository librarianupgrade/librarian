package com.strange.fix.engine.taint;

import soot.Local;
import soot.SootField;
import soot.Unit;
import soot.Value;

import java.util.Objects;

public class FlowAbstraction {

    private Unit source;

    private SootField field;

    private Local local;

    private Value value;

    public FlowAbstraction(Unit source, Local local) {
        this(source, local, (SootField) null);
    }

    public FlowAbstraction(Unit source, SootField field) {
        this(source, (Local) null, field);
    }

    public FlowAbstraction(Unit source, Local local, SootField field) {
        this.source = source;
        this.local = local;
        this.field = field;
    }

    public FlowAbstraction(Unit source, Value value) {
        this.source = source;
        this.value = value;
    }


    public Unit getSource() {
        return this.source;
    }

    public Local getLocal() {
        return this.local;
    }

    public void setLocal(Local l) {
        this.local = l;
    }

    public SootField getField() {
        return this.field;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((local == null) ? 0 : local.hashCode());
        result = prime * result + ((field == null) ? 0 : field.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FlowAbstraction that = (FlowAbstraction) o;

        if (!Objects.equals(local, that.local)) return false;
        return Objects.equals(field, that.field);
    }

    @Override
    public String toString() {
        if (local != null)
            return "LOCAL " + local;
        if (field != null)
            return "FIELD " + field;
        return "";
    }
}
