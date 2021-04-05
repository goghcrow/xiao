## Declare

```
let immutable_var = 1
let mut mutable_var = 1

// Tuple Immutable
let tuple1 = ("hello", 1)
let tuple2 = ("hello", 3.14)
let mut t: (Str, Float) = tuple1
t = tuple2


let vec = [0]
vec[0] = 42


let empty_record_literal = [:]
let immutable_rec = [ id: immutable_var, name: "xiao" ]
let mutable_rec = [ mut id: immutable_var, name: "xiao" ]
mutable_rec.id = 42


record { a: Int, b: Int, c: Int }
record {
    a: Int
    b: Int
    c: Int
}

// 属性默认immutable, 需要显示加 mut, 与变量绑定统一
record {
    a: Str     // 类型
    b = 1      // 默认值
    mut c: Int // 可修改
    mut d: Float = 3.14
}

// extends 纯粹是简化重复代码, 没有其他任何意义
{
    let Color = record {
        mut r :Int = 0
        mut g :Int = 0
        mut b :Int = 0
    }
    let Id = record {
        id :Int
    }

    // record 可以继承属性, 继承多个, 属性按声明顺序覆盖
    let Identified_PNG = record {
        mut alpha :Int
    } extends Color, Id
    let png = Identified_PNG(alpha = 255, id=42)
}
```

## Fun

```
fun() { }

// 函数 body 是简单表达式可以直接写等号
fun() = 42
fun() = {}

fun(a, b) = 42

fun(a: Int) = 42
fun(a: Int): Int = 42

fun(a: Int = 1) = 42
fun(a: Int = 1): Int = 42

fun(a: Int|Str = 1) = 42
fun(a: Int|Str = 1): Int = 42

fun(a :Int, b :Str) = 42
fun(a :Int, b :Str): Int|Str = 42

fun connect(a :Str, b :Str) = a ++ b

fun(a: Int) {
    debugger a = 1 // 默认是 immutable
}(42)

// 参数可修改
fun(mut a: Int) {
    a = 1
}(42)


() => 42
(): Int => 42

() => { }
(): Void => { }

// 这种形式不能声明参数与返回值类型,不能有默认值
a => 42
a => { 42 }

// a => { debugger a = 1 } // 参数默认不可以修改

// 注意: 不支持以下语法, 必须括号
// a: Int  => 42
// a: Int = 1 =>

(a) => 42
(a): Int => 42

(a :Int) => 42
(a :Int): Int => 42

(a :Int = 1) => 42
(a :Int = 1): Int => 42

(a :Int | Str = 1) => 42
(a :Int | Str = 1): Int => 42

(a, b) => 42
(a, b): Int => 42

(a :Int, b :Str) => 42
(a :Int, b :Str): Int => 42

// 可修改参数需要显式声明
(mut a: Int|Str) => {
    a = "str"
    a = 1
}

// 参数默认值可以根据作用域求值
{
    let x = 41
    let f1 = (a :Int = x + 1) => a
    assert f1() == 42

    let f2 = (a: Bool) => (v = if a 42 else 0) => v
    assert f2(true)() == 42
    assert f2(false)() == 0
}
{
    fun f(a: Bool) {
        // return 表达式起始字符必须放在 return 后面, 后面可以换行
        return fun(v = if a 42 else 0) = v
    }
    assert f(true)() == 42
    assert f(false)() == 0
}

// 两种调用方式
{
    let f = (a: Int, b: Float) => (a, b)
    assert f(42, 3.14) == (42, 3.14)
    assert f(a = 42, b = 3.14) == (42, 3.14)
}

{
    let f = (a: Int, b: Float = 3.14) => (a, b)
    // 传统方式调用函数默认值无作用
    debugger f(42) == (42, 3.14)
}

{
    // 默认值无顺序要求
    let f1 = (a: Int, b: Float = 3.14) => (a, b)
    assert f1(a = 42) == (42, 3.14)

    let f2 = (a: Int = 42, b: Float) => (a, b)
    assert f2(b = 3.14) == (42, 3.14)
}
```

```
// 多态函数
{
    let id = a => a
    assert id(42) == 42
    assert id(3.14) == 3.14
    assert id("Hi") == "Hi"
}

{
    let add = (a, b) =>
        match (a, b) {
            case (a:Float, b:Float) -> a + b
            case (a: Str, b: Str) -> a ++ b
        }
    assert add(1, 2) == 3
    assert add(100, 3.14) == 103.14
    assert add("Hello", "World") == "HelloWorld"
    // 没有匹配到为 Void
    debugger IO.println(add(1, "Str"))
}
```

## Type Declare

