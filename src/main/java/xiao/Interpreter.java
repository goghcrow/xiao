package xiao;

import org.jetbrains.annotations.NotNull;
import xiao.front.Ast;
import xiao.front.Literals;
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

/**
 * @author chuxiaofeng
 */
public class Interpreter implements Visitor<Scope, Value>, Evaluator<Scope, Value> {

    int stackOverThreshold = 256;
    CallStack stack = new CallStack(stackOverThreshold);

    AssertInterpreter assertInterp;
    boolean assertEnabled;

    Interpreter() {
        this(true);
    }

    Interpreter(boolean assertEnabled) {
        this.assertEnabled = assertEnabled;
        if (assertEnabled) {
            assertInterp = new AssertInterpreter(this);
        }
    }

    @Override
    public Value eval(@NotNull Node n, @NotNull Scope s) {
        return interp(n, s);
    }

    Value interp(@NotNull Node node, @NotNull Scope s) {
        if (assertEnabled) {
            return assertInterp.interp(node, s);
        } else {
            return visit(node, s);
        }
    }

    List<Value> interpList(@NotNull List<Node> nodes, @NotNull Scope s) {
        List<Value> vals = new ArrayList<>(nodes.size());
        for (Node n : nodes) {
            Value evaluated = interp(n, s);
            vals.add(evaluated);
        }
        return vals;
    }

//    @Override
//    public Value visit(@NotNull Group group, @NotNull Scope s) {
//        return interp(group.expr, s);
//    }

    @Override
    public Value visit(@NotNull Id id, @NotNull Scope s) {
        // 类型检查过变量一定定义过
        return s.lookup(id.name);
    }

    @Override
    public Value visit(@NotNull IntNum intNum, @NotNull Scope s) {
        return Int(intNum.value);
    }

    @Override
    public Value visit(@NotNull FloatNum floatNum, @NotNull Scope s) {
        return Value.Float(floatNum.value);
    }

    @Override
    public Value visit(@NotNull Str str, @NotNull Scope s) {
        String v = Literals.unescapeStr(str.loc, str.value, '"');
        return Value.Str(v);
    }

    @Override
    public Value visit(@NotNull TupleLiteral tuple, @NotNull Scope s) {
        List<Value> vals = interpList(tuple.elements, s);
        return Tuple(vals);
    }

    @Override
    public Value visit(@NotNull VectorLiteral tuple, @NotNull Scope s) {
        List<Value> vals = interpList(tuple.elements, s);
        return Vector(vals);
    }

    @Override
    public Value visit(@NotNull RecordLiteral literal, @NotNull Scope s) {
        Scope declareProps = new Scope();

        for (String name : literal.map.keySet()) {
            declareProps.put(name, KEY_DEFAULT, literal.node(name));
            // interp 阶段 mut 没用
            // record 字面量属性默认都是不可修改的
            // declareProps.put(name, KEY_MUT, id(Location.None, literal.mut(name) ? ID_TRUE : ID_FALSE));
        }

        Declare declare = Ast.declare(literal.loc, declareProps);
        // 重复利用 Declares.interp, interpCallRecord 会合并默认值
        Scope props = Declares.interp(this, declare, s);
        RecordType rec = RecordType(null, literal, props, s);
        return callRecord(literal.loc, rec, Ast.emptyArgs, s);
    }

    @Override
    public Value visit(@NotNull Unary unary, @NotNull Scope s) {
        Value op = s.lookup(unary.operator.name);
        assert op != null; // typechecker 保证

        if (op instanceof PrimFun) {
            // Value val = interp(unary.arg, s);
            return ((PrimFun) op).apply(unary.loc, new PrimArgs(this, s, Helper.lists(unary.arg)));
        } else if (op instanceof Closure) {
            Value val = interp(unary.arg, s);
            Closure closure = (Closure) op;
            Scope env = closure.env;
            Scope funScope = new Scope(env);
            List<Id> params = closure.fun.params;
            funScope.putValue(params.get(0).name, val);
            return interp(closure.fun.body, funScope);
        } else {
            throw Error.bug(unary.loc);
        }
    }

