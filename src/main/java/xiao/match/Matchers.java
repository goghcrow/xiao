package xiao.match;

import org.jetbrains.annotations.NotNull;
import xiao.Evaluator;
import xiao.Scope;
import xiao.Value;

import static xiao.front.Ast.Id;
import static xiao.front.Ast.Node;
import static xiao.Constant.WILDCARDS;

/**
 * Match API
 * @author chuxiaofeng
 */
public interface Matchers {

    // 匹配结构和值
    static Evaluator<Value, Boolean> patternMatcher(@NotNull Evaluator<Scope, Value> evaluator, @NotNull Scope env) {
        return new PatternMatcher(evaluator, env);
    }

    // 给模式匹配用的
    // 忽略不支持的 bind
    static void tryDefine(@NotNull Evaluator<Scope, Value> evaluator,
                          @NotNull Scope env,
                          @NotNull Node pattern,
                          @NotNull Value value
    ) {
        // RedefineChecker.check(pattern);
        new TryPatternDefiner(evaluator, env).match(pattern, value);
    }

    static void define(@NotNull Evaluator<Scope, Value> evaluator,
                       @NotNull Scope env,
                       @NotNull Node pattern,
                       @NotNull Value value,
                       boolean mut
    ) {
        // RedefineChecker.check(pattern);
        // 允许正常绑定时_是正常标识符 let _ = 42
        boolean skipWildcards = !isWildcards(pattern);
        new PatternDefiner(evaluator, env, mut, skipWildcards).match(pattern, value);
    }

    static void assign(Evaluator<Scope, Value> evaluator,
                       Scope env,
                       @NotNull Node pattern,
                       @NotNull Value value
    ) {
        // RedefineChecker.check(pattern);
        // 允许正常赋值时_是正常标识符 _ = 42
        boolean skipWildcards = !isWildcards(pattern);
        new PatternAssigner(evaluator, env, skipWildcards).match(pattern, value);
    }

    // ==============================================================================

    // 匹配结构和类型
    static Evaluator<Value, Boolean> checkPatternMatcher(@NotNull Evaluator<Scope, Value> evaluator, @NotNull Scope env) {
        return new PatternMatchChecker(evaluator, env);
    }

    static void reDefineCheck(@NotNull Node pattern) {
        RedefineChecker.check(pattern);
    }

    static void reDefineCheck(@NotNull Node pattern, @NotNull Scope env) {
        RedefineChecker.check(pattern, env);
    }

    static void checkDefine(@NotNull Evaluator<Scope, Value> evaluator,
                            @NotNull Scope env,
                            @NotNull Node pattern,
                            @NotNull Value value,
                            boolean mut
    ) {
        reDefineCheck(pattern, env);
        // 允许正常绑定时_是正常标识符 let _ = 42
        boolean skipWildcards = !isWildcards(pattern);
        new PatternDefineChecker(evaluator, env, mut, skipWildcards).match(pattern, value);
    }

    static void checkTryDefine(@NotNull Evaluator<Scope, Value> evaluator,
                               @NotNull Scope env,
                               @NotNull Node pattern,
                               @NotNull Value value
    ) {
        reDefineCheck(pattern, env);
        new TryPatternDefineChecker(evaluator, env).match(pattern, value);
    }

    static void checkAssign(Evaluator<Scope, Value> evaluator,
                            Scope env,
                            @NotNull Node pattern,
                            @NotNull Value value
    ) {
        reDefineCheck(pattern);
        // 允许正常赋值时_是正常标识符 _ = 42
        boolean skipWildcards = !isWildcards(pattern);
        new PatternAssignChecker(evaluator, env, skipWildcards).match(pattern, value);
    }

    // ==============================================================================

    static boolean isWildcards(@NotNull Node ptn) {
        return ptn instanceof Id && ((Id) ptn).name.equals(WILDCARDS);
    }

    static boolean isWildcards(@NotNull Id ptn) {
        return ptn.name.equals(WILDCARDS);
    }

    static boolean isWildcards(@NotNull String s) {
        return s.equals(WILDCARDS);
    }

}
