package org.example;

import java.io.IOException;
import org.example.llvm.LLVMFacade;

public class Main {
    public static void main(String[] args) throws IOException {
        LLVMFacade.compile(args[0], args[1]);
    }
}
