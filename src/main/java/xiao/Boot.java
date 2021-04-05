package xiao;

import org.jetbrains.annotations.NotNull;
import xiao.front.*;
import xiao.misc.Error;
import xiao.misc.Helper;

import java.util.ArrayList;
import java.util.List;

import static xiao.front.Ast.Block;
import static xiao.front.Ast.internalModule;
import static xiao.Constant.*;
import static xiao.Type.*;
import static xiao.Value.*;
import static xiao.misc.Helper.path;
import static xiao.misc.Helper.read;

/**
 * @author chuxiaofeng
 */
public class Boot {

    public static Iterable<Token> tokenize(@NotNull String input) {
        return lexer().tokenize(input);
    }

    public static Block parse(@NotNull String input) {
        return scopedParser().parse(input);
    }

    // for test
    public static Value evalRaw(@NotNull String input) {
        Block program = scopedParser().parse(input);
        typecheck(program);
        return interp().eval(program, interpScope());
    }

    public static Value eval(@NotNull String input) {
        Parser parser = scopedParser();
        parser.prelude(Helper.read(Helper.path("/operators")));
        Block program = parser.parse(input);
        typecheck(program);
        return interp().eval(program, interpScope());
    }

    public static Value typecheck(@NotNull String input) {
        Parser parser = scopedParser();
        parser.prelude(Helper.read(Helper.path("/operators")));
        Block program = parser.parse(input);
        return typecheck(program);
    }

    static Value typecheck(Block program) {
        TypeChecker checker = ((TypeChecker) typeChecker());
        Scope s = typeCheckScope();
        Value t = checker.eval(program, s);

        while(!checker.uncalled.isEmpty()) {
            // 过程中可能会加入新的 uncall, 所以要这么处理
            List<FunType> toRemove = new ArrayList<>(checker.uncalled);
            for (FunType ft : toRemove) {
                checker.invokeUncalled(ft, s, true);
            }
            checker.uncalled.removeAll(toRemove);
        }

        return t;
    }


    static Lexer lexer() { return scopedParser().lexer; }

    static Parser parser() { return new Parser(new Lexicon(), Grammar.simple()); }

    static Parser scopedParser() { return new Parser(new Lexicon(), Grammar.scoped()); }

    // 默认开启断言
    static Evaluator<Scope, Value> interp() { return new Interpreter(); }

    static Evaluator<Scope, Value> typeChecker() { return new TypeChecker(); }


    // 约定：类型变量大写开头，普通变量小写开头

    static Scope interpScope() {
        Scope s = new Scope();

        s.putValue("+", new PrimFun("+", Arity.leastOne, PrimFunInterp::add));
        s.putValue("-", new PrimFun("-", Arity.leastOne, PrimFunInterp::sub));
        s.putValue("*", new PrimFun("*", Arity.leastTwo, PrimFunInterp::multi));
        s.putValue("/", new PrimFun("/", Arity.leastTwo, PrimFunInterp::div));

        s.putValue("<", new PrimFun("<", Arity.two, PrimFunInterp::lt));
        s.putValue("<=", new PrimFun("<=", Arity.two, PrimFunInterp::lte));
        s.putValue(">", new PrimFun(">", Arity.two, PrimFunInterp::gt));
        s.putValue(">=", new PrimFun(">=", Arity.two, PrimFunInterp::gte));
        s.putValue("==", new PrimFun("==", Arity.two, PrimFunInterp::eq));
        s.putValue("!=", new PrimFun("!=", Arity.two, PrimFunInterp::ne));
        s.putValue("&&", new PrimFun("&&", Arity.two, PrimFunInterp::and));
        s.putValue("||", new PrimFun("||", Arity.two, PrimFunInterp::or));
        s.putValue("!", new PrimFun("!", Arity.one, PrimFunInterp::not));

        s.putValue("++", new PrimFun("++", Arity.leastOne, PrimFunInterp::connect));

        s.putValue("|", new PrimFun("|", Arity.two, PrimFunInterp::unionOf));

        s.putValue(ID_TRUE, TRUE);
        s.putValue(ID_FALSE, FALSE);

        s.putValue(ID_ANY, ANY);
        s.putValue(ID_VOID, Value.VOID);
        s.putValue(ID_Never, NEVER);

        s.putValue(ID_BOOL, BOOL);
        s.putValue(ID_INT, INT);
        s.putValue(ID_FLOAT, FLOAT);
        s.putValue(ID_STR, STR);

        s.putValue(ID_AS, new PrimFun(ID_AS, Arity.two, PrimFunInterp::as));
        s.putValue("if", new PrimFun("if", Arity.three, PrimFunInterp::iff));

        interpModule(s);

        return s;
    }

