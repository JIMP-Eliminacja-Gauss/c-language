package org.example.llvm;

import org.example.type.*;

import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

public class LLVMGenerator {

    private static final int MAX_READ_STRING_LENGTH = 100;
    static String headerText = "";
    static String mainText = "";
    static String functionText = "";
    static boolean insideFunction = false;
    static int reg = 1;
    static int str = 1;
    static int ifIndex = 1;
    static int elseIndex = 1;
    static int loopIndex = 1;
    static int matrixRowIndex = 1;
    static Deque<Integer> ifIndexStack = new ArrayDeque<>();
    static Deque<Integer> elseIndexStack = new ArrayDeque<>();
    static Deque<Integer> loopIndexStack = new ArrayDeque<>();

    static void printf(Value value) {
        Type type = value.getType();
        if (type == Type.BOOL) {
            printf_boolean(value);
            return;
        }

        final var text = "%" + reg + " = call i32 (i8*, ...) "
            + "@printf(i8* getelementptr inbounds ([4 x i8], [4 x i8]* "
            + "@"
            + type.getLlvmStringRepresentation() + ", i32 0, i32 0), "
            + type.getLlvmRepresentation()
            + " "
            + value.getName() + ")";
        addToText(text);
        reg++;
    }

    static void printf_boolean(Value value) {
        var text = "";
        text += "%";
        text += reg;
        text += " = icmp eq i1 ";
        text += value.getName();
        text += ", 1\n";
        reg++;

        text += "%";
        text += reg;
        text += " = select i1 %";
        text += reg - 1;
        text += ", i8* @true_text, i8* @false_text\n";
        reg++;

        text += "%";
        text += reg;
        text +=
            " = call i32 (i8*, ...) @printf(i8* getelementptr inbounds ([6 x i8], [6 x i8]* @strps, i32 0, i32 0), i8* %";
        text += reg - 1;
        text += ")";
        addToText(text);
        reg++;
    }

    static void scanf(Value value) {
        allocateString("str" + str, MAX_READ_STRING_LENGTH);
        var text = "%" + reg
            + " = getelementptr inbounds ["
            + (MAX_READ_STRING_LENGTH + 1)
            + " x i8], ["
            + (MAX_READ_STRING_LENGTH + 1)
            + " x i8]* %str"
            + str
            + ", i64 0, i64 0\n";
        reg++;
        text += "store i8* %" + (reg - 1) + ", i8** " + value.getName() + "\n";
        str++;
        text += "%"
            + reg
            + " = call i32 (i8*, ...) @scanf(i8* getelementptr inbounds ([5 x i8], [5 x i8]* @strs, i32 0, i32 0), i8* %"
            + (reg - 1)
            + ")";
        addToText(text);
        reg++;
    }

    static String constantString(String content) {
        int length = content.getBytes(StandardCharsets.UTF_8).length + 1;
        headerText += "@str" + str + " = constant [" + length + " x i8] c\"" + content + "\\00\"\n";
        String n = "str" + str;
        LLVMGenerator.allocateString(n, (length - 1));
        var text = "%" + reg + " = bitcast [" + length + " x i8]* %" + n + " to i8*\n";
        text += "call void @llvm.memcpy.p0i8.p0i8.i64(i8* align 1 %" + reg + ", "
            + "i8* align 1 getelementptr inbounds (["
            + length + " x i8], [" + length + " x i8]*" + " @"
            + n + ", i32 0, i32 0), i64 " + length + ", i1 false)";

        addToText(text);
        reg++;
        str++;
        return "" + (reg - 1);
    }

    static void declareClassMembers(Clazz clazz) {
        var text = clazz.getLlvmRepresentation() + " = type {\n";
        int counter = 0, size = clazz.getFields().size();
        for (Field field : clazz.getFields().values()) {
            text += "\t" + field.value().getType().getLlvmRepresentation();

            if (counter++ < size - 1) {
                text += ",\n";
            }
        }
        text += "}";
        headerText += text + "\n";
    }

