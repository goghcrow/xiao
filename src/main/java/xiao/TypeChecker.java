package xiao;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xiao.front.Ast;
import xiao.match.Matchers;
import xiao.misc.Error;
import xiao.misc.*;

import java.util.*;
import java.util.Map.Entry;

import static xiao.Constant.*;
import static xiao.Copy.copyType;
import static xiao.Type.*;
import static xiao.Value.*;
import static xiao.front.Ast.Str;
import static xiao.front.Ast.*;
import static xiao.misc.Diagnosis.Category;

/**
 * @author chuxiaofeng
 */
public class TypeChecker implements Visitor<Scope, Value>, Evaluator<Scope, Value> {

    TypeChecker() { }

    public final List<Diagnosis> typeErrors = new LinkedList<>();

    @Override
    public Value eval(@NotNull Node n, @NotNull Scope s) {
        return typecheck(n, s);
    }

    final Set<FunType> uncalled = new HashSet<>();
    final Set<FunType> callStack = new HashSet<>();

    Value typecheck(@NotNull Node node, @NotNull Scope s) {
        Stack.resetTypeStack();
        return visit(node, s);
    }

    void addError(Error.Type error) {
        addError(Category.WARNING, error);
    }

    // todo 其他错误
    void addError(Category cat, Error.Type error) {
        typeErrors.add(new Diagnosis(error.loc, cat, error.toString()));
    }

    void summary() {
        if (typeErrors.isEmpty()) {
            return;
        } else {
            // for (Diagnosis d : typeErrors) { Helper.log(d.toString()); }
            throw Error.diagnosisSummary(typeErrors);
        }
    }

    void invokeUncalled(FunType fun, Scope s, boolean checkRet) {
        Scope funScope = new Scope(fun.env);
        Declares.mergeType(funScope, fun.props); // 形参类型
        List<Id> params = fun.def.params;
        List<Value> paramTypes = new ArrayList<>(params.size());
        for (Id param : params) {
            Value t = funScope.lookupLocal(param.name);
            if (t == null) {
                // 形参如果没有写类型或者没有默认值, funScope 会缺失参数的绑定, 这种情况先标记未知类型
//                 funScope.putValue(param.name, t = UNKNOWN);

                // 做了个折中, 不打算允许不声明函数参数
                throw Error.type(param.loc, "参数 " + param + " 必须声明类型");
            }
            Voids.not(param.loc, t, "实参不能为 Void");
            paramTypes.add(t);
        }

        fun.paramsType = TupleType(paramTypes);

        // 有声明类型用声明类型, 没有用推导的类型
        fun.retType = callFunReturn(fun.def.loc, fun, funScope, checkRet);
    }

    List<Value> typecheckList(@NotNull List<Node> nodes, @NotNull Scope s) {
        List<Value> types = new ArrayList<>(nodes.size());
        for (Node node : nodes) {
            types.add(typecheck(node, s));
        }
        return types;
    }

//    @Override
//    public Value visit(@NotNull Group group, @NotNull Scope s) {
//        return typecheck(group.expr, s);
//    }

    @Override
    public Value visit(@NotNull Id id, @NotNull Scope s) {
        Value v = s.lookup(id.name);
        if (v == null) {
            // todo remove !!!!!!
            // hack for
            // type validate = (a: Bool) => if a { 42 } else { "???" }
            // fun answer(a: Bool, b: validate(a)) = b
            v = s.lookupType(id.name);
        }
        if (v != null) {
            if (inTypeDecl > 0 && v instanceof BoolType) {
                return LiteralType(v);
            } else {
                return v;
            }
        } else {
            throw Error.runtime(id.loc, "变量 " + id.name + " 未定义");
        }
    }

    @Override
    public Value visit(@NotNull IntNum intNum, @NotNull Scope s) {
        if (inTypeDecl > 0) {
            return LiteralType(IntType(intNum.value));
        } else {
            return IntType(intNum.value);
        }
    }

    @Override
    public Value visit(@NotNull FloatNum floatNum, @NotNull Scope s) {
        if (inTypeDecl > 0) {
            return LiteralType(FloatType(floatNum.value));
        } else {
            return FloatType(floatNum.value);
        }
    }

    @Override
    public Value visit(@NotNull Str str, @NotNull Scope s) {
        if (inTypeDecl > 0) {
            return LiteralType(StringType(str.value));
        } else {
            return StringType(str.value);
        }
    }

    @Override
    public Value visit(@NotNull TupleLiteral tuple, @NotNull Scope s) {
        List<Value> types = typecheckList(tuple.elements, s);
        TupleType tt = TupleType(types);
//        if (typeDecl > 0 && isLiteral(tt)) {
//            return LiteralType(tt);
//        } else {
            return tt;
//        }
    }

    @Override
    public Value visit(@NotNull VectorLiteral tuple, @NotNull Scope s) {
        if (tuple.elements.isEmpty()) {
             return VectorType(UNKNOWN, new ArrayList<>());
        } else {
            List<Value> types = typecheckList(tuple.elements, s);
            return VectorType(union(types), types);
        }
    }

    @Override
    public Value visit(@NotNull RecordLiteral literal, @NotNull Scope s) {
        Scope declareProps = new Scope();

        for (String name : literal.map.keySet()) {
            declareProps.put(name, KEY_DEFAULT, literal.node(name));
            // record 字面量属性默认都是不可修改的
            declareProps.put(name, KEY_MUT, Ast.id(Location.None, literal.mut(name) ? ID_TRUE : ID_FALSE));
        }

        Declare declare = Ast.declare(literal.loc, declareProps);
        // 重复利用 interpDeclare, interpCallRecord 会合并默认值
        Scope props = Declares.typecheck(this, declare, s);
        // 字面量，填充类型
        // let f = (r: record {a:Int}) => r.a
        // f([a:1, b:"str"])
        for (String k : props.keySet()) {
            // 这里不用 copy, 因为只有一个实例
            Value def = props.lookupLocalDefault(k);
            Value t = props.lookupLocalType(k);
            // 字面量一定有 default, 一定没有类型
            assert def != null && t == null;
            // 注意这里, 填充的如果 mut 不能用字面类型
            // let immutable_var = 1 // 推导成字面类型
            // let mutable_rec = [ mut id: immutable_var ] // 这里还原成实际类型
            // mutable_rec.id = 42
            if (def instanceof LiteralType) {
                def = ((LiteralType) def).type;
            }
            props.put(k, KEY_TYPE, def);
        }
        RecordType type = RecordType(null, literal, props, s);
        return callRecord(literal.loc, type, Ast.emptyArgs, s);
    }

