package xiao.match;

import org.jetbrains.annotations.NotNull;
import xiao.Evaluator;
import xiao.Scope;
import xiao.Value;

import static xiao.Value.*;
import static xiao.front.Ast.*;

/**
 * @author chuxiaofeng
 */
class PatternAssigner extends PatternBinder {

    final Evaluator<Scope, Value> evaluator;
    final Scope env;
    final boolean skipWildcards;

    PatternAssigner(Evaluator<Scope, Value> evaluator, Scope env, boolean skipWildcards) {
        this.evaluator = evaluator;
        this.env = env;
        this.skipWildcards = skipWildcards;
    }

    @Override
    public Value match(@NotNull Id ptn, @NotNull Value value) {
        if (skipWildcards && Matchers.isWildcards(ptn)) {
            return VOID;
        }
        Scope definedScope = env.findDefinedScope(ptn.name);
        assert definedScope != null;
        definedScope.putValue(ptn.name, value);
        return VOID;
    }

    @Override
    public Value match(@NotNull Subscript ptn, @NotNull Value value) {
        Value v = evaluator.eval(ptn.value, env);
        VectorValue vec = ((VectorValue) v);
        Value i = evaluator.eval(ptn.index, env);
        int idx = ((IntValue) i).intValue();
        vec.set(ptn.loc, idx, value);
        return VOID;
    }

    @Override
    public Value match(@NotNull Attribute ptn, @NotNull Value value) {
        Node recordExpr = ptn.value;
        RecordValue rec = ((RecordValue) evaluator.eval(recordExpr, env));
        rec.props.putValue(ptn.attr.name, value);
        return VOID;
    }
}
