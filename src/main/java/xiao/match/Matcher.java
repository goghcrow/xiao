package xiao.match;

import org.jetbrains.annotations.NotNull;
import xiao.misc.Error;

import static xiao.front.Ast.*;

/**
 * PatternMatcher
 * 用来处理模式匹配，结构赋值等
 * @author chuxiaofeng
 */
interface Matcher<C, R> {

    R match(@NotNull Id ptn,            @NotNull C ctx);
    R match(@NotNull IntNum ptn,        @NotNull C ctx);
    R match(@NotNull FloatNum ptn,      @NotNull C ctx);
    R match(@NotNull Str ptn,           @NotNull C ctx);
    R match(@NotNull TupleLiteral ptn,  @NotNull C ctx);
    R match(@NotNull VectorLiteral ptn,  @NotNull C ctx);
    R match(@NotNull RecordLiteral ptn, @NotNull C ctx);

    default R match(@NotNull Subscript ptn, @NotNull C ctx) {
        return missing(ptn, ctx);
    }

    default R match(@NotNull Attribute ptn, @NotNull C ctx) {
        return missing(ptn, ctx);
    }

    default R match(@NotNull Define ptn, @NotNull C ctx) {
        return missing(ptn, ctx);
    }

    default R missing(@NotNull Node ptn, @NotNull C ctx) throws Error {
        throw Error.type(ptn.loc, "不支持的 pattern: " + ptn);
    }

    @SuppressWarnings("AliControlFlowStatementWithoutBraces")
    default R match(@NotNull Node ptn, @NotNull C ctx) {
        // true false, 本质是 id 从 scope 找 bool val
        if (ptn instanceof Id)                  return match((Id) ptn, ctx);
        if (ptn instanceof IntNum)              return match((IntNum) ptn, ctx);;
        if (ptn instanceof FloatNum)            return match((FloatNum) ptn, ctx);;
        if (ptn instanceof Str)                 return match((Str) ptn, ctx);
        if (ptn instanceof TupleLiteral)        return match((TupleLiteral) ptn, ctx);
        if (ptn instanceof VectorLiteral)       return match((VectorLiteral) ptn, ctx);
        if (ptn instanceof RecordLiteral)       return match((RecordLiteral) ptn, ctx);
        if (ptn instanceof Subscript)           return match((Subscript) ptn, ctx);
        if (ptn instanceof Attribute)           return match((Attribute) ptn, ctx);
        if (ptn instanceof Define)              return match((Define) ptn, ctx);

        return missing(ptn, ctx);
    }
}