    int inTypeDecl = 0;

    @Override
    public Value visit(@NotNull LitType lit, @NotNull Scope s) {
        Value t = typecheck(lit.node, s);
        return LiteralType(t);
    }

    @Override
    public Value visit(@NotNull Typedef def, @NotNull Scope s) {
        if (def.rec) {
            typerec(def, s);
        } else {
            typedef(def, s);
        }
        return Type.VOID;
    }

    void typedef(Typedef def, Scope s) {
        Id name = ((Id) def.pattern);
        Value type;
        try {
            inTypeDecl++;
            type = typecheck(def.type, s);
        } finally {
            inTypeDecl--;
        }

        if (Voids.contains(type)) {
            throw Error.type(name.loc, "不能 define Void");
        }
        TypeChecker.emptyVectorAssert(def.loc, type);
        Matchers.reDefineCheck(name, s);
        s.put(name.name, KEY_MUT, FALSE);
        s.putValue(name.name, type);
        // s.putType(name, type);
    }

    void typerec(Typedef def, Scope s) {
        TupleLiteral tuple = (TupleLiteral) def.pattern;
        TupleLiteral val = (TupleLiteral) def.type;
        assert val != null;

        List<Node> lhs = tuple.elements;
        List<Node> rhs = val.elements;
        if (lhs.size() != rhs.size()) {
            throw Error.type(def.loc, "typerec 左右数量不匹配: " + lhs.size() + " vs " + rhs.size());
        }

        for (Node el : lhs) {
            Id id = (Id) el;
            Matchers.reDefineCheck(id, s);
            s.put(id.name, KEY_MUT, FALSE);
            s.putValue(id.name, new Undefined(() -> s.lookupLocal(id.name)));
            // s.putType();
        }

        Value type;
        for (int i = 0; i < rhs.size(); i++) {
            try {
                inTypeDecl++;
                type = typecheck(rhs.get(i), s);
            } finally {
                inTypeDecl--;
            }
            if (Voids.contains(type)) {
                throw Error.type(rhs.get(i).loc, "不能 define Void");
            }
            TypeChecker.emptyVectorAssert(def.loc, type);
            s.putValue(((Id) lhs.get(i)).name, type);
            // s.putType(name, type);
        }

        fixRec(def.loc, lhs, s);
    }

    @Override
    public Value visit(@NotNull Define var, @NotNull Scope s) {
        if (var.value == null) {
            throw Error.bug(var.loc, "不支持未初始化变量声明");
        }
        if (var.rec) {
            letrec(var, s);
        } else {
            let(var, s);
        }

        return Type.VOID;
    }

    void letrec(@NotNull Define var, @NotNull Scope s) {
        TupleLiteral tuple = (TupleLiteral) var.pattern;
        TupleLiteral val = (TupleLiteral) var.value;
        assert val != null;

        List<Node> lhs = tuple.elements;
        List<Node> rhs = val.elements;
        if (lhs.size() != rhs.size()) {
            throw Error.type(var.loc, "letrec 左右数量不匹配: " + lhs.size() + " vs " + rhs.size());
        }

        for (Node el : lhs) {
            Id id = (Id) el;
            Matchers.reDefineCheck(id, s);
            s.putValue(id.name, new Undefined(() -> s.lookupLocal(id.name)));
        }

        for (int i = 0; i < rhs.size(); i++) {
            Value v = typecheck(rhs.get(i), s);
            Matchers.checkDefine(this, s, lhs.get(i), v, var.mut);
        }

        fixRec(var.loc, lhs, s);
    }

    void fixRec(Location loc, List<Node> lhs, Scope s) {
        for (Node el : lhs) {
            Id id = (Id) el;
            Value v = s.lookupLocal(id.name);
            Value fixedV = LetRecFixer.fixType(loc, v);
            s.putValue(id.name, fixedV);
        }
    }

    void let(@NotNull Define var, @NotNull Scope s) {
        assert var.value != null;
        Value actual = typecheck(var.value, s);
        if (var.type == null) {
            Matchers.checkDefine(this, s, var.pattern, actual, var.mut);
        } else {
            letWithType(var, s, actual);
        }
    }

    void letWithType(@NotNull Define var, @NotNull Scope s, Value actual) {
        // 特殊处理有类型的 id 绑定，只有 id 绑定才允许写类型
        assert var.pattern instanceof Id;
        Id id = (Id) var.pattern;
        Matchers.reDefineCheck(id, s);
        assert var.type != null;
        Value expected = typecheck(var.type, s);
        if (!subtype(actual, expected)) {
            throw Error.type(var.loc, "变量期望绑定类型是 " + expected + " 的超类, 实际是 " + actual);
        }

        /*let t1 = ("hello", 1)
        let t2 = ("hello", 3.14)
        let TupleOfStrFloat = (Str, Float)
        let mut t : (Str, Float) = t1
        t = t2*/
        Value v = select(var.loc, var.mut, expected, actual);
        // let 基础类型的常量都处理成字面类型
        v = literalWrapIfNecessary(var.mut, v);
        s.putValue(id.name, v);
        s.put(id.name, KEY_MUT, var.mut ? TRUE : FALSE);
    }

    @Override
    public Value visit(@NotNull Assign assign, @NotNull Scope s) {
        Value v = typecheck(assign.value, s);
        // new Binder(this, s).assign(assign.pattern, v);
        Matchers.checkAssign(this, s, assign.pattern, v);
        return Type.VOID;
    }

