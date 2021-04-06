package xiao;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

import static xiao.Constant.ID_RETURN;
import static xiao.Constant.KEY_MUT;
import static xiao.Stack.MonoStack;
import static xiao.Stack.valueStack;
import static xiao.Type.*;
import static xiao.Value.*;

/**
 * @author chuxiaofeng
 */
class ToString {

    public static String stringfy(@NotNull Value v) {
        return stringfy(v, valueStack());
    }

    static String stringfy(@NotNull Value v, MonoStack st) {
        return stringfy(v, st, false);
    }

    // 这里没有处理所有的类型, 只处理可能造成环的, 避免 stackoverflow, 其他默认调用原来的 toString 方法
    static String stringfy(@NotNull Value v, MonoStack st, boolean literal) {
        if (st.contains(v)) {
            return "*recursive*";
        }

        if (v instanceof TupleValue) {
            st.push(v);
            List<Value> values = ((TupleValue) v).values;
            if (values.size() == 1) {
                Value fst = values.get(0);
                String s = "(" + stringfy(fst, st) + ",)";
                st.pop(v);
                return s;
            } else {
                String s = join(values, "(", ", ", ")", st);
                st.pop(v);
                return s;
            }
        } else if (v instanceof VectorValue) {
            st.push(v);
            String s = join(((VectorValue) v).values, "[", ", ", "]", st);
            st.pop(v);
            return s;
        } else if (v instanceof RecordValue) {
            st.push(v);
            RecordValue rv = (RecordValue) v;
            StringBuilder buf = new StringBuilder();
            buf.append(rv.name == null ? "record" : rv.name).append("( ");
            rv.props.table.forEach((k, v1) -> {
                Value val = rv.props.lookupLocal(k);
                assert val != null;
                buf.append(k).append(" = ").append(stringfy(val, st)).append(", ");
            });
            st.pop(v);
            return buf.append(")").toString();
        } else if (v instanceof TupleType) {
            // todo test
            if (literal) {
                // tupleValue
                st.push(v);
                Object lit = Literal.literal(v);
                assert lit != null;
                String s = stringfy(((TupleValue) lit), st);
                st.pop(v);
                return s;
            } else {
                st.push(v);
                List<Value> values = ((TupleType) v).eltTypes.values;
                if (values.size() == 1) {
                    Value fst = values.get(0);
                    String s = "(" + stringfy(fst, st) + ",)";
                    st.pop(v);
                    return s;
                } else {
                    String s = join(values, "(", ", ", ")", st);
                    st.pop(v);
                    return s;
                }
            }
        } else if (v instanceof VectorType) {
            st.push(v);
            String s = stringfy(((VectorType) v).eltType, st) + "[]";
            st.pop(v);
            return s;
        } else if (v instanceof RecordType) {
            st.push(v);
            RecordType rt = (RecordType) v;
            StringBuilder buf = new StringBuilder();
            buf.append((rt.name == null ? "" : rt.name)).append("[");
            boolean first = true;
            for (String k : rt.props.keySet()) {
                Value t = rt.props.lookup(k);
                if (t == null) {
                    t = rt.props.lookupType(k);
                }
                Object mut = rt.props.lookupLocalProp(k, KEY_MUT);
                if (t == null) {
                    // 解释器
                    if (first) {
                        buf.append(TRUE == mut ? "mut " : "").append(k).append(":").append("???");
                        first = false;
                    } else {
                        buf.append(", ").append(TRUE == mut ? "mut " : "").append(k).append(":").append("???");
                    }
                } else {
                    if (first) {
                        buf.append(TRUE == mut ? "mut " : "").append(k).append(":").append(stringfy(t, st));
                        first = false;
                    } else {
                        buf.append(", ").append(TRUE == mut ? "mut " : "").append(k).append(":").append(stringfy(t, st));
                    }
                }
            }
            if (first) {
                buf.append(":");
            }
            buf.append("]");
            st.pop(v);
            return buf.toString();
        } else if (v instanceof FunType) {
            FunType ft = (FunType) v;
            TupleType paramsType = ft.paramsType;
            Value retType = ft.retType;
            if (paramsType == null || retType == null) {
                return ft.def.toString();
            } else {
                st.push(v);
                String s = stringfy(paramsType, st) + " " + ID_RETURN + " " + stringfy(retType, st);
                st.pop(v);
                return s;
            }
        } else if (v instanceof LiteralType) {
            st.push(v);
            // String s = "☾" + stringfy(((LiteralType) v).type, st, true) + "☽";
            String s = stringfy(((LiteralType) v).type, st, true);
            st.pop(v);
            return s;
        } else if (v instanceof UnionType) {
            st.push(v);
            String s = join(((UnionType) v).types, "Union(", " | ", ")", st);
            st.pop(v);
            return s;
        } else {
            return v.toString();
        }
    }

    static String join(Collection<Value> list, String prefix, String sep, String postfix, MonoStack vs) {
        StringBuilder buf = new StringBuilder(prefix);
        boolean isFirst = true;
        for (Value it : list) {
            if (isFirst) {
                buf.append(stringfy(it, vs));
                isFirst = false;
            } else {
                buf.append(sep).append(stringfy(it, vs));
            }
        }
        return buf.append(postfix).toString();
    }
}
