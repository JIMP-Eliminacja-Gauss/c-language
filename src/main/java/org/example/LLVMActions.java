package org.example;

import main.java.org.example.ExprBaseListener;
import main.java.org.example.ExprParser;
import org.antlr.v4.runtime.tree.ParseTree;
import org.example.type.DoubleType;
import org.example.type.IntType;
import org.example.type.Type;
import org.example.util.Variable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.logging.Logger;

public class LLVMActions extends ExprBaseListener {
    private static final Logger logger = Logger.getLogger(LLVMActions.class.getName());
    private static final Map<String, Supplier<Type>> types = Map.of(
            "int", IntType::new,
            "double", DoubleType::new
    );
    private final String outputFileName;
    private final HashMap<String, Type> variables = new HashMap<>();

    public LLVMActions(String outputFileName) {
        this.outputFileName = outputFileName;
    }

    @Override
    public void exitVarDeclaration(ExprParser.VarDeclarationContext ctx) {
        ParseTree root = ctx.getChild(0);
        Type type = getVariableType(root);
        String id = root.getChild(1).getText();
        String value = getVariableValue(root.getChild(2));
        if (type == null) {
            logger.warning("Line " + ctx.getStart().getLine() + ", unknown type");
            return;
        }
        Variable variable = new Variable(value, id, type);
        if (!variables.containsKey(id)) {
            variables.put(id, type);
            LLVMGenerator.declare(id, variable.type());
        }
        if (value != null) {
            LLVMGenerator.assign(id, variable);
        }
    }

    @Override
    public void exitPrint(ExprParser.PrintContext ctx) {
        String id = ctx.ID().getText();
        Type type = variables.get(id);
        if (type != null) {
            LLVMGenerator.printf(id, type);
        } else {
            logger.warning("Line " + ctx.getStart().getLine() + ", unknown variable: " + id);
        }
    }

    @Override
    public void exitRead(ExprParser.ReadContext ctx) {
        String id = ctx.ID().getText();
        if (!variables.containsKey(id)) {
            Type type = new IntType();
            variables.put(id, type);
            LLVMGenerator.declare(id, type);
        }
        LLVMGenerator.scanf(id);
    }

    @Override
    public void exitProg(ExprParser.ProgContext ctx) {
        String finalLlvmCode = LLVMGenerator.generate();
        Path path = Paths.get(outputFileName);
        try {
            Files.write(path, finalLlvmCode.getBytes());
        } catch (IOException e) {
            logger.severe("Error occurred during writing to file: " + e.getMessage());
        }
    }

    private Type getVariableType(ParseTree ctx) {
        String typeName = ctx.getChild(0).getText();
        return Optional.ofNullable(types.get(typeName)).map(Supplier::get).orElse(null);
    }

    private String getVariableValue(ParseTree ctx) {
        return Optional.ofNullable(ctx)
                .map(ParseTree::getText)
                .map(text -> text.replace("=", ""))
                .orElse(null);
    }
}