```
import * from Vectors
import * from IO

// 用 record 字面量声明类型
{
    let f = (point: [x: Int]) => point.x
    let point1 = [ x: 1 ]
    let point2 = [ x: 2, y: 3 ]
    assert f(point1) == 1
    assert f(point2) == 2
    let point0 = [:]
    debugger f(point0)
}

// 字面类型声明
{
    let f = (s: "Hello" | "World") => s
    assert f("Hello") == "Hello"
    assert f("World") == "World"
    debugger f("Str")
    debugger f(1)
    debugger f(3.14)
    let a = "Hello"
    assert f(a) == a
}

// 字面类型声明
{
    let f = (a): false|42|3.14|"S" => a
    assert f(false) == false
    assert f(42) == 42
    assert f(3.14) == 3.14
    assert f("S") == "S"
    debugger f(1)
    debugger f(1.1)
    debugger f("")
    debugger f(true)
}

// 字面类型声明
{
    let mut a: 42|3.14 = 42
    a = 3.14
    debugger a = 1
    debugger a = 1.1
    debugger a = "s"
}


{
    //   ) = {
    // 以下几种情况的表达式读取遇到= => 停止

    let x: Int|Str = "x"
    let f1 = fun() : Int|Str { "f1" }
    let f2 = fun(a: Int|Str) : Int|Str { "f2" }
    let f3 = fun(a: Int|Str, b: Int|Str) : Int|Str { "f3" }
    let f4 = fun(a: Int|Str, b: Int|Str) : Int|Str = { 4 }
    let f5 = (a: Int|Str) : Int|Str => { 5 }
    let f6 = (a: Int|Str, b: Int|Str) : Int|Str => { "f6" }

    match x {
        case x: Str -> assert x == "x"
        case _ -> assert false
    }
    match f1() {
        case v: Str -> v == "f1"
        case _ -> assert false
    }
    match f2(1) {
        case v: Str -> assert v == "f2"
        case _ -> assert false
    }
    match f3(1, "") {
        case v: Str -> v == "f3"
        case _ -> assert false
    }
    match f4("", 1) {
        case v: Int -> assert v == 4
        case _ -> assert false
    }
    match f5(1) {
        case v: Int -> assert v == 5
        case _ -> assert false
    }
    match f6("", "") {
        case v: Str -> assert v == "f6"
        case _ -> assert false
    }
}

{
    let f1 = (): (Int,) => (42, )
    let f2 = (): (Int,Str) => (42, "str")
    let (a, ) = f1()
    let (b, c) = f2()

    assert a == 42 && b == 42 && c == "str"

    // 声明返回 fun 的 fun
    // 字面量声明返回值类型需要括号，否则会有歧义
    let f3 = (): ((a: Int, b:Str) => (Int, Str)) => (a: Int, b: Str) => (42, "Hello World")
    assert f3()(1, "str") == (42, "Hello World")
    debugger f3()(1.1, "str")
    debugger f3()(1, 3.14)

    let f4: (a: Int, b:Str) => (Int, Str)
        = (a: Int, b: Str) => (42, "Hello World")
    assert f4(1, "str") == (42, "Hello World")
    debugger f4(1.1, "str")
    debugger f4(1, 3.14)
}


{
    let Void2Str = () => Str
    let Int2Str = (a: Int) => Str

    // 函数作为返回值
    let f1 = (): Int2Str => (i = 1) => "str"
    assert f1()() == "str"
    let f2 = (): Int2Str => (i: Int) => "str"
    assert f2()(42) == "str"

    let f3 = (): (a: Int) => Str => (i: Int) => "str"
    let f4 = (): ((a: Int) => Str) => (i: Int) => "str"

    assert f3()(1) == "str"
    assert f4()(42) == "str"

    // 函数作为参数
    let f5 = (f: Int2Str, i: Int) => f(i)
    assert f5( (i: Int)=>"str", 42) == "str"

    // 函数字面量类型作为参数
    let f6 = (f: (a: Int) => Str, i: Int) => f(i)
    assert f6( (i: Int)=>"str", 42) == "str"
}


(): (a: Int) => Str => (i: Int) => "str"
(): ((a:Int,b:Int)=>Int, (a:Int,b:Int)=>Int) => ((a:Int,b:Int)=>1, (a:Int,b:Int)=>2)
(): ((a: Int, b:Str) => (Int, Str)) => (a: Int, b: Str) => (42, "Hello World")

fun(i: Int, s: Str) = Int
(): (Int, Int) => (Int, Int)


{
    let A1 = record {
        a: Float
    }
    let A2 = record {
        a: Int
    }
    let f = (arg: A1) => arg
    f( A2(a=1) )
    f( A1(a=1) )
}


{
    let f = (t: (Float,)) => t[0]
    assert f((42,)) == 42
    assert f((3.14,)) == 3.14

    // tuple 只读, 参数只读, 所以 t[0] 能推断为实际参数的类型
    // let f = (t: (Float,)) => t[0] = 3.14 // tuple 只读

    // vector 能修改, 但是 vector  不变, 所有 Int[] 不是 Float[] 子类型
    let f1 = (t: Float[]) => t[0] = 3.14

    let f2 = (t: Float[]) => t[0]
    debugger f2([42]) == 42
    assert f2([3.14]) == 3.14
}



{
    let A = record { name: Float }
    let B = record { mut name: Int}
    debugger let f = (a: A) => a.name = 42
    // f(B(42))
}
{
    let A = record { mut name: Float }
    let B = record { name: Int}
    let f = (a: A) => a.name = 42
    debugger f(B(42))
}


{
    type ID = record { id: 42 }
    let f = (id: ID) => id
    f([id: 42, name: "xiao"])
    debugger f([id: 1, name: "xiao"])
}
```

