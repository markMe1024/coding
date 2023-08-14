流是什么？



流的特征：

1. 流并不存储数据。
2. 流的操作不会修改其数据源。
3. 流的操作是尽可能惰性执行的。



流的创建：

| 方法                                     | 功能                         |
| ---------------------------------------- | ---------------------------- |
| java.util.Collection.stream()            | 将任何一个集合转换为一个流   |
| java.util.Stream.of(T... values)         | 将数组转换为一个流           |
| java.util.Arrays.stream(array, from, to) | 将数组中部分元素转换为一个流 |



lambda表达式

stream流



api：

- filter，过滤
- count，计数



函数式接口

| 函数     | 特性 |
| -------- | ---- |
| Supplier |      |
| Consumer |      |
|          |      |