    @Override
    public Value visit(@NotNull Binary binary, @NotNull Scope s) {
        Value op = s.lookup(binary.operator.name);
        assert op != null; // typechecker 保证

        if (op instanceof PrimFun) {
            // Value lval = interp(binary.lhs, s);
            // Value rval = interp(binary.rhs, s);
            PrimArgs args = new PrimArgs(this, s, Helper.lists(binary.lhs, binary.rhs));
            return ((PrimFun) op).apply(binary.loc, args);
        } else if (op instanceof Closure) {
            Value lval = interp(binary.lhs, s);
            Value rval = interp(binary.rhs, s);

            Closure closure = (Closure) op;
            Scope env = closure.env;
            Scope funScope = new Scope(env);
            List<Id> params = closure.fun.params;
            funScope.putValue(params.get(0).name, lval);
            funScope.putValue(params.get(1).name, rval);
            return interp(closure.fun.body, funScope);
        } else {
            throw Error.bug(binary.loc);
        }
    }

    @Override
    public Value visit(@NotNull LitType lit, @NotNull Scope s) {
        return interp(lit.node, s);
    }

    @Override
    public Value visit(@NotNull Typedef def, @NotNull Scope s) {
        if (def.rec) {
            typerec(def, s);
        } else {
            typedef(def, s);
        }
        return Value.VOID;
    }

    void typedef(Typedef def, Scope s) {
        Value type = interp(def.type, s);
        String name = ((Id) def.pattern).name;
        // interp 阶段 mut 没用
        // s.put(name, KEY_MUT, FALSE);
        s.putValue(name, type);
        // s.putType(name, type);
    }

    void typerec(Typedef def, Scope s) {
        TupleLiteral tuple = (TupleLiteral) def.pattern;
        TupleLiteral val = (TupleLiteral) def.type;
        assert val != null;

        List<Node> lhs = tuple.elements;
        List<Node> rhs = val.elements;

        for (Node el : lhs) {
            Id id = (Id) el;
            s.putValue(id.name, new Undefined(() -> s.lookupLocal(id.name)));
        }

        for (int i = 0; i < rhs.size(); i++) {
            Value type = interp(rhs.get(i), s);
            String name = ((Id) lhs.get(i)).name;
            // interp 阶段 mut 没用
            // s.put(name, KEY_MUT, FALSE);
            s.putValue(name, type);
            // s.putType(name, type);
        }

        fixRec(def.loc, lhs, s);
    }

    @Override
    public Value visit(@NotNull Define var, @NotNull Scope s) {
        if (var.rec) {
            letrec(var, s);
        } else {
            let(var, s);
        }
        return Value.VOID;
    }

    void letrec(@NotNull Define var, @NotNull Scope s) {
        TupleLiteral tuple = (TupleLiteral) var.pattern;
        TupleLiteral val = (TupleLiteral) var.value;
        assert val != null;

        List<Node> lhs = tuple.elements;
        List<Node> rhs = val.elements;

        for (Node el : lhs) {
            Id id = (Id) el;
            s.putValue(id.name, new Undefined(() -> s.lookupLocal(id.name)));
        }

        for (int i = 0; i < rhs.size(); i++) {
            Value v = interp(rhs.get(i), s);
            Matchers.define(this, s, lhs.get(i), v, var.mut);
        }

        fixRec(var.loc, lhs, s);
    }

    void fixRec(Location loc, List<Node> lhs, Scope s) {
        for (Node el : lhs) {
            Id id = (Id) el;
            Value v = s.lookupLocal(id.name);
            Value fixedV = LetRecFixer.fixValue(loc, v);
            s.putValue(id.name, fixedV);
        }
    }

    void let(@NotNull Define var, @NotNull Scope s) {
        assert var.value != null;
        Value v = interp(var.value, s);
        // new Binder(this, s).define(var.pattern, v, var.mut);
        Matchers.define(this, s, var.pattern, v, var.mut);
    }

    @Override
    public Value visit(@NotNull Assign assign, @NotNull Scope s) {
        Value v = interp(assign.value, s);
        // new Binder(this, s).assign(assign.pattern, v);
        Matchers.assign(this, s, assign.pattern, v);
        return Value.VOID;
    }

