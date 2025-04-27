package org.example.llvm;

import org.example.type.Array;
import org.example.type.Function;
import org.example.type.Type;
import org.example.type.Value;

public class LLVMGenerator {

    private static final int MAX_READ_STRING_LENGTH = 100;
    static String headerText = "";
    static String mainText = "";
    static String functionText = "";
    static boolean insideFunction = false;
    static int reg = 1;
    static int str = 1;

    static void printf(Value value) {
        Type type = value.getType();
        if (type == Type.BOOL) {
            printf_boolean(value);
            return;
        }

        final var text = "%" + reg +
            " = call i32 (i8*, ...) " +
            "@printf(i8* getelementptr inbounds ([4 x i8], [4 x i8]* " +
            "@" + type.getLlvmStringRepresentation() +
            ", i32 0, i32 0), " +
            type.getLlvmRepresentation() +
            " " + value.getName() +
            ")";
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
        text += " = call i32 (i8*, ...) @printf(i8* getelementptr inbounds ([6 x i8], [6 x i8]* @strps, i32 0, i32 0), i8* %";
        text += reg - 1;
        text += ")";
        addToText(text);
        reg++;
    }

    static void scanf(String id) {
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
        text += "store i8* %"
            + (reg - 1)
            + ", i8** "
            + "%" + id
            + "\n";
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
        int length = content.length() + 1;
        headerText += "@str" + str + " = constant [" + length + " x i8] c\"" + content + "\\00\"\n";
        String n = "str" + str;
        LLVMGenerator.allocateString(n, (length - 1));
        var text = "%" + reg + " = bitcast [" + length + " x i8]* %" + n + " to i8*\n";
        text += "call void @llvm.memcpy.p0i8.p0i8.i64(i8* align 1 %" + reg + ", " +
            "i8* align 1 getelementptr inbounds ([" + length + " x i8], [" + length + " x i8]*" +
            " @" + n + ", i32 0, i32 0), i64 " + length + ", i1 false)";

        addToText(text);
        reg++;
        str++;
        return "" + (reg - 1);
    }

    static void allocateString(String id, int length) {
        addToText("%" + id + " = alloca [" + (length + 1) + " x i8]");
    }

    static void declare(String id, Type type, boolean isGlobal) {
        String text = (isGlobal ? "@" : "%");
        text += id + " = " + (isGlobal ? "global" : "alloca") + " " + type.getLlvmRepresentation();
        text += (isGlobal ? " " + type.getDefaultValue() : "");

        addToText(text, isGlobal);
    }

    static void assign(String id, Value value, boolean isGlobal) {
        addToText("store " + value.getType().getLlvmRepresentation() + " " + value.getName() + ", " + value.getType().getLlvmRepresentation() + "* " + (isGlobal ? "@" : "%") + id);
    }

    static void declareArray(Array array) {
        addToText(array.getName() + " = alloca [" + array.values.size() + " x i32]");
    }

    static void assignArray(Array array) {
        int arraySize = array.values.size();
        for (int i = 0; i < arraySize; i++) {
            var text = "%" + reg + " = getelementptr inbounds [" + arraySize + " x i32], [" +
                arraySize + " x i32]* " + array.getName() + ", i32 0, i32 " + i + "\n";
            text += "store i32 " + array.values.get(i).getName() + ", i32* %" + reg;
            addToText(text);
            reg++;
        }
    }

    static String loadValueByIndex(Array array, String index) {
        var text = "%" + reg + " = getelementptr inbounds [" + array.values.size() + " x i32], [" +
            array.values.size() + " x i32]* " + array.getName() + ", i32 0, i32 " + index + "\n";
        reg++;
        text += "%" + reg + " = load i32, i32* %" + (reg - 1);
        addToText(text);
        reg++;
        return reg - 1 + "";
    }

    static Value load(String id, Value value, boolean isGlobal) {
        final var text = "%" +
            reg +
            " = load " +
            value.getType().getLlvmRepresentation() +
            ", " +
            value.getType().getLlvmRepresentation() +
            "* " +
            (isGlobal ? "@" : "%") +
            id;

        addToText(text);
        reg++;
        return value.withName(String.valueOf(reg - 1));
    }

    static Value mult(Value value1, Value value2) {
        final var text = "%" +
            reg +
            " = " +
            (value1.getType() == Type.DOUBLE ? "f" : "") +
            "mul " +
            value1.getType().getLlvmRepresentation() +
            " " +
            value1.getName() +
            ", " +
            value2.getName();

        addToText(text);
        reg++;
        return value1.withName(String.valueOf(reg - 1));
    }

    static Value div(Value value1, Value value2) {
        final var result = "%" + reg;
        final var op = value1.getType() == Type.DOUBLE ? "f" : "s";
        final var text = result + " = " + op + "div " + value1.getType().getLlvmRepresentation()
            + " " + value1.getName() + ", " + value2.getName();

        addToText(text);
        reg++;
        return value1.withName(String.valueOf(reg - 1));
    }


    static Value add(Value value1, Value value2) {
        final var text = "%" +
            reg +
            " = " +
            (value1.getType() == Type.DOUBLE ? "f" : "") +
            "add " +
            value1.getType().getLlvmRepresentation() +
            " " +
            value1.getName() +
            ", " +
            value2.getName();

        addToText(text);
        reg++;
        return value1.withName(String.valueOf(reg - 1));
    }

    static Value sub(Value value1, Value value2) {
        final var result = "%" + reg;
        final var op = value1.getType() == Type.DOUBLE ? "f" : "";
        final var text = result + " = " + op + "sub " + value1.getType().getLlvmRepresentation()
            + " " + value1.getName() + ", " + value2.getName();

        addToText(text);
        reg++;
        return value1.withName(String.valueOf(reg - 1));
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
        text += result + " = phi i1 [ " + trueVal + ", %" + labelTrue + " ], [ " + falseVal + ", %" + labelNotTrue + " ]";
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
        text += result + " = phi i1 [ " + trueVal + ", %" + labelTrue + " ], [ " + falseVal + ", %" + labelNotTrue + " ]";
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
            functionText += param.getType().getLlvmRepresentation()
                + " "
                + param.getName();

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

    static void ret(Value value) {
        functionText += "ret " +
            value.getType().getLlvmRepresentation() +
            " " +
            value.getName() +
            "\n";
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
