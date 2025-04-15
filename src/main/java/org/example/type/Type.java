package org.example.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Type {
    DOUBLE("double", "strpd"),
    INT("i32", "strpi"),
    BOOL("i1", "strps"),
    STRING("i8*", "strps");

    private final String llvmRepresentation;
    private final String llvmStringRepresentation;
}
