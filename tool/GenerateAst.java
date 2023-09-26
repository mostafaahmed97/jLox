package tool;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

// This file is a tool to generate the java classes
// needed to represent nodes in our syntax, the nodes
// consist of data fields & are mostly the same with minor differences.
// So instead of writing them manually we'll generate them.

public class GenerateAst {
    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.err.println("Usage: generate_ast <output directory>");
            System.exit(64);
        }

        String outputDir = args[0];

        // Description of the classes we want
        defineAst(outputDir, "Expr", Arrays.asList(
                "Assign     : Token name, Expr value",
                "Binary     : Expr left, Token operator, Expr right",
                "Call       : Expr callee, Token paren, List<Expr> arguments",
                "Grouping   : Expr expression",
                "Literal    : Object value",
                "Logical    : Expr left, Token operator, Expr right",
                "Unary      : Token operator, Expr right",
                "Variable : Token name"));

        /*
         * The syntax tree nodes for statements
         */
        defineAst(outputDir, "Stmt", Arrays.asList(
                "Block      : List<Stmt> statements",
                "Expression : Expr expression",
                "If         : Expr condition, Stmt thenBranch," +
                        " Stmt elseBranch",
                "Print      : Expr expression",
                "Var        : Token name, Expr initializer",
                "While      : Expr condition, Stmt body"));
    }

    private static void defineAst(
            String outputDir,
            String baseName,
            List<String> types)
            throws IOException {

        String path = outputDir + '/' + baseName + ".java";

        PrintWriter writer = new PrintWriter(path, "UTF-8");

        writer.println("package lox;");
        writer.println();
        writer.println("import java.util.List;");
        writer.println();
        writer.println("abstract class " + baseName + " {");

        // Define the vistor Interface
        defineVistor(writer, baseName, types);

        writer.println();
        writer.println("  abstract <R> R accept(Visitor<R> visitor);");
        writer.println();

        // Generate classes here
        for (String type : types) {
            String className = type.split(":")[0].trim();
            String fields = type.split(":")[1].trim();

            defineType(writer, baseName, className, fields);
        }

        writer.println("}");
        writer.close();
    }

    private static void defineType(
            PrintWriter writer,
            String baseName,
            String className,
            String fieldList) {

        // Declaration of the class inside the abstract one
        writer.println("  static class " + className + " extends " + baseName + " {");

        // Constructor.
        writer.println("    " + className + "(" + fieldList + ") {");

        // Store parameters in fields.
        String[] fields = fieldList.split(", ");
        for (String field : fields) {
            String name = field.split(" ")[1];
            writer.println("      this." + name + " = " + name + ";");
        }

        writer.println("    }");

        // Fields.
        writer.println();
        for (String field : fields) {
            writer.println("    final " + field + ";");
        }

        writer.println();
        writer.println("    @Override");
        writer.println("    <R> R accept(Visitor<R> visitor) {");
        writer.println("      return visitor.visit" +
                className + baseName + "(this);");
        writer.println("    }");
        writer.println();

        writer.println("  }");
    }

    private static void defineVistor(PrintWriter writer, String baseName, List<String> types) {

        writer.println("  interface Visitor<R> {");

        for (String type : types) {
            String typeName = type.split(":")[0].trim();
            writer.println("\t R visit" + typeName + baseName + "(" +
                    typeName + " " + baseName.toLowerCase() + ");");
        }

        writer.println("  }");

    }

}