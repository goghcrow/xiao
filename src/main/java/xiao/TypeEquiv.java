package xiao;

import org.jetbrains.annotations.NotNull;
import xiao.misc.Error;
import xiao.misc.Location;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import static xiao.Constant.KEY_MUT;
import static xiao.Stack.PairStack;
import static xiao.Stack.typeStack;
import static xiao.Type.*;
import static xiao.Value.*;

/**
 * @author chuxiaofeng
 */
class TypeEquiv {

    public static boolean isSubtype(@NotNull Value type1, @NotNull Value type2) {
        return subtype.subtype(type1, type2);
    }

    public static boolean castSafe(@NotNull Value type1, @NotNull Value type2) {
        // subtype 双向不行, 因为 [1] as (Int|Str)[] 应该能 cast, 但是 vec 不满足自类型关系
        // return subtype.subtype(type1, type2) || subtype.subtype(type2, type1)
        return cast.subtype(type1, type2) || cast.subtype(type2, type1);
    }

    public static boolean typeMatched(@NotNull Value type1, @NotNull Value type2) {
        return subtype.subtype(type1, type2) || subtype.subtype(type2, type1);
    }

    // 能判等的语义改成 subtype 关系
    public static boolean support(@NotNull Value v1, @NotNull Value v2) {
        return subtype.supportEquiv(v1, v2) || subtype.supportEquiv(v2, v1);
    }

    // cast 语义, 可能是正确的
    private final static TypeEquiv cast = new TypeEquiv(); // subtype
    private final static TypeEquiv subtype = new TypeEquiv();
    private final static TypeEquiv invariant = new TypeEquiv();

    // -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=

    private TypeEquiv() { }

    boolean subtype(@NotNull Value t1, @NotNull Value t2) {
        return subtype(t1, t2, new PairStack(), false);
    }

    boolean supportEquiv(@NotNull Value t1, @NotNull Value t2) {
        return subtype(t1, t2, new PairStack(), true);
    }

    private boolean isInvariant() {
        return this == invariant;
    }

    // for cast: 保证 cast 不会换成 subtype
    // private TypeEquiv subtype() { return this == cast ? cast : subtype; }

    // for cast: 保证 cast 不会换成 invariant, 因为 cast 时候数组不是不变, e.g. [1] as (Int|Str)[]
    // vector 和 record mut 域 强制不变
    private TypeEquiv invariant() {
        return this == cast ? cast : invariant;
    }

    // -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=

    // 能用父类型的都能用子类型, 里式替换
    // 子类型可以理解成集合的子集
    // 子类型更具体, 集合内元素数量更少, 父类型更抽象, 集合内元素数量更多
    // [] 空 vec 字面量也处理成 既是 Bottom 也是 Top
    boolean subtype(@NotNull Value t1, @NotNull Value t2/*, boolean ret*/, PairStack st, boolean equivable) {
        if (t1 == t2) {
            return true;
        } else if (st.contains(t1, t2)) {
            return true;
        } else if (t1 == NEVER || t2 == NEVER) {
            return false;
        } else if (t1 instanceof Undefined) {
            // 为了 letrec hack 的逻辑, letrec 不检查类型
            return true;
        } else if (/*!ret && */t2 == ANY) {
            return true;
        } else if (t2 instanceof LiteralType) {
            return subtype(t1, ((LiteralType) t2), st, equivable);
        } else if (t1 instanceof LiteralType) {
            return subtype(((LiteralType) t1), t2, st, equivable);
        } else if (t1 instanceof IntType && t2 instanceof FloatType) {
            return subtype(((IntType) t1), ((FloatType) t2), st, equivable);
        } else if (t1 instanceof TupleType && t2 instanceof TupleType) {
            return subtype((TupleType) t1, (TupleType) t2, st, equivable);
        } else if (t1 instanceof VectorType && t2 instanceof VectorType) {
            return subtype((VectorType) t1, (VectorType) t2, st, equivable);
        } else if (t1 instanceof RecordType && t2 instanceof RecordType) {
            return subtype((RecordType) t1, (RecordType) t2, st, equivable);
        } else if (t1 instanceof FunType && t2 instanceof FunType) {
            return subtype((FunType) t1, (FunType) t2, st, equivable);
        }  else if (t1 instanceof UnionType) {
            return subtype((UnionType) t1, t2, st, equivable);
        } else if (t2 instanceof UnionType) {
            return subtype(t1, (UnionType) t2, st, equivable);
        } else if (t1 instanceof PrimFun) {
            // todo: 暂时给 prim 开个口子, 需要检查类型
            return t2 instanceof FunType;
        } else {
            // 这个分支只比较类型相等, 不处理 literal, 前面已经处理了
            // return t1.equals(t2);
            return Equals.typeEquals(t1, t2, st, equivable);
        }
    }

