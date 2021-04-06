package xiao.match;

import org.jetbrains.annotations.NotNull;
import xiao.Evaluator;
import xiao.Scope;
import xiao.Value;
import xiao.misc.Error;

import static xiao.Constant.KEY_MUT;
import static xiao.Value.*;
import static xiao.front.Ast.*;

/**
 * @author chuxiaofeng
 */
class PatternDefiner extends PatternBinder {
    final Evaluator<Scope, Value> evaluator;
    final Scope env;
    final boolean mutable;
    final boolean skipWildcards;

    PatternDefiner(Evaluator<Scope, Value> evaluator, Scope env, boolean mutable, boolean skipWildcards) {
        this.evaluator = evaluator;
        this.env = env;
        this.mutable = mutable;
        this.skipWildcards = skipWildcards;
    }

    @Override
    public Value match(@NotNull Id ptn, @NotNull Value value) {
        if (skipWildcards && Matchers.isWildcards(ptn)) {
            return VOID;
        }

        env.putValue(ptn.name, value);
        env.put(ptn.name, KEY_MUT, mutable ? TRUE : FALSE);
        return VOID;
    }

    // 如果遇到绑定过程的 subscript 或者 attribute, 处理成 assign
    // 感觉不合理, 混合了 define + set, 取消支持
    @Override
    public Value match(@NotNull Subscript ptn, @NotNull Value value) {
        throw Error.type(ptn.loc, "变量声明不支持 subscript");
//        interp.subscriptSet(ptn, value, env);
//        return VOID;
    }

    @Override
    public Value match(@NotNull Attribute ptn, @NotNull Value value) {
        throw Error.type(ptn.loc, "变量声明不支持 attribute");
//        interp.attrSet(ptn, value, env);
//        return VOID;
    }
}
