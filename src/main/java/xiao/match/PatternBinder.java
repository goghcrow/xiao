package xiao.match;

import org.jetbrains.annotations.NotNull;
import xiao.Value;
import xiao.misc.Error;
import xiao.misc.Location;

import static xiao.front.Ast.Str;
import static xiao.front.Ast.*;
import static xiao.Value.*;

/**
 * 解构赋值
 * @author chuxiaofeng
 */
abstract class PatternBinder implements Matcher<Value, Value> {

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
        Location loc = ptn.loc;
        TupleValue tuple = ((TupleValue) value);
        for (int i = 0; i < ptn.elements.size(); i++) {
            match(ptn.elements.get(i), tuple.get(loc, i));
        }
        return VOID;
    }

    @Override
    public Value match(@NotNull VectorLiteral ptn, @NotNull Value value) {
        Location loc = ptn.loc;
        VectorValue vec = ((VectorValue) value);
        for (int i = 0; i < ptn.elements.size(); i++) {
            match(ptn.elements.get(i), vec.get(loc, i));
        }
        return VOID;
    }

    @Override
    public Value match(@NotNull RecordLiteral ptn, @NotNull Value value) {
        RecordValue rec = (RecordValue) value;
        for (String k : ptn.map.keySet()) {
            // 过滤掉通配符
            if (Matchers.isWildcards(k)) {
                continue;
            }
            Value rv = rec.props.lookupLocal(k);
            assert rv != null;
            match(ptn.node(k), rv);
        }
        return VOID;
    }
}