## Recursive

```
// 递归声明类型
{
    // letrec (A, B) = (B, A)
    typerec (A, B) = (
        (42, B),
        (3.14, A)
    )
}

{
    // 1. 递归函数必须写返回值
    // 2. 相互递归函数如果不用 letrec 声明, 没法推导类型
    let is_even = (n: Int): Bool => n == 0 || is_odd(n - 1)
    let is_odd = (n: Int): Bool => n != 0 && is_even(n - 1)
    assert is_even(0) && is_odd(1) && is_even(2) && is_odd(3) && is_even(4) && is_odd(5)
}

{
    // Mutually Recursive
    letrec (A, B) = (
        record {
            b: B
        },
        record {
            a: A
        }
    )
    letrec (a, b) = (
        A(b),
        B(a)
    )
    assert a.b == b
    assert b.a == a

    // tuple
    {
        letrec (X, Y) = ( (X, Y), (Y, X) )
        assert X[0] == X && X[1] == Y && Y[0] == Y && Y[1] == X
    }

    // vec
    {
        letrec (X, Y) = ( [X, Y], [Y, X] )
        assert X[0] == X && X[1] == Y && Y[0] == Y && Y[1] == X
    }
}

{
    record A {
        a: Int
        b = 1
    }
    let f = (i: Int) :A => {
        if i > 0 {
            f(i - 1)
        } else {
            A(i, i)
        }
    }
    let [a: a, b: b] = f(10)
    assert a == 0 && b == 0
}

{
    // 递归函数依赖与标注类型, 不会进行推导
    let f = () :(Int,) => f()
    // let (a,) = f() // stackoverflow
}

{
    record Node {
        val :Int
        mut next :Node|() = ()
        mut pre :Node|() = ()
    }
    let n1 = Node(val = 1)
    let n2 = Node(val = 1, next = Node(2))
    let n3 = Node(val = 1, pre = Node(2), next = Node(3))
}

{
    record A {
        a: Int
        b: Int
        c = 1
    }
    let f = () :A => f()
    // let {a, b, c} = f() // stackoverflow
}

{
    record Node {
        val: Int
        mut next: Node|() = ()
        mut pre: Node|() = ()
    }

    let a = Node(val = 1)
    let b = Node(val = 2)
    let c = Node(val = 3)

    // linked
    (a.pre, a.next, b.pre, b.next, c.pre, c.next) = ((), b, a, c, b, ())

    assert b.val == 2 && (b.next as Node).val == 3 && (b.pre as Node).val == 1

    // let {pre, next = {next = { next }}} = a
    // assert pre == () && next == ()

    assert (a.pre as ()) == () && (((a.next as Node).next as Node).next as ()) == ()
}
```

## Destructuring Assignment

