package org.example.type;

public class DoubleType implements Type {
    @Override
    public String llvmRepresentation() {
        return "double";
    }
}