    // -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=

    // literalEquals // 支持字面类型
    boolean subtype(Value t1, LiteralType t2, PairStack ts, boolean equivable) {
        if (Type.isLiteral(t1)) {
            Object l1 = Literal.literal(t1);
            Object l2 = Literal.literal(t2);
            assert l1 != null && l2 != null;

            if (l1 == l2) {
                return true;
            } else if (l1.getClass() != l2.getClass()) {
                // 因为字面类型都是子类型, 没法嵌套, 所以这里可以直接比较 class
                return false;
            } else if (l1 instanceof Value && l2 instanceof Value) {
                ts.push(t1, t2);
                // todo test case
                // tupleValue, vectorValue, recordValue
                boolean equals = subtype(((Value) l1), ((Value) l2), ts, equivable);
                ts.pop(t1, t2);
                return equals;
            } else {
                // boolean, int, float, string
                if (equivable) {
                    // return l1.getClass() == l2.getClass();
                    return true;
                } else {
                    return l1.equals(l2);
                }
            }
        } else {
            return false;
        }
    }

    // todo 写测试用例
    boolean subtype(LiteralType t1, Value t2, PairStack ts, boolean equivable) {
        ts.push(t1, t2);
        boolean equals = subtype(t1.type, t2, ts, equivable);
        ts.pop(t1, t2);
        return equals;
    }

    // int <: int  float <: float  int <: float
    @SuppressWarnings("unused")
    boolean subtype(IntType t1, FloatType t2, PairStack ts, boolean equivable) {
        //noinspection RedundantIfStatement
        if (isInvariant()) {
            return false;
        } else {
            return true;
        }
    }

    boolean subtype(TupleType t1, TupleType t2, PairStack ts, boolean equivable) {
        if (t1.eltTypes.size() != t2.eltTypes.size()) {
            return false;
        }

        ts.push(t1, t2);
        for (int i = 0; i < t1.eltTypes.size(); i++) {
            Value et1 = t1.eltTypes.get(Location.None, i);
            Value et2 = t2.eltTypes.get(Location.None, i);

            // e.g. 如果 [(int,)] <: [(float,)] 成立
            // let a : [(int,)] = [(1,)]
            // let b : [(float,)] = a
            // b.add((3.14,))
            // a[1][0] = 3.14 类型错误, 所以数组嵌套的 tuple 要求不变

            // e.g. 如果 {mut x:int} <: {mut x:float} 成立
            // let a : {mut x:int} = {x:1}
            // let b : {mut x:float} = a
            // b.x = 3.14
            // a.x 类型错误, 所以不成立

            // 当前是 invariant 返回 invariant, 当前
            if (!subtype(et1, et2, ts, equivable)) {
                ts.pop(t1, t2);
                return false;
            }
        }

        ts.pop(t1, t2);
        return true;
    }

    // 数组这里是 mutable 的可读可写, 所以永远是不变 (invariant)
    boolean subtype(VectorType t1, VectorType t2, PairStack ts, boolean equivable) {
        // 空数组既是子类又是超类

        // 处理 let v: Int[] = [], 空数组是所有数组的子类型
        // [] as Int[]
        if (t1.isEmptyLiteral()) {
            assert t2.eltType != UNKNOWN;
            return true;
        }

        // 不再支持没声明类型的空数组:  处理 let mut v = []   v = [1], 所有数组是空数组的子类
//        if (t1.eltType != UNKNOWN && t2.isEmptyLiteral()) {
//            return true;
//        }

        // todo: 做一个实验, 允许数组协变, 但是复制原来的类型变更成支持协变的条件
        ts.push(t1, t2);
        boolean equals = invariant().subtype(t1.eltType, t2.eltType, ts, equivable);
        ts.pop(t1, t2);
        return equals;
    }

