package lox;

import java.util.List;
import java.util.ArrayList;
import static lox.TokenType.*;

// A parser is responsible for ingesting the set of 
// tokens we got from scanning and making a syntax 
// tree out of them

// Each rule in our language's grammar is a method here

// This is the grammar at this chapter 6, it's an unambigious
// grammar that we will expand on later

// expression     → equality ;
// equality       → comparison ( ( "!=" | "==" ) comparison )* ;
// comparison     → term ( ( ">" | ">=" | "<" | "<=" ) term )* ;
// term           → factor ( ( "-" | "+" ) factor )* ;
// factor         → unary ( ( "/" | "*" ) unary )* ;
// unary          → ( "!" | "-" ) unary
//                | primary ;
// primary        → NUMBER | STRING | "true" | "false" | "nil"
//                | "(" expression ")" ;

public class Parser {

    private static class ParseError extends RuntimeException {
    }

    private final List<Token> tokens;
    private int current = 0;

    Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    /*
     * Takes in a sequence of tokens
     * and returns a list of statement
     * syntax trees
     */
    List<Stmt> parse() {
        List<Stmt> statements = new ArrayList<>();

        while (!isAtEnd())
            statements.add(declaration());

        return statements;
    }

    private Expr expression() {
        return assignment();
    }

    // declaration → varDeclr | statement ;
    private Stmt declaration() {
        try {
            if (match(VAR)) {
                return varDeclaration();
            } else {
                return statement();
            }
        } catch (ParseError e) {
            synchronize();
            return null;
        }
    }

    // varDecl -> "var" IDENTIFIER ( "=" expression )? ";" ;
    private Stmt varDeclaration() {

        // var keyword was already matched
        Token name = consume(IDENTIFIER, "Expected a variable name");

        Expr initializer = null;
        if (match(EQUAL))
            initializer = expression();

        consume(SEMICOLON, "Expect ';' after value.");

        return new Stmt.Var(name, initializer);
    }

    // statement -> exprStatement | printStatement | block
    private Stmt statement() {
        if (match(PRINT))
            return printStatement();
        if (match(LEFT_BRACE))
            return new Stmt.Block(block());

        return expressionStatement();
    }

    // printStatement -> "print" expression ";"
    private Stmt printStatement() {
        // "print" was consumed earlier, now we get the expression
        Expr value = expression();

        // & the ending semicolon
        consume(SEMICOLON, "Expect ';' after value.");

        // then return this syntax tree
        return new Stmt.Print(value);
    }

    // exprStmt -> expression ";" ;
    private Stmt expressionStatement() {
        Expr expr = expression();
        consume(SEMICOLON, "Expect ';' after value.");
        return new Stmt.Expression(expr);
    }

    private List<Stmt> block() {
        List<Stmt> statements = new ArrayList<>();

        while (!check(RIGHT_BRACE) && !isAtEnd())
            statements.add(declaration());

        consume(RIGHT_BRACE, "Expect '}' after block.");
        return statements;
    }

    private Expr assignment() {
        // Get left hand side
        Expr expr = equality();

        if (match(EQUAL)) {
            // Equal means it's an assignment

            Token equals = previous();
            // Get the expression of right hand side that will evaluate to value
            Expr value = equality();

            if (expr instanceof Expr.Variable) {
                /*
                 * Only return an assignment syntax node
                 * if lhs is a valid assignment target.
                 * an l-value that we can evaluate to sth in the environment
                 */

                // Cast it to a variable expr (we know it is bec of the if)
                Token name = ((Expr.Variable) expr).name;
                return new Expr.Assign(name, value);
            }

            error(equals, "Invalid assignment target");
        }

        return expr;
    }

    private Expr equality() {
        // Start off by matching the first
        // comparison in an equality
        Expr expr = comparison();

        // After we capture the first comparison
        // if we find an equal, eval the right side
        // & make a new binary expr out of left & right

        // Evaluating the right side will recurse
        // untill all right comparisons are
        // are exhausted & will return a tree

        // match() advances current token
        // so our operator is previous()
        while (match(BANG_EQUAL, EQUAL_EQUAL)) {
            Token operator = previous();

            Expr right = comparison();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    // Impl. of comparison rule
    private Expr comparison() {

        // Same idea as comparison
        // match the higher rule component first
        Expr expr = term();

        while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
            Token operator = previous();
            Expr right = term();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    // Addition & subtraction
    private Expr term() {
        Expr expr = factor();

        while (match(MINUS, PLUS)) {
            Token operator = previous();
            Expr right = factor();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    // Multiplicatin & division
    private Expr factor() {
        Expr expr = unary();

        while (match(SLASH, STAR)) {
            Token operator = previous();
            Expr right = unary();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr unary() {
        if (match(BANG, MINUS)) {
            Token operator = previous();
            Expr right = unary();
            return new Expr.Unary(operator, right);
        }

        // If no more unary operands, see
        // what primary terminal it is
        return primary();
    }

    private Expr primary() {
        if (match(FALSE))
            return new Expr.Literal(false);
        if (match(TRUE))
            return new Expr.Literal(true);
        if (match(NIL))
            return new Expr.Literal(null);

        if (match(NUMBER, STRING)) {
            return new Expr.Literal(previous().literal);
        }

        if (match(IDENTIFIER)) {
            return new Expr.Variable(previous());
        }

        if (match(LEFT_PAREN)) {
            Expr expr = expression();
            consume(RIGHT_PAREN, "Expect ')' after expression.");
            return new Expr.Grouping(expr);
        }

        throw error(peek(), "Expect expression.");
    }

    // Checks if current token is one of types
    // Consumes if so
    // Combination of check & advance operations
    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }

        return false;
    }

    // Primitive ops / utils
    private boolean isAtEnd() {
        return peek().type == EOF;
    }

    private boolean check(TokenType type) {
        if (isAtEnd())
            return false;
        return peek().type == type;
    }

    private Token consume(TokenType type, String message) {
        if (check(type))
            return advance();

        throw error(peek(), message);
    }

    private Token peek() {
        return tokens.get(current);
    }

    private Token previous() {
        return tokens.get(current - 1);
    }

    private Token advance() {
        if (!isAtEnd())
            current++;
        return previous();
    }

    private ParseError error(Token token, String message) {
        Lox.error(token, message);
        return new ParseError();
    }

    private void synchronize() {
        advance();

        while (!isAtEnd()) {
            if (previous().type == SEMICOLON)
                return;

            switch (peek().type) {
                case CLASS:
                case FUN:
                case VAR:
                case FOR:
                case IF:
                case WHILE:
                case PRINT:
                case RETURN:
                    return;
            }

            advance();
        }
    }

}