```
// basic
{
    let immutable = 1
    let mut a = 1
    let mut b = 2

    // assign
    (a, b) = (3, 4)
    assert a == 3 && b == 4

    // define
    let (c, d) = (a, b)
    assert c == 3 && d == 4

    // 通过tuple解构来swap变量值
    let mut x = 1
    let mut y = -1
    (y, x) = (x, y)
    assert x == -1 && y == 1
}

// tuple 解构
{
    let (a, b, c) = (1, 2, 3)
    assert a == 1 && b == 2 && c == 3

    let (d,,e) = (1,2,3)
    assert d == 1 && e == 3

    // 通配符可以忽略位置
    let (f,_,g) = (1,2,3)
    assert d == f && g == 3

    // ~注意~
    // 报错：绑定 tuple 数量不匹配: pattern 数量 2, value 数量 3
    // let (x, y) = (1, 2, 3)
    // 现在改成非严格模式匹配了, 不用匹配数量了
    let (h,) = (1,2,3)
    assert h == 1

    let () = ()

    // 支持嵌套
    let (x, (y, (z,))) = (1, (2, (3,)))
    assert x == 1 && y == 2 && z == 3
}

// record 解构
{
    // 声明：r1 绑定到 100
    let [r: r1] = Color(r=100, g=200, b=255)
    assert r1 == 100
    debugger r1 = 200 // 不可修改

    // 声明：可修改
    let mut [g: g1] = Color(r=100, g=200, b=255)
    assert g1 == 200
    g1 = 0
    assert g1 == 0

    let [r:r] = Color(r=100)
    assert r == 100

    {
        // 因为绑定和赋值采用非严格匹配, 忽略数量相等, 所以通配符，没什么用
        let [r:r, _:_, b:b] = Color(r=100, g=200, b=255)
        assert r == 100 && b == 255
    }

    // 赋值
    let mut b1 = 0
    [b: b1] = Color(b=42)
    assert b1 == 42
}

// 赋值：属性访问
{
    let P = Point
    let p = P(x=1, y=2)
    (p.x, p.y) = (3, 4)
    assert p.x == 3 && p.y == 4
}

// 赋值：下标访问
{
    let v = [0, 0]
    (v[0], v[1]) = (3, 4)
    assert v[0] == 3 && v[1] == 4
}

// 混合支持
{
    let p = Point(x=1, y=2)
    let v = [0, 0]
    let mut x = 0
    (x, p.x, v[0]) = (1, 2, 3)
    assert x == 1 && p.x == 2 && v[0] == 3
}

// record 解构
{
    // 声明
    // mut 表示被解构的变量都是 mut 的，这里指 r、g、b 三个变量 mut
    // 可以考虑独立控制, 但是貌似没有必要, 可以 再 let 一次，比如 let immu_r = r
    let mut ([r:r], [g:g], [b:b]) = (Color(r=255), Color(g=255), Color(b=255))
    assert r == 255 && g == 255 && b == 255

    // 赋值
    ([r:r], [g:g], [b:b]) = (Color(r=1), Color(g=2), Color(b=3))
    assert r == 1 && g == 2 && b == 3
}

// 综合的例子
{
    let attr = [
        mut id: 1
        tags: [""]
    ]

    let doc = [
        ver: 1.1
        id: 42
        title: "Hello World!"
        translations: [
            [
                locale: ""
                localization_tags: ("tag1",)
                url: "/ch/docs"
                title: "你好，世界！"
            ]
        ]
        url: "/en-US/docs"
    ]

    let [
        ver: ver
        // id: attr.id // 取消声明时 attr 与 subs 的支持
        title: englishTitle
        translations: [
            [
                title: localeTitle
                // localization_tags: (attr.tags[0],) // 取消声明时 attr 与 subs 的支持
            ],
        ]
    ] = doc
    assert ver == 1.1 && englishTitle == "Hello World!" && localeTitle == "你好，世界！"

    [
        id: attr.id // 取消声明时 attr 与 subs 的支持
        translations: [
            [
                localization_tags: (attr.tags[0],) // 取消声明时 attr 与 subs 的支持
            ],
        ]
    ] = doc
    assert attr.id == 42 && attr.tags[0] == "tag1"
}
```

## Union Type

```
fun printId(id: Float | Str)  = IO.println("Your ID is: " ++ id)
printId(101)
printId("202")
```

## Parameterized Type (Generic Type)

