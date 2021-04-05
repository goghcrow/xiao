package xiao.match;

import org.jetbrains.annotations.NotNull;
import xiao.Evaluator;
import xiao.Scope;
import xiao.Value;

import static xiao.front.Ast.*;
import static xiao.Constant.ID_FALSE;
import static xiao.Constant.ID_TRUE;
import static xiao.Value.VOID;


/**
 * 配合模式匹配的赋值
 * @author chuxiaofeng
 */
class TryPatternDefiner extends PatternDefiner {

    TryPatternDefiner(Evaluator<Scope, Value> evaluator, Scope env) {
        super(evaluator, env, false, true);
    }

    @Override
    public Value match(@NotNull Id id, @NotNull Value value) {
        // 因为 true 和 false 是普通的标识符, 所以特殊处理下
        // 感觉也不合理, 既然做成了标识符本来就允许重新定义 true 和 false，但是特么这样用起来反直觉
        // 应该 hack 一把, 不允许 redefine true、false
        if (Matchers.isWildcards(id)) {
            return VOID;
        } else if (id.name.equals(ID_TRUE)) {
            return VOID;
        } else if (id.name.equals(ID_FALSE)) {
            return VOID;
        } else {
            return super.match(id, value);
        }
    }

    @Override
    public Value match(@NotNull Define ptn, @NotNull Value value) {
        assert ptn.pattern instanceof Id;
        Id name = (Id) ptn.pattern;
        return match(name, value);
    }

    @Override
    public Value match(@NotNull IntNum ptn, @NotNull Value value) {
        return VOID;
    }

    @Override
    public Value match(@NotNull FloatNum ptn, @NotNull Value value) {
        return VOID;
    }

    @Override
    public Value match(@NotNull Str ptn, @NotNull Value value) {
        return VOID;
    }

    @Override
    public Value match(@NotNull Attribute ptn, @NotNull Value value) {
        return VOID;
        // return super.match(ptn, value);
    }

    @Override
    public Value match(@NotNull Subscript ptn, @NotNull Value value) {
        return VOID;
        // return super.match(ptn, value);
    }

    @Override
    public Value missing(@NotNull Node ptn, @NotNull Value value) {
        return VOID;
    }
}
