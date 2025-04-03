package org.example;

import org.example.type.Type;
import org.example.util.Variable;

public class LLVMGenerator {

    static String headerText = "";
    static String mainText = "";
    static int reg = 1;

    static void printf(String id, Type type) {
        mainText += "%" + reg + " = load i32, i32* %" + id + "\n";
        reg++;
        mainText += "%" + reg + " = call i32 (i8*, ...) @printf(i8* getelementptr inbounds ([4 x i8], [4 x i8]* @strp, i32 0, i32 0), i32 %" + (reg - 1) + ")\n";
        reg++;
    }

    static void scanf(String id) {
        mainText += "%" + reg + " = call i32 (i8*, ...) @__isoc99_scanf(i8* getelementptr inbounds ([3 x i8], [3 x i8]* @strs, i32 0, i32 0), i32* %" + id + ")\n";
        reg++;
    }

    static void declare(String id, Type type) {
        addToMainText("%" + id + " = alloca " + type.llvmRepresentation());
    }

    static void assign(String id, Variable variable) {
        addToMainText("store " + variable.type().llvmRepresentation() + " " + variable.value() + ", " + variable.type().llvmRepresentation() + "* %" + id);
    }

    static void addToMainText(String text) {
        mainText += text + "\n";
    }

    static String generate() {
        String text = "";
        text += "declare i32 @printf(i8*, ...)\n";
        text += "declare i32 @__isoc99_scanf(i8*, ...)\n";
        text += "@strp = constant [4 x i8] c\"%d\\0A\\00\"\n";
        text += "@strs = constant [3 x i8] c\"%d\\00\"\n";
        text += headerText;
        text += "define i32 @main() nounwind{\n";
        text += mainText;
        text += "ret i32 0 }\n";
        return text;
    }

}
