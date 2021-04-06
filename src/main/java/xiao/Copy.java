package xiao;

import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.Map.Entry;

import static xiao.Stack.MonoStack;
import static xiao.Stack.valueStack;
import static xiao.Type.*;
import static xiao.Value.*;

/**
 * @author chuxiaofeng
 */
public class Copy {

//    static Object copyValue(Object v) {
//        return copyValue(v, valueStack());
//    }

    public static <T> T copyType(T v) {
        return copyType(v, valueStack());
    }

    static Object copyValue(Object v, MonoStack vs) {
        if (vs.contains(v)) {
            return v;
        }

        if (v instanceof VoidValue)         { return v; }
        else if (v instanceof BoolValue)    { return v; }
        else if (v instanceof IntValue)     { return v; }
        else if (v instanceof FloatValue)   { return v; }
        else if (v instanceof StringValue)  { return v; }
        else if (v instanceof PrimFun)      { return v; }
        else if (v instanceof ModuleValue)  { return v; }
        else if (v instanceof TupleValue) {
            vs.push(v);
            TupleValue tuple = (TupleValue) v;
            List<Value> vals = new ArrayList<>(tuple.size());
            for (Value val : tuple.values) {
                vals.add(((Value) copyValue(val, vs)));
            }
            vs.pop(v);
            return Tuple(vals);
        } else if (v instanceof VectorValue) {
            vs.push(v);
            VectorValue vec = (VectorValue) v;
            List<Value> vals = new ArrayList<>(vec.size());
            for (Value val : vec.values) {
                vals.add(((Value) copyValue(val, vs)));
            }
            vs.pop(v);
            return Vector(vals);
        } else if (v instanceof RecordValue) {
            vs.push(v);
            RecordValue rec = (RecordValue) v;
            Scope props = copyScope(rec.props, vs);
            vs.pop(v);
            return Record(rec.name, rec.type, props);
        } else if (v instanceof Closure) {
            vs.push(v);
            Closure c = (Closure) v;
            Scope props = copyScope(c.props, vs);
            vs.pop(v);
            return Closure(c.fun, props, c.env);
        } else {
            return v;
        }
    }

    @SuppressWarnings("unchecked")
    static <T> T copyType(T v, MonoStack vs) {
        if (vs.contains(v)) {
            return v;
        }

        if (v instanceof BoolType)          { return v; }
        else if (v instanceof IntType)      { return v; }
        else if (v instanceof FloatType)    { return v; }
        else if (v instanceof StringType)   { return v; }
        else if (v instanceof ModuleType)   { return v; }
        else if (v instanceof LiteralType) {
            vs.push(v);
            Value t = LiteralType(copyType(((LiteralType) v).type));
            vs.pop(v);
            return (T) t;
        } else if (v instanceof TupleType) {
            vs.push(v);
            TupleType tuple = (TupleType) v;
            List<Value> types = new ArrayList<>(tuple.size());
            for (Value val : tuple.eltTypes.values) {
                types.add(copyType(val, vs));
            }
            vs.pop(v);
            return (T) TupleType(types);
        } else if (v instanceof VectorType) {
            vs.push(v);
            VectorType vec = (VectorType) v;
            Value eltType = copyType(vec.eltType, vs);
            if (vec.positional != null) {
                List<Value> types = new ArrayList<>(vec.positional.size());
                for (Value v1 : vec.positional.values) {
                    types.add(copyType(v1, vs));
                }
                vs.pop(v);
                return (T) VectorType(eltType, types);
            } else {
                vs.pop(v);
                return (T) VectorType(eltType);
            }
        } else if (v instanceof RecordType) {
            vs.push(v);
            RecordType rec = (RecordType) v;
            Scope props = copyScope(rec.props, vs);
            vs.pop(v);
            return (T) RecordType(rec.name, rec.def, props, rec.env);
        } else if (v instanceof FunType) {
            vs.push(v);
            FunType f = (FunType) v;
            Scope props = copyScope(f.props, vs);
            FunType copy =  FunType(f.def, props, f.env);
            copy.paramsType = copyType(f.paramsType);
            copy.retType = copyType(f.retType);
            vs.pop(v);
            return (T) copy;
        } else if (v instanceof UnionType) {
            vs.push(v);
            UnionType u = (UnionType) v;
            Set<Value> s = UnionType.createValueSet();
            for (Value t : u.types) {
                s.add(copyType(t, vs));
            }
            vs.pop(v);
            return (T) union(s);
        } else {
            return (T) copyValue(v, vs);
        }
    }

    // todo push - pop
    private static Scope copyScope(@NotNull Scope s, MonoStack vs) {
        Scope s1 = new Scope();
        for (Entry<String, Map<String, Object>> it1 : s.table.entrySet()) {
            Map<String, Object> copy = new LinkedHashMap<>();
            Map<String, Object> m = it1.getValue();
            for (Entry<String, Object> it : m.entrySet()) {
                Object val = it.getValue();
                if (val instanceof Value) {
                    copy.put(it.getKey(), copyType(((Value) val), vs));
                } else {
                    copy.put(it.getKey(), val);
                }
            }
            s1.putProps(it1.getKey(), copy);
        }
        return s1;
    }

}
