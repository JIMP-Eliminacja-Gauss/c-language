package org.example.type;

import java.util.LinkedList;
import java.util.List;

public class Matrix extends Value {

    public List<Array> rows = new LinkedList<>();

    public Matrix(String name, Type type, boolean isGlobal) {
        super(name, type, isGlobal);
    }
}
