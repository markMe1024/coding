[toc]



&emsp; &emsp;在API调用中，参数校验是一个普遍需求，如校验必传、字符长度、最大最小值等等。实际上业界已经形成规范即`JSR`，也有该标准的实现如`Hibernate Validator`、`Apache BVal`等。本文中我们就讨论下较常用的`Hibernate Validator`具备的能力和使用方法。

# 1.概述

&emsp;&emsp;`JSR`是一种用于Bean校验的Java API规范，用于保证Bean校验符合特定标准。这个规范本身也在不断迭代，历史版本包括`JSR303`、`JSR349`以及`JSR380`。本文基于`JSR380`版本进行讨论。

&emsp;&emsp;依据`JSR`规范，`Validation Api`定义了一系列标准接口，`Hibernate Validator`则实现并扩展了这些接口。以下为`JSR`的每个版本和`Validation Api`版本的对应关系：

| JSR版本 |        别名         | Validation Api版本 |
| :-----: | :-----------------: | :----------------: |
| JSR303  | Bean Validation 1.0 |        1.0         |
| JSR349  | Bean Validation 1.1 |        1.1         |
| JSR380  | Bean Validation 2.0 |        2.0         |

# 2.依赖

&emsp;&emsp;SpringBoot本身已经集成了`Hibernate Validator`。**所以我们不需要额外引入依赖，就已经可以使用`5.3.6.Final`（基于JSR349）版本`Hibernate Validator`了**。

&emsp;&emsp;但是这里也提供了一个额外选择，如果想要使用高版本的`Hibernate Validator`，获取更多的标准注解支持,也可以在**项目的client和service模块里**单独引入以下两个依赖（基于JSR380）：

```xml
<dependency>
    <groupId>javax.validation</groupId>
    <artifactId>validation-api</artifactId>
    <version>2.0.1.Final</version>
</dependency>
<dependency>
    <groupId>org.hibernate</groupId>
    <artifactId>hibernate-validator</artifactId>
    <version>6.0.17.Final</version>
</dependency>
```

*注：这两个依赖的依赖树已经导入研发内网nexus库中*：

# 3.注解

&emsp;&emsp;`Hibernate Validator`是基于注解发挥作用的，下面是一个使用示例：

```java
package com.inspur.validator.model;

import lombok.Data;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.Range;

import javax.validation.constraints.*;
import java.util.List;

/**
 * @author mark
 * @date 2020/10/28 10:38
 */
@Data
public class Department {

    /**
     * 主键
     */
    private Integer id;

    /**
     * 预算
     * 非null
     * 值为false，则抛出异常，异常信息为：只可以为true
     */
    @NotNull
    @AssertTrue(message = "只可以为true")
    private Boolean locatedInJiNan;

    /**
     * 部门名称
     * 非null、非空、非纯空格字符串
     */
    @NotBlank
    private String name;

    /**
     * 预算
     * 最大值100，最小值1
     */
    @Min(1)
    @Max(100)
    private Double budget;

    /**
     * 描述
     * 字符长度在[1, 100]之间
     */
    @Length(min = 1, max = 100)
    private String description;

    /**
     * 部门员工
     * 列表非null、非空
     * 最小员工数50，最大员工数300
     */
    @NotEmpty
    @Size(min = 50, max = 300)
    private List<Employee> employees;

}
```



## 3.1.validation-api标准注解（基于JSR380）

&emsp;&emsp;这些标准注解，定义在*validation-api* jar包内，*javax.validation.constraints*文件夹下。不同版本的jar包，包含的注解不同。

|        注解        |             作用             |                作用对象                |                使用示例                 |
| :----------------: | :--------------------------: | :------------------------------------: | :-------------------------------------: |
|   `@AssertTrue`    |            `true`            |          `boolean`或`Boolean`          | `@AssertTrue(message = "只可以为true")` |
|   `@AssertFalse`   |           `false`            |          `boolean`或`Boolean`          |                                         |
|       `@Max`       |            最大值            |                `Number`                |           `@Max(value = 256)`           |
|       `@Min`       |            最小值            |                `Number`                |            `@Min(value = 2)`            |
|     `Positive`     |             正数             |                `Number`                |                                         |
|  `PositiveOrZero`  |           正数或零           |                `Number`                |                                         |
|     `Negative`     |             负数             |                `Number`                |                                         |
|  `NegativeOrZero`  |           负数或零           |                `Number`                |                                         |
|      `@Null`       |             null             |                `Object`                |                                         |
|     `@NotNull`     |            非null            |                `Object`                |                                         |
|    `@NotEmpty`     |         非null、非空         | `String`、`Collection`、`Map`、`Array` |                                         |
|    `@NotBlank`     | 非null、非空、非纯空格字符串 |                `String`                |                                         |
|      `@Size`       |    限制字符长度或集合容量    |         `String`、`Collection`         |       `@Size(min = 1, max = 128)`       |
|      `@Past`       |         当前时间之前         |                  日期                  |                                         |
|  `@PastOrPresent`  |    当前时间或当前时间之前    |                  日期                  |                                         |
|     `@Future`      |         当前时间之后         |                  日期                  |                                         |
| `@FutureOrPresent` |    当前时间或当前时间之后    |                  日期                  |                                         |
|     `@Pattern`     |          正则表达式          |                                        |                                         |