    @Override
    public Value visit(@NotNull RecordDef record, @NotNull Scope s) {
        Scope props = new Scope();

        if (record.parents != null) {
            for (Id p : record.parents) {
                Value pv = typecheck(p, s);
                if (pv instanceof RecordType) {
                    Scope parentProps = ((RecordType) pv).props;
                    for (String k : parentProps.keySet()) {
                        if (props.keySet().contains(k)) {
                            throw Error.syntax(p.loc, "继承字段冲突: " + k);
                        }
                    }
                    props.putAll(parentProps);
                } else {
                    throw Error.syntax(p.loc, "必须继承自 record: " + p.name);
                }
            }
        }

        // TODO: 这种方式的递归声明有问题, 应该用 letrec !!!
        RecordType type;
        if (record.name == null) {
            type = RecordType(null, record, props, s);
        } else {
            // todo , 塞到作用域里头一个未初始化的 recordType, 被取出来使用一定有问题
            // 正常情况应该先创建好recordType再放到作用域
            // 这里要支持递归类型声明, 所以都先 put 进去, 然后再取出来
            String name = record.name.name;
            type = RecordType(name, record, props, s);
            s.putValue(name, type);
        }

        Scope selfProps = Declares.typecheck(this, record.declare, s);
        props.putAll(selfProps);


        for (String k : props.keySet()) {
            // 检查 default type 类型
            // 这一部分其实可以不用检查, 创建 record 实例时候会走参数检查
//            Value expected = props.lookupLocalType(k);
//            Value actual = props.lookupLocalDefault(k);
//            if (expected != null && actual != null) {
//                if (!subtype(actual, expected)) {
//                    throw Error.type(record.loc, "字段 " + k + " 默认值类型错误, 期望 " + expected + ", 实际是 " + actual);
//                }
//            }
//            if (expected == null) {
//                emptyVectorAssert(record.loc, actual);
//            }

            Object mut = props.lookupLocalProp(k, KEY_MUT);
            assert mut instanceof BoolValue;
            Value expected = props.lookupLocalType(k);
            Value actual = props.lookupLocalDefault(k);
            if (expected == null) {
                if (actual == null) {
                    throw Error.type(record.loc, "属性 " + k + " 必须声明类型或者默认值");
                } else {
                    emptyVectorAssert(record.loc, actual);
                    props.putType(k, expected = copyType(actual)); // 用默认值类型填充
                    // ??? 填充 value？？？
                }
            } else {
                if (actual != null) {
                    if (!subtype(actual, expected)) {
                        throw Error.type(record.loc, "字段 " + k + " 默认值类型错误, 期望 " + expected + ", 实际是 " + actual);
                    }
                    // record 的只读属性声明称字面量类型
                    if (mut == FALSE && isPrimLiteral(actual)) {
                        props.putType(k, literalWrapIfNecessary(false, actual));
                    }
                }
            }

//            // mut 属性干掉字面值
//            if (mut == TRUE) {
//                Value t = props.lookupLocalType(k);
//                assert t != null;
//                Value c = Literal.clear(t);
//                if (c != t) {
//                    props.putType(k, c);
//                }
//            }


            // todo ...  这里是否应该填充...
            // 填充 value
            Value val = props.lookupLocal(k);
            if (val == null) {
                props.putValue(k, expected);
            }
        }

        return type;
    }

    @Override
    public Value visit(@NotNull Attribute attr, @NotNull Scope s) {
        Value v;

        Node recordExpr = attr.value;
        Value tv = typecheck(recordExpr, s);
        if (tv instanceof UnionType) {
            List<Value> types = new ArrayList<>();
            for (Value t : ((UnionType) tv).types) {
                types.add(getAttr(attr, t));
            }
            v = union(types);
        } else {
            v = getAttr(attr, tv);
        }

        return Voids.not(attr.loc, v, "attribute 不能为 Void");
    }

    Value getAttr(Attribute attr, Value v) {
        if (v instanceof RecordType) {
            return getRecordAttr(attr, ((RecordType) v));
        } else if (v instanceof ModuleType) {
            return getModuleAttr(attr, ((ModuleType) v));
        } else {
            throw Error.type(attr.loc, "只能访问 record 或者 module 属性, 实际是: " + v);
        }
    }

    Value getRecordAttr(Attribute attr, RecordType rec) {
        String name = attr.attr.name;
        // let f = (r: record {a: Int}) => r.a // invokeUnCalled 时只有 type
        // f([a:1, b:"str"]) // 有 value
        Value v = rec.props.lookupLocal(name);
        if (v == null) {
            v = rec.props.lookupLocalType(name);
        }
        if (v != null) {
            return v;
        } else {
            throw Error.type(attr.loc, "record " + attr.value + " 中未定义属性 " + name);
        }
    }

    Value getModuleAttr(Attribute attr, ModuleType module) {
        String name = attr.attr.name;
        Value v = module.scope.lookupLocal(name);
        if (v != null) {
            return v;
        } else {
            throw Error.type(attr.loc, "module 中未定义属性 " + name);
        }
    }

    @Override
    public Value visit(@NotNull Subscript subscript, @NotNull Scope s) {
        Value t = getSubscript(subscript, s);
        return Voids.not(subscript.loc, t, "subscript 值不能为 Void");
    }

    Value getSubscript(Subscript subscript, Scope s) {
        Value idx = typecheck(subscript.index, s);
        if (isInt(idx)) {
            Value tv = typecheck(subscript.value, s);
            if (tv instanceof UnionType) {
                List<Value> types = new ArrayList<>();
                for (Value t : ((UnionType) tv).types) {
                    Value v = getSubscript(t, subscript, idx, s);
                    types.add(v);
                }
                return union(types);
            } else {
                return getSubscript(tv, subscript, idx, s);
            }
        } else {
            throw Error.type(subscript.index.loc, "下标必须为数字, 实际是 " + idx);
        }
    }

    Value getSubscript(Value tv, Subscript subscript, Value idx, Scope s) {
        if (tv instanceof TupleType) {
            return getTupleSubscript(subscript.index.loc, (TupleType) tv, idx, s);
        } else if (tv instanceof VectorType) {
            return getVectorSubscript(subscript.index.loc, (VectorType) tv, idx, s);
        } else {
            throw Error.type(subscript.value.loc, "下标只能访问 tuple 或者 vector, 实际是 " + tv);
        }
    }

    Value getTupleSubscript(Location loc, TupleType tuple, Value idx, Scope s) {
        if (isLiteral(idx)) {
            return tuple.get(loc, Math.toIntExact(litInt(idx)));
        } else if (tuple.size() == 0) {
            throw Error.type(loc, "() 不能内没有元素");
        } else {
            return tuple.eltType();
        }
    }

    Value getVectorSubscript(Location loc, VectorType vec, Value idx, Scope s) {
        if (isLiteral(idx)) {
            return vec.get(loc, Math.toIntExact(litInt(idx)));
        } else {
            return vec.eltType;
        }
    }

    @Override
    public Value visit(@NotNull FunDef fun, @NotNull Scope s) {
        Scope props = Declares.typecheck(this, fun.declare, s);
        FunType funType = FunType(fun, props, s);

        if (fun.name != null) {
            String name = fun.name.name;
            Matchers.reDefineCheck(fun.name, s);
            s.putValue(name, funType);
            s.put(name, KEY_MUT, FALSE);
        }

        // 声明时候先不处理, 如果该函数最终没有被调用到, 或者被传递或者复制
        // 结尾 uncalled 会统一处理
        funType.fillParamRetType = () -> fillParamAndRetType(funType, s);

        uncalled.add(funType);
        return funType;
    }

