package xiao;

import org.jetbrains.annotations.NotNull;
import xiao.misc.Error;

import static xiao.front.Ast.*;

/**
 * @author chuxiaofeng
 */
public interface Visitor<C, R> {
//    R visit(@NotNull Group group, @NotNull C ctx); // 代码生成时候可能有用
    R visit(@NotNull Id id, @NotNull C ctx);
    R visit(@NotNull IntNum intNum, @NotNull C ctx);
    R visit(@NotNull FloatNum floatNum, @NotNull C ctx);
    R visit(@NotNull Str str, @NotNull C ctx);

    R visit(@NotNull TupleLiteral tuple, @NotNull C ctx);
    R visit(@NotNull VectorLiteral vector, @NotNull C ctx);
    R visit(@NotNull RecordLiteral literal, @NotNull C ctx);

    R visit(@NotNull Unary unary, @NotNull C ctx);
    R visit(@NotNull Binary binary, @NotNull C ctx);

    R visit(@NotNull Define var, @NotNull C ctx);
    R visit(@NotNull Assign assign, @NotNull C ctx);

    R visit(@NotNull LitType lit, @NotNull C ctx);
    R visit(@NotNull Typedef def, @NotNull C ctx);

    R visit(@NotNull RecordDef record, @NotNull C ctx);
    R visit(@NotNull Attribute attribute, @NotNull C ctx);
    R visit(@NotNull Subscript subscript, @NotNull C ctx);

    R visit(@NotNull FunDef fun, @NotNull C ctx);
    R visit(@NotNull Call call, @NotNull C ctx);

    R visit(@NotNull Block block, @NotNull C ctx);
    R visit(@NotNull If if1, @NotNull C ctx);
    R visit(@NotNull MatchPattern match, @NotNull C ctx);

    R visit(@NotNull While while1, @NotNull C ctx);
    R visit(@NotNull Continue cont, @NotNull C ctx);
    R visit(@NotNull Break brk, @NotNull C ctx);
    R visit(@NotNull Return ret, @NotNull C ctx);

    R visit(@NotNull Module module, @NotNull C ctx);
    R visit(@NotNull Import import1, @NotNull C ctx);

    R visit(@NotNull VectorOf vectorOf, @NotNull C ctx);

    R visit(@NotNull Assert assert1, @NotNull C ctx);
    R visit(@NotNull Operator operator, @NotNull C ctx);
    R visit(@NotNull Debugger debugger, @NotNull C ctx);

    // R visit(@NotNull Try try1, @NotNull C ctx);
    // R visit(@NotNull Throw throw1, @NotNull C ctx);

    // 不喜欢在一堆 node 上挖个洞实现 visit 方法, 倾向把数据和逻辑分开, 所以写 switch-case
    @SuppressWarnings("AliControlFlowStatementWithoutBraces")
    default R visit(@NotNull Node n, @NotNull C ctx) {
//        if (n instanceof Group)          return visit((Group) n,            ctx);
        if (n instanceof Id)             return visit((Id) n,               ctx);
        if (n instanceof IntNum)         return visit((IntNum) n,           ctx);
        if (n instanceof FloatNum)       return visit((FloatNum) n,         ctx);
        if (n instanceof Str)            return visit((Str) n,              ctx);
        if (n instanceof TupleLiteral)   return visit((TupleLiteral) n,     ctx);
        if (n instanceof VectorLiteral)  return visit((VectorLiteral) n,    ctx);
        if (n instanceof RecordLiteral)  return visit((RecordLiteral) n,    ctx);

        if (n instanceof Unary)          return visit((Unary) n,            ctx);
        if (n instanceof Binary)         return visit((Binary) n,           ctx);

        if (n instanceof Assign)         return visit((Assign) n,           ctx);
        if (n instanceof Subscript)      return visit((Subscript) n,        ctx);
        if (n instanceof Attribute)      return visit((Attribute) n,        ctx);

        if (n instanceof Define)         return visit((Define) n,           ctx);
        if (n instanceof FunDef)         return visit((FunDef) n,           ctx);
        if (n instanceof RecordDef)      return visit((RecordDef) n,        ctx);

        if (n instanceof Typedef)        return visit((Typedef) n,          ctx);
        if (n instanceof LitType)        return visit((LitType) n,          ctx);

        if (n instanceof Call)           return visit((Call) n,             ctx);

        if (n instanceof Block)          return visit((Block) n,            ctx);
        if (n instanceof If)             return visit((If) n,               ctx);
        if (n instanceof MatchPattern)   return visit((MatchPattern) n,     ctx);
        if (n instanceof While)          return visit((While) n,            ctx);
        if (n instanceof Continue)       return visit((Continue) n,         ctx);
        if (n instanceof Break)          return visit((Break) n,            ctx);
        if (n instanceof Return)         return visit((Return) n,           ctx);

        if (n instanceof VectorOf)       return visit((VectorOf) n,         ctx);

        if (n instanceof Module)          return visit((Module) n,          ctx);
        if (n instanceof Import)         return visit((Import) n,           ctx);

        if (n instanceof Operator)       return visit((Operator) n,         ctx);
        if (n instanceof Assert)         return visit((Assert) n,           ctx);
        if (n instanceof Debugger)       return visit((Debugger) n,         ctx);

        throw Error.bug(n.loc, "不支持的 ast 节点: " + n);
    }

}
