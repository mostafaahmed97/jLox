package lox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static lox.TokenType.*;

public class Scanner {
    private final String source;
    private final List<Token> tokens = new ArrayList<>();

    // The start of lexeme being considered now
    private int start = 0;

    // The current character
    private int current = 0;

    // What line we're on
    private int line = 1;

    private static final Map<String, TokenType> keywords;

    static {
        keywords = new HashMap<>();
        keywords.put("and", AND);
        keywords.put("class", CLASS);
        keywords.put("else", ELSE);
        keywords.put("false", FALSE);
        keywords.put("for", FOR);
        keywords.put("fun", FUN);
        keywords.put("if", IF);
        keywords.put("nil", NIL);
        keywords.put("or", OR);
        keywords.put("print", PRINT);
        keywords.put("return", RETURN);
        keywords.put("super", SUPER);
        keywords.put("this", THIS);
        keywords.put("true", TRUE);
        keywords.put("var", VAR);
        keywords.put("while", WHILE);
    }

    Scanner(String s) {
        this.source = s;
    }

    List<Token> scanTokens() {

        while (!isAtEnd()) {
            start = current;
            scanToken();
        }

        tokens.add(new Token(EOF, "", null, line));
        return tokens;
    }

    private void scanToken() {
        char c = advance();

        switch (c) {
            // Handling one character lexemes
            case '(':
                addToken(LEFT_PAREN);
                break;
            case ')':
                addToken(RIGHT_PAREN);
                break;
            case '{':
                addToken(LEFT_BRACE);
                break;
            case '}':
                addToken(RIGHT_BRACE);
                break;
            case ',':
                addToken(COMMA);
                break;
            case '.':
                addToken(DOT);
                break;
            case '-':
                addToken(MINUS);
                break;
            case '+':
                addToken(PLUS);
                break;
            case ';':
                addToken(SEMICOLON);
                break;
            case '*':
                addToken(STAR);
                break;

            // For characters that can be followed by another
            // and they form a single unit (!=, >=, <= etc..)
            // Check if the next char matches to add the correct type
            case '!':
                addToken(match('=') ? BANG_EQUAL : BANG);
                break;
            case '=':
                addToken(match('=') ? EQUAL_EQUAL : EQUAL);
                break;
            case '<':
                addToken(match('=') ? LESS_EQUAL : LESS);
                break;
            case '>':
                addToken(match('=') ? GREATER_EQUAL : GREATER);
                break;

            // Handling division, it might be a division
            // or a start of a comment we cant be sure until we peek
            case '/':
                // If we match an upcoming /, then this is a comment
                // keep consuming till new line
                if (match('/')) {
                    while (peek() != '\n' && !isAtEnd())
                        advance();
                } else {
                    addToken(SLASH);
                }
                break;

            // Ignore white space
            case ' ':
            case '\r':
            case '\t':
                break;

            // On new line inc. counter
            // The source code input is one giant string
            case '\n':
                line++;
                break;

            // Handling longer string lexemes, string literals
            case '"':
                string();
                break;

            default:
                /*
                 * Handling numbers & identifiers
                 * in the default case to avoid
                 * switching on each digit/letter
                 */
                if (isDigit(c))
                    number();
                else if (isAlpha(c))
                    identifier();
                else
                    Lox.error(line, "Unexpected character leh keda a7amo");
                break;
        }
    }

    private boolean match(char expected) {
        if (isAtEnd())
            return false;

        // At start of scanToken current was incremented, so we don't here
        // bec. we're already at the next char
        if (source.charAt(current) != expected)
            return false;

        current++;
        return true;
    }

    // Lookahead, we support two levels at max
    private char peek() {
        if (isAtEnd())
            return '\0';

        // No increment here too bec. it happened in setToken
        return source.charAt(current);
    }

    private char peekNext() {
        if (current + 1 >= source.length())
            return '\0';
        return source.charAt(current + 1);
    }

    private char advance() {
        // Get the current char & post increment
        return source.charAt(current++);
    }

    private boolean isAtEnd() {
        return current >= source.length();
    }

    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z') ||
                (c >= 'A' && c <= 'Z') ||
                c == '_';
    }

    private boolean isAlphaNumeric(char c) {
        return isAlpha(c) || isDigit(c);
    }

    // An overload for
    private void addToken(TokenType type) {
        addToken(type, null);
    }

    private void addToken(TokenType type, Object literal) {
        String text = source.substring(start, current);
        tokens.add(new Token(type, text, literal, line));
    }

    private void string() {
        while (peek() != '"' && !isAtEnd()) {
            // Support for multi line string
            if (peek() == '\n')
                line++;

            // Keep consuming, we haven't met the other quote yet
            advance();
        }

        if (isAtEnd()) {
            Lox.error(line, "Unterminated string");
            return;
        }

        // The closing ""
        advance();

        // Trim quotes
        String literalValue = source.substring(start + 1, current - 1);
        addToken(STRING, literalValue);
    }

    private void number() {

        // Consume digits as long as we peek another digit
        while (isDigit(peek()))
            advance();

        // If we peek a dot followed by digits
        if (peek() == '.' && isDigit(peekNext())) {
            // Consume the dot
            advance();

            // And the digits till they end
            while (isDigit(peek()))
                advance();
        }

        addToken(NUMBER, Double.parseDouble(source.substring(start, current)));
    }

    private void identifier() {
        while (isAlphaNumeric(peek()))
            advance();

        String text = source.substring(start, current);
        TokenType type = keywords.get(text);

        if (type == null)
            type = IDENTIFIER;

        addToken(type);
    }

}
