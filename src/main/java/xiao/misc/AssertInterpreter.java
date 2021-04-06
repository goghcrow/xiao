package xiao.misc;

import org.jetbrains.annotations.NotNull;
import xiao.Interpreter;
import xiao.Scope;
import xiao.TypeChecker;
import xiao.Value;

import static xiao.Value.*;
import static xiao.front.Ast.Str;
import static xiao.front.Ast.*;
import static xiao.misc.AssertRender.render;
import static xiao.misc.Helper.log;

/**
 * @author chuxiaofeng
 */
public class AssertInterpreter {

    @NotNull final Interpreter interp;

    AssertValueRecord rec = new AssertValueRecord();
    Assert assertNode = null;

    public AssertInterpreter(@NotNull Interpreter interp) {
        this.interp = interp;
    }

    public Value interp(@NotNull Node node, @NotNull Scope s) {
        if (assertNode == null) {
            return interp.visit(node, s);
        } else {
            return interpInAssert(node, s);
        }
    }

    public Value interp(@NotNull Assert assert1, @NotNull Scope s) {
        try {
            assertNode = assert1;
            return assert1(assert1, s);
        } finally {
            assertNode = null;
            rec.clear();
        }
    }

    @SuppressWarnings("unchecked")
    final Class<? extends Node>[] assertBlacklist = new Class[]{
            IntNum.class, FloatNum.class, Str.class,
            TupleLiteral.class,
            RecordLiteral.class,
            FunDef.class,
    };

    boolean assertShow(Node node) {
        for (Class<? extends Node> c : assertBlacklist) {
            if (c.isInstance(node)) {
                return false;
            }
        }
        return true;
    }

    Value interpInAssert(@NotNull Node node, @NotNull Scope s) {
        if (assertShow(node) && assertNode.expr.loc.contains(node.loc)) {
            int col = node.oploc.colBegin - assertNode.expr.loc.colBegin + 1;
            Value v = interp.visit(node, s);
            return rec.rec(v, col);
        } else {
            return interp.visit(node, s);
        }
    }

    Value assert1(Assert a, Scope s) {
        Node expr = a.expr;
        String src = expr.loc.codeSpan();
        Value test = interpInAssert(expr, s);

        TypeChecker.boolAssert(a.loc, test);
        if (test == TRUE) {
            log("\n" + render(expr.loc, src, rec));
            return VOID;
        } else {
            String msg;
            if (a.msg == null) {
                msg = "断言跪了";
            } else {
                msg = ((StringValue) interp(a.msg, s)).value;
            }
            // backtrace();
            // todo throw assert error
            throw Error.runtime(a.loc, msg + "\n" + render(expr.loc, src, rec));
        }
    }
}
