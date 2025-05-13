package org.example.llvm;

import main.java.org.example.ExprBaseListener;
import main.java.org.example.ExprParser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.example.type.*;
import org.example.util.ValidationParam;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.BiFunction;
import java.util.logging.Logger;

import static java.lang.System.exit;
import static org.example.llvm.LLVMGenerator.matrixRowIndex;

public class LLVMActions extends ExprBaseListener {
    private static final Logger logger = Logger.getLogger(LLVMActions.class.getName());
    private static final Map<String, Type> types = Map.of(
        "int", Type.INT,
        "double", Type.DOUBLE,
        "bool", Type.BOOL,
        "string", Type.STRING,
        "void", Type.VOID,
        "var", Type.DYNAMIC);
    private static final Map<String, BiFunction<Value, Value, Value>> llvmAction = Map.of(
        "+", LLVMGenerator::add,
        "-", LLVMGenerator::sub,
        "*", LLVMGenerator::mult,
        "/", LLVMGenerator::div,
        "==", LLVMGenerator::xand,
        "!=", LLVMGenerator::xor,
        "&&", LLVMGenerator::and,
        "||", LLVMGenerator::or);
    private final String outputFileName;
    private final HashMap<String, Value> localVariables = new HashMap<>();
    private final HashMap<String, Value> globalVariables = new HashMap<>();
    private final HashMap<String, Function> functions = new HashMap<>();
    private final HashMap<String, Clazz> classes = new HashMap<>();
    private final Deque<Value> valueStack = new ArrayDeque<>();
    private final Deque<Function> functionStack = new ArrayDeque<>();
    private final Deque<Value> arrayValueStack = new ArrayDeque<>();
    private final Deque<Value> matrixValueStack = new ArrayDeque<>();
    private final Deque<String> ifStack = new ArrayDeque<>();
    private final Deque<String> loopStack = new ArrayDeque<>();
    private final Deque<FunctionCall> functionCallStack = new ArrayDeque<>();
    private final Deque<Clazz> clazzStack = new ArrayDeque<>();
    private final HashMap<String, Clazz> complexValues = new HashMap<>();
    private final ShortCircuit shortCircuit;
    private boolean isGlobalContext = true;
    private boolean inFunction = false;
    private boolean inClass = false;