    void fillParamAndRetType(FunType ft, Scope s) {
        // 先调用一次, 获取参数与返回值类型
        // TODO: 这里是万恶之源 !!!, 尝试移除
        invokeUncalled(ft, s, false);
    }

    @Override
    public Value visit(@NotNull Unary unary, @NotNull Scope s) {
        Value op = s.lookup(unary.operator.name);
        if (op == null) {
            throw Error.runtime(unary.loc, "操作符 " + unary.operator.name + " 未定义");
        } else if (op instanceof PrimFun) {
            PrimFun prim = (PrimFun) op;
            // Value type = typecheck(unary.arg, s);
            // notVoid(unary.arg.loc, type, "unary.arg 不能为 Void");
            CheckedPrimArgs args = new CheckedPrimArgs(this, s, Helper.lists(unary.arg));
            return prim.apply(unary.loc, args);
        } else if (op instanceof FunType) {
            FunType fun = (FunType) op;
            uncalled.remove(fun);
            Scope funScope = new Scope(fun.env);
            callFunWithPositional(funScope, unary.loc, fun, Helper.lists(unary.arg), s);
            return callFunReturn(unary.loc, fun, funScope);
        } else {
            throw Error.runtime(unary.loc, "操作符 " + unary.operator.name + " 必须为 Fun");
        }
    }

    @Override
    public Value visit(@NotNull Binary binary, @NotNull Scope s) {
        Value op = s.lookup(binary.operator.name);
        if (op == null) {
            throw Error.runtime(binary.loc, "操作符fun " + binary.operator.name + " 未定义");
        } else if (op instanceof PrimFun) {
            PrimFun prim = (PrimFun) op;
            // Value ltype = typecheck(binary.lhs, s);
            // Value rtype = typecheck(binary.rhs, s);
            // notVoid(binary.lhs.loc, ltype, "binary.lhs 不能为 Void");
            // notVoid(binary.rhs.loc, rtype, "binary.rhs 不能为 Void");
            CheckedPrimArgs args = new CheckedPrimArgs(this, s, Helper.lists(binary.lhs, binary.rhs));
            return prim.apply(binary.loc, args);
        } else if (op instanceof FunType) {
            FunType fun = (FunType) op;
            uncalled.remove(fun);
            Scope funScope = new Scope(fun.env);
            callFunWithPositional(funScope, binary.loc, fun, Helper.lists(binary.lhs, binary.rhs), s);
            return callFunReturn(binary.loc, fun, funScope);
        } else {
            throw Error.runtime(binary.loc, "操作符 " + binary.operator.name + " 必须为 Fun");
        }
    }

    // todo interp 处理 selfType
    @Override
    public Value visit(@NotNull Call call, @NotNull Scope s) {
        Value callee;
        Value selfType = null;
        if (call.callee instanceof Attribute) {
            // todo test, 这里的 selfType 暂时没用, 可以替代 Closure 里头的 bind, but 会导致 self 变成动态的玩意...
            // a.b.c.f()
            Attribute attr = ((Attribute) call.callee);
            Value targetType = typecheck(attr.value, s);
            // 只有 record 才绑定 self
            if (targetType instanceof RecordType) {
                selfType = targetType;
            }
            callee = getAttr(attr, targetType);
        } else {
            callee = typecheck(call.callee, s);
        }

        // todo 这里有个潜在的问题, args 被typecheck多次!!!!
        if (callee instanceof UnionType) {
            List<Value> types = new ArrayList<>();
            for (Value t1 : ((UnionType) callee).types) {
                Value v = call(call, t1, selfType, s);
                types.add(v);
            }
            return union(types);
        } else {
            return call(call, callee, selfType, s);
        }
    }

    Value call(Call call, Value callee, Value self, Scope s) {
        if (callee instanceof FunType) {
            return callFunType(call.loc, ((FunType) callee), self, call.args, s);
        } else if (callee instanceof RecordType) {
            return callRecord(call.loc, ((RecordType) callee), call.args, s);
        } else if (callee instanceof PrimFun) {
            return callPrimFun(call.loc, ((PrimFun) callee), call.args, s);
        } else {
            throw Error.type(call.loc, "不支持的调用类型: " + call.callee + " 不是 fun 或者 record");
        }
    }

    Value callFunType(Location loc, FunType fun, Value self, Arguments args, Scope s) {
        uncalled.remove(fun);

        Scope funScope = new Scope(fun.env);
        if (!args.positional.isEmpty() && args.keywords.isEmpty()) {
            callFunWithPositional(funScope, loc, fun, args.positional, s);
        } else {
            callFunWithKeyword(funScope, loc, fun, args.keywords, s);
        }

        return callFunReturn(loc, fun, funScope);
    }

    @Nullable Value calcParamType(Scope funScope, FunType fun, String name) {
        // 因为类型需要根据参数进行计算, 所以参数类型不能缓存, 返回值类型也一样
        // Value expected = funScope.lookupLocal(name);
        // 支持依赖类型, 参数类型重新计算
        Value expected;
        Object typeNode = fun.def.declare.props.lookupLocalProp(name, KEY_TYPE);
        if (typeNode == null) {
            expected = funScope.lookupLocal(name);
        } else {
            expected = typecheck(((Node) typeNode), funScope);
        }
        return expected;
    }

    void callFunWithPositional(Scope funScope, Location loc, FunType fun, List<Node> args, Scope s) {
        List<Id> params = fun.def.params;
        Declares.mergeType(funScope, fun.props); // 形参类型

        int actualSz = args.size();
        int expectedSz = params.size();
        if (actualSz != expectedSz) {
            throw Error.type(loc, "参数个数错误, 期望 " + expectedSz + ", 实际 " + actualSz);
        }
        for (int i = 0; i < args.size(); i++) {
            String name = params.get(i).name;
            Node arg = args.get(i);
            Value actual = typecheck(arg, s);
            Value expected = calcParamType(funScope, fun, name);
            if (expected == null/* || expected == UNKNOWN*/) {
                throw Error.type(arg.loc, "参数 " + name + " 必须声明类型");
                // 允许无类型声明的参数
//                emptyVectorAssert(arg.loc, actual);
//                funScope.putValue(name, actual);
//                paramTypes.add(actual);
            } else if (!subtype(actual, expected)) {
                throw Error.type(arg.loc, "类型错误，期望 " + expected + ", 实际 " + actual);
            } else {
                Voids.not(arg.loc, actual, "实参 " + name + " 不能为 Void");
                Voids.not(arg.loc, expected, "形参 " + name + " 不能为 Void");
                Object mut = funScope.lookupLocalProp(name, KEY_MUT);
                // 这里逻辑是 mut 用声明类型, 非 mut 用实际类型
                funScope.putValue(name, select(loc, mut == TRUE, expected, actual));
            }
        }
    }

