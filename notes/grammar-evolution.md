

### Before adding statements

This was the simple grammar we started with for expressions

```typescript
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

We started with this

```typescript

program        → statement* EOF ;

statement      → exprStmt
               | printStmt ;

exprStmt       → expression ";" ;
printStmt      → "print" expression ";" ;

```

#### Declaring variables

Then to support declaring variables & to not have them show up as the only statement in a control flow clause:

```typescript
program        → declaration* EOF ;

declaration    → varDecl
               | statement ;

statement      → exprStmt
               | printStmt ;

varDecl        → "var" IDENTIFIER ( "=" expression )? ";" ;
exprStmt       → expression ";" ;
printStmt      → "print" expression ";" ;
```

& also added IDENTIFIER to a primary expression
```typescript
primary        → "true" | "false" | "nil"
               | NUMBER | STRING
               | "(" expression ")"
               | IDENTIFIER ;
```

#### Blocks

And finally to add support for nesting & block scopes, a statement can now evaluate to a block which is a group of declarations

```typescript
statement      → exprStmt
               | printStmt
               | block ;

block          → "{" declaration* "}" ;
```

### Supporting assignment

Extending expression grammar for assignment

```typescript
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

Statement grammar becomes

```typescript
declaration    → varDecl
               | statement ;

statement      → exprStmt
               | forStmt
               | ifStmt
               | printStmt
               | whileStmt
               | block ;

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

```typescript

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

To call a function, the additions to the expression grammar are:

```typescript
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

A call rule is slotted between Primary & Unary to have the highest precedence.

A call is a Primary expression followed by a list of arguments, each of those arguments can expand to an Expression itself.


To declare a function we extended the Declaration rule
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