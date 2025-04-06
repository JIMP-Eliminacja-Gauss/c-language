package org.example;

import org.example.llvm.LLVMFacade;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        LLVMFacade.compile(args[0], args[1]);
    }
}