package org.example.type;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class Value {
    private static final String LOCAL_PREFIX = "%";
    private static final String GLOBAL_PREFIX = "@";
    protected final boolean isGlobal;
    protected String name;
    protected Type type;

    public Value(String name, Type type, boolean isGlobal) {
        this.name = name;
        this.type = type;
        this.isGlobal = isGlobal;
    }

    public Value(String name, Type type) {
        this.name = name;
        this.type = type;
        this.isGlobal = false;
    }

    public Value withName(String name) {
        return new Value(name, this.type, isGlobal);
    }

    public String getName() {
        return (isGlobal ? GLOBAL_PREFIX : LOCAL_PREFIX) + name;
    }

    public Value toLocal() {
        return new Value(name, type, false);
    }

    public Value toGlobal() {
        return new Value(name, type, true);
    }
}
