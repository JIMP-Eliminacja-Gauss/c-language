package org.example.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Type {
    DOUBLE("double", "strpd", "fcmp"),
    INT("i32", "strpi", "icmp"),
    BOOL("i1", "strpi", "icmp"),
    STRING("i8*", "strps", "");

    private final String llvmRepresentation;
    private final String llvmStringRepresentation;
    private final String llvmComparator;
}
