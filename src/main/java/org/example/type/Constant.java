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
}
