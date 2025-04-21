package org.example.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Type {
    DOUBLE("double", "strpd", "fcmp", "0.0"),
    INT("i32", "strpi", "icmp", "0"),
    BOOL("i1", "strpi", "icmp", "false"),
    STRING("i8*", "strps", "", "null");

    private final String llvmRepresentation;
    private final String llvmStringRepresentation;
    private final String llvmComparator;
    private final String defaultValue;
}
