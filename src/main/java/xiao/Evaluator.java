package xiao;

import org.jetbrains.annotations.NotNull;

import static xiao.front.Ast.Node;

/**
 * Node Evaluator With Context
 * 对 Node 执行一些计算, 用来隔离一些依赖
 * @author chuxiaofeng
 */
public interface Evaluator<C, R> {

    R eval(@NotNull Node node, @NotNull C ctx);

}
