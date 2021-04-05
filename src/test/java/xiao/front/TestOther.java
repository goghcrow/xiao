package xiao.front;

import org.junit.Test;
import xiao.front.Lexicon;

import java.util.TreeSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static xiao.front.Lexicon.*;
import static xiao.front.TokenType.NAME;

/**
 * @author chuxiaofeng
 */
public class TestOther {

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    @Test
    public void test_treeSet() {
        TreeSet<StrRule> set = new TreeSet<>();
        final StrRule s1 = new StrRule("hello", NAME, true);
        final StrRule s2 = new StrRule("hello", NAME, true);
        final StrRule s3 = new StrRule("hello", NAME, true);
        final StrRule s4 = new StrRule("helloWorld", NAME, true);
        set.add(s1);
        set.add(s2);
        set.add(s3);
        set.add(s4);
        assertEquals(s3.id, set.stream().filter(it -> it.toMatch.equals("hello")).findFirst().get().id);
        set.remove(s3);
        assertEquals(s2.id, set.stream().filter(it -> it.toMatch.equals("hello")).findFirst().get().id);
        set.remove(s2);
        assertEquals(s1.id, set.stream().filter(it -> it.toMatch.equals("hello")).findFirst().get().id);
        set.remove(s1);
        assertFalse(set.stream().anyMatch(it -> it.toMatch.equals("hello")));
    }


    @SuppressWarnings("NonAsciiCharacters")
    @Test
    public void test_treeSet根据comparator来判等() {
        class S {
            final String str;
            final int i;
            S(String str, int i) {
                this.str = str;
                this.i = i;
            }
            @Override public String toString() { return "S{str='" + str + '\'' + ", i=" + i + '}'; }
        }
        TreeSet<S> t = new TreeSet<>((o1, o2) -> {
            if (o1.str.equals(o2.str)) {
                return -1;
            } else {
                return -o1.str.compareTo(o2.str);
            }
        });
        S s1 = new S("hello", 1);
        S s2 = new S("hello", 2);
        S s3 = new S("hello", 3);
        t.add(s1);
        t.add(s2);
        t.add(s3);
        t.add(new S("hello world", 4));

        //        assertEquals(3, t.stream().filter(it -> it.str.equals("hello")).findFirst().get().i);
//        t.remove(s3); // 这里会用 Comparator 来获取元素... 所以不行
//        assertEquals(2, t.stream().filter(it -> it.str.equals("hello")).findFirst().get().i);
//        t.remove(s2);
//        assertEquals(1, t.stream().filter(it -> it.str.equals("hello")).findFirst().get().i);
    }

}
