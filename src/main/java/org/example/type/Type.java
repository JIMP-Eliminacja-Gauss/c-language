package org.example.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Type {
    DOUBLE("double", "strpd", true),
    INT("i32", "strpi", true),
    BOOL("i1", "strps", true),
    STRING("i8*", "strps", false);

    private final String llvmRepresentation;
    private final String llvmStringRepresentation;
    private final boolean autoAssignable;
}