## 3.2.hibernate-validator扩展注解（基于版本6.0.17.Final）

|       注解        |        作用        |           作用对象            |           使用示例            |
| :---------------: | :----------------: | :---------------------------: | :---------------------------: |
|     `@Length`     |     字符串长度     |           `String`            | `@Length(min = 10, max = 20)` |
|     `@Range`      |      数值范围      | `Number`或值为数值的 `String` | `@Range(min = 1, max = 100)`  |
| `@UniqueElements` | 集合中每个元素唯一 |         `Collection`          |                               |



# 4.使用

&emsp;&emsp;在API调用中，有两种传参方式，一种以`Bean`的形式，另外一种以附加在`URL`上的形式。对于这两种传参方式，`Hibernate Validator`都可以提供校验能力，下面就依次说明它们分别如何处理。

## 4.1.Bean

1. 在`Bean`的属性上添加注解，示例如下：

   ```java
   @Data
   public class Department {
   
       @NotBlank
       private String name;
   
   }
   ```

2. 在<span style="color: red">`Api`</span>和<span style="color: red">`Controller`</span>层入参`Bean`上添加`@org.springframework.validation.annotation.Validated`注解，示例如下：

   ```java
   @RestController
   @RequestMapping("/department")
   public class DepartmentController {
   
       @PostMapping
       @RequestMapping("/add")
       public ReturnBean add(@Validated @RequestBody Department department) {
           return ReturnBean.success();
       }
       
   }
   ```

   

## 4.2.URL

1. 在<span style="color: red">`Api`</span>和<span style="color: red">`Controller`</span>层添加注解`@org.springframework.validation.annotation.Validated`，示例如下：

   ```java
   @Validated
   @RestController
   @RequestMapping("/department")
   public class DepartmentController {
   
   }
   ```

2. 在<span style="color: red">`Api`</span>和<span style="color: red">`Controller`</span>层需要校验的`URL`参数上加上注解，示例如下：

   ```java
   @Validated
   @RestController
   @RequestMapping("/department")
   public class DepartmentController {
   
       @RequestMapping("/update/{name}")
       @PostMapping
       public ReturnBean update(
   	    @Length(min = 1, max = 3) @PathVariable("name") String name,
           @Length(min = 1, max = 3) @RequestParam(name = "desc", required = false) String desc) {
           return ReturnBean.success();
       }
   }
   ```

## 4.3.Q&A

### 4.3.1.特殊的null值

&emsp;&emsp;如果入参值为`null`，注解通常不做校验。什么意思呢？举个栗子，你在一个Bean的`name`属性上加了`@Length`注解来校验名称长度：

```java
@Length(min = 1, max = 10)
private String name;
```

&emsp;&emsp;但是入参`name`传了`null`，那么*Hibernate Validator*的校验结果是通过校验，为什么呢？我们看下源码就知道了。（实现校验逻辑的源码在*hibernate-validator* jar包，*org.hibernate.validator.internal.constraintvalidators*文件夹内）我们找到类`SizeValidatorForCharSequence`：

```java
@Override
public boolean isValid(CharSequence charSequence, ConstraintValidatorContext constraintValidatorContext) {
    if ( charSequence == null ) {
        return true;
    }
    int length = charSequence.length();
    return length >= min && length <= max;
}
```

&emsp;&emsp;从源码中，我们可以看到，当入参为`null`时，直接返回`true`了，也就是通过校验了。

&emsp;&emsp;所以为了能把`null`也校验住，我们这里除了使用`@Length`注解，也还要配合使用`@NotNull`注解：

```java
@NotNull
@Length(min = 1, max = 10)
private String name;
```

&emsp;&emsp;这样，无论入参的`name`传的值是`null`或者不符合长度限制，就都不能通过校验了。

&emsp;&emsp;当然，也还有一小部分注解，比如`@NotBlank`、`@NotEmpty`，会校验`null`值，入参为`null`校验是不会通过的。下面是`@NotEmpty`校验的源码：

```java
@Override
public boolean isValid(CharSequence charSequence, ConstraintValidatorContext constraintValidatorContext) {
    if ( charSequence == null ) {
        return false;
    }
    return charSequence.length() > 0;
}
```

