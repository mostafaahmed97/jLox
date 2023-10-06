package lox;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
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

    // declaration → classDecl | varDecl | funDecl | statement ;
    private Stmt declaration() {
        try {
            if (match(CLASS))
                return classDeclaration();
            if (match(FUN))
                return function("function");
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

    // classDecl → "class" IDENTIFIER "{" function* "}" ;
    private Stmt classDeclaration() {
        Token name = consume(IDENTIFIER, "Expect class name.");
        consume(LEFT_BRACE, "Expect '{' before class body.");

        List<Stmt.Function> methods = new ArrayList<>();

        while (!check(RIGHT_BRACE) && !isAtEnd()) {
            methods.add(function("method"));
        }

        consume(RIGHT_BRACE, "Expect '}' after class body.");

        return new Stmt.Class(name, methods);
    }

    /*
     * funDecl → "fun" function ;
     * function → IDENTIFIER "(" parameters? ")" block ;
     */
    private Stmt.Function function(String kind) {
        Token name = consume(IDENTIFIER, "Expect " + kind + " name.");

        consume(LEFT_PAREN, "Expect '(' after " + kind + " name.");

        List<Token> parameters = new ArrayList<>();

        if (!check(RIGHT_PAREN)) {
            do {
                if (parameters.size() >= 255) {
                    error(peek(), "Can't have more than 255 parameters.");
                }

                parameters.add(
                        consume(IDENTIFIER, "Expect parameter name."));
            } while (match(COMMA));
        }

        consume(RIGHT_PAREN, "Expect ')' after parameters.");
        consume(LEFT_BRACE, "Expect '{' before " + kind + " body.");

        List<Stmt> body = block();
        return new Stmt.Function(name, parameters, body);
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

    /*
     * statement -> exprStatement |
     * returnStatement |
     * printStatement |
     * ifStmt |
     * whileStmt |
     * block
     */
    private Stmt statement() {
        if (match(FOR))
            return forStatement();
        if (match(IF))
            return ifStatement();
        if (match(RETURN))
            return returnStatement();
        if (match(PRINT))
            return printStatement();
        if (match(WHILE))
            return whileStatement();
        if (match(LEFT_BRACE))
            return new Stmt.Block(block());

        return expressionStatement();
    }

    /*
     * ifStmt → "if" "(" expression ")" statement
     * ( "else" statement )? ;
     */
    private Stmt ifStatement() {
        consume(LEFT_PAREN, "");
        Expr condition = expression();

        consume(RIGHT_PAREN, "null");

        /*
         * Block is a statement
         * Also we cant declare a var
         * in body of if bec. we
         * separated it into
         * another rule
         */
        Stmt thenBranch = statement();

        Stmt elseBranch = null;
        if (match(ELSE))
            elseBranch = statement();

        return new Stmt.If(condition, thenBranch, elseBranch);
    }

    /*
     * We are desugaring the for into
     * simpler stmts (while loop & others) our interpretor
     * can already handle. So we haven't added
     * a Stmt syntax node for it.
     * 
     * forStmt → "for" "(" ( varDecl | exprStmt | ";" )
     * expression? ";"
     * expression? ")" statement ;
     */
    private Stmt forStatement() {
        consume(LEFT_PAREN, "Expect '('' after 'for'.");

        Stmt initializer;

        // Possible causes for the first clause
        if (match(SEMICOLON)) {
            initializer = null;
        } else if (match(VAR)) {
            initializer = varDeclaration();
        } else {
            initializer = expressionStatement();
        }

        Expr condition = null;
        if (!check(SEMICOLON)) {
            condition = expression();
        }

        consume(SEMICOLON, "Expect ';' after loop condition.");

        Expr increment = null;
        if (!check(SEMICOLON)) {
            increment = expression();
        }

        consume(RIGHT_PAREN, "Expect ')' after for clauses.");

        Stmt body = statement();

        /*
         * Now all the individual parts are parsed
         * 
         * for (var i = 0; i < 10; i = i + 1) print i;
         * 
         * is equivalent to
         * 
         * {
         * 
         * // initializer
         * var i = 0;
         * 
         * // body & condition
         * while (i < 10) {
         * print i;
         * 
         * // increment
         * i = i + 1;
         * }
         * }
         */

        /*
         * If we have an increment
         */
        if (increment != null) {
            body = new Stmt.Block(
                    Arrays.asList(
                            body,
                            new Stmt.Expression(increment)));
        }

        // if no condition was provided make it always true
        if (condition == null)
            condition = new Expr.Literal(true);

        // Wrap up the body so far (statement of for + increment) with the condition
        body = new Stmt.While(condition, body);

        // run init once before loop block
        if (initializer != null) {
            body = new Stmt.Block(Arrays.asList(
                    initializer,
                    body));
        }

        return body;
    }

    // whileStmt -> "while" "(" expression ")" statement ;
    private Stmt whileStatement() {
        consume(LEFT_PAREN, "Expect '(' after 'while'.");
        Expr condition = expression();
        consume(RIGHT_PAREN, "Expect ')' after condition.");
        Stmt body = statement();

        return new Stmt.While(condition, body);
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

    // returnStmt → "return" expression? ";" ;
    private Stmt returnStatement() {
        Token keyword = previous();

        Expr value = null;

        if (!check(SEMICOLON))
            value = expression();

        consume(SEMICOLON, "Expect ';' after return value.");

        return new Stmt.Return(keyword, value);
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
        Expr expr = or();

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

    private Expr or() {
        Expr expr = and();

        while (match(OR)) {
            Token operator = previous();
            Expr right = and();

            expr = new Expr.Logical(expr, operator, right);
        }

        return expr;
    }

    private Expr and() {
        Expr expr = equality();

        while (match(AND)) {
            Token operator = previous();
            Expr right = equality();

            expr = new Expr.Logical(expr, operator, right);
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

        return call();
    }

    private Expr call() {
        Expr expr = primary();

        while (true) {
            if (match(LEFT_PAREN))
                expr = finishCall(expr);
            else
                break;
        }

        return expr;
    }

    private Expr finishCall(Expr expr) {
        List<Expr> arguements = new ArrayList<>();

        if (!check(RIGHT_PAREN)) {
            do {
                if (arguements.size() >= 255)
                    error(peek(), "Can't have more than 255 arguments.");

                arguements.add(expression());
            } while (match(COMMA));
        }

        Token paren = consume(RIGHT_PAREN, "Expect ')' after arguments.");

        return new Expr.Call(expr, paren, arguements);
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
