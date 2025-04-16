package org.example.type;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ShortCircuit {
    private boolean shortCircuit;
    private boolean result;

    public boolean getResult() {
        return result;
    }
}