    static void allocateString(String id, int length) {
        addToText("%" + id + " = alloca [" + (length + 1) + " x i8]");
    }

    static void declare(String id, AbstractType type, boolean isGlobal) {
        String text = (isGlobal ? "@" : "%");
        text += id + " = " + (isGlobal ? "global" : "alloca") + " " + type.getLlvmRepresentation();
        text += (isGlobal ? " " + type.getDefaultValue() : "");

        addToText(text, isGlobal);
    }

    static void assign(String id, Value value, boolean isGlobal) {
        addToText("store " + value.getType().getLlvmRepresentation() + " " + value.getName() + ", "
            + value.getType().getLlvmRepresentation() + "* " + (isGlobal ? "@" : "%") + id);
    }

    static Value getStructMember(Clazz clazz, Value value, Field field) {
        var text = "%" + reg + " = getelementptr inbounds " + clazz.getLlvmRepresentation() + ", "
            + clazz.getLlvmRepresentation() + "* " + value.getName() + ", i32 0, i32 " + field.offset();
        addToText(text);
        reg++;
        return new Value(String.valueOf(reg - 1), field.value().getType());
    }

    static void declareArray(Array array) {
        addToText(array.getName() + " = alloca [" + array.values.size() + " x i32]");
    }

    static void declareMatrix(Matrix matrix) {
        addToText(matrix.getName() + " = alloca [" + matrix.rows.size() + " x ["
            + matrix.rows.getFirst().values.size() + "x i32]*]");
    }

