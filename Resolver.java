import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

class Resolver implements Expr.Visitor<Void>, Stmt.Visitor<Void> {
    private final Interpreter interpreter;
    // keeps track the of scopes currrently used.
    private final Stack<Map<String, Boolean>> scopes = new Stack<>();

    Resolver(Interpreter interpreter) {
        this.interpreter = interpreter;
    }

    // Goes through all the statements and resolves them
    void resolve(List<Stmt> statements) {
        for (Stmt statement : statements) {
            resolve(statement);
        }
    }

    // Starts a new scope, goes through all the statements, then gets rid of scope
    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        beginScope();
        resolve(stmt.statements);
        endScope();
        return null;
    }

    // binds the function names and starts a new scope. Defines the function name before resolving the body to allow recursion. 
    @Override
    public Void visitFunctionStmt(Stmt.Function stmt){
        declare(stmt.name);
        define(stmt.name);
        resolveFunction(stmt);
        return null;
    }

    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
        declare(stmt.name);
        if (stmt.initializer != null) {
            resolve(stmt.initializer);
        }
        define(stmt.name);
        return null;
    }

    //resolve to check for references to other variables
    @Override
    public Void visitAssignExpr(Expr.Assign expr){
        resolve(expr.value);
        resolveLocal(expr, expr.name);
        return null;
    }

    @Override
    public Void visitVariableExpr(Expr.Variable expr) {
        if (!scopes.isEmpty() && scopes.peek().get(expr.name.lexeme) == Boolean.FALSE) {
            Lox.error(expr.name, "Can't read local variable in it's own initializer.");
        }
        resolveLocal(expr, expr.name);
        return null;
    }

    // does the actual resolving for statements
    private void resolve(Stmt stmt) {
        stmt.accept(this);
    }

    // does the actual resolving for expressions
    private void resolve(Expr expr) {
        expr.accept(this);
    }

    //creates a new scope for the body of the function and binds the params. 
    private void resolveFunction(Stmt.Function function){
        beginScope();
        for(Token param: function.params){
            declare(param);
            define(param);
        }
        resolve(function.body);
        endScope();
    }

    private void beginScope() {
        scopes.push(new HashMap<String, Boolean>());
    }

    private void endScope() {
        scopes.pop();
    }

    // First step of binding. Declaring the variable in the innermost scope. 'false'
    // indicates that it's not ready yet.
    private void declare(Token name) {
        if (scopes.isEmpty())
            return;

        Map<String, Boolean> scope = scopes.peek();
        scope.put(name.lexeme, false);
    }

    // Second step of binding. Marks the variable as ready.
    private void define(Token name) {
        if (scopes.isEmpty())
            return;
        scopes.peek().put(name.lexeme, true);
    }

    private void resolveLocal(Expr expr, Token name) {
        for (int i = scopes.size() - 1; i >= 0; i--) {
            if (scopes.get(i).containsKey(name.lexeme)) {
                interpreter.resolve(expr, scopes.size() - 1 - i);
                return;
            }
        }
    }
}