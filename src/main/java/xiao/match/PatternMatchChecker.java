package xiao.match;

import org.jetbrains.annotations.NotNull;
import xiao.Evaluator;
import xiao.Scope;
import xiao.Value;

import static xiao.Constant.ID_FALSE;
import static xiao.Constant.ID_TRUE;
import static xiao.Type.*;
import static xiao.front.Ast.*;

/**
 * 模式匹配
 * @author chuxiaofeng
 */
class PatternMatchChecker extends PatternMatcher {

    PatternMatchChecker(@NotNull Evaluator<Scope, Value> evaluator, @NotNull Scope env) {
        super(evaluator, env);
    }

    @Override
    public Boolean match(@NotNull Define ptn, @NotNull Value value) {
        assert ptn.type != null;
        Value type = evaluator.eval(ptn.type, env);
        // 这里的语义是可能是对的, 这里是不是可以用 castSafe, 因为解释器使用的是 subtype, 处理协变关系的
        return typeMatched(value, type);
    }

    @Override
    public Boolean match(@NotNull Id id, @NotNull Value t) {
        // 因为 true 和 false 是普通的标识符, 所以特殊处理下
        // 感觉也不合理, 既然做成了标识符本来就允许重新定义 true 和 false，但是特么这样用起来反直觉
        // 应该 hack 一把, 不允许 redefine true、false
        if (id.name.equals(ID_TRUE)) {
            if (isBool(t)) {
                if (isLiteral(t)) {
                    Boolean lit = ((BoolType) t).literal;
                    assert lit != null;
                    return lit;
                } else {
                    return true;
                }
            } else {
                return false;
            }
        } else if (id.name.equals(ID_FALSE)) {
            if (isBool(t)) {
                if (isLiteral(t)) {
                    Boolean lit = ((BoolType) t).literal;
                    assert lit != null;
                    return !lit;
                } else {
                    return true;
                }
            } else {
                return false;
            }
        } else {
            // 这个分支是处理变量绑定的, 默认匹配返回 true
            return true;
        }
    }

    @Override
    public Boolean match(@NotNull TupleLiteral ptn, @NotNull Value t) {
        if (t instanceof TupleType) {
            TupleType v = ((TupleType) t);
            if (v.size() == ptn.elements.size()) {
                for (int i = 0; i < ptn.elements.size(); i++) {
                    Node ptn1 = ptn.elements.get(i);
                    if (!match(ptn1, v.get(ptn1.loc, i))) {
                        return false;
                    }
                }
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    @Override
    public Boolean match(@NotNull VectorLiteral ptn, @NotNull Value t) {
        if (t instanceof VectorType) {
            VectorType v = ((VectorType) t);
            if (v.size() == ptn.elements.size()) {
                for (int i = 0; i < ptn.elements.size(); i++) {
                    Node ptn1 = ptn.elements.get(i);
                    if (!match(ptn1, v.get(ptn1.loc, i))) {
                        return false;
                    }
                }
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }
}
