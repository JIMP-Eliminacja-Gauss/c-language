package org.example.llvm;

import main.java.org.example.ExprBaseListener;
import main.java.org.example.ExprParser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.example.type.Constant;
import org.example.type.Type;
import org.example.type.Value;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.BiFunction;
import java.util.logging.Logger;

public class LLVMActions extends ExprBaseListener {
    private static final Logger logger = Logger.getLogger(LLVMActions.class.getName());
    private static final Map<String, Type> types = Map.of(
            "int", Type.INT,
            "double", Type.DOUBLE,
            "bool", Type.BOOL,
            "string", Type.STRING
    );
    private final String outputFileName;
    private final HashMap<String, Value> localVariables = new HashMap<>();
    private final Deque<Value> valueStack = new ArrayDeque<>();

    public LLVMActions(String outputFileName) {
        this.outputFileName = outputFileName;
    }

    @Override
    public void exitVarDeclaration(ExprParser.VarDeclarationContext ctx) {
        ParseTree root = ctx.getChild(0);
        Type type = getVariableType(root);
        String id = root.getChild(1).getText();

        if (type == null) {
            logger.warning("Line " + ctx.getStart().getLine() + ", unknown type");
            return;
        }

        if (localVariables.containsKey(id)) {
            logger.warning("Line " + ctx.getStart().getLine() + ", variable already declared: " + id);
            return;
        }

        LLVMGenerator.declare(id, type);
        Value value = new Value(id, type);
        localVariables.putIfAbsent(id, value);

        // declaration with assignment
        if (type.autoAssignable() && root.getChildCount() > 2) {
            value = valueStack.pop();
            LLVMGenerator.assign(id, value);
            localVariables.put(id, value);
        }
    }

    @Override
    public void exitPrint(ExprParser.PrintContext ctx) {
        String id = ctx.ID().getText();
        Value value = localVariables.get(id);
        Type type = value.getType();
        if (type == null) {
            logger.warning("Line " + ctx.getStart().getLine() + ", unknown variable: " + id);
            return;
        }
        if (type == Type.STRING) {
            value = LLVMGenerator.load(id, value);
        }
        LLVMGenerator.printf(value);
    }

    @Override
    public void exitRead(ExprParser.ReadContext ctx) {
        String id = ctx.ID().getText();
        Value value = localVariables.get(id);
        if (value == null) {
            logger.warning("Line " + ctx.getStart().getLine() + ", unknown variable: " + id);
        }
        LLVMGenerator.scanf(id);
    }

    @Override
    public void exitMultiplicativeExpression(ExprParser.MultiplicativeExpressionContext ctx) {
        doArithmetics(ctx, LLVMGenerator::mult);
    }

    @Override
    public void exitAdditiveExpression(ExprParser.AdditiveExpressionContext ctx) {
        doArithmetics(ctx, LLVMGenerator::add);
    }

    @Override
    public void exitIntAssignement(ExprParser.IntAssignementContext ctx) {
        // arithmetics are handled in exitAdditiveExpression
        if (ctx.INT_VALUE() != null) {
            Value value = new Constant(ctx.INT_VALUE().getText(), Type.INT);
            valueStack.push(value);
        }
    }

    @Override
    public void exitFloatAssignement(ExprParser.FloatAssignementContext ctx) {
        // arithmetics are handled in exitAdditiveExpression
        if (ctx.FLOAT_VALUE() != null) {
            Value value = new Constant(ctx.FLOAT_VALUE().getText(), Type.DOUBLE);
            valueStack.push(value);
        }
    }

    @Override
    public void exitBoolAssignement(ExprParser.BoolAssignementContext ctx) {
        // arithmetics are handled in exitAdditiveExpression
        if (ctx.BOOL_VALUE() != null) {
            Value value = new Constant(ctx.BOOL_VALUE().getText(), Type.BOOL);
            valueStack.push(value);
        }
    }

    @Override
    public void exitStringAssignement(ExprParser.StringAssignementContext ctx) {
        if (ctx.STRING_VALUE() != null) {
            String text = ctx.STRING_VALUE().getText();
            String textWithoutQuotes = text.substring(1, text.length() - 1);
            Value value = new Constant(textWithoutQuotes, Type.STRING);
            valueStack.push(value);
        }
    }

    @Override
    public void exitExpressionFactor(ExprParser.ExpressionFactorContext ctx) {
        if (ctx.ID() != null) {
            String id = ctx.ID().getText();
            if (localVariables.containsKey(id)) {
                valueStack.push(localVariables.get(id));
            } else {
                logger.warning("Line " + ctx.getStart().getLine() + ", unknown variable: " + id);
            }
        } else if (ctx.FLOAT_VALUE() != null) {
            String id = ctx.FLOAT_VALUE().getText();
            valueStack.push(new Constant(id, Type.DOUBLE));
        } else if (ctx.INT_VALUE() != null) {
            String id = ctx.INT_VALUE().getText();
            valueStack.push(new Constant(id, Type.INT));
        } else if (ctx.arrayValueByIndex() != null) {
            // TODO
        }
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

    private void doArithmetics(ParserRuleContext ctx, BiFunction<Value, Value, Value> arithmeticStrategy) {
        int nodes = (ctx.getChildCount() + 1) / 2;
        Value newValue = valueStack.pop();

        for (int i = 1; i < nodes; i++) {
            Value value2 = valueStack.pop();
            newValue = arithmeticStrategy.apply(newValue, value2);
        }

        valueStack.push(newValue);
    }

    private Type getVariableType(ParseTree ctx) {
        String typeName = ctx.getChild(0).getText();
        return types.get(typeName);
    }

    private String getVariableValue(ParseTree ctx) {
        return Optional.ofNullable(ctx)
                .map(ParseTree::getText)
                .map(text -> text.replace("=", ""))
                .orElse(null);
    }
}
