package xiao.match;

import org.jetbrains.annotations.NotNull;
import xiao.*;
import xiao.misc.Error;
import xiao.misc.Location;

import static xiao.Constant.KEY_MUT;
import static xiao.Type.*;
import static xiao.Value.FALSE;
import static xiao.front.Ast.*;

/**
 * @author chuxiaofeng
 */
class PatternAssignChecker extends PatternChecker {

    final Evaluator<Scope, Value> evaluator;
    final Scope env;
    final boolean skipWildcards;

    PatternAssignChecker(Evaluator<Scope, Value> evaluator, Scope env, boolean skipWildcards) {
        this.evaluator = evaluator;
        this.env = env;
        this.skipWildcards = skipWildcards;
    }

    @Override
    public Value match(@NotNull Id ptn, @NotNull Value value) {
        if (Voids.contains(value)) {
            throw Error.type(ptn.loc, "不能 assign Void");
        }

        if (skipWildcards && Matchers.isWildcards(ptn)) {
            return Type.VOID;
        }

        String name = ptn.name;
        Location loc = ptn.loc;
        Scope definedScope = env.findDefinedScope(name);
        if (definedScope == null) {
            throw Error.type(loc, "变量 " + name + " 未定义");
        }

        doAssign(loc, definedScope, name, value);
        return Type.VOID;
    }

    @Override
    public Value match(@NotNull Subscript ptn, @NotNull Value value) {
        if (Voids.contains(value)) {
            throw Error.type(ptn.loc, "不能 assign Void");
        }

        Value v = evaluator.eval(ptn.value, env);
        if (v instanceof UnionType) {
            for (Value t : ((UnionType) v).types) {
                match(t, ptn, value);
            }
            return Type.VOID;
        } else {
            return match(v, ptn, value);
        }
    }

    Value match(Value v, @NotNull Subscript ptn, @NotNull Value value) {
        if (v instanceof VectorType) {
            VectorType vec = ((VectorType) v);
            Value idx = evaluator.eval(ptn.index, env);

            //noinspection UnnecessaryLocalVariable
            Value actual = value;
            Value expected = vec.eltType;

            Location loc = ptn.index.loc;
            if (!isInt(idx)) {
                throw Error.type(loc, "访问 vector 下标必须为数字, 实际是 " + idx);
            }
            if (!subtype(actual, expected)) {
                throw Error.type(loc, "vector 期望元素类型 " + expected + ", 实际是 " + actual);
            }

            if (isLiteral(idx)) {
                int idxInt = Math.toIntExact(litInt(idx));
                vec.set(loc, idxInt, TypeChecker.select(ptn.loc, expected, actual));
            } else {
                vec.set(loc, idx, TypeChecker.select(ptn.loc, expected, actual));
            }
            return Type.VOID;
        } else {
            throw Error.type(ptn.loc, "下标只能赋值 vector, 实际是 " + v);
        }
    }


    @Override
    public Value match(@NotNull Attribute ptn, @NotNull Value value) {
        if (Voids.contains(value)) {
            throw Error.type(ptn.loc, "不能 assign Void");
        }

        Node recordExpr = ptn.value;
        Value eval = evaluator.eval(recordExpr, env);

        if (eval instanceof UnionType) {
            for (Value t : ((UnionType) eval).types) {
                match(t, ptn, value);
            }
            return Type.VOID;
        } else {
            return match(eval, ptn, value);
        }
    }

    Value match(Value eval, @NotNull Attribute ptn, @NotNull Value value) {
        Node recordExpr = ptn.value;
        if (!(eval instanceof RecordType)) {
            throw Error.type(recordExpr.loc, "只有 record 允许属性访问, 实际是 " + eval);
        } else {
            RecordType rec = ((RecordType) eval);
            Value v = rec.props.lookupLocal(ptn.attr.name);
            if (v == null) {
                throw Error.type(ptn.loc, "record 未定义属性" + ptn.attr.name);
            } else {
                doAssign(ptn.loc, rec.props, ptn.attr.name, value);
            }
        }
        return Type.VOID;
    }

    private void doAssign(Location loc, Scope s, String name, Value actual) {
        Object mut = s.lookupLocalProp(name, KEY_MUT);

        boolean immutable = mut == null || TypeChecker.boolAssert(loc, mut) == FALSE;
        if (immutable) {
            throw Error.type(loc, name + " 不允许修改");
        } else {
            // 检查类型
            Value expected = s.lookupLocal(name);
            if (expected == null) {
                throw Error.bug(loc, name + " 类型未知");
            }
            if (!subtype(actual, expected)) {
                throw Error.type(loc, "期望赋值类型 " + expected + ", 实际是 " + actual);
            }

            // 必须用期望类型
            // let mut a: Float = 3.14
            // a = 1
            // a = 3.14
            Value v = TypeChecker.select(loc, true, expected, actual);
            if (expected != v) {
                s.putValue(name, v);
            }
        }
    }
}