    // 这里 struct 采用了 structural typing, 如果采用 nominal typing 直接对比引用是否相等
    // OO 语言一般 用 nominally type, FP 语言一般用 structural typing
    // Struct 又或者是 types of records 子类型有两个维度
    // 不变要求宽度和域深度都不变
    boolean subtype(RecordType t1, RecordType t2, PairStack ts, boolean equivable) {
        int t1Sz = t1.props.keySet().size();
        int t2Sz = t2.props.keySet().size();
        if (isInvariant()) {
            // 不变要求宽度不变
            if (t1Sz != t2Sz) {
                // e.g. 如果 [{x,y}] <: [{x}] 成立
                // let a : [{x,y}] = [{x=1,y=2}]
                // let b : [{x}]  = a
                // b.add({x=2})
                // a[1].y 类型错误, 所以不成立
                return false;
            }
        } else {
            // width subtyping 宽度子类型
            // 更“小”的类型却包含更多的域
            // e.g. { X Y } <: { X }
            // {X Y} <: {Y X}   {Y X} <: {X Y}
            if (t1Sz < t2Sz) {
                return false;
            }
        }

        ts.push(t1, t2);
        // t1Sz >= t2Sz
        for (String key : t2.props.keySet()) {
            Value f1t = t1.props.lookupLocalType(key);
            Value f2t = t2.props.lookupLocalType(key);
            assert f2t != null;
            if (f1t == null) {
                return false;
            }

            Object f2mut = t2.props.lookupLocalProp(key, KEY_MUT);
            assert f2mut == null || f2mut == TRUE || f2mut == FALSE;
            boolean mut = f2mut != FALSE;

            // 当域 mutable 与数组一样对待, 强制要求不变
            // e.g. 如果 (struct {mut x: int}) <: (struct {mut x: float}) 成立
            // let a : (struct {mut int}) = ({1})
            // let b : (struct {mut float}) = a
            // b[0].x = 3.14
            // a[0].x 类型错误, mut 域不构成 subtype 关系
            // depth subtyping 深度子类型, 只能用于 immutable 域
            TypeEquiv equiv = (mut || isInvariant()) ? invariant() : this;
            if (!equiv.subtype(f1t, f2t, ts, equivable)) {
                ts.pop(t1, t2);
                return false;
            }
        }
        ts.pop(t1, t2);
        return true;
    }

    // 函数子类型 参数逆变contravariant 返回值协变covariant
    // T1 <: S1  S2 <: T2
    // S1 → S2 <: T1 → T2
    // 函数参数逆变返回值协变, but 在数组里或者 struct mut 域都必须不变
    // e.g. 如果 [float->int] <: [int->float] 成立
    // let a : [float->int] = [ (x :float) => 1 ]
    // let b : [int->float] = a
    // b.add((x :int) => 3.14)
    // a[1] 类型是 int -> float, 所以子类型关系不成立
    // e.g. 如果 [float->int] <: [int->float] 成立
    // let a :{ mut f :[float->int] } = {f = (x :float) => 1}
    // let b :{mut f :[int->float]} = a
    // b.f = (x :int) => 3.14
    // a.f 类库错误, 所以子类型关系不成立
    boolean subtype(Arrow t1, Arrow t2, PairStack ts, boolean equivable) {
        if (ts.contains(t1, t2)) {
            return true;
        }

        if (t1.paramsType.size() != t2.paramsType.size()) {
            return false;
        }

        ts.push(t1, t2);
        if (!subtype(t2.paramsType, t1.paramsType)) {
            // 参数 contravariant
            ts.pop(t1, t2);
            return false;
        } else if (!subtype(t1.retType, t2.retType, ts, equivable)) {
            // 返回值 covariant
            ts.pop(t1, t2);
            return false;
        } else {
            ts.pop(t1, t2);
            return true;
        }
    }

    boolean subtype(FunType t1, FunType t2, PairStack ts, boolean equivable) {
        // 解释器中 fun 判等, 在 match type 中严格一致
        if (t1.paramsType == null || t1.retType == null) {
            // assert in interpreter
            // throw Error.bug("t1.paramType == null || t1.retType == null");
            return t1.def == t2.def;
        }
        if (t2.paramsType == null || t2.retType == null) {
            // assert in interpreter
            // throw Error.bug("t2.paramType == null || t2.retType == null");
            return t1.def == t2.def;
        }

        // 同一个 fun, fast routine
        if (t1.def == t2.def) {
            return true;
        }

        //noinspection RedundantCast
        return subtype(((Arrow) t1), ((Arrow) t2), ts, equivable);
    }