    void callFunWithKeyword(Scope funScope, Location loc, FunType fun, Map<String, Node> args, Scope s) {
        List<Id> params = fun.def.params;
        checkKeywordArgs(loc, fun, args, params);
        Declares.mergeType(funScope, fun.props); // 形参类型

        for (Id param : params) {
            String name = param.name;
            Node arg = args.get(name);
            if (arg != null) {
                Value actual = typecheck(arg, s);
                Value expected = calcParamType(funScope, fun, name);
                if (expected == null/* || expected == UNKNOWN*/) {
                    throw Error.type(arg.loc, "参数 " + name + " 必须声明类型");
                    // 允许无类型声明的参数
//                    emptyVectorAssert(arg.loc, actual);
//                    funScope.putValue(name, actual);
//                    paramTypes.add(actual);
                } else if (!subtype(actual, expected)) {
                    throw Error.type(arg.loc, "类型错误，期望 " + expected + ", 实际 " + actual);
                } else {
                    Voids.not(arg.loc, actual, "实参 " + name + " 不能为 Void");
                    Voids.not(arg.loc, expected, "形参 " + name + " 不能为 Void");
                    Object mut = funScope.lookupLocalProp(name, KEY_MUT);
                    funScope.putValue(name, select(loc, mut == TRUE, expected, actual));
                }
            } else {
                assert funScope.lookupLocal(name) != null;
            }
        }
    }

    void checkKeywordArgs(Location loc, FunType fun, Map<String, Node> args, List<Id> params) {
        List<String> seen = new ArrayList<>(params.size());
        List<String> missing = new ArrayList<>(0);

        for (Id param : params) {
            String name = param.name;
            Node arg = args.get(name);
            if (arg == null) {
                Value defVal = fun.props.lookupLocalDefault(param.name);
                if (defVal == null) {
                    missing.add(name);
                }
            } else {
                seen.add(name);
            }
        }
        if (!missing.isEmpty()) {
            throw Error.type(loc, "缺失参数 " + missing);
        }
        List<String> extra = new ArrayList<>();
        for (String id : args.keySet()) {
            if (!seen.contains(id)) {
                extra.add(id);
            }
        }
        if (!extra.isEmpty()) {
            throw Error.type(loc, "多余的参数: " + extra);
        }
    }

    Value declaredFunRetType(Location loc, FunType fun, Scope funScope) {
        // 注意，这里不能用 lookupLocalType , 因为不是 value 是 node
        // Object retTypeNode = fun.props.lookupLocalType(ID_RETURN);
        Object retTypeNode = fun.props.lookupLocalProp(ID_RETURN, KEY_TYPE);
        if (!(retTypeNode instanceof Node)) {
            throw Error.bug(loc, "返回类型错误");
        } else {
            return typecheck(((Node) retTypeNode), funScope);
        }
    }

    Value callFunReturn(Location loc, FunType fun, Scope funScope) {
        return callFunReturn(loc, fun, funScope, true);
    }

    Value callFunReturn(Location loc, FunType fun, Scope funScope, boolean checkRet) {
        // 注意，这里不能用 lookupLocalType , 因为不是 value 是 node
        // Object retTypeNode = fun.props.lookupLocalType(ID_RETURN);
        Object retTypeNode = fun.props.lookupLocalProp(ID_RETURN, KEY_TYPE);
        if (retTypeNode != null) {
            // 原来的策略
            // 有声明直接用声明
            // 这里就是 retTypeNode 为啥保留 node 的原因, 可能依赖参数的类型, 要等到参数的类型确定之后
            // return typecheck(((Node) retTypeNode), funScope);

            // 现在的策略, 如果是递归函数直接取声明类型
            // 如果不是递归函数, 检查 body 的类型和声明类型
            // todo: 是否需要复制
            // 返回值是每次现场计算的
            Value declaredRetType = declaredFunRetType(loc, fun, funScope);
            if (callStack.contains(fun)) {
                // 递归不执行 body 直接取声明, 保证停机
                return declaredRetType;
            } else if (!checkRet) {
                return declaredRetType;
            } else {
                callStack.add(fun);
                Value actual;
                try {
                    actual = typecheck(fun.def.body, funScope);
                } finally {
                    callStack.remove(fun);
                }

                if (subtype(actual, declaredRetType/*, true*/)) {
                    // let f = (): (Int|Str)[] => []
                    // append(f(), 1)
                    // append(f(), "str")
                    return select(loc, declaredRetType, actual);
                } else {
                    throw Error.type(fun.def.loc, "返回值类型错误, 声明 " + declaredRetType + ", 实际是 " + actual);
                }
            }
        } else {
            if (callStack.contains(fun)) {
                // 否则不会停机了...
                throw Error.type(loc, "递归函数必须声明返回值类型");
            } else {
                callStack.add(fun);
                Value actual;
                try {
                    actual = typecheck(fun.def.body, funScope);
                    emptyVectorAssert(loc, actual);
                } finally {
                    callStack.remove(fun);
                }
                return actual;
            }
        }
    }

    Value callRecord(Location loc, RecordType rec, Arguments args, Scope s) {
        Scope out = new Scope();
        Declares.mergeDefault(this, out, loc, rec.props);
        if (!args.positional.isEmpty() && args.keywords.isEmpty()) {
            callRecordWithPositional(out, loc, rec, args, s);
        } else {
            callRecordWithKeyword(out, loc, rec, args, s);
        }

        // todo: 想一下这里用不用深拷贝
        // RecordType 合并入实际 Value, 可以用是否有 value 区分
        // typecheck 阶段是 recordDef 产生的 recordType 还是 其实是recordValue
        // 后面对 record 字段的类型检查都基于实际 value 进行
        Scope props = rec.props.shadowCopy();
        for (String k : props.keySet()) {
            Value v = out.lookupLocal(k); // type 是声明类型, value 是实际类型
            assert v != null;
            props.putValue(k, v);
        }

        return RecordType(rec.name, rec.def, props, rec.env);
    }

    void callRecordWithPositional(Scope out, Location loc, RecordType rec, Arguments args, Scope s) {
        String[] propNames = rec.props.keySet().toArray(new String[0]);

        if (args.positional.size() > propNames.length) {
            throw Error.type(loc, "期望参数不超过 " + propNames.length + ", 实际参数 " + args.positional.size());
        }

        for (int i = 0; i < args.positional.size(); i++) {
            Node node = args.positional.get(i);
            String name = propNames[i];
            initRecordField(out, node, name, rec, s);
        }

        for (String name : propNames) {
            if (out.lookupLocal(name) == null) {
                throw Error.type(loc, "字段 " + name + " 尚未初始化");
            }
        }
    }

