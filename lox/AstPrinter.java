package lox;

import lox.Expr.Binary;
import lox.Expr.Grouping;
import lox.Expr.Literal;
import lox.Expr.Unary;

// An implementation of a visitor
// This is the behaviour associated with printing the sytnax tree
// The concrete visitor defines what each node will do
// when it comes to printing it

public class AstPrinter implements Expr.Visitor<String> {

    // This method calls the accept method for the
    // expression & passes itself (a visitor implementation) to it
    // accept() takes in a visitor & each node calls the correct behaviour
    // for it
    String print(Expr expr) {
        return expr.accept(this);
    }

    @Override
    public String visitBinaryExpr(Binary expr) {
        return parenthesize(expr.operator.lexeme, expr.left, expr.right);
    }

    @Override
    public String visitGroupingExpr(Grouping expr) {
        return parenthesize("group", expr.expression);
    }

    @Override
    public String visitLiteralExpr(Literal expr) {
        if (expr.value == null)
            return "nill";
        return expr.value.toString();
    }

    @Override
    public String visitUnaryExpr(Unary expr) {
        return parenthesize(expr.operator.lexeme, expr.right);
    }

    // The function that builds the strings
    // it goes through the provided sub expressions
    // and calls their accept
    private String parenthesize(String name, Expr... exprs) {
        StringBuilder builder = new StringBuilder();

        builder.append("(").append(name);

        for (Expr expr : exprs) {
            builder.append(" ");

            // This goes recursively through the tree & each
            // returned string is appended
            builder.append(expr.accept(this));
        }

        builder.append(")");

        return builder.toString();
    }

}