### 4.3.2.注解可重复添加

&emsp;&emsp;注解是可以重复添加到一个Bean属性上的，示例如下：

```java
@Length(min = 1, max = 10)
@Length(min = 20, max = 30)
private String name;
```

&emsp;&emsp;如上，`name`属性的长度会限制在两个范围之内`[1, 10]` & `[20, 30]`，也还有另外一种写法：

```java
@Length.List({
    @Length(min = 1, max = 10),
    @Length(min = 20, max = 30)
})
private String name;
```

&emsp;&emsp;之所以可以这样定义，是因为在`@Length`注解内定义了内部注解`@List`，`@Length`源码如下：

```java
...
public @interface Length {
	...

	/**
	 * Defines several {@code @Length} annotations on the same element.
	 */
	@Target({ METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER, TYPE_USE })
	@Retention(RUNTIME)
	@Documented
	public @interface List {
		Length[] value();
	}
}

```

&emsp;&emsp;以上两种写法，最终实现效果相同。

### 4.3.3.嵌套Bean校验

&emsp;&emsp;如果在一个Bean里，嵌套了另一个Bean，这个Bean内的属性`Hibernate Validator`也是可以校验的，只需要在嵌套Bean上加上`javax.validation.Valid`注解就可以了，示例代码如下：

```java
package com.inspur.validator.model;

import lombok.Data;
import javax.validation.Valid;

@Data
public class Department {

    private Integer id;

    @Valid
    private Employee employee;

}
```

### 4.3.4.校验集合内每个元素

&emsp;&emsp;通过在泛型上增加注解，`Hibernate Validator`可以校验集合内每个元素，以校验`List`内元素为例，示例代码如下

```java
package com.inspur.validator.model;

import lombok.Data;
import javax.validation.constraints.*;

@Data
public class Department {

    private Integer id;

    private List<@NotEmpty String> employees;

}
```

同大部分注解一样，以上示例中，如果`employees`为null，则校验通过。

### 4.3.5.如果请求入参是对象集合，该如何对集合内每个对象开启校验？

1. 在类上加上`org.springframework.validation.annotation.Validated`注解，示例代码如下：

   ```java
   @Validated
   @RestController
   public class PoolMemberController implements PoolMemberApi {}	
   ```

2. 在集合上加上`javax.validation.Valid`注解，示例代码如下：

   ```java
   @Override
   @ResponseBody
   public OperationResult<List<PoolMemberApiModel>> add(@Valid @NotEmpty @RequestBody List<PoolMember4Create> pm4CreateList) {}
   ```

   



# 5.异常统一处理

&emsp;&emsp;Bean属性校验失败，`Hibernate Validator`抛出`MethodArgumentNotValidException`异常，示例如下：

```json
{
    "timestamp": 1604281270618,
    "status": 400,
    "error": "Bad Request",
    "exception": "org.springframework.web.bind.MethodArgumentNotValidException",
    "errors": [
        {
            "codes": [
                "AssertTrue.department.locatedInJiNan",
                "AssertTrue.locatedInJiNan",
                "AssertTrue.java.lang.Boolean",
                "AssertTrue"
            ],
            "arguments": [
                {
                    "codes": [
                        "department.locatedInJiNan",
                        "locatedInJiNan"
                    ],
                    "arguments": null,
                    "defaultMessage": "locatedInJiNan",
                    "code": "locatedInJiNan"
                }
            ],
            "defaultMessage": "只可以为true",
            "objectName": "department",
            "field": "locatedInJiNan",
            "rejectedValue": false,
            "bindingFailure": false,
            "code": "AssertTrue"
        }
    ],
    "message": "Validation failed for object='department'. Error count: 1",
    "path": "/department/add"
}
```

&emsp;&emsp;URL参数校验失败，`Hibernate Validator`抛出`ConstraintViolationException`异常，示例如下：

```json
{
    "timestamp": 1604281366585,
    "status": 500,
    "error": "Internal Server Error",
    "exception": "javax.validation.ConstraintViolationException",
    "message": "update.desc: 长度需要在1和3之间",
    "path": "/department/update/abf"
}
```

&emsp;&emsp;这两个原生异常，对用户不友好，不能使用错误码，也不符合项目返回数据格式，所以需要进行统一异常处理。统一异常处理定义在`incloudmanager-common`模块下`DefaultRestErrorResolver`文件内。

&emsp;&emsp;通过该统一异常处理，在注解的`message`中声明错误码，最终可以根据语言环境获取到错误码翻译。注解使用示例如下：

```java
@Data
public class Department {

    @NotBlank(message = "INETWORK_NET_VDC_ID_NOT_BLANK")
    private String name;

}
```

