package xiao;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings({"rawtypes", "UnnecessaryLocalVariable", "unchecked", "ConstantConditions", "StatementWithEmptyBody"})
public class Test {

    // 泛型上界 extends 下界 super

    static class Fruit { }
    static class Apple extends Fruit { }

    // Liskov Substitution
    static void subtype() {
        // 1. subtyping fruit
        // Collection<? extends Fruit>


        // https://zh.wikipedia.org/wiki/%E5%AD%90%E7%B1%BB%E5%9E%8B
        // 子类型, 自反,传递

        List<Fruit> fruits = new ArrayList<>();
        List<Apple> apples = new ArrayList<>();

        List<? extends Fruit> extendsFruits = new ArrayList<>();
        List<? extends Apple> extendsApples = new ArrayList<>();

        List<? super Fruit> superFruits = new ArrayList<>();
        List<? super Apple> superApples = new ArrayList<>();


        // 哪个可以, 哪个不可以, and why !!!

        // 用赋值来测试子类型关系
        // a = b   (fun(a) => xxx)(b)  α-替换

        // 所有自身的赋值(自反)都被省略了

        // 不变 : Apple <: Fruit !=> List<Apple> <: List<Fruit>
        // fruits = apples;
        // apples = fruits;

        // fruits = extendsFruits;
        // fruits = extendsApples;
        // apples = extendsFruits;
        // apples = extendsApples;

        // fruits = superFruits;
        // fruits = superApples;
        // apples = superFruits;
        // apples = superApples;


        // 协变 : Apple <: Fruit => List<Apple> <: List<Fruit>
        extendsFruits = extendsApples;
        // extendsApples = extendsFruits;

        extendsFruits = fruits;
        extendsFruits = apples;
        extendsApples = apples;
        // extendsApples = fruits;

        // extendsFruits = superFruits;
        // extendsFruits = superApples;
        // extendsApples = superFruits;
        // extendsApples = superApples;


        // 逆变 : Apple <: Fruit => List<Fruit> <: List<Apple>
        // superFruits = superApples;
        superApples = superFruits;

        superFruits = fruits;
        // superFruits = apples;
        superApples = fruits;
        superApples = apples;

        // superFruits = extendsFruits;
        // superFruits = extendsApples;
        // superApples = extendsFruits;
        // superApples = extendsApples;


        // 直觉上可以的不一定可以, 上次讲 java 数组协变的问题 也说过
        // 背后的逻辑: 类型安全会阻碍表达能力, 是一种取舍, extends super 兼顾表达能力和安全
        // https://www.zhihu.com/question/64750890/answer/223813028
        // https://www.zhihu.com/question/20400700 没有觉得点赞最高的回答容易理解和说到最本质


        // -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
        // 数组是不变被设计成协变
        // A <: B  =>  A[] <: B[]
        Apple[] applesArr = new Apple[1];
        Fruit[] fruitsArr = applesArr;
        fruitsArr[0] = new Fruit(); // runtime: ArrayStoreException
        System.out.println(applesArr[0]);
        // why
        // 表达能力 public static void sort(Object[] a)


        // List 不变, A <: B  不成立  List<A> <: List<B>
        // List 不变, B <: A  不成立  List<A> <: List<B>
        // 混用泛型和 rawtype 的问题
        // 实际是 A <: 语义上的Object  =>  List<A> => List<语义上的Object>
        List<Apple> genericLst = new ArrayList<>();
        List lst = genericLst;
        lst.add(new Fruit());
        Apple apple = genericLst.get(0); // runtime: ClassCastException
        System.out.println(apple);
        // why
        // 兼容性  旧类库方法, sort(List lst),  sort(List<String>) ???
    }

    // PECS: Producer extends and Consumer super
    // 生产者和消费者指的是 容器本身, 生产元素还是消费元素

    // 宽于律人(super:参数,add,消费,放松加入元素类型)，严于律己(extends:返回值,get,生产,严格返回元素类型)
    // 宽进(消费)严出(生产)
    // “人”就是参数，“己”就是返回值。
    // 返回更抽象(严格), 可消费更具体(宽)


