package org.example;

import main.java.org.example.ExprBaseListener;
import main.java.org.example.ExprParser;

import java.util.HashSet;
import java.util.Optional;

public class LLVMActions extends ExprBaseListener {
    HashSet<String> variables = new HashSet<>();

    @Override
    public void exitIntDeclaration(ExprParser.IntDeclarationContext ctx) {
        String ID = ctx.ID().getText();
        String value = Optional.ofNullable(ctx.intAssignement())
                .map(it -> it.INT_VALUE().getText()).orElse(null);
        if (!variables.contains(ID)) {
            variables.add(ID);
            LLVMGenerator.declare(ID);
        }
        LLVMGenerator.assign(ID, value);
    }

    @Override
    public void exitPrint(ExprParser.PrintContext ctx) {
        String ID = ctx.ID().getText();
        if (variables.contains(ID)) {
            LLVMGenerator.printf(ID);
        } else {
            System.err.println("Line " + ctx.getStart().getLine() + ", unknown variable: " + ID);
        }
    }

    @Override
    public void exitRead(ExprParser.ReadContext ctx) {
        String ID = ctx.ID().getText();
        if (!variables.contains(ID)) {
            variables.add(ID);
            LLVMGenerator.declare(ID);
        }
        LLVMGenerator.scanf(ID);
    }

    @Override
    public void exitProg(ExprParser.ProgContext ctx) {
        System.out.println(LLVMGenerator.generate());
    }

}