```
{
    letrec (Tree, Node) = (
        Node | (),
        record {
            left: Tree = ()
            right: Tree = ()
        }
    )

    let max = (a: Float, b: Float) => if(a > b, a, b)

    fun depth(tree: Tree): Int {
      match tree {
        case u: () -> 0
        case tree: Node -> 1 + max(depth(tree.left), depth(tree.right))
      }
    }

    fun assertDepth(d: Int, root: Tree) = assert depth(root) == d

    assertDepth(0, ())
    assertDepth(1, Node())
    assertDepth(2, Node(left = Node()))
    assertDepth(3, Node(left = Node(right = Node())))
}


{
    letrec (List, Node) = (
        Node | (),
        record {
            next: List
        }
    )

    fun size(lst: List): Int {
        match lst {
            case a: () -> 0
            case a: Node -> 1 + size(a.next)
        }
    }

    assert size(()) == 0
    assert size(Node(next = ())) == 1
    assert size(Node(next = Node(next = ()))) == 2
}

// immutable 结构
{
    fun ListOf(T) {
        module {
            letrec (List, Node) = (
                Node|(),
                record {
                    val: T
                    next: List = ()
                }
            )

            fun append(lst: List, val: T): Node {
                match lst {
                    case n: () -> Node(val)
                    case n: Node -> Node(val, n)
                }
            }

            fun size(lst: List): Int {
                match lst {
                    case n: () -> 0
                    case n: Node -> 1 + size(n.next)
                }
            }

            fun each(lst: List, f: (v: T) => Void): Void {
                match lst {
                    case n: () -> Void
                    case n: Node -> {
                        f(n.val)
                        each(n.next, f)
                    }
                }
            }

            fun foldr(lst: List, f: (v: T, carry: T) => T, init: T): T {
                match lst {
                    case n: () -> init
                    case n: Node -> f(n.val, foldr(n.next, f, init))
                }
            }
        }
    }

    import append as cons, * from ListOf(Str)

    assert size(()) == 0

    debugger cons((), 42)
    debugger cons((), 3.14)

    let lst = cons((), "!"),
    assert size(lst) == 1

    let lst1 = cons(lst, "World")
    assert size(lst1) == 2

    let lst2 = cons(lst1, "Hello")
    assert size(lst2) == 3

    each(lst2, IO.print) // HelloWorld!

    // (Hello, (World, (!, )))
    assert foldr(lst2, `++`, "") == "HelloWorld!"
}

{
    fun NodeOf(T) {
        fun(sumTwo: (a: T, b: T) => T, init: T) {
            module {
                record Pair {
                    car: T
                    cdr: Pair|() = ()
                }

                fun sum(n: Pair): T {
                    sumTwo(n.car, match n.cdr {
                            case a: () -> init
                            case a: Pair -> sum(a)
                        }
                    )
                }
            }
        }
    }

    {
        // `+`  (a: Int, b: Int) => a + b
        import Pair as Node, sum from NodeOf(Int)(`+`, 0)
        let lst = Node(car = 1, cdr = Node(car = 2, cdr = Node(car = 3)))
        assert sum(lst) == 6

        debugger sum(Node(car = "str")) // Error
    }

    {
        // `++` (a: Str, b: Str) => a ++ b
        import Pair as Node, sum from NodeOf(Str)(`++`, "")
        let lst = Node(car = "A", cdr = Node(car = "B", cdr = Node(car = "C")))
        assert sum(lst) == "ABC"

        debugger sum(Node(car = 1)) // Error
    }
}

{
    fun PairOf(T) =
        record Pair {
            car: T
            cdr: Pair|() = ()
        }

    let Node = PairOf(Int)

    fun sum(n: Node): Int {
        n.car + match n.cdr {
            case a: () -> 0
            case a: Node -> sum(a)
        }
    }

    let lst = Node(car = 1, cdr = Node(car = 2, cdr = Node(car = 3)))
    assert sum(lst) == 6
}
```

## Literal Type

