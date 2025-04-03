package org.example.type;

public class IntType implements Type {
    @Override
    public String llvmRepresentation() {
        return "i32";
    }
}
