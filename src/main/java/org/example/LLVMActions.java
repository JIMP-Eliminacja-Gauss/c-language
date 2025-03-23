package org.example;

import main.java.org.example.PLwypiszBaseListener;
import main.java.org.example.PLwypiszParser;

import java.util.HashMap;

public class LLVMActions extends PLwypiszBaseListener {

    HashMap<String, String> memory = new HashMap<>();
    String value;

    @Override
    public void exitAssign(PLwypiszParser.AssignContext ctx) {
        String tmp = ctx.STRING().getText();
        tmp = tmp.substring(1, tmp.length() - 1);
        memory.put(ctx.ID().getText(), tmp);
    }

    @Override
    public void exitProg(PLwypiszParser.ProgContext ctx) {
        System.out.println(LLVMGenerator.generate());
    }

    @Override
    public void exitValue(PLwypiszParser.ValueContext ctx) {
        if (ctx.ID() != null) {
            value = memory.get(ctx.ID().getText());
        }
        if (ctx.STRING() != null) {
            String tmp = ctx.STRING().getText();
            value = tmp.substring(1, tmp.length() - 1);
        }
    }

    @Override
    public void exitPrint(PLwypiszParser.PrintContext ctx) {
        LLVMGenerator.print(value);
    }
}
