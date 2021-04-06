package xiao.match;

import org.jetbrains.annotations.NotNull;
import xiao.Value;
import xiao.Voids;
import xiao.misc.Error;
import xiao.misc.Location;

import static xiao.Type.*;
import static xiao.Value.VOID;
import static xiao.front.Ast.*;

/**
 * PatternBinder 的 typecheck
 * @author chuxiaofeng
 */
abstract class PatternChecker implements Matcher<Value, Value> {

    boolean strict = false; // 绑定时候还是非严格模式方便一些

    @Override
    public Value match(@NotNull IntNum ptn, @NotNull Value value) {
        return missing(ptn, value);
    }

    @Override
    public Value match(@NotNull FloatNum ptn, @NotNull Value value) {
        return missing(ptn, value);
    }

    @Override
    public Value match(@NotNull Str ptn, @NotNull Value value) {
        return missing(ptn, value);
    }

    @Override
    public Value missing(@NotNull Node ptn, @NotNull Value value) throws Error {
        throw Error.type(ptn.loc, "不支持的 pattern 绑定: " + ptn);
    }

    @Override
    public Value match(@NotNull TupleLiteral ptn, @NotNull Value value) {
        if (Voids.contains(value)) {
            throw Error.type(ptn.loc, "不能 bind Void");
        }

        Location loc = ptn.loc;
        if (value instanceof TupleType) {
            TupleType tuple = (TupleType) value;

            // todo tuple 应该严格匹配数量... !!!
            if (strict) {
                if (ptn.elements.size()  != tuple.size()) {
                    throw Error.type(loc, "绑定 tuple 数量不匹配: pattern 数量 "
                            + ptn.elements.size() + ", value 数量 " + tuple.size());
                }
            }

            for (int i = 0; i < ptn.elements.size(); i++) {
                // 非严格模式 会发生下标越界 但是支持少于数量的匹配
                Node ptn1 = ptn.elements.get(i);
                match(ptn1, tuple.get(ptn1.loc, i));
            }
        } else {
            throw Error.type(loc, "绑定类型不匹配: 期望 tuple, 实际是 " + value);
        }
        return VOID;
    }

    @Override
    public Value match(@NotNull VectorLiteral ptn, @NotNull Value value) {
        if (Voids.contains(value)) {
            throw Error.type(ptn.loc, "不能 bind Void");
        }

        Location loc = ptn.loc;
        if (value instanceof VectorType) {
            VectorType vec = (VectorType) value;

            if (strict) {
                if (ptn.elements.size()  != vec.size()) {
                    throw Error.type(loc, "绑定 vector 数量不匹配: pattern 数量 "
                            + ptn.elements.size() + ", value 数量 " + vec.size());
                }
            }

            for (int i = 0; i < ptn.elements.size(); i++) {
                // 非严格模式 会发生下标越界 但是支持少于数量的匹配
                Node ptn1 = ptn.elements.get(i);
                match(ptn1, vec.get(ptn1.loc, i));
            }
        } else {
            throw Error.type(loc, "绑定类型不匹配: 期望 vector, 实际是 " + value);
        }
        return VOID;
    }

    @Override
    public Value match(@NotNull RecordLiteral ptn, @NotNull Value value) {
        if (Voids.contains(value)) {
            throw Error.type(ptn.loc, "不能 bind Void");
        }

        Location loc = ptn.loc;
        if (!(value instanceof RecordType)) {
            throw Error.type(loc, "绑定 record 类型不匹配: record 与 " + value);
        }
        RecordType rec = (RecordType) value;

        // 严格模式要求两边属性完全一致, 非严格模式只需要匹配 pattern 中的 key
        if (strict) {
            if (ptn.map.keySet().size() != rec.props.keySet().size()) {
                throw Error.type(loc, "绑定 record 属性数量不匹配: pattern 属性 "
                        + ptn.map.keySet() + ", values 属性 " + rec.props.keySet());
            }
        }
        for (String k : ptn.map.keySet()) {
            // 过滤掉通配符
            if (Matchers.isWildcards(k)) {
                continue;
            }
            Value rv = rec.props.lookupLocal(k);
            if (rv == null) {
                // strict 模式下不会进入这个分支
                throw Error.type(loc, "绑定 record 属性不匹配, 缺失 " + k + " : pattern 属性 "
                        + ptn.map.keySet() + ", values 属性 " + rec.props.keySet());
            } else {
                Node v = ptn.node(k);
                match(v, rv);
            }
        }
        return VOID;
    }
}
