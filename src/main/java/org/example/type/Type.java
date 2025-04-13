package org.example.type;

public enum Type {
    DOUBLE("double"),
    INT("i32"),
    BOOL("i1");

    private final String llvmRepresentation;

    Type(String llvmRepresentation) {
        this.llvmRepresentation = llvmRepresentation;
    }

    public String llvmRepresentation() {
        return this.llvmRepresentation;
    }
}
