package lox;

import java.util.List;

// Things that can be called will implement this
public interface LoxCallable {

    // The number of parameters a function defines
    int arity();

    Object call(Interpreter interpreter, List<Object> arguments);
}
