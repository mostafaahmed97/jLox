package lox;

/* 
 *  We take over instead of letting Java
 * handle runtime exceptions & unwind the stack
*/

public class RuntimeError extends RuntimeException {
    final Token token;

    RuntimeError(Token token, String message) {
        super(message);
        this.token = token;
    }
}
