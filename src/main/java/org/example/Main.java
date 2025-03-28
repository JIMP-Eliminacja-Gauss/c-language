package org.example;

import main.java.org.example.PLwypiszLexer;
import main.java.org.example.PLwypiszParser;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        CharStream input = CharStreams.fromFileName(args[0]);

        PLwypiszLexer lexer = new PLwypiszLexer(input);

        CommonTokenStream tokens = new CommonTokenStream(lexer);
        PLwypiszParser parser = new PLwypiszParser(tokens);

        ParseTree tree = parser.prog();

//        System.out.println(tree.toStringTree(parser));

        ParseTreeWalker walker = new ParseTreeWalker();
        walker.walk(new LLVMActions(), tree);
    }
}