```
{
    // 抄了点 ts 文档的用例

    let mut x: "hello" = "hello"
    // OK
    x = "hello"
    debugger x = "howdy"


    let mut mutable_str = "str" // 推导成 Str
    let immutable_str = "str"   // 推导成 字面类型 "str"
    {
        fun f1(a: immutable_str) = a
        fun f2(a: "str") = a

        f1(immutable_str)
        f2(immutable_str)

        // 这里应该报错的, 但是不报错貌似也没问题
        // PatternDefineChecker.java  Literal.clear() 取消注释 就可以了
        f1(mutable_str)
        f2(mutable_str)
    }

    fun printText(s: Str, alignment: "left" | "right" | "center") {}
    printText("Hello, world", "left")
    debugger printText("G'day, mate", "centre")


    fun compare(a: Str, b: Str): -1 | 0 | 1 = if(a == b, 0, if(a > b, 1, -1))
    assert compare("a", "a") == 0
    assert compare("a", "b") == -1
    assert compare("b", "a") == 1

    // -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=

    let Num = Int | Float
    record Options {
      width: Num
    }

    fun configure(x: Options | "auto") {}
    configure([ width: 100 ])
    configure("auto")
    debugger configure("automatic")

    // -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=

    fun handleRequest(url: Str, method: "GET"|"POST") {}
    debugger handleRequest("", "PUT")

    let req = [ url: "https://example.com", method: "GET" ]
    handleRequest(req.url, req.method)

    let req1 = [ url: "https://example.com", mut method: "GET" ]
    debugger handleRequest(req1.url, req1.method)

    let req2 = record { url: Str = "https://example.com", method: "GET"|"POST" = "GET" } ()
    handleRequest(req2.url, req2.method)

    {
        fun req(method: "GET"|"POST") {}
        let conf = [method: "GET"]
        req(conf.method) // method 不能被修改, 通过
    }

    {
        fun req(method: "GET"|"POST") {}
        let conf = [mut method: "GET"] // 这里 mut, method 被推导为 Str
        debugger req(conf.method) // 所以这里类型检查不通过
    }

    {
        fun req(method: "GET"|"POST") {}
        let conf = [mut method: "GET"]
        conf.method = "POST" // assign 不会变更 method 类型
        debugger req(conf.method) // 不通过
    }

    {
        fun req(method: "GET"|"POST") {}
        let conf = [mut method: "GET"] // 这里 mut, method 被推导为 Str
        req(conf.method as "GET") // 可以这么解决, Str cast 成 "GET" 字面类型
    }

    {
        record Req {
            url: Str = "https://example.com"
            mut method: Str = "GET"
        }
        let req = Req()
        debugger handleRequest(req.url, req.method)
    }

    {
        record Req {
            url: Str = "https://example.com"
            mut method: "GET"|"POST" = "GET"
        }
        let req = Req()
        debugger req.method = "PUT"
        // 这里 method 虽然是 mut, 但是怎么被 assign 都是 GET|POST, 所以类型检查通过
        handleRequest(req.url, req.method)
    }
}

{
    // 注意这里必须用 type 声明
    // let CardinalDirection = "North" | "East" | "South" | "West" // 会被推导成 Str
    type CardinalDirection = "North" | "East" | "South" | "West"
    fun move(distance: Float, direction: CardinalDirection) = Void
    move(1,"North")
    move(1,"East")
    move(1,"South")
    move(1,"West")
    debugger move(1,"Nurth")

    type OneToFive = 1 | 2 | 3 | 4 | 5
    type Bools = true | false
}

{
    type Sex = "male" | "female"
    let f = (s: Sex) => s
    f("male")
    f("female")
    debugger f("hello")
    debugger f(1)
}

{
    type A = record {
        a: Int
        b: 42|3.14
    }
    A(a=1, b=42)
    A(a=1, b=3.14)
    debugger A(a=1, b=1)
    debugger A(a=1, b=1.1)
    debugger A(a=1, b="s")
}

{
    type T = (42, "xiao")
    let f = (t: T) => t
    f(t = (42, "xiao"))
    debugger f(t = (1, "xiao"))
}

{
    type T = ((1, true), (2, false))
    let f = (t: T) => t
    f(t = ((1, true), (2, false)))
    debugger f(t = ((2, true), (2, false)))
    debugger f(t = ((1, true), (1, false)))
}

{
    {
        // 参数协变
        let a: ((a: Int) => Int, ) = ((a: Float) => Int,)
        let b: ((a: Int) => Int, ) = a
    }


    {
        // 返回值逆变
        let a: ((a: Int) => Float, ) = ((a: Int) => Int,)
        let b: ((a: Int) => Float, ) = a
    }

    let a: ((a: Int) => Float, ) = ((a: Float) => Int,)
    let b: ((a: Int) => Float, ) = a
}

```

## Dependent Type

```
{
    let isSingleton = (b: Bool) => if(b, Int, Int[])
    // 当然这个返回类型省略也可以
    fun mkSingle (x : Bool): isSingleton(x) {
        match x {
            case true -> 0
            case false -> []
        }
    }
    assert mkSingle(true) == 0
    assert Vectors.size(mkSingle(false)) == 0
}


{
    fun validate(a: Bool) = if a { Int } else { Str }
    fun answer(a: Bool, b: validate(a)) = b

    assert answer(true, 42) == 42
    assert answer(true, 1) == 1
    assert answer(false, "Hello") == "Hello"
    assert answer(false, "World") == "World"

    debugger answer(true, "Hi") // Error
    debugger answer(false, 42)  // Error
}


{
    // 字面类型
    type Answer = 42
    type Question = "???"

    fun validate(a: Bool) = if a { Answer } else { Question }
    fun answer(a: Bool, b: validate(a)) = b

    assert answer(true, 42) == 42
    assert answer(false, "???") == "???"

    debugger answer(true, 1)        // Error
    debugger answer(false, 42)      // Error
    debugger answer(true, "???")    // Error
    debugger answer(false, "hello") // Error
}

{
    import size from Vectors

    // 返回类型声明
    fun boundedCheck(v: Int[], i: Int) {
        if i >= 0 && i < size(v) {
            Int
        } else {
            Void
        }
    }

    fun idx(v: Int[], i: Int): boundedCheck(v, i) = v[i]

    // 动态长度数组类型编译期越界检查
    assert idx([42], 0) + 1 == 43
    assert idx([0, 42], 1) + 1 == 43

    // 根据 boundedCheck 类型检查阶段发现数组越界
    // 返回值类型错误, 声明 (), 实际是 Int
    debugger idx([], 0)     // Error
    debugger idx([], 1)     // Error
    debugger idx([42], 1)   // Error

    // 动态长度数组类型编译期越界检查

    let vec = if Math.random() > 0.5 { [] as Int[] } else { [1, 2, 3] }
    // 动态数组返回类型 ()|Int
    // v[i]: Int    <:    ()|Int, 类型检查通过, 但是运行时可能报错
    // idx(vec, 1)
}
```