&emsp;&emsp;*当然，使用`Hibernate Validator`本身的错误信息也是可以的，并且它也做了国际化*。

&emsp;&emsp;*各模块如果想自定义自己的统一异常处理，可以选择继承该类，并覆盖父类方法。*

# 6.自定义注解及验证器



&emsp;&emsp;除了`validation-api`提供的标准注解，和`hibernate-validator`提供的扩展注解，我们也可以自定义注解，以实现特定的校验需求。下面将以基于自定义注解完成入参必须在指定枚举值范围内校验为例，讨论下怎么实现注解和验证器的自定义：

1. 自定义注解

   1. 与普通注解相比，这种自定义注解需要增加元注解`@Constraint`，并通过`validatedBy`参数指定验证器。
   2. 依据JSR规范，定义三个通用参数：`message`（校验失败保存信息）、`groups`（分组）和`payload`（负载）。
   3. 自定义额外所需配置参数
   4. 定义内部`List`接口，参数是该自定义注解数组，配合元注解`@Repeatable`，可使该注解可以重复添加。

   示例代码如下：

```java
package com.inspur.incloud.common.hibernate.validator.annotations;

import com.inspur.incloud.common.hibernate.validator.validator.EnumsValidatorForCharSequence;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;

/**
 * 校验字符串在指定枚举值范围之内
 * @author mark
 * @date 2020/11/05
 */
@Documented
@Target({FIELD, PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {EnumsValidatorForCharSequence.class})
@Repeatable(Enums.List.class)
public @interface Enums {

    String message() default "不在指定枚举值范围内";

    Class<?>[] groups() default { };

    Class<? extends Payload>[] payload() default { };

    /**
     * 是否可为空<br><br>
     * <b>默认为<code>true</code>；如果值为<code>false</code>，则入参为空串会校验失败</b>
     */
    boolean canBeBlank() default true;

    /**
     * 指定枚举值<br><br>
     * <b>英文逗号分隔的String类型，示例：'asc, desc'</b><br><br>
     */
    String value() default "";

    /**
     * 指定枚举值<br><br>
     * <b>枚举类型，校验优先级低于value</b>
     */
    Class<? extends Enum> target() default Enum.class;

    /**
     * Defines several {@code @Enums} annotations on the same element.
     */
    @Documented
    @Target(ElementType.PARAMETER)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface List {
        Enums[] value();
    }

}
```

2. 自定义验证器

   1. 该验证器需要实现`ConstraintValidator`接口，``ConstraintValidator``接口包含两个类型参数，第一个指定验证器要校验的注解，第二个参数指定要验证的数据类型。
   2. 实现`initialize`方法，通常在该注解中拿到注解的参数值。
   3. 实现`isValid`方法，方法第一个参数是要校验的属性值；校验逻辑写在该方法内；校验通过返回`true`，校验失败返回`false`。

   示例代码如下

```java
package com.inspur.incloud.common.hibernate.validator.validator;

import com.inspur.incloud.common.hibernate.validator.annotations.Enums;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 校验字符串在指定枚举值范围之内
 * @author mark
 * @date 2020/11/2 10:57
 */
@Slf4j
public class EnumsValidatorForCharSequence implements ConstraintValidator<Enums, CharSequence> {

    private boolean canBeBlank;

    private String value;

    private Class<?> target;


    @Override
    public void initialize(Enums parameters) {
        value = parameters.value();
        canBeBlank = parameters.canBeBlank();
        target = parameters.target();
    }

    /**
     * 校验字符串在指定枚举值范围之内
     * @param charSequence The character sequence to validate.
     * @param constraintValidatorContext context in which the constraint is evaluated.
     * @return 如果字符串为空，或字符串在指定枚举值范围之内，返回<code>true</code>，否则返回<code>false</code>
     */
    @Override
    public boolean isValid(CharSequence charSequence, ConstraintValidatorContext constraintValidatorContext) {
        if (charSequence == null) {
            return true;
        }

        // can be blank?
        if (StringUtils.isBlank(charSequence)) {
            return canBeBlank;
        }

        // 根据value校验
        if (StringUtils.isNotBlank(value)) {
            return Arrays.stream(value.split(",")).map(String::trim).anyMatch(charSequence::equals);
        }

        // 根据target校验
        if (target != Enum.class) {
            try {
                target.getDeclaredField(charSequence.toString());
                return true;
            } catch (NoSuchFieldException e) {
                return false;
            }
        }

        return true;
    }

}
```

- ==项目内自定义注解定义在incloudmanager-common模块下hibernate.validator.annotations包内。==

- ==项目内自定义验证器定义在incloudmanager-common模块下hibernate.validator.validator包内。==

- ==当前项目提交了一个自定义注解@Enums，用来校验枚举值，使用方法可参考注解注释==

























