package lox;

import java.util.List;

// Things that can be called will implement this
public interface LoxCallable {

    // The number of parameters a function defines
    int arity();

    /*
     * A thing that can be called will get the interpreter
     * and a list of evaluated arguments
     */
    Object call(Interpreter interpreter, List<Object> arguments);
}