    // 有什么用, 表达语义
    // extends 可以关闭写
    // super 可以关闭读

    // list 里头放的是 Fruit 或者 Fruit 的某个具体的子类型, 你取出来时候, 只能用 Fruit 表达
    // List<? extends SomeValueObject> produceValueList() {}
    // 语义表达你不能无脑动我的对象, 编译期检查, 你动了责任在你, 而不用 ImmutableList
    // 我只生产对象给你用, 你可以拿出来用, 但是不能动我的集合, 可以当 immutable 用
    // 黑点 add(null), why  null instanceof (? extends Fruit)
    //      null 是 Null Type 类型的唯一值,
    //      Null <: Any Reference Type, Null 是引用类型的底类型, bottom type
    //      不管内部是啥类型, Null 都是他们的子类型, 没法声明一个 List<? extends NotNullFruit>
    // 类比 super read 只能读Object, Object 是 top type
    static List<? extends Fruit> produceFruit() {
        return new ArrayList<>();
    }

    static void produceTest(String[] args) {
        List<? extends Fruit> fruits = produceFruit();
        for (Fruit fruit : fruits) { }
        // fruits.add(null); // ...
        // fruits.add(new Fruit()); // why ??? 你知道里头具体是什么类型么, 你加错了, 别人取出来是错的咋办
        // fruits.add(new Apple());

        Fruit f = null;
    }

    // 我接受到的参数的List里面放的不知道是Fruit的哪个父类型，所以我不能乱加，我只能往里面加 Fruit 和 Fruit 的子类型
    // 然后取的时候，我不不知道你里面放的具体是哪一个Fruit的父类型，但不管是哪个类型都是Object的子类型，所以get()出来的就是个Object
    // 语义: 我不从里面取东西, 我不(能)改变你内部的元素, 我只能添加符合语义的元素, 内部元素状态被修改了跟我没关系
    static void consumeFruit(List<? super Fruit> fruits) {
        fruits.add(new Apple());
        fruits.add(new Fruit());
        // fruits.add(new Object());
    }


//    static class Husky extends Dog { }
//    static class Dog extends Animal {}
//    static class Animal { }

    // java 的返回值类型不是多态参数
//    static Husky f(Husky husky) { return null; }
//    static Animal f(Husky husky) { return null; }
//    static Animal f(Animal animal) { return null; }
//    static Husky f(Animal animal) { return null; }





    // A ≼ B 意味着 A 是 B 的子类型。 <:
    // A → B 指的是以 A 为参数类型，以 B 为返回值类型的函数类型。
    // x : A 意味着 x 的类型为 A。


    // Husky ≼ Dog ≼ Animal
    // 哪个是 Dog → Dog 的子类型
    // Husky  → Husky
    // Husky  → Animal
    // Animal → Animal
    // Animal → Husky

    // 测试方法 f: (f1: Dog → Dog) → Void

    // 假设 g: Husky → Husky, f(g) 的类型是否安全?
        // f 内部, f1(Samoyed)
    // 假设 g: Husky → Animal, f(g) 的类型是否安全?
        // f 内部, f1(Samoyed)
    // 假设 g: Animal → Animal, f(g) 的类型是否安全?
        // Dog = f1(), 实际 f1 返回 CAT
    // 假设 g: Animal → Husky, f(g) 的类型是否安全?
        // bingo

    // (Animal → Husky) ≼ (Dog → Dog)
    // 参数逆变, 返回值协变
    // 需要一个处理能力更强, 能兼容更多类型的参数才安全
    // 需要一个返回 更窄、更具体的的类型才安全
    // 所以, 如果方法作为接口参数? 描述能力大小


    // Dog 参数类型
    // List<Dog> ≼ ImmutableList<Animal>

    // java 泛型表达能力更强一些
    // List<Dog> ≼ ReadonlyList<Animal>
    // List<Animal> ≼ WriteOnlyList<Dog>
}