## Pattern Matching

```

{
    let tuple_len = tuple => match tuple {
        case () -> 0
        // 数量为 1, 绑定到 a
        case (a,) -> a
        // 数量为 2, 绑定到 a b
        case (a, b) -> a + b
        // 数量为 3, 绑定到 a c
        case (a, _, c) -> a + c
        case _ -> -1
    }
    assert tuple_len(()) == 0
    assert tuple_len((1,)) == 1
    assert tuple_len((1, 2)) == 3
    assert tuple_len((1, 2, 3)) == 4
    assert tuple_len((1, 2, 3, 4)) == -1
}

{
    let rec_sz = rec => match rec {
        case [:] -> 0
        // 数量为 1，为 a, 且绑定到 a
        case [a:a] -> a
        // 数量为 2, 为 a、b,且绑定到 a、b
        case [a:a, b:b] -> a + b
        // 数量为 3, 包含 a、c, 且绑定到 a、c
        case [a:a, _:_, c:c] -> a + c
        case _ -> -1
    }
    IO.println(rec_sz([a: 1]))
    IO.println("_-------------------------------------")
    assert rec_sz([:]) == 0
    assert rec_sz([a: 1]) == 1
    assert rec_sz([a: 1, b: 2]) == 3
    assert rec_sz([a: 1, b: 2, c: 3]) == 4
    assert rec_sz([a: 1, b: 2, c: 3, d: 4]) == -1
}

{
    // guard 语句
    let test = tuple => match tuple {
        case (a, b) if a > b -> 1
        case (a, b) if a == b -> 0
        case (a, b) if a < b -> -1
    }
    assert test((2, 1)) == 1
    assert test((1, 1)) == 0
    assert test((1, 2)) == -1
}


{
    let tuple = ( "subscript", )
    let rec = [ val: "attribute" ]
    let a = 100

    fun test(val) = match val {
        case 1 -> 42                // int 字面量
        case 42.0 -> 1.0            // float 字面量
        case "hello" -> "world"     // str 字面量
        case () -> "empty tuple"      // empty tuple
        case [:] -> "empty rec"      // empty record
        case (3.14, _, a) -> a      // tuple + wildcards + bind
        case [id:1, val:val] -> val     // record + bind
        case 1 + a -> a             // expr
        case tuple[0] -> tuple[0]       // 这里 tuple[0] 与 rec.val 当成表达式执行, 没有当成 bind
        case rec.val -> rec.val
        case ("tuple", tuple[0]) -> tuple[0]
        case (tuple[0], rec.val) -> "x"
        case [a:tuple[0], b:rec.val] -> "y"
        case ("x+y", x, y) -> x + y // id bind
        // case v -> v              // 这里会永远匹配, 不会走到_ 位置
        case _ -> "default"         // wildcards
    }

    assert test(1) == 42
    assert test(42.0) == 1.0
    assert test("hello") == "world"
    assert test(("x+y", 1, 2)) == 3
    assert test(()) == "empty tuple"
    assert test((3.14,)) == "default"
    assert test((3.14, 1)) == "default"
    assert test((3.14, 1, 42)) == 42
    assert test([:]) == "empty rec"
    // assert test([id:1, val:42]) == 42 // todo
    assert test([id:2, val:42]) == "default"
    assert test([id:1]) == "default"
    assert test(101) == a

    assert test(("tuple", tuple[0])) == tuple[0]
    assert test(("tuple", 1)) == "default"

    // assert test(["tuple", tuple[0]]) == tuple[0] // todo
    assert test(["tuple", 1]) == "default"

    assert test(tuple[0]) == tuple[0]
    assert test(rec.val) == rec.val
    assert test((tuple[0], rec.val)) == "x"
    assert test([a: tuple[0], b: rec.val]) == "y"
}

```

## Control Flow

```
if true 1 else 2
if 1 + 1 > 2 { 1 } else { 2 } == 2

{
    let mut i = 1
    while i < 42 {
        i = i + 1
    }
    assert i == 42
}
```

