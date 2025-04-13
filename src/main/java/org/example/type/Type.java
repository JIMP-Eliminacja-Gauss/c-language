package org.example.type;

public enum Type {
    DOUBLE("double", "strpd", true),
    INT("i32", "strpi", true),
    BOOL("i1", "strps", true),
    STRING("i8*", "strps", false);

    private final String llvmRepresentation;
    private final String llvmStringRepresentation;
    private final boolean autoAssignable;

    Type(String llvmRepresentation, String llvmStringRepresentation, boolean autoAssignable) {
        this.llvmRepresentation = llvmRepresentation;
        this.llvmStringRepresentation = llvmStringRepresentation;
        this.autoAssignable = autoAssignable;
    }

    public String llvmRepresentation() {
        return this.llvmRepresentation;
    }

    public String llvmStringRepresentation() {
        return this.llvmStringRepresentation;
    }

    public boolean autoAssignable() {
        return this.autoAssignable;
    }
}
