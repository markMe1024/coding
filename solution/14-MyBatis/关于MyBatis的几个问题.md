1. MyBatis是什么？

   答：MyBatis是一个Java持久层框架。

2. MyBatis与Hibernate的主要区别？

   - 与Hibernate不同，MyBatis是一款半自动ORM框架。虽然它也允许以操作对象的方式操作数据库，但是用户最终还是要写原生SQL的。

   - MyBatis没有将Java对象和数据库表关联，而是将Java方法与SQL语句关联起来。 这为MyBatis提供了更大的灵活性，特别是在写复杂查询语句时，尤为明显。

3. MyBatis与Spring的JdbcTemplate的主要区别？

   - 和JdbcTemplate一样，最终都是写原生Sql。但是，基于MyBatis能更好的将返回结果映射为一个Java对象。
   - 相比于JdbcTemplate手动编写动态SQL，MyBatis的动态SQL功能更简单易用。

4. 为什么要使用MyBatis？

   答：结合MyBatis的逆向工程插件，通过数据库表自动生成实体类、Mapper接口，以及Mapper的xml文件。MyBatis可以以很少的代码量，兼具ORM框架的以操作对象的方式操作数据库的能力，以及JdbcTemplate写复杂查询语句的能力，并且可以将查询结果很好的映射为一个Java对象。

5. MyBatis-Plus是啥？

   答：MyBatis-Plus是一个MyBatis的增强工具，在MyBatis的基础上只做增强不做改变，是国内作者开发的一个开源项目。

6. MyBatis-Plus和Hibernate可以共存吗？

   答：可以。

7. MyBatisX是什么？

   答：IDEA插件，用以支持MyBatis-Plus，支持跳转、自动补全生成SQL，代码生成。

8. 为什么需要MyBatis和Hibernate共存？

   答：其实仅使用MyBatis也可以，但是因为历史代码大部分都是Hibernate，所以需要两者共存，以使用MyBatis的特性。