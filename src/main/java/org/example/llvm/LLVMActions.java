package org.example.llvm;

import main.java.org.example.ExprBaseListener;
import main.java.org.example.ExprParser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.example.type.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.BiFunction;
import java.util.logging.Logger;

import static java.lang.System.exit;

public class LLVMActions extends ExprBaseListener {
    private static final Logger logger = Logger.getLogger(LLVMActions.class.getName());
    private static final Map<String, Type> types = Map.of(
            "int", Type.INT,
            "double", Type.DOUBLE,
            "bool", Type.BOOL,
            "string", Type.STRING
    );
    private static final Map<String, BiFunction<Value, Value, Value>> llvmAction = Map.of(
            "+", LLVMGenerator::add,
            "-", LLVMGenerator::sub,
            "*", LLVMGenerator::mult,
            "/", LLVMGenerator::div,
            "==", LLVMGenerator::xand,
            "!=", LLVMGenerator::xor,
            "&&", LLVMGenerator::and,
            "||", LLVMGenerator::or
    );
    private final String outputFileName;
    private final HashMap<String, Value> localVariables = new HashMap<>();
    private final Deque<Value> valueStack = new ArrayDeque<>();
    private final Deque<Value> arrayValueStack = new ArrayDeque<>();
    private final ShortCircuit shortCircuit;

    public LLVMActions(String outputFileName) {
        this.outputFileName = outputFileName;
        this.shortCircuit = new ShortCircuit();
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
        if (root.getChildCount() > 2) {
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
            return;
        }
        LLVMGenerator.scanf(id);
    }

    @Override
    public void exitMultiplicativeExpression(ExprParser.MultiplicativeExpressionContext ctx) {
        doArithmetics(ctx);
    }

    @Override
    public void exitAdditiveExpression(ExprParser.AdditiveExpressionContext ctx) {
        doArithmetics(ctx);
    }

    @Override
    public void exitIntAssignement(ExprParser.IntAssignementContext ctx) {
        // arithmetics are handled in exitAdditiveExpression
        if (ctx.INT_VALUE() != null) {
            Value value = new Constant(ctx.INT_VALUE().getText(), Type.INT);
            valueStack.addLast(value);
        } else if (ctx.arrayValueByIndex() != null) {
            String arrayId = ctx.arrayValueByIndex().ID().getText();
            String index = ctx.arrayValueByIndex().arrayIndex().INT_VALUE().getText();
            Array array = (Array) localVariables.get(arrayId);
            String newValueName = LLVMGenerator.loadValueByIndex(array, index);
            valueStack.addLast(new Value(newValueName, Type.INT));
        }
    }

    @Override
    public void exitFloatAssignement(ExprParser.FloatAssignementContext ctx) {
        // arithmetics are handled in exitAdditiveExpression
        if (ctx.FLOAT_VALUE() != null) {
            Value value = new Constant(ctx.FLOAT_VALUE().getText(), Type.DOUBLE);
            valueStack.addLast(value);
        }
    }

    @Override
    public void exitBoolAssignement(ExprParser.BoolAssignementContext ctx) {
        // arithmetics are handled in exitAdditiveExpression
        if (ctx.BOOL_VALUE() != null) {
            Value value = new Constant(ctx.BOOL_VALUE().getText(), Type.BOOL);
            valueStack.addLast(value);
        }
    }

    @Override
    public void exitStringAssignement(ExprParser.StringAssignementContext ctx) {
        if (ctx.STRING_VALUE() != null) {
            String text = ctx.STRING_VALUE().getText();
            String textWithoutQuotes = text.substring(1, text.length() - 1);
            String id = LLVMGenerator.constantString(textWithoutQuotes);
            Value value = new Value(id, Type.STRING);
            valueStack.addLast(value);
        }
    }

    @Override
    public void enterArrayDeclaration(ExprParser.ArrayDeclarationContext ctx) {
        Array array = new Array(ctx.ID().getText(), Type.INT, false);
        arrayValueStack.addLast(array);
    }

    @Override
    public void exitArrayDeclaration(ExprParser.ArrayDeclarationContext ctx) {
        Array array = (Array) arrayValueStack.pop();
        arrayValueStack.addLast(array);
        localVariables.put(ctx.ID().getText(), array);
        LLVMGenerator.declareArray(array);
        LLVMGenerator.assignArray(array);
    }

    @Override
    public void exitArrayValues(ExprParser.ArrayValuesContext ctx) {
        Array array = (Array) arrayValueStack.peek();
        array.values.add(new Constant(ctx.INT_VALUE().getText(), Type.INT));
    }

