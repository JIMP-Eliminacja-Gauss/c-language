package org.example.type;

import java.util.LinkedList;
import java.util.List;

public class Array extends Value {

    public List<Value> values = new LinkedList<>();

    public Array(String name, Type type, boolean isGlobal) {
        super(name, type, isGlobal);
    }
}
