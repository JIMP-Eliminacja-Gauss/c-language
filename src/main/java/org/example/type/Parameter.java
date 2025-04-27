package org.example.type;

import lombok.Builder;
import lombok.Getter;

@Builder
public class Parameter extends Value {
    private final String name;

    @Getter
    private final Type type;

    public Parameter(String name, Type type) {
        super(name, type);
        this.name = name;
        this.type = type;
    }
}
