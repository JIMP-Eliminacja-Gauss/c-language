package org.example.llvm;

import lombok.experimental.UtilityClass;
import main.java.org.example.ExprLexer;
import main.java.org.example.ExprParser;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.io.IOException;

@UtilityClass
public class LLVMFacade {
    public void compile(String input, String output) throws IOException {
        CharStream inputStream = CharStreams.fromFileName(input);

        ExprLexer lexer = new ExprLexer(inputStream);

        CommonTokenStream tokens = new CommonTokenStream(lexer);
        ExprParser parser = new ExprParser(tokens);

        ParseTree tree = parser.prog();

//        System.out.println(tree.toStringTree(parser));

        ParseTreeWalker walker = new ParseTreeWalker();
        walker.walk(new LLVMActions(output), tree);
    }
}
