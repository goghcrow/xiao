package xiao.match;

import org.jetbrains.annotations.NotNull;
import xiao.*;
import xiao.misc.Error;

import static xiao.front.Ast.*;
import static xiao.Constant.KEY_MUT;
import static xiao.Value.*;

/**
 * @author chuxiaofeng
 */
public class PatternDefineChecker extends PatternChecker {

    final Evaluator<Scope, Value> evaluator;
    final Scope env;
    final boolean mutable;
    final boolean skipWildcards;

    PatternDefineChecker(Evaluator<Scope, Value> evaluator, Scope env, boolean mutable, boolean skipWildcards) {
        this.evaluator = evaluator;
        this.env = env;
        this.mutable = mutable;
        this.skipWildcards = skipWildcards;
    }

    @Override
    public Value match(@NotNull Id ptn, @NotNull Value value) {
        if (Voids.contains(value)) {
            throw Error.type(ptn.loc, "不能 define Void");
        }

        if (skipWildcards && Matchers.isWildcards(ptn)) {
            return VOID;
        }

        String name = ptn.name;
        Value existed = env.lookupLocal(name);
        // assert existed == null;
        assert existed == null || existed instanceof Type.Undefined;

        TypeChecker.emptyVectorAssert(ptn.loc, value);
        // 模式匹配绑定这里的分支都是没声明类型的, 都用实际推导的类型
        // let a = []
        Value actual = value;
        // let 基础类型的常量都处理成字面类型
        actual = TypeChecker.literalWrapIfNecessary(mutable, actual);
        // let mut a = "s"
        // fun f(a: "s") = a
        // f(a)
//        if (mutable) {
//            actual = Literal.clear(actual);
//        }
        env.putValue(name, actual);
        env.put(name, KEY_MUT, mutable ? TRUE : FALSE);

        return VOID;
    }

    // 如果遇到绑定过程的 subscript 或者 attribute, 处理成 assign
    // 感觉不合理, 混合了 define + set, 取消支持
    @Override
    public Value match(@NotNull Subscript ptn, @NotNull Value value) {
        throw Error.type(ptn.loc, "变量声明不支持 subscript");
    }

    @Override
    public Value match(@NotNull Attribute ptn, @NotNull Value value) {
        throw Error.type(ptn.loc, "变量声明不支持 attribute");
    }

}
