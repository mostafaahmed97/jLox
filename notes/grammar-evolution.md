# Evolution of Lox grammar <!-- omit in toc -->

- [Note about syntax](#note-about-syntax)
- [Expressions](#expressions)
- [Adding statements](#adding-statements)
  - [Declaring variables](#declaring-variables)
  - [Blocks](#blocks)
- [Supporting assignment](#supporting-assignment)
- [Control Flow \& Looping](#control-flow--looping)
- [Functions](#functions)
- [Classes](#classes)
  - [Get Expressions](#get-expressions)
  - [Set Expressions](#set-expressions)
  - [This](#this)
  - [Inheritance](#inheritance)
  - [Super](#super)
- [Finale](#finale)

### Note about syntax

Some symbols are used to express specific meanings in our grammar rules.

Some of the most common ones are :

- **`|`** , means or, represents a choice.
- **`?`** , means zero or one times, represents an optional token.
- **`*`** , means zero or more times, represents an optional token that can repeat.

<br/>

For example, a rule that states

```Java
NUMBER -> ("1")* | "0" ("1")?
```

means that a **NUMBER** rule can evaluate to either
- Any number of repeating ones (including zero)
- A zero followed by an optional one.

<br/>

### Expressions

This was the simple grammar we started with for expressions

```Java
expression     → equality ;
equality       → comparison ( ( "!=" | "==" ) comparison )* ;
comparison     → term ( ( ">" | ">=" | "<" | "<=" ) term )* ;
term           → factor ( ( "-" | "+" ) factor )* ;
factor         → unary ( ( "/" | "*" ) unary )* ;
unary          → ( "!" | "-" ) unary
               | primary ;
primary        → NUMBER | STRING | "true" | "false" | "nil"
               | "(" expression ")" ;
```

### Adding statements

We started with this set of rules for statements.

A statement for now can either print something or be an expression.

```Java

program        → statement* EOF ;

statement      → exprStmt
               | printStmt ;

exprStmt       → expression ";" ;
printStmt      → "print" expression ";" ;

```

#### Declaring variables

Then to support declaring variables we added a declaration rule.

A declaration can either define a variable or be a statement.

Variable declaration was hoisted above statements to not have them show up as the only statement in a control flow clause.

```Java
program        → declaration* EOF ;

declaration    → varDecl
               | statement ;

statement      → exprStmt
               | printStmt ;

varDecl        → "var" IDENTIFIER ( "=" expression )? ";" ;
exprStmt       → expression ";" ;
printStmt      → "print" expression ";" ;
```

<br>

And to the expression grammar we added **IDENTIFIER** which is a primary terminal.
```Java
primary        → "true" | "false" | "nil"
               | NUMBER | STRING
               | "(" expression ")"
               | IDENTIFIER ;
```

#### Blocks

And finally to add support for nesting & block scopes, a statement can now evaluate to a block which is a group of declarations

```Java
statement      → exprStmt
               | printStmt
               | block ;

block          → "{" declaration* "}" ;
```


### Supporting assignment

Adding the rule to expression grammar for assignment

```Java
expression     → assignment ;
assignment     → IDENTIFIER "=" assignment
               | equality ;
equality       → comparison ( ( "!=" | "==" ) comparison )* ;
comparison     → term ( ( ">" | ">=" | "<" | "<=" ) term )* ;
term           → factor ( ( "-" | "+" ) factor )* ;
factor         → unary ( ( "/" | "*" ) unary )* ;
unary          → ( "!" | "-" ) unary
               | primary ;
primary        → "true" | "false" | "nil"
               | NUMBER | STRING
               | "(" expression ")"
               | IDENTIFIER ;
```

### Control Flow & Looping

To support control flow (if statement) & loops (while & for), the grammar was extended to the following:


Statement grammar

```Java
declaration    → varDecl
               | statement ;

statement      → exprStmt
               | forStmt
               | ifStmt
               | printStmt
               | whileStmt
               | block ;

block          → "{" declaration* "}" ;

varDecl        → "var" IDENTIFIER ( "=" expression )? ";" ;

exprStmt       → expression ";" ;

printStmt      → "print" expression ";" ;

ifStmt         → "if" "(" expression ")" statement
               ( "else" statement )? ;

whileStmt      → "while" "(" expression ")" statement ;

forStmt        → "for" "(" ( varDecl | exprStmt | ";" )
                 expression? ";"
                 expression? ")" statement ;

```

And we added logical operators or/and to expression grammar

They have the lowest precedence

```Java

expression     → assignment ;

assignment     → IDENTIFIER "=" assignment
               | logic_or ;

logic_or       → logic_and ( "or" logic_and )* ;
logic_and      → equality ( "and" equality )* ;

equality       → comparison ( ( "!=" | "==" ) comparison )* ;
comparison     → term ( ( ">" | ">=" | "<" | "<=" ) term )* ;
term           → factor ( ( "-" | "+" ) factor )* ;
factor         → unary ( ( "/" | "*" ) unary )* ;
unary          → ( "!" | "-" ) unary
               | primary ;

primary        → "true" | "false" | "nil"
               | NUMBER | STRING
               | "(" expression ")"
               | IDENTIFIER ;
```

### Functions

We added multiple rules to support calling & declaring functions

To call a function, the additions to the expression grammar are the call & argument rules.

```Java
expression     → assignment ;

assignment     → IDENTIFIER "=" assignment
               | logic_or ;

logic_or       → logic_and ( "or" logic_and )* ;
logic_and      → equality ( "and" equality )* ;

equality       → comparison ( ( "!=" | "==" ) comparison )* ;
comparison     → term ( ( ">" | ">=" | "<" | "<=" ) term )* ;
term           → factor ( ( "-" | "+" ) factor )* ;
factor         → unary ( ( "/" | "*" ) unary )* ;

unary          → ( "!" | "-" ) unary | call  ;
call           → primary ( "(" arguments? ")" )* ;

arguments      → expression ( "," expression )* ;

primary        → "true" | "false" | "nil"
               | NUMBER | STRING
               | "(" expression ")"
               | IDENTIFIER ;
```

The call rule is slotted between Primary & Unary to have the highest precedence.

Call is a Primary expression followed by a list of arguments, each of those arguments can expand to an Expression itself.


To declare a function we extended the **declaration** rule
in statement grammar to expand to a function declaration.

```Java
declaration    → funDecl
               | varDecl
               | statement ;

funDecl        → "fun" function ;
function       → IDENTIFIER "(" parameters? ")" block ;
parameters     → IDENTIFIER ( "," IDENTIFIER )* ;

```

Last step was to add a return statement

```Java
statement      → exprStmt
               | forStmt
               | ifStmt
               | printStmt
               | returnStmt
               | whileStmt
               | block ;

returnStmt     → "return" expression? ";" ;
```

So far our statement grammar is

```Java
declaration    → funDecl
               | varDecl
               | statement ;

funDecl        → "fun" function ;
function       → IDENTIFIER "(" parameters? ")" block ;
parameters     → IDENTIFIER ( "," IDENTIFIER )* ;

statement      → exprStmt
               | forStmt
               | ifStmt
               | printStmt
               | returnStmt
               | whileStmt
               | block ;

returnStmt     → "return" expression? ";" ;

varDecl        → "var" IDENTIFIER ( "=" expression )? ";" ;

exprStmt       → expression ";" ;
printStmt      → "print" expression ";" ;
ifStmt         → "if" "(" expression ")" statement
               ( "else" statement )? ;
            
whileStmt      → "while" "(" expression ")" statement ;
forStmt        → "for" "(" ( varDecl | exprStmt | ";" )
                 expression? ";"
                 expression? ")" statement ;
```

### Classes

The **declaration** rule was extended to

```Java
declaration    → classDecl
               | funDecl
               | varDecl
               | statement ;

classDecl      → "class" IDENTIFIER "{" function* "}" ;
```

#### Get Expressions

A get expressions accesses a property on a class instance. For example `instance.methodName()` or `instance.variableName`

To support this the **call** rule was updated.

```Java
// From
call           → primary ( "(" arguments? ")" )* ;

// To
call           → primary ( "(" arguments? ")" | "." IDENTIFIER )* ;
```

Now any chain of method calls / property accesses can be created.

#### Set Expressions

To assign properties on an instance

```Java
// From
assignment     → IDENTIFIER "=" assignment
               | logic_or ;

// To
assignment     → ( call "." )? IDENTIFIER "=" assignment
               | logic_or ;
```

#### This

To allow a method to access it's calling object properties.

```Java
primary        → "true" | "false" | "nil" | "this"
               | NUMBER | STRING | IDENTIFIER | "(" expression ")" ;
```

#### Inheritance

Syntax for inheritence in Lox is `class Child < Parent {}`

So the class declaration rule becomes

```Java
classDecl      → "class" IDENTIFIER ( "<" IDENTIFIER )?
                 "{" function* "}" ;
```

#### Super

A child object can invoke a method in it's parent using super.

So the updated primary expression rule becomes :

```Java
primary        → "true" | "false" | "nil" | "this"
               | NUMBER | STRING | IDENTIFIER | "(" expression ")"
               | "super" "." IDENTIFIER ;
```


### Finale

The grammar we reached at the end of our journey was

For statements & declarations

```Java
declaration    → classDecl
               | funDecl
               | varDecl
               | statement ;

classDecl      → "class" IDENTIFIER ( "<" IDENTIFIER )?
                 "{" function* "}" ;
                 
funDecl        → "fun" function ;
varDecl        → "var" IDENTIFIER ( "=" expression )? ";" ;

statement      → exprStmt
               | forStmt
               | ifStmt
               | printStmt
               | returnStmt
               | whileStmt
               | block ;

exprStmt       → expression ";" ;

forStmt        → "for" "(" ( varDecl | exprStmt | ";" )
                           expression? ";"
                           expression? ")" statement ;

ifStmt         → "if" "(" expression ")" statement
                 ( "else" statement )? ;

printStmt      → "print" expression ";" ;
returnStmt     → "return" expression? ";" ;
whileStmt      → "while" "(" expression ")" statement ;
block          → "{" declaration* "}" ;
```

For expressions

```Java
expression     → assignment ;

assignment     → ( call "." )? IDENTIFIER "=" assignment
               | logic_or ;

logic_or       → logic_and ( "or" logic_and )* ;
logic_and      → equality ( "and" equality )* ;
equality       → comparison ( ( "!=" | "==" ) comparison )* ;
comparison     → term ( ( ">" | ">=" | "<" | "<=" ) term )* ;
term           → factor ( ( "-" | "+" ) factor )* ;
factor         → unary ( ( "/" | "*" ) unary )* ;

unary          → ( "!" | "-" ) unary | call ;
call           → primary ( "(" arguments? ")" | "." IDENTIFIER )* ;
primary        → "true" | "false" | "nil" | "this"
               | NUMBER | STRING | IDENTIFIER | "(" expression ")"
               | "super" "." IDENTIFIER ;
```

With some helper rules
```Java
function       → IDENTIFIER "(" parameters? ")" block ;
parameters     → IDENTIFIER ( "," IDENTIFIER )* ;
arguments      → expression ( "," expression )* ;
```