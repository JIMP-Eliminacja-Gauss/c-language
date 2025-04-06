package org.example.type;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class Value {
    private static final String LOCAL_PREFIX = "%";
    private static final String GLOBAL_PREFIX = "@";
    protected String name;
    private Type type;
    private boolean isGlobal;

    public Value(String name, Type type) {
        this.name = name;
        this.type = type;
    }

    public Value withName(String name) {
        return new Value(name, this.type, false);
    }

    public String getName() {
        return isGlobal ? GLOBAL_PREFIX : LOCAL_PREFIX + name;
    }


}