    static void interpModule(Scope s) {
        Scope io = new Scope();
        io.putValue("print", new PrimFun("print", Arity.one, (loc, args) -> {
            Value v = args.get(loc, 0);
            if (v instanceof StringValue) {
                System.out.print(((StringValue) v).value);
            } else {
                System.out.print(v);
            }
            return Value.VOID;
        }));
        io.putValue("println", new PrimFun("println", Arity.one, (loc, args) -> {
            Value v = args.get(loc, 0);
            if (v instanceof StringValue) {
                System.out.println(((StringValue) v).value);
            } else {
                System.out.println(v);
            }
            return Value.VOID;
        }));
        s.putValue("IO", ModuleValue(Ast.internalModule("IO"), io));

        // 🍏🍎🍐🍊🍋🍌🍉🍇🍓🫐🍈🍒🍑🥭🍍🥥🥝🍅🍏🍎🍐🍊🍋🍌🍉🍇🍓🫐🍈🍒🍑🥭🍍🥥🥝🍅🍏🍎🍐🍊🍋🍌🍉🍇🍓🫐🍈🍒🍑🥭🍍🥥🥝🍅

        Scope vectors = new Scope();
        vectors.putValue("append", new PrimFun("append", Arity.two, (loc, args) -> {
            VectorValue vec = ((VectorValue) args.get(loc, 0));
            Value val = args.get(loc, 1);
            vec.add(loc, val);
            return vec;
        }));
        vectors.putValue("size", new PrimFun("size", Arity.one, (loc, args) -> {
            VectorValue vec = ((VectorValue) args.get(loc, 0));
            return Int(vec.size());
        }));
        s.putValue("Vectors", ModuleValue(Ast.internalModule("Vectors"), vectors));

        // 🍏🍎🍐🍊🍋🍌🍉🍇🍓🫐🍈🍒🍑🥭🍍🥥🥝🍅🍏🍎🍐🍊🍋🍌🍉🍇🍓🫐🍈🍒🍑🥭🍍🥥🥝🍅🍏🍎🍐🍊🍋🍌🍉🍇🍓🫐🍈🍒🍑🥭🍍🥥🥝🍅

        Scope math = new Scope();
        math.putValue("random", new PrimFun("random", Arity.zero, (loc, args) -> Float(Math.random())));
        s.putValue("Math", ModuleValue(Ast.internalModule("Math"), math));

        // 🍏🍎🍐🍊🍋🍌🍉🍇🍓🫐🍈🍒🍑🥭🍍🥥🥝🍅🍏🍎🍐🍊🍋🍌🍉🍇🍓🫐🍈🍒🍑🥭🍍🥥🥝🍅🍏🍎🍐🍊🍋🍌🍉🍇🍓🫐🍈🍒🍑🥭🍍🥥🥝🍅

        Scope types = new Scope();
        // types.putValue("typeof", new PrimFun("typeof", Arity.one, PrimFunInterp::typeOf));
        types.putValue("eltType", new PrimFun("eltType", Arity.one, PrimFunInterp::eltType));
        s.putValue("Types", ModuleValue(Ast.internalModule("Types"), types));
    }

    static Scope typeCheckScope() {
        Scope s = new Scope();

        s.putValue("+", new PrimFun("+", Arity.leastOne, PrimFunTypeChecker::add));
        s.putValue("-", new PrimFun("-", Arity.leastOne, PrimFunTypeChecker::sub));
        s.putValue("*", new PrimFun("*", Arity.leastTwo, PrimFunTypeChecker::multi));
        s.putValue("/", new PrimFun("/", Arity.leastTwo, PrimFunTypeChecker::div));

        s.putValue("<", new PrimFun("<", Arity.two, PrimFunTypeChecker::lt));
        s.putValue("<=", new PrimFun("<=", Arity.two, PrimFunTypeChecker::lte));
        s.putValue(">", new PrimFun(">", Arity.two, PrimFunTypeChecker::gt));
        s.putValue(">=", new PrimFun(">=", Arity.two, PrimFunTypeChecker::gte));
        s.putValue("==", new PrimFun("==", Arity.two, PrimFunTypeChecker::eq));
        s.putValue("!=", new PrimFun("!=", Arity.two, PrimFunTypeChecker::ne));
        s.putValue("&&", new PrimFun("&&", Arity.two, PrimFunTypeChecker::and));
        s.putValue("||", new PrimFun("||", Arity.two, PrimFunTypeChecker::or));
        s.putValue("!", new PrimFun("!", Arity.one, PrimFunTypeChecker::not));

        s.putValue("++", new PrimFun("++", Arity.leastOne, PrimFunTypeChecker::connect));

        s.putValue("|", new PrimFun("|", Arity.two, PrimFunTypeChecker::unionOf));

        s.putValue(ID_TRUE, TYPE_TRUE);
        s.putValue(ID_FALSE, TYPE_FALSE);

        s.putValue(ID_ANY, ANY);
        s.putValue(ID_VOID, Type.VOID);
        s.putValue(ID_Never, NEVER);

        s.putValue(ID_BOOL, BOOL);
        s.putValue(ID_INT, INT);
        s.putValue(ID_FLOAT, FLOAT);
        s.putValue(ID_STR, STR);

        s.putValue(ID_AS, new PrimFun(ID_AS, Arity.two, PrimFunTypeChecker::as));
        s.putValue("if", new PrimFun("if", Arity.three, PrimFunTypeChecker::iff));

        typeCheckModule(s);

        return s;
    }