    void callRecordWithKeyword(Scope out, Location loc, RecordType rec, Arguments args, Scope s) {
        Set<String> propNames = rec.props.keySet();
        for (Entry<String, Node> it : args.keywords.entrySet()) {
            String name = it.getKey();
            Node node = it.getValue();
            if (!propNames.contains(name)) {
                throw Error.type(args.keywords.get(name).loc, "多余的 keyword 参数");
            }
            initRecordField(out, node, name, rec, s);
        }
        for (String name : propNames) {
            if (out.lookupLocal(name) == null) {
                throw Error.type(loc, "字段 " + name + " 尚未初始化");
            }
        }
    }

    void initRecordField(Scope out, Node arg, String name, RecordType rec, Scope s) {
        Value actual = typecheck(arg, s);
        initRecordField1(arg.loc, out, actual, name, rec.props);
    }

    void initRecordField1(Location loc, Scope out, Value actual, String name, Scope props) {
        Value expected = props.lookupLocalType(name);
        // 字面量会填充类型, record 会强制声明类型
        assert expected != null;
        // 没声明类型取默认值的类型
//        if (expected == null || expected == UNKNOWN) {
//            emptyVectorAssert(loc, actual);
//            expected = actual;
//        } else {
            if (!subtype(actual, expected)) {
                throw Error.type(loc, "类型错误，期望 " + expected + ", 实际 " + actual);
            }
//        }
        Voids.not(loc, actual, "record 参数不能为 Void");
        Voids.not(loc, expected, "record 预期属性不能为 Void");
        Object mut = props.lookupLocalProp(name, KEY_MUT);

        // let o = record { mut a: (Str, Float) = ("s", 1) } ()
        // o.a = ("s", 3.14)
        out.putValue(name, select(loc, mut == TRUE, expected, actual));
    }

    Value callPrimFun(Location loc, PrimFun primFun, Arguments args, Scope s) {
        Arity expected = primFun.arity;
        int actual = args.positional.size();
        expected.validate(loc, actual);
        // List<Value> argTypes = typecheckList(args.positional, s);
        // for (Value argType : argTypes) {
        //     notVoid(loc, argType, "不能为 Void");
        // }
        CheckedPrimArgs primArgs = new CheckedPrimArgs(this, s, args.positional);
        return primFun.apply(loc, primArgs);
    }

    @Override
    public Value visit(@NotNull Block block, @NotNull Scope s) {
        return typecheckBlock(block, new Scope(s));
    }

    Value typecheckBlock(Block block, Scope s) {
        boolean returned = false;
        Value retType = Type.VOID;
        List<Node> seq = block.stmts;
        for (Node stmt : seq) {
            if (returned) {
                throw Error.type(stmt.loc, "已经 return, 发现不可达代码");
            }

            Value t = typecheck(stmt, s);
            if (stmt == seq.get(seq.size() - 1)) {
                // retType = UnionType.remove(t, VOID);
                retType = t;
            } else if (stmt instanceof Return) {
                returned = true;
            }
        }
        return retType;
    }

    @Override
    public Value visit(@NotNull If if1, @NotNull Scope s) {
        Value tv = typecheck(if1.test, s);
        if (isBool(tv)) {
            Value t1 = typecheck(if1.then, s);
            Value t2;
            // 两个分支无条件类型检查...
            if (if1.orelse == null) {
                t2 = Type.VOID;
            } else {
                t2 = typecheck(if1.orelse, s);
            }


            if (isLitTrue(tv)) {
                return t1;
            } else if (isLitFalse(tv)) {
                return t2;
            } else {
                return union(t1, t2);
            }
        } else {
            throw Error.type(if1.test.loc, "if 条件必须是 bool 类型");
        }
    }

    // 1. 先执行匹配 2. 在执行绑定 3. 执行 guard 4. 执行 body
    @Override
    public Value visit(@NotNull MatchPattern match, @NotNull Scope s) {
        Value t = typecheck(match.value, s);
        Evaluator<Value, Boolean> patternMatcher = Matchers.checkPatternMatcher(this, s);

        boolean hit = false;
        List<Value> types = new ArrayList<>();
        for (CaseMatch cm : match.cases) {
            Node ptn = cm.pattern;
            // 只匹配类型符合, 所以可以 match 多个分支
            if (patternMatcher.eval(ptn, t)) {
                hit = true;
                Scope s1 = new Scope(s);
                if (ptn instanceof Define) {
                    typeMatchDefine(s, ptn, s1);
                } else if (ptn instanceof TupleLiteral) {
                    typeMatchTuple(s, t, ptn, s1);
                } else {
                    Matchers.checkTryDefine(this, s1, ptn, t); // 不是所有 pattern 都能 define 的
                }

                boolean never = false;
                if (cm.guard != null) {
                    Value guard = typecheck(cm.guard, s1);
                    if (!isBool(guard)) {
                        throw Error.type(cm.guard.loc, "卫语句必须返回 Bool, 实际是 " + guard);
                    }
                    if (isLitFalse(guard)) {
                        never = true;
                    }
                }

                // interp Match Then
                Value v = typecheckBlock(cm.body, s1);
                if (!never) {
                    types.add(v);
                }
            }
        }

        if (hit) {
            return union(types);
        } else {
            // throw Error.type(match.loc, "未匹配到任何 case");
            return Type.VOID;
        }
    }

    Value castType(@NotNull Scope s, Define def) {
        Node castNode = def.type;
        assert castNode != null;
        return typecheck(castNode, s);
    }

    void typeMatchDefine(@NotNull Scope s, Node ptn, Scope s1) {
        Value castT = castType(s, ((Define) ptn));
        Matchers.checkTryDefine(this, s1, ptn, castT); // 不是所有 pattern 都能 define 的
    }