```
// if 函数 if(test, then, else), 求值策略特殊 其中 then 和 else 会按照 test 结果进行求值

let f = (test: Bool) => if (test, 42, "Hello")
assert f(true) == 42
assert f(false) == "Hello"

{
    let mut i: Int = 0
    let r = if(true, 1, {  i = i + 1  "str" })
    // 等同于 if true { 1 } else { i = i + 1 "str" }
    assert r == 1 && i == 0
}
```


## Module

```
{
    // 模块可以嵌套
    module A {
        module B {
            module C {
                let a = 1
                let c = 2
            }
        }
    }

    // import * 之后也可以起别名
    import *, c as b from A.B.C
    assert a == 1
    assert b == 2
}

import *, print as echo from IO
echo("HELLO" ++ "\n")
echo("WORLD" ++ "\n")

{
    let a: Int[] = [] // 在 assign 时候处理
    import append from Vectors
    IO.println(a)
    append(a, 2)
    IO.println(a)

    debugger let union_vec: (Int|Str)[] = [1]

    let union_vec: (Int|Str)[] = []
    append(union_vec, 1)
    append(union_vec, "Hello")
    IO.println(union_vec)
}
```

## Operators

```
// 测试 -操作符

// assert 1--1 // 注意: 要先声明 -- 操作符
assert - -1   == 1
assert 0 - 1  == -1
assert 1-1    == 0
assert 1 - 1  == 0
assert 1- -1  == 2
assert 1 - -1 == 2


// 测试 函数声明成中缀操作符

// 函数声明成中缀操作符
infixr `add` // 默认优先级 20 最高
let add = (a: Int, b: Int) => a + b
assert 1 `add` 2 == 3
assert 1 `add` 2 `add` 3 - 1 == 5
// (((1 `add` 2) `add` 3) - 1) == 5

// 测试 操作符提取成函数

let addInt = (_: Int, _: Int)=>Int
let fab = (f: addInt, a: Int, b: Int) => f(a, b)
assert fab(`+` as addInt, 1, 2) == fab((a: Int, b: Int) => a + b, 1, 2)

// 测试 中缀操作符转前缀使用

// 语义是提取 fun 调用
// !!! 注意, 右结合操作符转成前缀（提取出来使用）的逻辑目前没处理, 需要依赖 fun 本身处理参数的顺序决定 !!!
// 不过似乎也没啥右结合操作符

assert `-`(1) == -1
assert `-`(1, 2) == 1 - 2
assert `-`(1, 2, 3) == 1 - 2 - 3

assert "Hello" == "Hello"
assert "Hello" ++ " World" == "Hello World"
assert "Hello" ++ " World" ++ "!" == "Hello World!"

assert `++`("Hello") == "Hello"
assert `++`("Hello", " World") == "Hello" ++ " World"
assert `++`("Hello", " World", "!") == "Hello" ++ " World" ++ "!"
assert `<`(1, 2) == 1 < 2


// 测试 局部重载操作符
// case 1
{
    // 可以获取到原来的 +
    let prim_plus = `+`
    let + = (a: Int, b: Int) => a - b
    assert 1 + 2 == -1
    assert prim_plus(1, 2) == 3
}
// 作用域外不影响
assert 1 + 2 == 3

// case 2
{
    let + = (a: Int, b: Int) => a * b
    assert 3 + 4 == 12
}
// 离开作用域回复
assert 3 + 4 == 7


// 测试 自定义操作符

{
    infixn >..<
    let >..< = (a: Int, b: Int) => a * b
    assert 3 >..< 4 == 12
}
// 离开作用域 >..< 失效
// 3 >..< 4 == 12

{
    infixn >..<
    let >..< = (a: Int, b: Int) => a - b
    assert 3 >..< 4 == -1
    // 同一个作用域不能定义相同符号
    // infixn >..<
}

// 在局部把 - 改成右结合
assert 1 - 2 - 3 == -4
{
    infixr 7 -
    assert 1 - 2 - 3 == 2
}
assert 1 - 2 - 3 == -4


{
    import * from Vectors
    import * from IO
    infixl <<
    // v 没声明类型, 默认 Any
    let << = (vec: Any[], v) => append(vec, v)
    let a: (Int|Str)[] = []
    a << 1 << "2" << 3
    assert  a[0] == 1 && a[1] == "2" && a[2] == 3
    debugger a << 3.14
}

// 类型声明的 `:` parser 特殊处理过, 不受自定义操作符影响
{
    infixn :
    let : = (a: Int, b: Int) => a + b
    let f = (a : Int, b : Int) => a : b
    assert f(1, 2) == 3
}

```