package xiao;

import org.jetbrains.annotations.NotNull;
import xiao.misc.Error;
import xiao.misc.Location;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static xiao.Stack.MonoStack;
import static xiao.Stack.valueStack;
import static xiao.Type.*;
import static xiao.Value.*;

/**
 * @author chuxiaofeng
 */
class LetRecFixer {

    static <T> @NotNull T fixValue(Location loc, Object v) {
        return new LetRecFixer(loc, false).doFix(v);
    }

    static <T> @NotNull T fixType(Location loc, Object v) {
        return new LetRecFixer(loc, true).doFix(v);
    }

    MonoStack stack = valueStack();

    final Location loc;
    final boolean fixType;

    private LetRecFixer(Location loc, boolean fixType) {
        this.loc = loc;
        this.fixType = fixType;
    }

    <T> @NotNull T doFix(Object v) {
        if (fixType) {
            return fixType1(v);
        } else {
            return fixValue1(v);
        }
    }

    @SuppressWarnings("unchecked")
    <T> @NotNull T fixValue1(Object v) {
        if (stack.contains(v)) {
            return (T) v;
        }

        if (v instanceof IntValue) { return (T) v; }
        else if (v instanceof FloatValue) { return (T) v; }
        else if (v instanceof StringValue) { return (T) v; }
        else if (v instanceof ModuleValue) { return (T) v;
        } else if (v instanceof TupleValue) {
            stack.push(v);
            TupleValue tv = (TupleValue) v;
            fixList(tv.values);
            stack.pop(v);
            return (T) tv;
        } else if (v instanceof VectorValue) {
            stack.push(v);
            VectorValue vv = (VectorValue) v;
            fixList(vv.values);
            stack.pop(v);
            return (T) v;
        } else if (v instanceof RecordValue) {
            stack.push(v);
            fixProps(((RecordValue) v).props);
            stack.pop(v);
            return (T) v;
        } else if (v instanceof Closure) {
            stack.push(v);
            fixProps(((Closure) v).props);
            stack.pop(v);
            return (T) v;
        } else if (v instanceof Undefined) {
            stack.push(v);
            T t = (T) ((Undefined) v).lazy.get();
            stack.pop(v);
            return t;
        } else {
            return fixType1(v);
        }
    }

    @SuppressWarnings("unchecked")
    <T> @NotNull T fixType1(Object v) {
        if (stack.contains(v)) {
            return (T) v;
        }

        if (v == TRUE || v == FALSE) { return (T) v; }
        else if (v instanceof AnyType) { return (T) v; }
        else if (v instanceof NeverType) { return (T) v; }
        else if (v instanceof BoolType) { return (T) v; }
        else if (v instanceof IntType) { return (T) v; }
        else if (v instanceof FloatType) { return (T) v; }
        else if (v instanceof StringType) { return (T) v; }
        else if (v instanceof ModuleType) { return (T) v; }
        else if (v instanceof LiteralType) {
            stack.push(v);
            Value lt = LiteralType(doFix(((LiteralType) v).type));
            stack.pop(v);
            return (T) lt;
        } else if (v instanceof TupleType) {
            stack.push(v);
            TupleType tt = (TupleType) v;
            fixList(tt.eltTypes.values);
            stack.pop(v);
            return (T) tt;
        } else if (v instanceof VectorType) {
            stack.push(v);
            VectorType vt = (VectorType) v;
            if (vt.positional != null) {
                fixList(vt.positional.values);
            }
            stack.pop(v);
            return (T) v;
        } else if (v instanceof RecordType) {
            stack.push(v);
            fixProps(((RecordType) v).props);
            stack.pop(v);
            return (T) v;
        } else if (v instanceof FunType) {
            stack.push(v);
            ((FunType) v).retType = doFix(((FunType) v).retType);
            ((FunType) v).paramsType = doFix(((FunType) v).paramsType);
            fixProps(((FunType) v).props);
            stack.pop(v);
            return (T) v;
        } else if (v instanceof UnionType) {
            stack.push(v);
            UnionType ut = (UnionType) v;
            Set<Value> vals = UnionType.createValueSet();
            for (Value t : ut.types) {
                vals.add(doFix(t));
            }
            ut.types.clear();
            ut.types.addAll(vals);
            stack.pop(v);
            return (T) ut;
        } else if (v instanceof Undefined) {
            stack.push(v);
            T t = (T) ((Undefined) v).lazy.get();
            stack.pop(v);
            return t;
        } else {
            throw Error.bug("不支持的 letrec type: " + v);
        }
    }

    void fixList(List<Value> vals) {
        for (int i = 0; i < vals.size(); i++) {
            Value val = vals.get(i);
            vals.set(i, doFix(val));
        }
    }

    void fixProps(Scope props) {
        for (String k : props.keySet()) {
            Map<String, Object> m = props.lookupAllProps(k);
            assert m != null;
            for (Map.Entry<String, Object> it : m.entrySet()) {
                Object val = it.getValue();
                m.put(it.getKey(), doFix(val));
            }
        }
    }
}