    void typeMatchTuple(@NotNull Scope s, Value t, Node ptn, Scope s1) {
        TupleLiteral tl = (TupleLiteral) ptn;
        if (Voids.contains(t)) {
            throw Error.type(ptn.loc, "不能 bind Void");
        }
        if (t instanceof TupleType) {
            int elSz = tl.elements.size();
            List<Value> vals = new ArrayList<>(elSz);
            for (Node el : tl.elements) {
                if (el instanceof Define) {
                    vals.add(castType(s, ((Define) el)));
                }
            }
            if (vals.size() == 0) {
                Matchers.checkTryDefine(this, s1, ptn, t);
            } else if ( vals.size() == elSz) {
                Matchers.checkTryDefine(this, s1, ptn, TupleType(vals));
            } else {
                throw Error.type(ptn.loc, "tuple pattern 必须 (a, b, ..) 或者 (a: T, b: T, ...) 形式");
            }
        } else {
            throw Error.type(ptn.loc, "绑定类型不匹配: 期望 tuple, 实际是 " + t);
        }
    }

    @Override
    public Value visit(@NotNull While while1, @NotNull Scope s) {
        Value tv = typecheck(while1.test, s);
        if (isBool(tv)) {
            // todo 想一下 while 的语义
            // todo 测试 while(true) { return 1 \n "hello" }
            if (isLitTrue(tv)) {
                return Type.VOID;
                // todo while true while1.body 没有 break return 推导成 Never 类型
                // return NEVER;
            } else if (isLitFalse(tv)) {
                return Type.VOID;
            } else {
                return typecheck(while1.body, s);
            }
        } else {
            throw Error.type(while1.test.loc, "while 条件必须是 bool 类型");
        }
    }

    @Override
    public Value visit(@NotNull Continue cont, @NotNull Scope s) {
        throw Error.type(cont.loc, "不支持 cont");
    }

    @Override
    public Value visit(@NotNull Break brk, @NotNull Scope s) {
        throw Error.type(brk.loc, "不支持 break");
    }

    @Override
    public Value visit(@NotNull Return ret, @NotNull Scope s) {
        if (ret.expr == null) {
            return Type.VOID;
        } else {
            Value v = typecheck(ret.expr, s);
            return Voids.not(ret.expr.loc, v, "不能 return Void");
        }
    }

    @Override
    public Value visit(@NotNull Module module, @NotNull Scope s) {
        Scope blockS = new Scope(s);
        typecheckBlock(module.block, blockS);
        ModuleType moduleT = ModuleType(module, blockS.shadowCopy());
        if (module.name != null) {
            s.putValue(module.name.name, moduleT);
        }
        return moduleT;
    }

    @Override
    public Value visit(@NotNull Import import1, @NotNull Scope s) {
        Value v = typecheck(import1.from, s);
        if (!(v instanceof ModuleType)) {
            throw Error.type(import1.from.loc, "只能从 module 中导入符号, 实际是 " + v);
        }

        ModuleType module = (ModuleType) v;
        Location starLoc = null;
        Set<String> imported = new HashSet<>();

        for (Pair<Id, Id> it : import1.aliasMap) {
            Id key = it.fst;
            Id val = it.snd;
            if (key.name.equals(IMPORT_STAR)) {
                starLoc = key.loc;
            } else {
                importOne(key.loc, module, key.name, val.name, s);
                imported.add(key.name);
            }
        }

        // import * 时候可以有个别的别名, 所以分两步处理
        if (starLoc != null) {
            for (String name : module.scope.keySet()) {
                if (!imported.contains(name)) {
                    importOne(starLoc, module, name, name, s);
                }
            }
        }
        return Type.VOID;
    }

    // 数组声明
    @Override
    public Value visit(@NotNull VectorOf vectorOf, @NotNull Scope s) {
        Value elemT = typecheck(vectorOf.elem, s);
        return VectorType(elemT);
    }

    void importOne(Location loc, ModuleType module, String name, String alias, Scope s) {
        Map<String, Object> allProps = module.scope.lookupAllProps(name);
        if (allProps == null) {
            throw Error.type(loc, "module 中 属性不存在 " + name);
        }

        Value existing = s.lookupLocal(alias);
        if (existing != null) {
            throw Error.type(loc, "import 名称冲突 " + name);
        }

        s.putAllProps(alias, allProps);
    }

    @Override
    public Value visit(@NotNull Assert assert1, @NotNull Scope s) {
        Value cond = typecheck(assert1.expr, s);
        if (isBool(cond)) {
            if (assert1.msg != null) {
                Value msg = typecheck(assert1.msg, s);
                if (!(msg instanceof StringType)) {
                    throw Error.type(assert1.loc, "断言信息必须为 " + ID_STR + ", 实际是 " + msg);
                }
            }
            if (isLitFalse(cond)) {
                // todo 这个开启好多测试没法写, 弄个开关??
                // throw Error.type(assert1.loc, "断言注定失败😭");
            }
        } else {
            throw Error.type(assert1.loc, "断言表达式必须为 " + ID_BOOL + ", 实际是 " + cond);
        }
        return Type.VOID;
    }

    @Override
    public Value visit(@NotNull Operator operator, @NotNull Scope s) {
        return Type.VOID;
    }

    @Override
    public Value visit(@NotNull Debugger debugger, @NotNull Scope s) {
        // 这里 debugger 用作 typecheck 的调试, 用来断言一定失败的类型检查
        if (debugger.expr != null) {
            try {
                typecheck(debugger.expr, s);
                throw Error.bug(debugger.loc, "期望类型检查失败: " + debugger.expr);
            } catch (Error.Type e) {
                Helper.err(e.getMessage());
            }
        }
        return Type.VOID;
    }

    static class Declares {

        static Scope typecheck(TypeChecker typechecker, Declare declare, Scope s) {
            Scope evaluated = new Scope(s);


            for (String name : declare.props.keySet()) {
                Map<String, Object> props = declare.props.lookupAllProps(name);
                assert props != null;

                if (name.equals(ID_RETURN)) {
                    // 返回值类型可能依赖参数的类型, 所以这里不 eval
                    // 需要将 Node延迟到等到参数类型绑定好之后再 eval
                    evaluated.putProps(name, props);
                } else {
                    for (Entry<String, Object> it : props.entrySet()) {
                        String propK = it.getKey();
                        Object propV = it.getValue();
                        if (!(propV instanceof Node)) {
                            throw Error.bug(declare.loc, "声明值必须是 Node");
                        }

                        if (propK.equals(KEY_MUT)) {
                            if (!(propV instanceof Id)) {
                                throw Error.bug(declare.loc, "mut 声明必须是 Id");
                            }

                            String kn = ((Id) propV).name;
                            if (kn.equals(ID_TRUE)) {
                                evaluated.put(name, propK, TRUE);
                            } else if (kn.equals(ID_FALSE)) {
                                evaluated.put(name, propK, FALSE);
                            } else {
                                throw Error.bug(declare.loc, "mut 声明值必须是 true false, 实际是 " + kn);
                            }
                        } else {
                            // ! 这里改成 evaluated 而不是 s 作为作用域, 可以让参数可以依赖之前参数
                            // e.g.
                            // let validate = (a: Bool) => if a { Int } else { Str }
                            // fun answer(a: Bool, b: validate(a)) = b
                            // fundef 要执行 validate(a) scope s 里头没有a, 所以这里必须特殊处理作用域
                            //  当然如果外部作用域有同名变量, 会被遮盖, 副作用无效, 因为解释阶段忽略, 但会导致类型检查异常
                            //  所以类型检查的函数不能写副作用的逻辑...
                            Value v = typechecker.typecheck(((Node) propV), evaluated/*s*/);
                            Voids.not(declare.loc, v, "属性 " + propK + " 不能为 Void");
                            evaluated.put(name, propK, v);
                        }
                    }
                }
            }

            return evaluated.shadowCopy();
        }