    boolean subtype(UnionType t1, Value t2, PairStack ts, boolean equivable) {
        if (isInvariant()) {
            // 不用处理 cast, cast 不是 invariant
            /*
            let a= [1, "s"]
            let b: (Float|Str)[] = a
            a[0] = 3.14
            */
            if (t2 instanceof UnionType) {
                ts.push(t1, t2);
                boolean equals = subtype(t1, ((UnionType) t2), ts, equivable);
                ts.pop(t1, t2);
                return equals;
            } else {
                return false;
            }
        } else {
            ts.push(t1, t2);
            assert !t1.types.isEmpty();
            for (Value t : t1.types) {
                if (!subtype(t, t2, ts, equivable)) {
                    ts.pop(t1, t2);
                    return false;
                }
            }
            ts.pop(t1, t2);
            return true;
        }
    }

    boolean subtype(Value t1, UnionType t2, PairStack ts, boolean equivable) {
        if (isInvariant()) {
            // 不用处理 cast, cast 不是 invariant
            if (t1 instanceof UnionType) {
                ts.push(t1, t2);
                boolean equals = subtype(((UnionType) t1), t2, ts, equivable);
                ts.pop(t1, t2);
                return equals;
            } else {
                return false;
            }
        } else {
            ts.push(t1, t2);
            assert !t2.types.isEmpty();
            for (Value t : t2.types) {
                if (subtype(t1, t, ts, equivable)) {
                    ts.pop(t1, t2);
                    return true;
                }
            }
            ts.pop(t1, t2);
            return false;
        }
    }

    // todo 所有 type 自动处理 literal 的 equals 方法
    boolean subtype(UnionType t1, UnionType t2, PairStack ts, boolean equivable) {
        assert isInvariant();
        // todo !!! invariant 兼容单向的 literalType
        // todo !!! invariant 兼容单向的 literalType
        // todo !!! invariant 兼容单向的 literalType


        // todo
        boolean equals = Equals.typeEquals(t1, t2, ts, equivable);

        return equals;
    }

    // -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=

    final static Comparator<Value> unionComp = (v1, v2) -> {
        if (v1 == v2) {
            return 0;
        }

        if (Equals.typeEquals(v1, v2)) {
            return 0;
        } else {
            int cmp = Integer.compare(v1.hashCode(), v2.hashCode());
            return cmp == 0 ? 1 : cmp; // 一定不能相等
        }
    };


    /**
     * todo: 保证符合类型没有调用 equals
     * @author chuxiaofeng
     */
    static class Equals {

        public static boolean typeEquals(@NotNull Value self, Object other) {
            return typeEquals(self, other, typeStack(), false);
        }

    //    public static boolean valueEquals(@NotNull Value self, Object other) {
    //        return valueEquals(self, other, typeStack());
    //    }

        static boolean typeEquals(@NotNull Value self, Object other, PairStack st, boolean equivable) {
            if (self == other) {
                return true;
            }

            if (other == null || self.getClass() != other.getClass()) {
                return false;
            }

            // todo !!!
            // todo !!!
            // todo !!!
            // todo !!!
            // todo !!!
            // todo !!!

            if (self instanceof LiteralType) {
                Object ll = Literal.literal(self);
                Object rl = Literal.literal(((Value) other));
                assert ll != null && rl != null;
                return literalTypeEquals(ll, rl, st, equivable);
            } else {
                return nonLiteralTypeEquals(self, other, st, equivable);
            }
        }

        static boolean literalTypeEquals(@NotNull Object left, @NotNull Object right, PairStack st, boolean equivable) {
            if (left == right) {
                return true;
            }

            if (left.getClass() != right.getClass()) {
                return false;
            }

            if (left instanceof Value && right instanceof Value) {
                // todo 测试
                // tupleValue, vectorValue, recordValue
                return valueEquals(((Value) left), right, st, equivable);
            } else {
                if (equivable) {
                    // return left.getClass() == right.getClass();
                    return true;
                } else {
                    // boolean, int, float, string
                    return left.equals(right);
                }
            }
        }

