package org.example.type;

import lombok.Getter;

@Getter
public class Constant extends Value {
    public Constant(String constantValue, Type type, boolean isGlobal) {
        super(constantValue, type, isGlobal);
    }

    public Constant(String constantValue, Type type) {
        super(constantValue, type, false);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Value toLocal() {
        return new Constant(name, type, false);
    }

    @Override
    public Value toGlobal() {
        return new Constant(name, type, true);
    }
}
