package xiao;


import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static xiao.Boot.eval;
import static xiao.Boot.typecheck;
import static xiao.Type.BoolType;
import static xiao.Type.IntType;
import static xiao.misc.Helper.path;
import static xiao.misc.Helper.read;

@SuppressWarnings({"UnusedReturnValue"})
public class Play {

    @Test
    public void tmp() {
        // String s = "if Math.random() > 3.14 { [] } else { [1, 2, 3] }";
        // String s = "(  (f:(_:Int,_:Int)=>Int, a:Int, b:Int) => f(a, b)  )  ((a: Int, b: Int) => a + b, 1, 2) == 3";
        // String s = "((f:(_:Int,_:Int)=>Int, a:Int, b:Int) => f(a, b))((a: Int, b: Int) => a + b, 1, 2) == 3";
//        String s = "let a:Int[][][] = [[[]],[[]]] a";
         String s = "let a:Int[][] = [[],[3]] a";
        Value eval = typecheck(s);
        System.out.println(eval);
    }

    @Test public void test_basic() { test(); }
    @Test public void test_generic() { test(); }
    @Test public void test_operator() { test(); }
    @Test public void test_controlflow() { test(); }
    @Test public void test_dependenttype() { test(); }
    @Test public void test_patternmatching() { test(); }
    @Test public void test_polymorphism() { test(); }
    @Test public void test_redefine() { test(); }
    @Test public void test_if() { test(); }
    @Test public void test_literaltype() { test(); }
    @Test public void test_declare() { test(); }
    @Test public void test_typecheck() { test(); }
    @Test public void test_destruct() { test(); }
    @Test public void test_fun() { test(); }
    @Test public void test_typedeclare() { test(); }
    @Test public void test_subtype() { test(); }
    @Test public void test_cast() { test(); }
    @Test public void test_module() { test(); }
    @Test public void test_recursive() { test(); }



    void test() {
        evalResource("/" + Thread.currentThread().getStackTrace()[2].getMethodName());
    }

    Value evalResource(String resourcePath) {
        return eval(read(path(resourcePath)));
    }

    @Test
    public void test_literal_record_literal() {
        assertTrue(typecheck("if [a : Int, b : true].b { 1 } else { 3.14 }") instanceof IntType);
    }

    @Test
    public void test_literal_tuple() {
        //noinspection ConstantConditions
        assertTrue(((BoolType) typecheck("((42,), (3.14,)) == ((42,), (3.14,))")).literal);
        assertTrue(typecheck("if ((42,), (3.14,)) == ((42,), (3.14,)) { 1 } else { 3.14 }") instanceof IntType);
    }

}