    @Override
    public void exitUnaryExpression(ExprParser.UnaryExpressionContext ctx) {
        if (ctx.BOOL_VALUE() != null) {
            boolean eval = evaluateUnaryExpression(ctx);
            Constant value = new Constant(Boolean.toString(eval), Type.BOOL);
            valueStack.addLast(value);
        } else {
            boolean shouldNegate = shouldNegate(ctx);
            if (localVariables.containsKey(ctx.ID().getText())) {
                Value value = localVariables.get(ctx.ID().getText());
                if (shouldNegate) {
                    value = LLVMGenerator.neg(value);
                }

                valueStack.addLast(value);
            } else {
                logger.warning("Line " + ctx.getStart().getLine() + ", unknown variable: " + ctx.ID().getText());
            }

        }
    }

    @Override
    public void enterBooleanExpression(ExprParser.BooleanExpressionContext ctx) {
        shortCircuit.setShortCircuit(false);
    }

    @Override
    public void enterBooleanDisjunctionExpression(ExprParser.BooleanDisjunctionExpressionContext ctx) {
        shortCircuit.setShortCircuit(checkShortCircuit(ctx, true));
        shortCircuit.setResult(true);
    }

    @Override
    public void exitExpressionFactor(ExprParser.ExpressionFactorContext ctx) {
        if (ctx.ID() != null) {
            String id = ctx.ID().getText();
            if (localVariables.containsKey(id)) {
                valueStack.addLast(localVariables.get(id));
            } else {
                logger.warning("Line " + ctx.getStart().getLine() + ", unknown variable: " + id);
            }
        } else if (ctx.FLOAT_VALUE() != null) {
            String id = ctx.FLOAT_VALUE().getText();
            valueStack.addLast(new Constant(id, Type.DOUBLE));
        } else if (ctx.INT_VALUE() != null) {
            String id = ctx.INT_VALUE().getText();
            valueStack.addLast(new Constant(id, Type.INT));
        }
    }

    @Override
    public void exitBooleanDisjunctionExpression(ExprParser.BooleanDisjunctionExpressionContext ctx) {
        if (shortCircuit.isShortCircuit()) {
            Constant constant = new Constant(Boolean.toString(shortCircuit.getResult()), Type.BOOL);
            valueStack.push(constant);
            return;
        }

        doArithmetics(ctx);
    }

    @Override
    public void exitBooleanEqualityExpression(ExprParser.BooleanEqualityExpressionContext ctx) {
        if (shortCircuit.isShortCircuit()) {
            return;
        }

        doArithmetics(ctx);
    }

    @Override
    public void enterBooleanConjunctionExpression(ExprParser.BooleanConjunctionExpressionContext ctx) {
        if (shortCircuit.isShortCircuit() || wasPreviouslyChecked(ctx)) {
            return;
        }

        shortCircuit.setShortCircuit(checkShortCircuit(ctx, false));
        shortCircuit.setResult(false);
    }

    @Override
    public void exitBooleanConjunctionExpression(ExprParser.BooleanConjunctionExpressionContext ctx) {
        if (shortCircuit.isShortCircuit()) {
            return;
        }

        doArithmetics(ctx);
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

    private void doArithmetics(ParserRuleContext ctx) {
        int nodes = (ctx.getChildCount() + 1) / 2;
        ArrayList<Value> values = new ArrayList<>();
        for (int i = 0; i < nodes; i++) {
            values.addFirst(valueStack.removeLast());
        }

        Value newValue = values.getFirst();

        for (int i = 1; i < nodes; i++) {
            Value value = values.get(i);

            if (value.getType() != newValue.getType()) {
                logger.severe("Value type mismatch: " + value.getType() + " != " + newValue.getType());
                exit(1);
            }
            final var arithmeticStrategy = llvmAction.get(ctx.getChild(2 * i - 1).getText());
            newValue = arithmeticStrategy.apply(newValue, value);
        }

        valueStack.addLast(newValue);
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

    private boolean evaluateUnaryExpression(ExprParser.UnaryExpressionContext ctx) {
        if (ctx.BOOL_VALUE() == null) {
            return false;
        }

        int negCount = ctx.getChildCount() - 1;
        boolean value = ctx.BOOL_VALUE().getText().equals("true");
        return (negCount % 2 == 0) == value;

    }

    private boolean shouldNegate(ExprParser.UnaryExpressionContext ctx) {
        int negCount = ctx.getChildCount() - 1;
        return negCount % 2 == 1;
    }

    private boolean checkShortCircuit(ParserRuleContext ctx, boolean lookFor) {
        Stack<ParseTree> stack = new Stack<>();
        stack.addAll(ctx.children);

        while (!stack.isEmpty()) {
            ParseTree child = stack.pop();
            if (child.getChildCount() == 1) {
                stack.push(child.getChild(0));
            }

            if (child instanceof ExprParser.UnaryExpressionContext c) {
                if (evaluateUnaryExpression(c) == lookFor) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean wasPreviouslyChecked(ExprParser.BooleanConjunctionExpressionContext ctx) {
        return ctx.getChildCount() == 1;
    }
}
