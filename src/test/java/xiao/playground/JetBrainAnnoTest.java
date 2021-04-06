package xiao.playground;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static java.util.stream.Collectors.*;

/**
 * Contract 使用场景
 * @author chuxiaofeng
 */
@SuppressWarnings("unused")
public class JetBrainAnnoTest {

    @Nullable String mayNull() { return Math.random() > 0.5 ? null : "Hello World!"; }

    // 提示方法调用者要处理 null, 如果不处理 ide 会提示
    @Nullable String methodMayReturnNull() {
        if (Math.random() > 0.5) {
            return null;
        } else {
            return "Hello World!";
        }
    }

    void test_methodMayReturnNull() {
        methodMayReturnNull().substring(0, 1); // IDE 提示 NPE
        mayNull().substring(0, 1);
    }

    interface Iface {
        @NotNull String methodMustNotReturnNull();
    }

    static class Impl1 implements Iface {
        @Override
        public @NotNull String methodMustNotReturnNull() {
            return new JetBrainAnnoTest().mayNull(); // 跨过程静态分析会告诉你这里会返回 null
        }
    }
    static class Impl2 implements Iface {
        @Override
        public @NotNull String methodMustNotReturnNull() {
            if (Math.random() > 0.5) {
                return null; // IDE 提示不应该返回 null
            } else {
                return "Hello World!";
            }
        }
    }

    int strSize1(@Nullable String nullableStr) {
        if (nullableStr.isEmpty()) { // NPE
            return 0;
        } else {
            // 这里不提示, 是因为如果 nullableStr 是 null, 这里是死代码
            // 你解决掉 isEmpty 的 NPE 这里也会提示
            return 1 + strSize1(nullableStr.substring(1)); // NPE
        }
    }

    int strSize2(@NotNull String notNullStr) {
        if (notNullStr.isEmpty()) {
            return 0;
        } else {
            return 1 + strSize2(notNullStr.substring(1));
        }
    }

    void test_strSize() {
        strSize1(null);
        strSize2(null); // NPE
    }

    // 🍏🍎🍐🍊🍋🍌🍉🍇🍓🫐🍈🍒🍑🥭🍍🥥🥝🍅🍏🍎🍐🍊🍋🍌🍉🍇🍓🫐🍈🍒🍑🥭🍍🥥🥝🍅🍏🍎🍐🍊🍋🍌🍉🍇🍓🫐🍈🍒🍑🥭🍍🥥🥝🍅


//    A contract can have 1 or more clauses associated with it
//    A clause is always [args] -> [effect]
//    Args are 1 or more constraints, which are defined as any | null | !null | false | true
//    Effects are only one constraint or fail

    // contract 大于等于 1 个子句, ; 分隔
    // 子句 [...args] -> [effect
    //

//    You want to guarantee that you return true or false
//    You want to guarantee that you return a non-null value given constraints
//    You want to make clear that you can return a null value given constraints
//    You want to make clear that you will throw an exception given constraints


    @Contract("null -> fail")
    void paramNullable1(String nullable) {
        // violate
    }

    @Contract("null -> fail")
    void paramNullable2(String nullable) {
        if (nullable == null) {
            throw new RuntimeException();
        }
    }


    // !null -> param1 这部分这个 case 其实没什么用
    @Contract("null -> null; !null -> param1")
    List<String> reverseStrInList(@Nullable List<String> lst) {
        if (lst == null) {
            return null;
        } else {
            Function<String, String> reverse = it -> new StringBuilder(it).reverse().toString();
            return lst.stream().map(reverse).collect(toList());
        }
    }

    void reverseStrInList() {
        // 如果不写 contract ide 不会提示, 当然这个简单的场景可以标记返回值 @Nullable, 或者不返回 null
        reverseStrInList(null).subList(0, 0); // NPE

        List<String> lst = new ArrayList<>();
        reverseStrInList(lst);
    }

    // todo。。。
}