    public LLVMActions(String outputFileName) {
        this.outputFileName = outputFileName;
        this.shortCircuit = new ShortCircuit();
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

    @Override
    public void exitVarDeclaration(ExprParser.VarDeclarationContext ctx) {
        ParseTree root = ctx.getChild(0);
        Type type = getVariableType(root);
        String id = root.getChild(1).getText();

        if (type == null) {
            logger.severe("Line " + ctx.getStart().getLine() + ", unknown type");
            exit(1);
            return;
        }

        if (variableIsAlreadyDeclared(id)) {
            logger.warning("Line " + ctx.getStart().getLine() + ", variable already declared: " + id);
            return;
        }

        if (type == Type.DYNAMIC) {
            type = valueStack.peek().getType();
        }

        LLVMGenerator.declare(id, type, isGlobalContext);
        Value value = new Value(id, type, isGlobalContext);
        addVariableToDeclared(id, value);

        // declaration with assignment
        if (root.getChildCount() > 2) {
            value = valueStack.pop();
            LLVMGenerator.assign(id, value, isGlobalContext);
            addVariableToDeclared(id, value);
        }
    }

    @Override
    public void exitPrint(ExprParser.PrintContext ctx) {
        String id = ctx.ID().getText();
        Value value = getVariable(id, ctx);
        Type type = value.getType();

        if (type == Type.STRING) {
            value = LLVMGenerator.load(id, value, isGlobalContext);
        }
        LLVMGenerator.printf(value);
    }

    @Override
    public void exitRead(ExprParser.ReadContext ctx) {
        String id = ctx.ID().getText();
        Value value = getVariable(id, ctx);
        LLVMGenerator.scanf(value);
    }

    @Override
    public void exitMultiplicativeExpression(ExprParser.MultiplicativeExpressionContext ctx) {
        if (shortCircuit.isShortCircuit()) {
            return;
        }

        doArithmetics(ctx);
    }

    @Override
    public void exitAdditiveExpression(ExprParser.AdditiveExpressionContext ctx) {
        if (shortCircuit.isShortCircuit()) {
            return;
        }
        doArithmetics(ctx);
    }

    @Override
    public void exitIntAssignement(ExprParser.IntAssignementContext ctx) {
        // arithmetics are handled in exitAdditiveExpression
        if (ctx.INT_VALUE() != null) {
            Value value = new Constant(ctx.INT_VALUE().getText(), Type.INT, isGlobalContext);
            valueStack.addLast(value);
        } else if (ctx.arrayValueByIndex() != null) {
            String arrayId = ctx.arrayValueByIndex().ID().getText();
            String index = ctx.arrayValueByIndex().arrayIndex().INT_VALUE().getText();
            Array array = (Array) localVariables.get(arrayId);
            String newValueName = LLVMGenerator.loadValueByIndex(array, index);
            valueStack.addLast(new Value(newValueName, Type.INT));
        } else if (ctx.matrixValueByIndex() != null) {
            String matrixId = ctx.matrixValueByIndex().ID().getText();
            String rowIndex = ctx.matrixValueByIndex().INT_VALUE(0).getText();
            String columnIndex = ctx.matrixValueByIndex().INT_VALUE(1).getText();
            Matrix matrix = (Matrix) localVariables.get(matrixId);
            String newValueName = LLVMGenerator.loadValueByIndex(matrix, rowIndex, columnIndex);
            valueStack.addLast(new Value(newValueName, Type.INT));
        } else if (ctx.membersAccess() != null) {
            final var value = valueStack.removeLast();
            final var id = value.getName().replace("%", "");
            final var newValue = LLVMGenerator.load(id, value, false);
            valueStack.addLast(newValue);
        }
    }

    @Override
    public void exitFloatAssignement(ExprParser.FloatAssignementContext ctx) {
        // arithmetics are handled in exitAdditiveExpression
        if (ctx.FLOAT_VALUE() != null) {
            Value value = new Constant(ctx.FLOAT_VALUE().getText(), Type.DOUBLE, isGlobalContext);
            valueStack.addLast(value);
        }
    }

    @Override
    public void exitBoolAssignement(ExprParser.BoolAssignementContext ctx) {
        // arithmetics are handled in exitAdditiveExpression
        if (ctx.BOOL_VALUE() != null) {
            Value value = new Constant(ctx.BOOL_VALUE().getText(), Type.BOOL, isGlobalContext);
            valueStack.addLast(value);
        }
    }

    @Override
    public void exitStringAssignement(ExprParser.StringAssignementContext ctx) {
        if (ctx.STRING_VALUE() != null) {
            String text = ctx.STRING_VALUE().getText();
            String textWithoutQuotes = text.substring(1, text.length() - 1);
            String id = LLVMGenerator.constantString(textWithoutQuotes);
            // must be local
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
    public void enterMatrixDeclaration(ExprParser.MatrixDeclarationContext ctx) {
        Matrix matrix = new Matrix(ctx.ID().getText(), Type.INT, false);
        matrixValueStack.addLast(matrix);
    }

    @Override
    public void exitMatrixDeclaration(ExprParser.MatrixDeclarationContext ctx) {
        Matrix matrix = (Matrix) matrixValueStack.pop();
        matrixValueStack.addLast(matrix);
        localVariables.put(ctx.ID().getText(), matrix);
        LLVMGenerator.declareMatrix(matrix);
        LLVMGenerator.assignMatrix(matrix);
    }

    @Override
    public void enterMatrixRow(ExprParser.MatrixRowContext ctx) {
        Array array = new Array("mat" + matrixRowIndex, Type.INT, false);
        arrayValueStack.push(array);
        matrixRowIndex++;
    }

    @Override
    public void exitMatrixRow(ExprParser.MatrixRowContext ctx) {
        Matrix matrix = (Matrix) matrixValueStack.peek();
        Array array = (Array) arrayValueStack.pop();
        matrix.rows.add(array);
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
        if (shortCircuit.isShortCircuit()) {
            return;
        }

        if (ctx.BOOL_VALUE() != null) {
            boolean eval = evaluateUnaryExpression(ctx);
            Constant value = new Constant(Boolean.toString(eval), Type.BOOL, isGlobalContext);
            valueStack.addLast(value);
        } else {
            boolean shouldNegate = shouldNegate(ctx);
            Value value = getVariable(ctx.ID().getText(), ctx);
            if (shouldNegate) {
                value = LLVMGenerator.neg(value);
            }

            valueStack.addLast(value);
        }
    }

    @Override
    public void enterBooleanExpression(ExprParser.BooleanExpressionContext ctx) {
        shortCircuit.setShortCircuit(false);
    }

    @Override
    public void exitBooleanExpression(ExprParser.BooleanExpressionContext ctx) {
        shortCircuit.setShortCircuit(false);
    }

    @Override
    public void enterBooleanDisjunctionExpression(ExprParser.BooleanDisjunctionExpressionContext ctx) {
        shortCircuit.setShortCircuit(checkShortCircuit(ctx, true));
        shortCircuit.setResult(true);
    }

    @Override
    public void exitExpressionFactor(ExprParser.ExpressionFactorContext ctx) {
        if (shortCircuit.isShortCircuit()) {
            return;
        }

        if (ctx.ID() != null) {
            String id = ctx.ID().getText();
            Value value = getVariable(id, ctx);
            valueStack.addLast(value);
        } else if (ctx.FLOAT_VALUE() != null) {
            String id = ctx.FLOAT_VALUE().getText();
            valueStack.addLast(new Constant(id, Type.DOUBLE, isGlobalContext));
        } else if (ctx.INT_VALUE() != null) {
            String id = ctx.INT_VALUE().getText();
            valueStack.addLast(new Constant(id, Type.INT, isGlobalContext));
        }
    }

    @Override
    public void exitBooleanDisjunctionExpression(ExprParser.BooleanDisjunctionExpressionContext ctx) {
        if (shortCircuit.isShortCircuit()) {
            Constant constant = new Constant(Boolean.toString(shortCircuit.getResult()), Type.BOOL, isGlobalContext);
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
    public void enterFunction(ExprParser.FunctionContext ctx) {
        if (functions.containsKey(ctx.ID().getText())) {
            logger.severe("Function " + ctx.ID().getText() + " already declared");
            exit(1);
        }

        final var name = inClass ? Clazz.methodNameFor(clazzStack.peekLast(), ctx.ID().getText()) : ctx.ID().getText();
        final var function = Function.builder()
            .name(name)
            .returnType(getVariableType(ctx.returnType()))
            .build();

        functions.put(name, function);
        functionStack.addLast(function);

        isGlobalContext = false;
        inFunction = true;
    }

    @Override
    public void exitFunction(ExprParser.FunctionContext ctx) {
        functionStack.removeLast();
        isGlobalContext = true;
        inFunction = false;
    }

    @Override
    public void exitArgsDeclaration(ExprParser.ArgsDeclarationContext ctx) {
        final var type = getVariableType(ctx);
        final var id = ctx.ID().getText();
        final var function = functionStack.getLast();
        final var parameter = Parameter.builder().name(id).type(type).build();
        function.addParameter(parameter);
        addVariableToDeclared(id, parameter);
    }

    @Override
    public void enterFunctionBlock(ExprParser.FunctionBlockContext ctx) {
        final var function = functionStack.getLast();
        LLVMGenerator.defineFunction(function);
    }

    @Override
    public void exitFunctionBlock(ExprParser.FunctionBlockContext ctx) {
        final var function = functionStack.getLast();
        LLVMGenerator.closeFunction(function);
    }

    @Override
    public void enterFunctionCall(ExprParser.FunctionCallContext ctx) {
        if (ctx.getParent() instanceof ExprParser.MethodAccessContext) {
            return;
        }
        final var functionName = ctx.ID().getText();
        var function = functions.get(functionName);
        if (function == null) {
            logger.severe("Function " + functionName + " not found");
            exit(1);
        }

        final var functionCall = new FunctionCall(function, new ArrayList<>());
        functionCallStack.addLast(functionCall);
    }

    @Override
    public void exitFunctionCall(ExprParser.FunctionCallContext ctx) {
        if (ctx.getParent() instanceof ExprParser.MethodAccessContext) {
            return;
        }
        final var functionCall = functionCallStack.removeLast();
        final var value = LLVMGenerator.callFunction(functionCall.function(), functionCall.arguments());
        if (value != null) {
            valueStack.addLast(value);
        }
    }

    @Override
    public void exitArgs(ExprParser.ArgsContext ctx) {
        final var functionCall = functionCallStack.peekLast();
        Type type = getArgumentType(ctx);
        final var id = ctx.ID();
        if (id != null) {
            final var value = getVariable(id.getText(), ctx);
            functionCall.arguments().add(value);
        } else {
            final var value = new Constant(getVariableValue(ctx), type, false);
            functionCall.arguments().add(value);
        }
    }

    @Override
    public void enterLoop(ExprParser.LoopContext ctx) {
        final var id = ctx.ID().getText();
        final var value = getVariable(id, ctx);
        final var line = ctx.getStart().getLine();
        final var validations = Arrays.asList(
            new ValidationParam(() -> variableNotDeclared(id), line, "variable doesn't exist: " + id),
            new ValidationParam(() -> value.getType() != Type.INT, line, "variable isn't int: " + id));
        if (isNotValid(validations)) {
            return;
        }
        final var loadedValue = LLVMGenerator.load(id, value, value.isGlobal());
        isGlobalContext = false;
        loopStack.push(loadedValue.getName());
        LLVMGenerator.loopStart(loadedValue);
    }

    @Override
    public void exitLoop(ExprParser.LoopContext ctx) {
        LLVMGenerator.loopEnd();
        loopStack.pop();
        if (loopStack.isEmpty() && !inFunction) {
            this.isGlobalContext = true;
        }
    }

    @Override
    public void enterIfStatement(ExprParser.IfStatementContext ctx) {
        final var conditionId = ctx.ID().getText();
        final var value = getVariable(conditionId, ctx);
        final var line = ctx.getStart().getLine();
        final var validations = Arrays.asList(
            new ValidationParam(
                () -> variableNotDeclared(conditionId), line, "variable doesn't exist: " + conditionId),
            new ValidationParam(() -> value.getType() != Type.BOOL, line, "variable isn't bool: " + conditionId));
        if (isNotValid(validations)) {
            return;
        }
        ifStack.push(conditionId);
        this.isGlobalContext = false;
        final var loadedValue = LLVMGenerator.load(conditionId, value, value.isGlobal());
        LLVMGenerator.ifStart();
        LLVMGenerator.evaluateIfCondition(loadedValue);
    }

    @Override
    public void exitIfBlock(ExprParser.IfBlockContext ctx) {
        LLVMGenerator.ifEnd();
    }

    @Override
    public void exitIfStatement(ExprParser.IfStatementContext ctx) {
        final var ifWithoutElse = ctx.elseStatement() == null;
        if (ifWithoutElse) {
            ifStack.pop();
        }
        if (ifStack.isEmpty() && !inFunction) {
            this.isGlobalContext = true;
        }
    }

    @Override
    public void enterElseStatement(ExprParser.ElseStatementContext ctx) {
        final var conditionId = ifStack.pop();
        final var value = getVariable(conditionId, ctx);
        final var loadedValue = LLVMGenerator.load(conditionId, value, value.isGlobal());
        LLVMGenerator.elseStart();
        LLVMGenerator.evaluateElseCondition(loadedValue);
    }

    @Override
    public void exitElseStatement(ExprParser.ElseStatementContext ctx) {
        LLVMGenerator.elseEnd();
    }

    @Override
    public void enterReturnStmt(ExprParser.ReturnStmtContext ctx) {
        if (ctx.FLOAT_VALUE() != null) {
            valueStack.addLast(new Constant(ctx.FLOAT_VALUE().getText(), Type.DOUBLE));
        } else if (ctx.INT_VALUE() != null) {
            valueStack.addLast(new Constant(ctx.INT_VALUE().getText(), Type.INT));
        } else if (ctx.BOOL_VALUE() != null) {
            valueStack.addLast(new Constant(ctx.BOOL_VALUE().getText(), Type.INT));
        } else if (ctx.STRING_VALUE() != null) {
            valueStack.addLast(new Constant(ctx.STRING_VALUE().getText(), Type.STRING));
        } else if (ctx.ID() != null) {
            String id = ctx.ID().getText();
            Value value = getVariable(id, ctx);
            valueStack.addLast(value);
        }
    }

    @Override
    public void exitReturnStmt(ExprParser.ReturnStmtContext ctx) {
        final var value = valueStack.removeLast();
        LLVMGenerator.ret(value);
    }

    @Override
    public void exitClassInstantiation(ExprParser.ClassInstantiationContext ctx) {
        final var className = ctx.ID(0).getText();
        final var varName = ctx.ID(1).getText();
        if (!classes.containsKey(className)) {
            logger.severe("Class " + className + " not found");
            exit(1);
        }
        final var clazz = classes.get(className);
        LLVMGenerator.declare(varName, clazz, false);
        final var value = new Value(varName, Type.COMPLEX, false);
        addVariableToDeclared(varName, value);
        complexValues.put(varName, clazz);
    }

    @Override
    public void enterClassDeclaration(ExprParser.ClassDeclarationContext ctx) {
        final var className = ctx.ID().getText();
        final var clazz = Clazz.of(className);
        clazzStack.addLast(clazz);
        inClass = true;
//        LLVMGenerator.declareClass(clazz);
    }

    @Override
    public void exitClassDeclaration(ExprParser.ClassDeclarationContext ctx) {
        final var clazz = clazzStack.removeLast();
        classes.put(clazz.getName(), clazz);
        LLVMGenerator.declareClassMembers(clazz);
        inClass = false;
    }

    @Override
    public void exitMethodsDeclaration(ExprParser.MethodsDeclarationContext ctx) {
        final var methodName = ctx.function().ID().getText();
        final var clazz = clazzStack.peekLast();
        final var function = functions.get(Clazz.methodNameFor(clazz, methodName));
        assert clazz != null;
        clazz.addMethod(methodName, function);
    }

    @Override
    public void exitMembersDeclaration(ExprParser.MembersDeclarationContext ctx) {
        final var memberName = ctx.ID().getText();
        final var memberType = getVariableType(ctx);
        final var clazz = clazzStack.peekLast();
        assert clazz != null;
        clazz.addField(memberName, new Value(memberName, memberType));
    }

    @Override
    public void exitMembersAssignement(ExprParser.MembersAssignementContext ctx) {
        final var type = getArgumentType(ctx);
        Value value = new Constant("", type);
        if (ctx.INT_VALUE() != null) {
            value = new Constant(ctx.INT_VALUE().getText(), Type.INT);
        } else if (ctx.FLOAT_VALUE() != null) {
            value = new Constant(ctx.FLOAT_VALUE().getText(), Type.DOUBLE);
        }

        final var classMember = valueStack.removeLast();
        final var id = classMember.getName().replace("%", "");

        LLVMGenerator.assign(id, value, false);
    }

    @Override
    public void exitMembersAccess(ExprParser.MembersAccessContext ctx) {
        final var varName = ctx.ID(0).getText();
        final var memberName = ctx.ID(1).getText();
        if (variableNotDeclared(varName)) {
            logger.severe("Object " + varName + " not found");
            exit(1);
        }
        final var value = getVariable(varName, ctx);
        final var clazz = complexValues.get(varName);
        final var field = clazz.getField(memberName);
        final var assignment = LLVMGenerator.getStructMember(clazz, value, field);
        valueStack.addLast(assignment);
    }

    @Override
    public void exitMethodAccess(ExprParser.MethodAccessContext ctx) {
        final var objectName = ctx.ID().getText();
        final var clazz = complexValues.get(objectName);
        final var functionName = ctx.functionCall().ID().getText();

        if (clazz == null) {
            logger.severe("Object " + objectName + " not found");
            exit(1);
        }
        final var method = Clazz.methodNameFor(clazz, functionName);
        if (!functions.containsKey(method)) {
            logger.severe("Method " + method + " not found");
            exit(1);
        }
        final var function = functions.get(method);
        FunctionCall functionCall = new FunctionCall(function, new ArrayList<>());
        final var value = LLVMGenerator.callFunction(functionCall.function(), functionCall.arguments());
        if (value != null) {
            valueStack.addLast(value);
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
            final var arithmeticStrategy =
                llvmAction.get(ctx.getChild(2 * i - 1).getText());
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
            ParseTree child = stack.removeFirst();
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

    private boolean variableIsAlreadyDeclared(String id) {
        return isGlobalContext ? globalVariables.containsKey(id) : localVariables.containsKey(id);
    }

    private boolean variableNotDeclared(String id) {
        return !(globalVariables.containsKey(id) || (!isGlobalContext && localVariables.containsKey(id)));
    }

    private void addVariableToDeclared(String id, Value value) {
        if (isGlobalContext) {
            globalVariables.put(id, value);
        } else {
            localVariables.put(id, value);
        }
    }

    private Value getVariable(String id, ParserRuleContext ctx) {
        Value value = Optional.ofNullable(localVariables.get(id)).orElseGet(() -> globalVariables.get(id));

        if (value == null) {
            Token start = ctx.getStart();
            logger.severe("Line: " + start.getLine() + " Variable " + id + " not found");
            exit(1);
        }

        return value;
    }

    private boolean isNotValid(List<ValidationParam> validations) {
        boolean notValid = false;
        for (ValidationParam validationParam : validations) {
            if (validationParam.condition().getAsBoolean()) {
                logger.warning("Line " + validationParam.line() + ", " + validationParam.message());
                notValid = true;
            }
        }
        return notValid;
    }

    private Type getArgumentType(ExprParser.ArgsContext ctx) {
        if (ctx.INT_VALUE() != null) {
            return Type.INT;
        } else if (ctx.FLOAT_VALUE() != null) {
            return Type.DOUBLE;
        } else if (ctx.BOOL_VALUE() != null) {
            return Type.BOOL;
        } else if (ctx.STRING_VALUE() != null) {
            return Type.STRING;
        } else if (ctx.ID() != null) {
            final var id = ctx.ID().getText();
            final var value = getVariable(id, ctx);
            return value.getType();
        }

        return null;
    }

    private Type getArgumentType(ExprParser.MembersAssignementContext ctx) {
        if (ctx.INT_VALUE() != null) {
            return Type.INT;
        } else if (ctx.FLOAT_VALUE() != null) {
            return Type.DOUBLE;
        } else if (ctx.BOOL_VALUE() != null) {
            return Type.BOOL;
        } else if (ctx.STRING_VALUE() != null) {
            return Type.STRING;
        }

        return null;
    }
}