    static void assignMatrix(Matrix matrix) {
        int rows = matrix.rows.size();
        int cols = matrix.rows.getFirst().values.size();
        matrix.rows.forEach(LLVMGenerator::declareArray);
        var text = "";

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                text += "%" + reg + " = getelementptr inbounds [" + cols + " x i32], [" + cols + " x i32]* "
                    + matrix.rows.get(i).getName() + ", i32 0, i32 " + j + "\n";
                text += "store i32 " + matrix.rows.get(i).values.get(j).getName() + ", i32* %" + reg;
                reg++;
            }
        }
        for (int i = 0; i < rows; i++) {
            text += "%" + reg + " = getelementptr inbounds [" + rows + " x [" + cols + " x i32]*], [" + rows + " x ["
                + cols + " x i32]*]* " + matrix.getName() + ", i32 0, i32 " + i + "\n";
            text += "store [" + cols + " x i32]* " + matrix.rows.get(i).getName() + ", [" + cols + " x i32]** %" + reg;
            reg++;
        }

        addToText(text);
    }

    static void assignArray(Array array) {
        int arraySize = array.values.size();
        for (int i = 0; i < arraySize; i++) {
            var text = "%" + reg + " = getelementptr inbounds [" + arraySize + " x i32], [" + arraySize + " x i32]* "
                + array.getName() + ", i32 0, i32 " + i + "\n";
            text += "store i32 " + array.values.get(i).getName() + ", i32* %" + reg;
            addToText(text);
            reg++;
        }
    }

    static String loadValueByIndex(Array array, String index) {
        var text = "%" + reg + " = getelementptr inbounds [" + array.values.size() + " x i32], [" + array.values.size()
            + " x i32]* " + array.getName() + ", i32 0, i32 " + index + "\n";
        reg++;
        text += "%" + reg + " = load i32, i32* %" + (reg - 1);
        addToText(text);
        reg++;
        return reg - 1 + "";
    }

    static String loadValueByIndex(Matrix matrix, String rowIndex, String colIndex) {
        final var rows = matrix.rows.size();
        final var cols = matrix.rows.getFirst().values.size();
        var text = "%" + reg + " = getelementptr inbounds [" + rows + " x [" + cols + " x i32]*], [" + rows + " x ["
            + cols + " x i32]*]* " + matrix.getName() + ", i32 0, i32 " + rowIndex + "\n";
        reg++;
        text += "%" + reg + " = load [" + cols + " x i32]*, [" + cols + " x i32]* %" + (reg - 1) + "\n";
        reg++;
        text += "%" + reg + " = getelementptr inbounds [" + cols + " x i32], [" + cols + " x i32]* %" + (reg - 1)
            + ", i32 0, i32 " + colIndex + "\n";
        reg++;
        text += "%" + reg + " = load i32, i32* %" + (reg - 1);
        addToText(text);
        reg++;
        return reg - 1 + "";
    }

    static Value load(String id, Value value, boolean isGlobal) {
        final var text = "%" + reg
            + " = load "
            + value.getType().getLlvmRepresentation()
            + ", "
            + value.getType().getLlvmRepresentation()
            + "* "
            + (isGlobal ? "@" : "%")
            + id;

        addToText(text);
        reg++;
        // local var
        return new Value(String.valueOf(reg - 1), value.getType());
    }

    static Value mult(Value value1, Value value2) {
        final var text = "%" + reg
            + " = "
            + (value1.getType() == Type.DOUBLE ? "f" : "")
            + "mul "
            + value1.getType().getLlvmRepresentation()
            + " "
            + value1.getName()
            + ", "
            + value2.getName();

        addToText(text);
        reg++;
        return value1.withName(String.valueOf(reg - 1)).toLocal();
    }

    static Value div(Value value1, Value value2) {
        final var result = "%" + reg;
        final var op = value1.getType() == Type.DOUBLE ? "f" : "s";
        final var text = result + " = " + op + "div " + value1.getType().getLlvmRepresentation() + " "
            + value1.getName() + ", " + value2.getName();

        addToText(text);
        reg++;
        return value1.withName(String.valueOf(reg - 1)).toLocal();
    }

    static Value add(Value value1, Value value2) {
        final var text = "%" + reg
            + " = "
            + (value1.getType() == Type.DOUBLE ? "f" : "")
            + "add "
            + value1.getType().getLlvmRepresentation()
            + " "
            + value1.getName()
            + ", "
            + value2.getName();

        addToText(text);
        reg++;
        return value1.withName(String.valueOf(reg - 1)).toLocal();
    }

    static Value sub(Value value1, Value value2) {
        final var result = "%" + reg;
        final var op = value1.getType() == Type.DOUBLE ? "f" : "";
        final var text = result + " = " + op + "sub " + value1.getType().getLlvmRepresentation() + " "
            + value1.getName() + ", " + value2.getName();

        addToText(text);
        reg++;
        return value1.withName(String.valueOf(reg - 1)).toLocal();
    }

    static Value and(Value value1, Value value2) {
        final var labelTrue = "and_true_" + reg;
        final var labelNotTrue = "and_not_true_" + reg;
        final var labelEnd = "and_end_" + reg;
        final var result = "%" + reg;
        final var trueVal = "%true_" + reg;
        final var falseVal = "%false_" + reg;
        var text = "";
        reg++;

        // Jeśli value1 jest fałszywe, skaczemy od razu do końca
        text += "br i1 " + value1.getName() + ", label %" + labelTrue + ", label %" + labelNotTrue + "\n";

        // Blok jeśli value1 == true sprawdzamy value2
        text += labelTrue + ":\n";
        text += trueVal + " = and i1 " + value1.getName() + ", " + value2.getName() + "\n";
        text += "br label %" + labelEnd + "\n";

        // Jeśli value1 == false, zwracamy 0
        text += labelNotTrue + ":\n";
        text += falseVal + " = and i1 0, 0\n";
        text += "br label %" + labelEnd + "\n";

        // PHI node – wybór zależnie od ścieżki
        text += labelEnd + ":\n";
        text += result + " = phi i1 [ " + trueVal + ", %" + labelTrue + " ], [ " + falseVal + ", %" + labelNotTrue
            + " ]";
        addToText(text);
        return new Value(String.valueOf(reg - 1), Type.BOOL);
    }

    static Value or(Value value1, Value value2) {
        final var labelTrue = "or_true_" + reg;
        final var labelNotTrue = "or_not_true_" + reg;
        final var labelEnd = "or_end_" + reg;
        final var result = "%" + reg;
        final var trueVal = "%true_" + reg;
        final var falseVal = "%false_" + reg;
        var text = "";
        reg++;

        // Jeśli value1 jest prawdziwe, skaczemy od razu do labelTrue
        text += "br i1 " + value1.getName() + ", label %" + labelTrue + ", label %" + labelNotTrue + "\n";

        // Blok jeśli value1 == true — zwracamy od razu true
        text += labelTrue + ":\n";
        text += trueVal + " = or i1 1, 1\n";
        text += "br label %" + labelEnd + "\n";

        // Blok jeśli value1 != true — obliczamy or
        text += labelNotTrue + ":\n";
        text += falseVal + " = or i1 " + value1.getName() + ", " + value2.getName() + "\n";
        text += "br label %" + labelEnd + "\n";

        // PHI node — wynik końcowy
        text += labelEnd + ":\n";
        text += result + " = phi i1 [ " + trueVal + ", %" + labelTrue + " ], [ " + falseVal + ", %" + labelNotTrue
            + " ]";
        addToText(text);
        return new Value(String.valueOf(reg - 1), Type.BOOL);
    }

    static Value neg(Value value) {
        final var result = "%" + reg;
        final var text = result + " = xor i1 " + value.getName() + ", 1";
        addToText(text);
        reg++;
        return value.withName(String.valueOf(reg - 1));
    }

    static Value xor(Value value1, Value value2) {
        final var result = "%" + reg;
        final var text = result + " = xor i1 " + value1.getName() + ", " + value2.getName();
        addToText(text);
        reg++;
        return new Value(String.valueOf(reg - 1), Type.BOOL);
    }

    static Value xand(Value value1, Value value2) {
        final var result = "%" + reg;
        final var text = result + " = " + value1.getType().getLlvmComparator()
            + " eq " + value2.getType().getLlvmRepresentation()
            + " " + value1.getName() + ", " + value2.getName();

        addToText(text);
        reg++;

        return new Value(String.valueOf(reg - 1), Type.BOOL);
    }

    static void defineFunction(Function function) {
        insideFunction = true;
        functionText += "define " + function.getReturnType().getLlvmRepresentation() + " @" + function.getName() + "(";

        for (int i = 0; i < function.getParameters().size(); i++) {
            final var param = function.getParameters().get(i);
            functionText += param.getType().getLlvmRepresentation() + " " + param.getName();

            if (i != function.getParameters().size() - 1) {
                functionText += ", ";
            }
        }

        functionText += ") {\n";
    }

    static void closeFunction(Function function) {
        if (function.getReturnType() == Type.VOID) {
            functionText += "ret void\n";
        }

        functionText += "}\n";
        insideFunction = false;
    }

    static Value callFunction(Function function, List<Value> args) {
        boolean isNotVoid = function.getReturnType() != Type.VOID;
        if (isNotVoid) {
            addToText("%" + reg + " = ");
        }

        var text = "call " + function.getReturnType().getLlvmRepresentation() + " @" + function.getName() + "(";

        for (var i = 0; i < args.size(); i++) {
            final var arg = args.get(i);

            text += arg.getType().getLlvmRepresentation() + " " + arg.getName();

            if (i != args.size() - 1) {
                text += ", ";
            }
        }

        text += ")";
        addToText(text);
        reg++;
        return isNotVoid ? new Value(String.valueOf(reg - 1), function.getReturnType()) : null;
    }

    static void ret(Value value) {
        functionText += "ret " + value.getType().getLlvmRepresentation() + " " + value.getName() + "\n";
    }

    static void loopStart(Value repeats) {
        loopIndexStack.push(loopIndex);
        var text = "";
        text += "%counter_" + loopIndex + " = alloca i32\n";
        text += "store i32 0, i32* %counter_" + loopIndex + "\n";
        text += "br label %loop_" + loopIndex + "\n";
        text += "loop_" + loopIndex + ":\n";
        text += "%counter_val_" + loopIndex + " = load i32, i32* %counter_" + loopIndex + "\n";
        text += "%cmp_" + loopIndex + " = icmp slt i32 %counter_val_" + loopIndex + ", " + repeats.getName() + "\n";
        text += "br i1 %cmp_" + loopIndex + ", label %body_" + loopIndex + ", label %end_" + loopIndex + "\n";
        text += "body_" + loopIndex + ":";

        loopIndex++;
        addToText(text, false);
    }

    static void loopEnd() {
        final var index = loopIndexStack.pop();
        var text = "";
        text += "%counter_val_after_" + index + " = load i32, i32* %counter_" + index + "\n";
        text += "%inc_" + index + " = add i32 %counter_val_after_" + index + ", 1\n";
        text += "store i32 %inc_" + index + ", i32* %counter_" + index + "\n";
        text += "br label %loop_" + index + "\n";
        text += "end_" + index + ":\n";
        addToText(text, false);
    }

    static void ifStart() {
        ifIndexStack.push(ifIndex);
        var text = "";
        text += "br label %if_start_" + ifIndex + "\n";
        text += "if_start_" + ifIndex + ":";
        ifIndex++;
        addToText(text, false);
    }

    static void ifEnd() {
        final var ifIndex = ifIndexStack.pop();
        var text = "";
        text += "br label %if_end_" + ifIndex + "\n";
        text += "if_end_" + ifIndex + ":";
        addToText(text, false);
    }

    static void elseStart() {
        elseIndexStack.push(elseIndex);
        var text = "";
        text += "br label %else_start_" + elseIndex + "\n";
        text += "else_start_" + elseIndex + ":";
        elseIndex++;
        addToText(text, false);
    }

    static void evaluateIfCondition(Value value) {
        final var ifIndex = ifIndexStack.peek();
        var text = "";
        text += "br i1 " + value.getName() + ", label %if_true_" + ifIndex + ", label %if_end_" + ifIndex + "\n";
        text += "if_true_" + ifIndex + ":";
        addToText(text, false);
    }

    static void evaluateElseCondition(Value value) {
        final var elseIndex = elseIndexStack.peek();
        var text = "";
        text += "br i1 " + value.getName() + ", label %else_end_" + elseIndex + ", label %else_true_" + elseIndex
            + "\n";
        text += "else_true_" + elseIndex + ":";
        addToText(text, false);
    }

    static void elseEnd() {
        final var elseIndex = elseIndexStack.pop();
        var text = "";
        text += "br label %else_end_" + elseIndex + "\n";
        text += "else_end_" + elseIndex + ":";
        addToText(text, false);
    }

    static void addToText(String text) {
        if (insideFunction) {
            functionText += text + "\n";
        } else {
            mainText += text + "\n";
        }
    }

    static void addToText(String text, boolean isGlobal) {
        if (isGlobal) {
            headerText += text + "\n";
        } else {
            addToText(text);
        }
    }

    static String generate() {
        String text = "";
        text += "declare i32 @printf(i8*, ...)\n";
        text += "declare i32 @scanf(i8*, ...)\n";
        text += "@strps = constant [4 x i8] c\"%s\\0A\\00\"\n";
        text += "@strpi = constant [4 x i8] c\"%d\\0A\\00\"\n";
        text += "@strpd = constant [4 x i8] c\"%f\\0A\\00\"\n";
        text += "@strs = constant [5 x i8] c\"%10s\\00\"\n";
        text += "@strspi = constant [3 x i8] c\"%d\\00\"\n";
        text += "@true_text = constant [5 x i8] c\"true\\00\"\n";
        text += "@false_text = constant [6 x i8] c\"false\\00\"\n";
        text += headerText;
        text += functionText;
        text += "define i32 @main() nounwind{\n";
        text += mainText;
        text += "ret i32 0 }\n";
        return text;
    }
}
