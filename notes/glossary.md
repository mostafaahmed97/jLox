# Glossary <!-- omit in toc -->

- [**Lexeme**](#lexeme)
- [**Lexical Grammar**](#lexical-grammar)
- [**Token**](#token)
- [**Scanner**](#scanner)
- [**CFG**](#cfg)
- [**Parser**](#parser)
- [**Precedence**](#precedence)
- [**Associativty**](#associativty)
- [**Top Down Parser**](#top-down-parser)
- [**Statement**](#statement)
- [**Environment**](#environment)
- [**l-value vs r-value**](#l-value-vs-r-value)
- [**Scope**](#scope)
- [**Shadowing**](#shadowing)


### **Lexeme**

The smallest meaningful unit in a language.

Can be an identifier, bracket, operator, etc...

Ex: `(` , `{`, `if`, `while`, variable names are all valid lexemes

<br>

### **Lexical Grammar**

Rules about how the language groups characters into lexemes

Usually expressed as a regular grammar

Lexical = مفردات
Grammar = قواعد

The **scanner** implements this grammar

<br>

### **Token**

Token is a lexeme and some metadata associated with it to help the next steps like

- Type of lexeme: bracket, equal operator,
- Location in code, line & offset
- Value if it's a number or a string literal

<br>

### **Scanner**

Consumes a set of characters & outputs a list of tokens

<br>

### **CFG**

A set of rules that combine the tokens into meaningful expressions

Basically the rules to form sentences of a language.

The rules are usually called productions. Rules are terminal or non-terminal.

Ex:

```
expression     → literal
               | unary
               | binary
               | grouping ;

literal        → NUMBER | STRING | "true" | "false" | "nil" ;
grouping       → "(" expression ")" ;
unary          → ( "-" | "!" ) expression ;
binary         → expression operator expression ;
operator       → "==" | "!=" | "<" | "<=" | ">" | ">="
               | "+"  | "-"  | "*" | "/" ;
```

The CFG is implemented in the **parser**

<br>

### **Parser**

Consumes a set of tokens & outputs a syntax tree of expressions

<br>


### **Precedence**

Which operators are evaluated first in an expression having multiple different operators

ex: multiply before adding

<br>

### **Associativty** 

Order of evaluation in a sequence of the same operator

ex: adding is right-associative & assigning is left-associative

```
5 - 3 - 1 --> (5 - 3) - 1

a = b = c --> a = (b = c)
```

<br>

### **Top Down Parser**

A parser that starts from the top rule in CFG (one with the least precedence) & works it's way to the leaves of the syntax tree & then back.

<br>

### **Statement**

Statements are things that produce a side effect, wether it's user visible (say a print or statement), or not (like changing internal state of the interpreter by modifying/declaring a variable).

An expression evaluates to something, while a statement does something. This is in very broad terms of course.

<br>

### **Environment**

The thing that holds our state during runtime. Basically a data structure that maps all the variable to their values as our code executes. The interpretor uses it.

It's also the place we implement scoping in, more on that later.

<br>

### **l-value vs r-value**

`l-value` evaluates to storage location or target. `r-value` evaluates to a value.

`var x = 3 + 3;`

Here x is an `l-value`, it evaluates to the assignment target, a storage location in memory.

While the RHS `3 + 3` is the `r-value`

This distinction is important for a syntax tree of an assignment expression, we dont want to evaluate the LHS & instead use it as our target.

<br>

### **Scope**

Defines the context of where a name maps to an entity

**Lexical scoping** is when the text of the source code defines the boundaries of those contexts. This is mostly the case for variables in most languages.

```typescript
{
  var a = "first";
  print a; // "first".
}

{
  var a = "second";
  print a; // "second".
}
```

**Dynamic scoping** is when those contexts are defined during runtime.

```typescript
class Saxophone {
  play() {
    print "Careless Whisper";
  }
}

class GolfClub {
  play() {
    print "Fore!";
  }
}

// Depending on what thing is passed
// we get it's play()
function playIt(thing) {
  thing.play();
}
```

Scopes are implemented using environments, the basic idea is that each new scope we go into (usually marked by curly brackets) gets it's own environment.

<br>

### **Shadowing**

When a locally scoped variable has the same name as a variable in the enclosing scope.

```typescript
var global = "text";

{
    var global = "another text";
    // outputs "another text"
    print global;
}

```