    @Override
    public Value visit(@NotNull Subscript subscript, @NotNull Scope s) {
        return getSubscript(subscript, s);
    }

    Value getSubscript(Subscript subscript, Scope s) {
        Value v = interp(subscript.value, s);
        if (v instanceof TupleValue) {
            TupleValue tuple = ((TupleValue) v);
            Value idx = interp(subscript.index, s);
            int idx1 = ((IntValue) idx).intValue();
            return tuple.get(subscript.loc, idx1);
        } else {
            VectorValue vec = ((VectorValue) v);
            Value idx = interp(subscript.index, s);
            int idx1 = ((IntValue) idx).intValue();
            return vec.get(subscript.loc, idx1);
        }
    }

    @Override
    public Value visit(@NotNull Attribute attr, @NotNull Scope s) {
        Value av = interp(attr.value, s);
        if (av instanceof RecordValue) {
            return ((RecordValue) av).props.lookup(attr.attr.name);
        } else {
            return ((ModuleValue) av).scope.lookupLocal(attr.attr.name);
        }
    }

    @Override
    public Value visit(@NotNull FunDef fun, @NotNull Scope s) {
        Scope props = Declares.interp(this, fun.declare, s);
        Closure closure = Closure(fun, props, s);
        if (fun.name != null) {
            String name = fun.name.name;
            s.putValue(name, closure);
            // interp 阶段 mut 没用
            // s.put(name, KEY_MUT, FALSE);
        }
        return closure;
    }

    // 这里的结构可以处理继承属性冲突的, A extends B, C :  C 覆盖 B, A 覆盖 C
    // 但规则会引起歧义, 所以 typecheck 直接禁止属性冲突
    @Override
    public Value visit(@NotNull RecordDef record, @NotNull Scope s) {
        Scope props = new Scope();

        if (record.parents != null) {
            for (Id p : record.parents) {
                // 这里可以改成表达式, extends 动态决定, 但是会给 typecheck 带来一些麻烦
                RecordType parent = ((RecordType) interp(p, s));
                props.putAll(parent.props);
            }
        }

        RecordType type;
        if (record.name == null) {
            type = RecordType(null, record, props, s);
        } else {
            // 正常情况应该先创建好recordType再放到作用域
            // 这里要支持递归类型声明, 所以都先 put 进去, 然后再取出来
            String name = record.name.name;
            type = RecordType(name, record, props, s);
            s.putValue(name, type);
        }

        Scope selfProps = Declares.interp(this, record.declare, s);
        props.putAll(selfProps);
        return type;
    }

    @Override
    public Value visit(@NotNull Call call, @NotNull Scope s) {
        Value callee = interp(call.callee, s);
        try {
            if (callee instanceof Closure) {
                stack.push(new CallStack.Frame(call, callee, s));
                return callClosure(call.loc, ((Closure) callee), call.args, s);
            } else if (callee instanceof RecordType) {
                return callRecord(call.loc, ((RecordType) callee), call.args, s);
            } else if (callee instanceof PrimFun) {
                stack.push(new CallStack.Frame(call, callee, s));
                return callPrimFun(call.loc, ((PrimFun) callee), call.args, s);
            } else {
                throw Error.bug(call.loc, "不支持的调用类型");
            }
        } finally {
            if (callee instanceof Closure) {
                stack.pop();
            } else if (callee instanceof PrimFun) {
                stack.pop();
            }
        }
    }

    Value callClosure(Location loc, Closure closure, Arguments args, Scope s) {
        Scope env = closure.env;
        Scope funScope = new Scope(env);

        // 参数默认值
        Declares.mergeDefault(funScope, closure.props);

        List<Value> argVals = new ArrayList<>(args.positional.size());
        // 两种调用方式二选一
        List<Id> params = closure.fun.params;
        if (args.keywords.isEmpty()) {
            for (int i = 0; i < args.positional.size(); i++) {
                Value argVal = interp(args.positional.get(i), s);
                funScope.putValue(params.get(i).name, argVal);
                argVals.add(argVal);
            }
        } else {
            for (Id param : params) {
                Node arg = args.keywords.get(param.name);
                if (arg != null) {
                    Value argVal = interp(arg, s);
                    funScope.putValue(param.name, argVal);
                    argVals.add(argVal);
                }
            }
        }

        stack.peek().args = argVals;

        try {
            return interp(closure.fun.body, funScope);
        } catch (ControlFlow.Return ret) {
            return ret.val == null ? Value.VOID : ret.val;
        }
    }