        // 处理 record 默认值
        static void mergeDefault(TypeChecker typechecker, Scope out, Location loc, Scope props) {
            for (String name : props.keySet()) {
                Object mutable = props.lookupLocalProp(name, KEY_MUT);
                if (mutable == null) {
                    out.put(name, KEY_MUT, FALSE);
                    //noinspection UnnecessaryContinue
                    continue;
                } else if (mutable == TRUE || mutable == FALSE) {
                    out.put(name, KEY_MUT, mutable);
                } else {
                    throw Error.bug(KEY_MUT + " 类型错误");
                }
            }

            for (String name : props.keySet()) {
                Value defaultVal = props.lookupLocalDefault(name);
                if (defaultVal == null) {
                    //noinspection UnnecessaryContinue
                    continue;
                } else {
                    Value existedVal = out.lookup(name);
                    assert existedVal == null;
                    // 默认值也要进行类型检查
                    // copy 防止共享 let f = (a = [1,2,4]) => IO.println(Vectors.append(a, 1))  f() f()
                    Value actual = copyType(defaultVal);
                    typechecker.initRecordField1(loc, out, actual, name, props);
                }
            }
        }

        // 处理函数形参, 添加 value 和 mut
        static void mergeType(Scope funScope, Scope props) {
            for (String name : props.keySet()) {
                if (name.equals(ID_RETURN)) {
                    continue;
                }
                // 参数类型, 没有取默认值
                Value t = propType(props, name);
                Value existedVal = funScope.lookupLocal(name);
                assert existedVal == null;
                // if (t != null) {
                funScope.putValue(name, copyType(t));
                // }
                Object mut = props.lookupLocalProp(name, KEY_MUT);
                // 处理参数 mut
                if (mut != null) {
                    assert mut instanceof Value;
                    funScope.put(name, KEY_MUT, mut);
                }
            }
        }

        static @NotNull Value propType(Scope props, String name) {
            Value type = props.lookupLocalType(name);
            // 没声明, 用默认值的类型替代
            if (type == null) {
                Value defVal = props.lookupLocalDefault(name);
                if (defVal != null) {
                    type = copyType(defVal);
                }
            }
            // 连默认值都没有, 先标记未知
            if (type == null) {
                // return UNKNOWN;
                // null;
                return ANY; // 这里最终决定没写就 Any, 否则自己写
            } else {
                return type;
            }
        }

    }

    // -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=

    public static void emptyVectorAssert(Location loc, Value value) {
        if (isEmptyVectorLiteral(value)) {
            throw Error.type(loc, "不支持无类型空数组, 没法推导类型");
        }
    }

    public static BoolValue boolAssert(Location loc, Object value) {
        if (value != TRUE && value != FALSE) {
            throw Error.type(loc, "期望 bool 类型, 实际是 " + value);
        } else {
            return ((BoolValue) value);
        }
    }



    public static Value literalWrapIfNecessary(boolean mut, @NotNull Value t) {
        if (!mut && isPrimLiteral(t)) {
            return Type.LiteralType(t);
        } else {
            return t;
        }
    }

    public static Value select(Location loc, Value expected, Value actual) {
        return select(loc, false, expected, actual);
    }

    public static Value select(Location loc, boolean mut, Value expected, Value actual) {
        if (isEmptyVectorLiteral(actual)) {
            // 空数组字面量用声明类型
            if (/*expected == UNKNOWN || */isEmptyVectorLiteral(expected)) {
                throw Error.type(loc, "无法推断类型");
            }
            expected = hackExpected(expected, actual);
            return expected;
        }
        if (mut) {
            expected = hackExpected(expected, actual);
            // 是不是所有的 mut 都需要清除 literal
            // fun req(method: "GET"|"POST") {}
            // let conf = [mut method: "GET"] // 这里 mut 的 method 应该被推导为 Str, 因为可能被修改
            // req(conf.method) // 这里不应该检查通过
            expected = Literal.clear(expected);
            return expected;
        } else {
            actual = hackActual(expected, actual);
            return actual;
//        if (expected instanceof UnionType) {

//            return expected;
//        } else {
            // actual = hackActual(expected, actual);
//            return actual;
//        }
        }
    }

    public static Value hackActual(Value expected, Value actual) {
        if (expected == actual) {
            // 处理 record { a = [1] } () 处理 a 的类型
            // let a = [1] 不处理这种情况, 未声明类型, 无条件按照 rhs 类型处理 lhs
            return actual;
        }
        if (actual instanceof VectorType && expected instanceof VectorType) {
            Value acEltType = ((VectorType) actual).eltType;
            Value exEltType = ((VectorType) expected).eltType;
            VectorValue acPositional = ((VectorType) actual).positional;

            if (acPositional != null) {
                if (acEltType == UNKNOWN) {
                    assert exEltType != UNKNOWN;
                    // let a: (Int|Str)[][] = []
                    // append(a, [])
                    actual = VectorType(exEltType, acPositional.values);
                }
            } else {
                // 返回值, ... fun f(T): T[] = T[]
                // throw Error.bug("actual positional == null");
            }
        }
        return actual;
    }

    public static Value hackExpected(Value expected, Value actual) {
        if (expected == actual) {
            return actual;
        }
        // 配合空数组既是所有数组子类又是所有数组超类
        if (expected instanceof VectorType && actual instanceof VectorType) {
            // 给 expected 加入实际类型的 positional, 处理 更新 positional 和 size
            Value exEltType = ((VectorType) expected).eltType;
            VectorValue acPositional = ((VectorType) actual).positional;
            assert exEltType != UNKNOWN;
            if (acPositional != null) {
                // let a: Int[] = []
                expected = VectorType(exEltType, acPositional.values);
            }
        }
        return expected;
    }

}
