package xiao;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static xiao.Stack.MonoStack;
import static xiao.Type.*;

/**
 * @author chuxiaofeng
 */
public class Literal {

    public static <T> @Nullable T literal(@NotNull Value v) {
        //noinspection unchecked
        return (T) literal(v, Stack.valueStack());
    }

    // 不改变原来类型
    public static Value clear(@NotNull Value v) {
        // 理论上清理需要复制, 只处理基础类型不复制了
        // Value v1 = Copy.copyType(v);
        return clear(v, Stack.valueStack());
    }

    // 值类型才有literal: bool,int,float,string,tuple
    static @Nullable Object literal(Value v, MonoStack st) {
        if (st.contains(v)) {
            return v;
        }
        if (v instanceof BoolType)        { return ((BoolType) v).literal;     }
        else if (v instanceof IntType)    { return ((IntType) v).literal;      }
        else if (v instanceof FloatType)  { return ((FloatType) v).literal;    }
        else if (v instanceof StringType) { return ((StringType) v).literal;   }
        else if (v instanceof LiteralType){
            st.push(v);
            Object l = literal(((LiteralType) v).type, st);
            st.pop(v);
            return l;
        }
        // todo uniontype
//        else if (v instanceof UnionType) {
            // todo ...
            // ???
//            throw Error.bug("....");
//        }


        // 加入 tupleType 的 字面量原因
        // tuple 是值类型, 支持 literal 提取, 需要满足所有元素本身都是 literal
        // e.g.  (1, 3.14) == (1, 3.14) 在类型检查阶段算出来 true
        else if (v instanceof TupleType)  {
            st.push(v);
            for (Value v1 : ((TupleType) v).eltTypes.values) {
                if (literal(v1, st) == null) {
                    st.pop(v);
                    return null;
                }
            }
            st.pop(v);
            return ((TupleType) v).eltTypes;
        }


        // vector record 貌似不需要 literal 类型
        // vector 读取和 record 字段访问处理的字段支持就行
        /*
        else if (v instanceof VectorType) {
            VectorType vt = (VectorType) v;
            if (vt.positional == null) {
                return null;
            } else {
                st.push(v);
                for (Value v1 : vt.positional.values) {
                    if (literal(v1, st) == null) {
                        st.pop(v);
                        return null;
                    }
                }
                st.pop(v);
                return vt.positional;
            }
        } else if (v instanceof RecordType) {
            st.push(v);
            for (String k : ((RecordType) v).props.keySet()) {
                if (literal(((RecordType) v).props.lookupLocal(k), st) == null) {
                    st.pop(v);
                    return null;
                }
            }
            st.pop(v);
            return Record(((RecordType) v).name, (RecordType) v, ((RecordType) v).props);
        }
        */
        else {
            return null;
        }
    }

    static Value clear(Value v, MonoStack st) {
        if (st.contains(v)) {
            return v;
        }
        else if (v instanceof BoolType) {
            if (((BoolType) v).literal == null) {
                return v;
            } else {
                return BOOL;
            }
        }
        else if (v instanceof IntType)    {
            if (((IntType) v).literal == null) {
                return v;
            } else {
                return INT;
            }
        }
        else if (v instanceof FloatType)  {
            if (((FloatType) v).literal == null) {
                return v;
            } else {
                return FLOAT;
            }
        }
        else if (v instanceof StringType) {
            if (((StringType) v).literal == null) {
                return v;
            } else {
                return STR;
            }
        } else if (v instanceof VectorType) {
            st.push(v);
            // todo 这里同时清除了 positional // ???
            VectorType vt = VectorType(clear(((VectorType) v).eltType, st));
            st.pop(v);
            return vt;
        }

        // todo 处理其他复杂类型的 clear 逻辑
//        else if (v instanceof LiteralType){
//            st.push(v);
//            Object l = literal(((LiteralType) v).type, st);
//            st.pop(v);
//            return l;
//        }
//        // todo uniontype
//        else if (v instanceof TupleType)  {
//            st.push(v);
//            for (Value v1 : ((TupleType) v).eltTypes.values) {
//                if (literal(v1, st) == null) {
//                    st.pop(v);
//                    return null;
//                }
//            }
//            st.pop(v);
//            return ((TupleType) v).eltTypes;
//        }

        else {
            return v;
        }
    }

}