        static boolean nonLiteralTypeEquals(@NotNull Value self, Object other, PairStack st, boolean equivable) {
            // never any 都是单例
            if (st.contains(self, ((Value) other)))   { return true; }
            else if (self instanceof NeverType)       { return self == other; }
            else if (self instanceof AnyType)         { return self == other; }
            else if (self instanceof BoolType)        { return true; }
            else if (self instanceof IntType)         { return true; }
            else if (self instanceof FloatType)       { return true; }
            else if (self instanceof StringType)      { return true; }
            else if (self instanceof FunType)         { return equals(((FunType) self),     ((FunType) other), st, equivable);    }
            else if (self instanceof ModuleType)      { return equals(((ModuleType) self),  ((ModuleType) other), st, equivable); }
            else if (self instanceof TupleType)       { return equals(((TupleType) self),   ((TupleType) other), st, equivable);  }
            else if (self instanceof VectorType)      { return equals(((VectorType) self),  ((VectorType) other), st, equivable); }
            else if (self instanceof RecordType)      { return equals(((RecordType) self),  ((RecordType) other), st, equivable); }
            else if (self instanceof UnionType)       { return equals(((UnionType) self),   ((UnionType) other), st, equivable);  }
            else if (self instanceof Undefined)       { return equals(((Undefined) self),   ((Undefined) other), st, equivable);  }
            else {
                // 因为 interp 也要使用字面量类型, 字面量是 value
                // throw Error.bug("不支持判等的类型 : " + self.getClass() + " vs " + other.getClass());
                // assert in interpreter
                return valueEquals(self, other, st, equivable);
            }
        }

        // 注意这里只给字面量类型使用
        // value 的 equals 方法与 值类型或者引用类型有关, 这里 eq 的语义都是值
        static boolean valueEquals(@NotNull Value self, Object other, PairStack st, boolean equivable) {
            if (equivable) {
                return self.getClass() == other.getClass();
            } else
            // todo 把没用的分支删了...
            // void 分支走不到, Void 是单例
            // bool 分支走不到, TRUE FALSE 也是单例
            // closure|primfun|module false
            if (self == other) { return true; }
            else if (other == null || self.getClass() != other.getClass()) { return false; }
            else if (st.contains(self, ((Value) other))) { return true; }
            else if (self instanceof VoidValue)     { return self.equals(other); }
            else if (self instanceof BoolValue)     { return self.equals(other); }
            else if (self instanceof IntValue)      { return self.equals(other); }
            else if (self instanceof FloatValue)    { return self.equals(other); }
            else if (self instanceof StringValue)   { return self.equals(other); }
            else if (self instanceof Closure)       { return self.equals(other); }
            else if (self instanceof PrimFun)       { return self.equals(other); }
            else if (self instanceof ModuleValue)   { return self.equals(other); }
            else if (self instanceof TupleValue)    { return equals(((TupleValue) self), ((TupleValue) other), st, equivable); }
            else if (self instanceof VectorValue)   { return equals(((VectorValue) self), ((VectorValue) other), st, equivable); }
            else if (self instanceof RecordValue)   { return equals(((RecordValue) self), ((RecordValue) other), st, equivable); }
            else { throw Error.bug("不支持判等的类型 : " + self.getClass() + " vs " + other.getClass()); /*return self.equals(other);*/ }
        }

        static boolean equals(@NotNull RecordValue self, RecordValue other, PairStack st, boolean equivable) {
             return self.equals(other);
//            st.push(self, other);
//            if (!typeEquals(self.type, other.type, st)) {
//                st.pop(self, other);
//                return false;
//            }
//            for (String k : self.props.keySet()) {
//                Value lv = self.props.lookupLocal(k);
//                Value rv = other.props.lookupLocal(k);
//                assert lv != null;
//                if (!typeEquals(lv, rv, st)) {
//                    st.pop(self, other);
//                    return false;
//                }
//
//            }
//            st.pop(self, other);
//            return true;
        }

        static boolean equals(@NotNull VectorValue self, VectorValue other, PairStack st, boolean equivable) {
             return self.equals(other);
//            if (self.values.size() != other.values.size()) {
//                return false;
//            }
//
//            st.push(self, other);
//            for (int i = 0; i < self.values.size(); i++) {
//                Value lv = self.values.get(i);
//                Value rv = other.values.get(i);
//                if (!typeEquals(lv, rv, st)) {
//                    st.pop(self, other);
//                    return false;
//                }
//            }
//            st.pop(self, other);
//            return true;
        }

