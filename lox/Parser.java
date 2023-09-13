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
            statements.add(statement());

        return statements;
    }

    // Impl. for the expression rule,
    // it simply returns what the rule
    // for equality finds
    private Expr expression() {
        return equality();
    }

    // statement -> exprStatement | printStatement
    private Stmt statement() {
        if (match(PRINT))
            return printStatement();

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

    private Stmt expressionStatement() {
        Expr expr = expression();
        consume(SEMICOLON, "Expect ';' after value.");
        return new Stmt.Expression(expr);
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