    Value callRecord(Location loc, RecordType rec, Arguments args, Scope s) {
        Scope props = new Scope();
        Declares.mergeDefault(props, rec.props);

        // todo test
        // 两种调用方式二选一
        if (!args.positional.isEmpty() && args.keywords.isEmpty()) {
            String[] propNames = rec.props.keySet().toArray(new String[0]);
            // Positional
            for (int i = 0; i < args.positional.size(); i++) {
                Node field = args.positional.get(i);
                String name = propNames[i];
                Value v = interp(field, s);
                props.putValue(name, v);
            }
        } else {
            for (String name : rec.props.keySet()) {
                Node field = args.keywords.get(name);
                if (field != null) {
                    Value v = interp(field, s);
                    props.putValue(name, v);
                }
            }
        }

        return Record(rec.name, rec, props);
    }

    Value callPrimFun(Location loc, PrimFun primFun, Arguments args, Scope s) {
        // primFun 因为不知道参数名字, 不能使用 keywords 方式调用
        // List<Value> argVals = interpList(args.positional, s);
        PrimArgs primArgs = new PrimArgs(this, s, args.positional);
        stack.peek().args = primArgs;
        return primFun.apply(loc, primArgs);
    }

    @Override
    public Value visit(@NotNull Block block, @NotNull Scope s) {
        return interpBlock(block, new Scope(s));
    }

    Value interpBlock(Block block, Scope s) {
        Value v = Value.VOID;
        for (Node stmt : block.stmts) {
            v = interp(stmt, s);
        }
        return v;
    }

    @Override
    public Value visit(@NotNull If if1, @NotNull Scope s) {
        // todo if 语句做成一定要写 else 分支!!!
        // then 和 orelse 不用新建作用域, 因为本身可能已经是block, 递归解释 block 会处理作用域
        Value test = interp(if1.test, s);
        TypeChecker.boolAssert(if1.loc, test);
        if (test == TRUE) {
            return interp(if1.then, s);
        } else if (if1.orelse != null) {
            return interp(if1.orelse, s);
        }
        // todo 做成有返回值的表达式，返回 void
        return Value.VOID;
    }

    // 1. 先执行匹配 2. 在执行绑定 3. 执行 guard 4. 执行 body
    @Override
    public Value visit(@NotNull MatchPattern match, @NotNull Scope s) {
        Value val = interp(match.value, s);
        Evaluator<Value, Boolean> patternMatcher = Matchers.patternMatcher(this, s);

        for (CaseMatch cm : match.cases) {
            Node ptn = cm.pattern;
            if (patternMatcher.eval(ptn, val)) {
                Scope s1 = new Scope(s);
                Matchers.tryDefine(this, s1, ptn, val); // 不是所有 pattern 都能 define 的
                boolean guardHit;
                if (cm.guard == null) {
                    guardHit = true;
                } else {
                    Value guard = interp(cm.guard, s1);
                    TypeChecker.boolAssert(cm.guard.loc, guard);
                    guardHit = guard == TRUE;
                }
                if (guardHit) {
                    // interp Match Then
                    Value v = Value.VOID;
                    for (Node stmt : cm.body.stmts) {
                        v = interp(stmt, s1);
                    }
                    return v;
                }
            }
        }
        // throw Error.runtime(match.loc, "未匹配到任何 case");
        return Value.VOID;
    }

    @Override
    public Value visit(@NotNull While while1, @NotNull Scope s) {
        Value v = Value.VOID;
        while(TypeChecker.boolAssert(while1.loc, interp(while1.test, s)) == TRUE) {
            try {
                v = interp(while1.body, s);
            } catch (ControlFlow.Continue cont) {
                //noinspection UnnecessaryContinue
                continue;
            } catch (ControlFlow.Break brk) {
                break;
            }
        }
        return v;
    }

    @Override
    public Value visit(@NotNull Continue cont, @NotNull Scope s) {
        throw ControlFlow.Continue();
    }