        static boolean equals(@NotNull TupleValue self, TupleValue other, PairStack st, boolean equivable) {
            if (self.values.size() != other.values.size()) {
                return false;
            }

            st.push(self, other);
            for (int i = 0; i < self.values.size(); i++) {
                Value lv = self.values.get(i);
                Value rv = other.values.get(i);
                if (!typeEquals(lv, rv, st, equivable)) {
                    st.pop(self, other);
                    return false;
                }
            }
            st.pop(self, other);
            return true;
        }

        static boolean equals(@NotNull FunType self, FunType other, PairStack st, boolean equivable) {
            if (self.def.equals(other.def)) {
                return true;
            }

            if (self.paramsType.size() != other.paramsType.size()) {
                return false;
            }

            st.push(self, other);
            if (!typeEquals(other.paramsType.eltTypes, self.paramsType.eltTypes, st, equivable)) {
                st.pop(self, other);
                return false;
            } else if (!typeEquals(self.retType, other.retType, st, equivable)) {
                st.pop(self, other);
                return false;
            } else {
                st.pop(self, other);
                return true;
            }
        }

        static boolean equals(@NotNull ModuleType self, ModuleType other, PairStack st, boolean equivable) {
            return self == other;
        }

        static boolean equals(@NotNull TupleType self, TupleType other, PairStack st, boolean equivable) {
            List<Value> lst1 = self.eltTypes.values;
            List<Value> lst2 = other.eltTypes.values;
            if (lst1.size() != lst2.size()) {
                return false;
            }

            st.push(self, other);
            for (int i = 0; i < lst1.size(); i++) {
                if (!typeEquals(lst1.get(i), lst2.get(i), st, equivable)) {
                    st.pop(self, other);
                    return false;
                }
            }
            st.pop(self, other);
            return true;
        }

        static boolean equals(@NotNull VectorType self, VectorType other, PairStack st, boolean equivable) {
            st.push(self, other);
            boolean r = typeEquals(self.eltType, other.eltType, st, equivable);
            st.pop(self, other);
            return r;
        }

        static boolean contains(Collection<Value> vals, Value val, PairStack st, boolean equivable) {
            for (Value v : vals) {
                if (typeEquals(val, v, st, equivable)) {
                    return true;
                }
            }
            return false;
        }

        static boolean equals(@NotNull RecordType self, RecordType other, PairStack st, boolean equivable) {
            if (self.def.equals(other.def)) {
                return true;
            }

            Scope props1 = self.props;
            Scope props2 = other.props;
            if (props1.table.size() != props2.table.size()) {
                return false;
            }

            st.push(self, other);
            for (String k : props1.keySet()) {
                Value a = props1.lookupLocal(k);
                Value b = props2.lookupLocal(k);
                if (b == null) {
                    return false;
                }
                assert a != null;
                if (!typeEquals(a, b, st, equivable)) {
                    st.pop(self, other);
                    return false;
                }
            }
            st.pop(self, other);
            return true;
        }

        static boolean equals(@NotNull UnionType self, UnionType other, PairStack st, boolean equivable) {
            // Int|String == String|Int
            Set<Value> values1 = self.types;
            Set<Value> values2 = other.types;
            if (values1.size() != values2.size()) {
                return false;
            }

            /* // 因为是 set 所以一定没有重复的
                for (Value val1 : values1) {
                    boolean eq = false;
                    for (Value val2 : values2) {
                        if (val1.equals(val2)) {
                            eq = true;
                            break;
                        }
                    }
                    if (!eq) {
                        return false;
                    }
                }
                return true; */

            st.push(self, other);
            for (Value t2 : values2) {
                if (!contains(values1, t2, st, equivable)) {
                    st.pop(self, other);
                    return false;
                }
            }

            for (Value t1 : values1) {
                if (!contains(values2, t1, st, equivable)) {
                    st.pop(self, other);
                    return false;
                }
            }
            st.pop(self, other);
            return true;
        }

        static boolean equals(@NotNull Undefined self, Undefined other, PairStack st, boolean equivable) {
            // todo
            return true;
        }
    }
}