    static void typeCheckModule(Scope s) {
        Scope io = new Scope();
        io.putValue("print", new PrimFun("print", Arity.one, (loc, args) -> {
            args.get(loc, 0); // 因为是惰性的, 这里必须获取参数, 触发类型检查
            return Type.VOID;
        }));
        io.putValue("println", new PrimFun("println", Arity.one, (loc, args) -> {
            args.get(loc, 0); // 因为是惰性的, 这里必须获取参数, 触发类型检查
            return Type.VOID;
        }));
        s.putValue("IO", ModuleType(Ast.internalModule("IO"), io));

        // 🍏🍎🍐🍊🍋🍌🍉🍇🍓🫐🍈🍒🍑🥭🍍🥥🥝🍅🍏🍎🍐🍊🍋🍌🍉🍇🍓🫐🍈🍒🍑🥭🍍🥥🥝🍅🍏🍎🍐🍊🍋🍌🍉🍇🍓🫐🍈🍒🍑🥭🍍🥥🥝🍅

        Scope vectors = new Scope();
        vectors.putValue("append", new PrimFun("append", Arity.two, (loc, args) -> {
            Value t = args.get(loc, 0);
            // todo 字面量
            if (!(t instanceof VectorType)) {
                throw Error.type(loc, "append 第一个参数期望 Vector, 实际是 " + t);
            }
            VectorType vec = (VectorType) t;
            Value actual = args.get(loc, 1);
            Value expected = vec.eltType;
            if (!subtype(actual, expected)) {
                throw Error.type(loc, "vector 期望元素类型 " + expected + ", 实际是 " + actual);
            }
            actual = TypeChecker.hackActual(expected, actual);
            vec.add(loc, actual);
            return vec;
        }));
        vectors.putValue("size", new PrimFun("size", Arity.one, (loc, args) -> {
            Value t = args.get(loc, 0);
            // todo 字面量
            if (!(t instanceof VectorType)) {
                throw Error.type(loc, "size 第一个参数期望 Vector, 实际是 " + t);
            }
            VectorType vec = (VectorType) t;
            int size = vec.size();
            if (size == VectorType.UNKNOWN_SZ) {
                return INT;
            } else {
                return IntType((long) size);
            }
        }));
        s.putValue("Vectors", ModuleType(Ast.internalModule("Vectors"), vectors));

        // 🍏🍎🍐🍊🍋🍌🍉🍇🍓🫐🍈🍒🍑🥭🍍🥥🥝🍅🍏🍎🍐🍊🍋🍌🍉🍇🍓🫐🍈🍒🍑🥭🍍🥥🥝🍅🍏🍎🍐🍊🍋🍌🍉🍇🍓🫐🍈🍒🍑🥭🍍🥥🥝🍅

        Scope math = new Scope();
        math.putValue("random", new PrimFun("random", Arity.zero, (loc, args) -> FLOAT));
        s.putValue("Math", ModuleType(Ast.internalModule("Math"), math));

        // 🍏🍎🍐🍊🍋🍌🍉🍇🍓🫐🍈🍒🍑🥭🍍🥥🥝🍅🍏🍎🍐🍊🍋🍌🍉🍇🍓🫐🍈🍒🍑🥭🍍🥥🥝🍅🍏🍎🍐🍊🍋🍌🍉🍇🍓🫐🍈🍒🍑🥭🍍🥥🥝🍅

        Scope types = new Scope();
        // types.putValue("typeof", new PrimFun("typeof", Arity.one, PrimFunTypeChecker::typeOf));
        types.putValue("eltType", new PrimFun("eltType", Arity.one, PrimFunTypeChecker::eltType));
        s.putValue("Types", ModuleType(Ast.internalModule("Types"), types));
    }
}