    @Override
    public Value visit(@NotNull Break brk, @NotNull Scope s) {
        throw ControlFlow.Break();
    }

    @Override
    public Value visit(@NotNull Return ret, @NotNull Scope s) {
        if (ret.expr == null) {
            throw ControlFlow.Return(null);
        } else {
            Value v = interp(ret.expr, s);
            throw ControlFlow.Return(v);
        }
    }

    @Override
    public Value visit(@NotNull Module module, @NotNull Scope s) {
        Scope blockS = new Scope(s);
        interpBlock(module.block, blockS);
        ModuleValue moduleV = ModuleValue(module, blockS.shadowCopy());
        if (module.name != null) {
            s.putValue(module.name.name, moduleV);
        }
        return moduleV;
    }

    @Override
    public Value visit(@NotNull Import import1, @NotNull Scope s) {
        Value v = interp(import1.from, s);
        ModuleValue module = (ModuleValue) v;
        boolean hasStar = false;
        Set<String> imported = new HashSet<>();

        for (Pair<Id, Id> it : import1.aliasMap) {
            Id key = it.fst;
            Id val = it.snd;
            if (key.name.equals(IMPORT_STAR)) {
                hasStar = true;
            } else {
                importOne(module, key.name, val.name, s);
                imported.add(key.name);
            }
        }

        // import * 时候可以有个别的别名, 所以分两步处理
        if (hasStar) {
            for (String name : module.scope.keySet()) {
                if (!imported.contains(name)) {
                    importOne(module, name, name, s);
                }
            }
        }
        return Value.VOID;
    }

    void importOne(ModuleValue module, String name, String alias, Scope s) {
        Map<String, Object> allProps = module.scope.lookupAllProps(name);
        assert allProps != null;
        s.putAllProps(alias, allProps);
    }

    @Override
    public Value visit(@NotNull VectorOf vectorOf, @NotNull Scope s) {
        return VectorType(interp(vectorOf.elem, s));
    }


    @Override
    public Value visit(@NotNull Assert assert1, @NotNull Scope s) {
        return assertInterp.interp(assert1, s);
    }

    @Override
    public Value visit(@NotNull Debugger debugger, @NotNull Scope ctx) {
        return Value.VOID;
    }

    @Override
    public Value visit(@NotNull Operator operator, @NotNull Scope s) {
        return Value.VOID;
    }

    static class Declares {

        // evalProperties
        static Scope interp(Interpreter interp, Declare declare, Scope s) {
            Scope evaluated = new Scope();

            declare.props.forEach((name, props) -> {
                if (name.equals(ID_RETURN)) {
                    // 忽略返回值, 只在类型检查阶段起作用
                    return;
                }

                for (Entry<String, Object> it : props.entrySet()) {
                    String propK = it.getKey();
                    if (propK.equals(KEY_TYPE)) {
                        // 解释器不处理类型...
                        continue;
                    }
                    Object propV = it.getValue();
                    if (propV instanceof Node) {
                        // 原因同 typechecker
                        Value v = interp.interp(((Node) propV), s);
                        evaluated.put(name, propK, v);
                    } else {
                        throw Error.bug(declare.loc, "声明值必须是 Node");
                    }
                }
            });

            return evaluated;
        }

        static void mergeDefault(Scope out, Scope props) {
            // interp 阶段 mut 没用
//            for (String name : props.keySet()) {
//                Object mutable = props.lookupLocalProp(name, KEY_MUT);
//                if (mutable == null) {
//                    out.put(name, KEY_MUT, FALSE);
//                } else {
//                    TypeChecker.boolAssert(Location.None, mutable);
//                    out.put(name, KEY_MUT, mutable);
//                }
//            }

            for (String name : props.keySet()) {
                Value defaultVal = props.lookupLocalDefault(name);
                if (defaultVal == null) {
                    //noinspection UnnecessaryContinue
                    continue;
                } else {
                    Value existedVal = out.lookup(name);
                    if (existedVal == null) {
                        // copy 防止共享 let f = (a = [1,2,4]) => IO.println(Vectors.append(a, 1))  f() f()
                        out.putValue(name, copyType(defaultVal));
                    }
                }
            }
        }
    }

}
