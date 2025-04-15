package org.example.llvm;

import org.example.type.Type;
import org.example.type.Value;

public class LLVMGenerator {

    private static final int MAX_READ_STRING_LENGTH = 100;
    static String headerText = "";
    static String mainText = "";
    static int reg = 1;
    static int str = 1;

    static void printf(Value value) {
        Type type = value.getType();
        mainText += "%" + reg +
            " = call i32 (i8*, ...) " +
            "@printf(i8* getelementptr inbounds ([4 x i8], [4 x i8]* " +
            "@" + type.getLlvmStringRepresentation() +
            ", i32 0, i32 0), " +
            type.getLlvmRepresentation() +
            " " + value.getName() +
            ")\n";
        reg++;
    }

    static void scanf(String id) {
        allocateString("str" + str, MAX_READ_STRING_LENGTH);
        mainText += "%" + reg
            + " = getelementptr inbounds ["
            + (MAX_READ_STRING_LENGTH + 1)
            + " x i8], ["
            + (MAX_READ_STRING_LENGTH + 1)
            + " x i8]* %str"
            + str
            + ", i64 0, i64 0\n";
        reg++;
        mainText += "store i8* %"
            + (reg - 1)
            + ", i8** "
            + "%" + id
            + "\n";
        str++;
        mainText += "%"
            + reg
            + " = call i32 (i8*, ...) @scanf(i8* getelementptr inbounds ([5 x i8], [5 x i8]* @strs, i32 0, i32 0), i8* %"
            + (reg - 1)
            + ")\n";
        reg++;
    }

    static String constantString(String content) {
        int length = content.length() + 1;
        headerText += "@str" + str + " = constant [" + length + " x i8] c\"" + content + "\\00\"\n";
        String n = "str" + str;
        LLVMGenerator.allocateString(n, (length - 1));
        mainText += "%" + reg + " = bitcast [" + length + " x i8]* %" + n + " to i8*\n";
        mainText += "call void @llvm.memcpy.p0i8.p0i8.i64(i8* align 1 %" + reg + ", " +
            "i8* align 1 getelementptr inbounds ([" + length + " x i8], [" + length + " x i8]*" +
            " @" + n + ", i32 0, i32 0), i64 " + length + ", i1 false)\n";
        reg++;
        str++;
        return "" + (reg - 1);
    }

    static void allocateString(String id, int length) {
        mainText += "%" + id + " = alloca [" + (length + 1) + " x i8]\n";
    }

    static void declare(String id, Type type) {
        addToMainText("%" + id + " = alloca " + type.getLlvmRepresentation());
    }

    static void assign(String id, Value value) {
        addToMainText("store " + value.getType().getLlvmRepresentation() + " " + value.getName() + ", " + value.getType().getLlvmRepresentation() + "* %" + id);
    }

    static Value load(String id, Value value) {
        mainText += "%" +
            reg +
            " = load " +
            value.getType().getLlvmRepresentation() +
            ", " +
            value.getType().getLlvmRepresentation() +
            "* " +
            "%" +
            id +
            "\n";
        reg++;
        return value.withName(String.valueOf(reg - 1));
    }

    static Value mult(Value value1, Value value2) {
        mainText += "%" +
            reg +
            " = " +
            (value1.getType() == Type.DOUBLE ? "f" : "") +
            "mul " +
            value1.getType().getLlvmRepresentation() +
            " " +
            value1.getName() +
            ", " +
            value2.getName() +
            "\n";
        reg++;
        return value1.withName(String.valueOf(reg - 1));
    }

    static Value add(Value value1, Value value2) {
        mainText += "%" +
            reg +
            " = " +
            (value1.getType() == Type.DOUBLE ? "f" : "") +
            "add " +
            value1.getType().getLlvmRepresentation() +
            " " +
            value1.getName() +
            ", " +
            value2.getName() +
            "\n";
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
        reg++;

        // Jeśli value1 jest fałszywe, skaczemy od razu do końca
        mainText += "br i1 " + value1.getName() + ", label %" + labelTrue + ", label %" + labelNotTrue + "\n";

        // Blok jeśli value1 == true sprawdzamy value2
        mainText += labelTrue + ":\n";
        mainText += trueVal + " = and i1 " + value1.getName() + ", " + value2.getName() + "\n";
        mainText += "br label %" + labelEnd + "\n";

        // Jeśli value1 == false, zwracamy 0
        mainText += labelNotTrue + ":\n";
        mainText += falseVal + " = and i1 0, 0\n";
        mainText += "br label %" + labelEnd + "\n";

        // PHI node – wybór zależnie od ścieżki
        mainText += labelEnd + ":\n";
        mainText += result + " = phi i1 [ " + trueVal + ", %" + labelTrue + " ], [ " + falseVal + ", %" + labelNotTrue + " ]\n";
        return new Value(String.valueOf(reg - 1), Type.BOOL);
    }

    static Value or(Value value1, Value value2) {
        final var labelTrue = "or_true_" + reg;
        final var labelNotTrue = "or_not_true_" + reg;
        final var labelEnd = "or_end_" + reg;
        final var result = "%" + reg;
        final var trueVal = "%true_" + reg;
        final var falseVal = "%false_" + reg;
        reg++;

        // Jeśli value1 jest prawdziwe, skaczemy od razu do labelTrue
        mainText += "br i1 " + value1.getName() + ", label %" + labelTrue + ", label %" + labelNotTrue + "\n";

        // Blok jeśli value1 == true — zwracamy od razu true
        mainText += labelTrue + ":\n";
        mainText += trueVal + " = or i1 1, 1\n";
        mainText += "br label %" + labelEnd + "\n";

        // Blok jeśli value1 != true — obliczamy or
        mainText += labelNotTrue + ":\n";
        mainText += falseVal + " = or i1 " + value1.getName() + ", " + value2.getName() + "\n";
        mainText += "br label %" + labelEnd + "\n";

        // PHI node — wynik końcowy
        mainText += labelEnd + ":\n";
        mainText += result + " = phi i1 [ " + trueVal + ", %" + labelTrue + " ], [ " + falseVal + ", %" + labelNotTrue + " ]\n";
        return new Value(String.valueOf(reg - 1), Type.BOOL);
    }

    static Value neg(Value value) {
        final var result = "%" + reg;
        mainText += result + " = xor i1 " + value.getName() + ", 1\n"; // NOT = XOR z 1
        reg++;
        return value.withName(String.valueOf(reg - 1));
    }

    static Value xor(Value value1, Value value2) {
        final var result = "%" + reg;
        mainText += result + " = xor i1 " + value1.getName() + ", " + value2.getName() + "\n";
        reg++;
        return new Value(String.valueOf(reg - 1), Type.BOOL);
    }

    static Value xand(Value value1, Value value2) {
        final var result = "%" + reg;
        mainText += result + " = " + value1.getType().getLlvmComparator()
            + " eq " + value2.getType().getLlvmRepresentation()
            + " " + value1.getName() + ", " + value2.getName() + "\n";
        reg++;

        return new Value(String.valueOf(reg - 1), Type.BOOL);
    }


    static void addToMainText(String text) {
        mainText += text + "\n";
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
        text += headerText;
        text += "define i32 @main() nounwind{\n";
        text += mainText;
        text += "ret i32 0 }\n";
        return text;
    }

}
