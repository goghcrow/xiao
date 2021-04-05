package xiao.match;

import org.jetbrains.annotations.NotNull;
import xiao.Evaluator;
import xiao.Scope;
import xiao.Value;
import xiao.misc.Error;

import static xiao.front.Ast.Str;
import static xiao.front.Ast.*;
import static xiao.Constant.ID_FALSE;
import static xiao.Constant.ID_TRUE;
import static xiao.Type.*;
import static xiao.Value.*;

/**
 * 模式匹配
 * @author chuxiaofeng
 */
class PatternMatcher implements Matcher<Value, Boolean>, Evaluator<Value, Boolean> {

    final Evaluator<Scope, Value> evaluator;
    final Scope env;

    PatternMatcher(@NotNull Evaluator<Scope, Value> evaluator, @NotNull Scope env) {
        this.evaluator = evaluator;
        this.env = env;
    }

    @Override
    public Boolean eval(@NotNull Node n, @NotNull Value val) {
        return match(n, val);
    }

    @Override
    public Boolean match(@NotNull IntNum ptn, @NotNull Value v) {
        return evaluator.eval(ptn, env).equals(v);
    }

    @Override
    public Boolean match(@NotNull FloatNum ptn, @NotNull Value v) {
        return evaluator.eval(ptn, env).equals(v);
    }

    @Override
    public Boolean match(@NotNull Str ptn, @NotNull Value v) {
        return evaluator.eval(ptn, env).equals(v);
    }

    @Override
    public Boolean match(@NotNull Id id, @NotNull Value v) {
        // 因为 true 和 false 是普通的标识符, 所以特殊处理下
        // 感觉也不合理, 既然做成了标识符本来就允许重新定义 true 和 false，但是特么这样用起来反直觉
        // 应该 hack 一把, 不允许 redefine true、false
        if (id.name.equals(ID_TRUE)) {
            return v == TRUE;
        } else if (id.name.equals(ID_FALSE)) {
            return v == FALSE;
        } else {
            // 这个分支是处理变量绑定的, 默认匹配返回 true
            return true;
        }
    }

    @Override
    public Boolean match(@NotNull Define ptn, @NotNull Value value) {
        assert ptn.type != null;
        Value type = evaluator.eval(ptn.type, env);
        return subtype(typeof(value), typeof(type));
    }

    @Override
    public Boolean match(@NotNull TupleLiteral ptn, @NotNull Value value) {
        if (value instanceof TupleValue) {
            TupleValue v = ((TupleValue) value);
            if (v.size() == ptn.elements.size()) {
                for (int i = 0; i < ptn.elements.size(); i++) {
                    if (!match(ptn.elements.get(i), v.get(ptn.loc, i))) {
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
    public Boolean match(@NotNull VectorLiteral ptn, @NotNull Value value) {
        if (value instanceof VectorValue) {
            VectorValue v = ((VectorValue) value);
            if (v.size() == ptn.elements.size()) {
                for (int i = 0; i < ptn.elements.size(); i++) {
                    if (!match(ptn.elements.get(i), v.get(ptn.loc, i))) {
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
    public Boolean match(@NotNull RecordLiteral ptn, @NotNull Value value) {
        if (value instanceof RecordValue) {
            RecordValue rec = (RecordValue) value;

            // 这个分支其实只要 typecheck 阶段检查就好了, runtime 不需要检查
            // 解构赋值可以不严格匹配属性个数, 模式匹配严格匹配个数
            if (ptn.map.keySet().size() != rec.props.keySet().size()) {
                return false;
            }

            for (String k : ptn.map.keySet()) {
                // 处理通配符
                if (Matchers.isWildcards(k)) {
                    continue;
                }
                Value rv = rec.props.lookupLocal(k);
                if (rv == null) {
                    return false;
                }
                Node v = ptn.node(k);
                if (!match(v, rv)) {
                    return false;
                }
            }
            return true;
        } else {
            return false;
        }
    }

    // 这里要做个决策
    // 解释执行：subscript attribute 需要解释执行满足相等
    // 绑定：还是 无条件返回 true，相当于通配符，之后执行绑定
    //      如果需要改成绑定, 需要在 TryPatternDefiner 中把 match attr 与 subs 中return VOID 干掉

    // 决定执行表达式, 这里相等于 define, 不处理 subs 与 attr 直接当表达式执行

    @Override
    public Boolean match(@NotNull Subscript ptn, @NotNull Value v) {
//        return true;
        return evaluator.eval(ptn, env).equals(v);
    }

    @Override
    public Boolean match(@NotNull Attribute ptn, @NotNull Value v) {
//        return true;
        return evaluator.eval(ptn, env).equals(v);
    }

    // 涉及到变量绑定: id, recordLiteral, vectorLiteral
    // 其他所有执行直接执行判断相等

    @Override
    public Boolean missing(@NotNull Node ptn, @NotNull Value v) throws Error {
        return evaluator.eval(ptn, env).equals(v);
    }
}
