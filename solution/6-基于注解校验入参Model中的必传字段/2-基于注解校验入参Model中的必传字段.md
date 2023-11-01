[toc]

# 一、概述

后端服务提供的接口，通常需要校验必传参数。Url中的必传参数不需要在代码里单独校验，因为基于Spring注解，缺少必传参数的接口将无法访问。但是当请求入参是一个实体类时，则需要单独对实体类内必传字段进行校验。

本方案就是用来解决这个问题的，<span style="color: red">使用者无需在代码里单独进行校验，只需要加上一个自定义注解，程序即可自动完成入参Model必传字段校验。即使入参Model内嵌套其他需要校验的Model或Model集合，该解决方案也可以以递归方式完成多层校验。</span>

项目基于`Swagger`生成接口文档，我们也是通过`Swagger`的`@ApiModelProperty`注解通知前端哪些属性是必传的。因此，该方案选择使用`@ApiModelProperty`注解的`required`参数标识必传字段，也算水到渠成、名正言顺。

以下将通过两部分内容，分别阐述该方案的实现细节，以及使用方法。

# 二、实现细节

## 1. 自定义注解

通过自定义注解，标识需要校验的入参Model，标识入参Model内需要校验的嵌套Model。代码如下：

```java
package com.inspur.incloud.aop.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 将该注解加在入参<b>Model</b>上之后，系统会自动校验该<b>Model</b>中的必传字段，如果必传字段未传值，将会封装异常返回。
 * <br><br>
 * 请对<b>Controller</b>层的入参Model使用该注解，因为系统扫描范围设定在<b>Controller</b>层。
 * <br><br>
 * 示例：
 * <pre>
 *     <code>@PostMapping</code>
 *     public OperationResult<CommonReturnBean> plant(@CheckRequiredArgs @RequestBody Tree tree) throws Exception {
 *
 *     }
 * </pre>
 * 此外，该注解还可以加在入参Model内嵌套Model属性上，用来标识该嵌套Model需要校验。
 */
@Target({ElementType.PARAMETER, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface CheckRequiredArgs {

}

```

## 2. 工具类

入参Model中必传字段校验，以及入参Model内嵌套Model中必传字段校验，实际上是通过该工具类完成的，该工具类内包含该方案实现的主要代码逻辑。代码如下：

```java
package com.inspur.incloud.aop.util;

import com.inspur.incloud.aop.annotation.CheckRequiredArgs;
import io.swagger.annotations.ApiModelProperty;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 校验入参model中的必传字段<br><br>
 *
 * 必传字段由Swagger注解@ApiModelProperty的required参数等于true标识
 */
@Slf4j
public class CheckRequiredArgsUtils {

    /**
     * 校验请求入参<br><br>
     * 如果目标方法有{@code @CheckRequiredArgs}注解修饰的入参Model，且该Model有未赋值的必传字段，则返回该字段的{@code @ApiModelProperty}注解的{@code value}参数值（如果{@code value}参数值为空，返回该参数名）；其余情况返回<b>null</b>
     * @param joinPoint
     */
    public static String check(ProceedingJoinPoint joinPoint) {
        try {
            // 1.匹配
            Class<?> type = getType(joinPoint);
            if (type == null) {
                return null;
            }

            // 2.校验
            Object[] args = joinPoint.getArgs();
            if (args == null || args.length == 0) {
                return null;
            }

            for (Object arg : args) {
                if (arg.getClass() == type) {
                    return requiredArgsNonEmpty(arg);
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        // 3.
        return null;
    }

    /**
     * 获取目标方法的参数列表中{@code @CheckRequiredArgs}注解修饰的参数的参数类型<br><br>
     * 如果未匹配到，返回<b>null</b>
     * @param joinPoint
     */
    private static Class<?> getType(ProceedingJoinPoint joinPoint) {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        if (method == null) {
            return null;
        }

        Parameter[] params = method.getParameters();
        if (params == null || params.length == 0) {
            return null;
        }

        for (Parameter param : params) {
            Annotation[] annotations = param.getDeclaredAnnotations();
            if (annotations == null && annotations.length == 0) {
                continue;
            }

            for (Annotation annotation : annotations) {
                if (annotation instanceof CheckRequiredArgs) {
                    return param.getType();
                }
            }
        }

        return null;
    }

    /**
     * 校验必传字段非空<br><br>
     * 通过校验，返回null。否则，返回未赋值的必传字段的{@code @ApiModelProperty}注解的{@code value}参数值（如果{@code value}参数值为空，返回该参数名）
     * @param obj
     */
    private static String requiredArgsNonEmpty(Object obj) throws IllegalAccessException {
        // 递归调用时obj可能为null
        if (obj == null) {
            return null;
        }

        Field[] fields = obj.getClass().getDeclaredFields();
        if (fields == null || fields.length == 0) {
            return null;
        }

        for (Field field : fields) {
            ApiModelProperty apiModelProperty = field.getAnnotation(ApiModelProperty.class);
            if (apiModelProperty != null && apiModelProperty.required()) {
                try {
                    field.setAccessible(true);
                    Object value = field.get(obj);

                    // 判空
                    boolean isEmpty = isFieldEmpty(value);

                    if (isEmpty) {
                        if (StringUtils.isNotBlank(apiModelProperty.value())) {
                            return apiModelProperty.value();
                        } else {
                            return field.getName();
                        }
                    }
                } catch (IllegalAccessException e) {
                    log.error(e.getMessage(), e);
                }
            }

            // 嵌套model必传字段校验
            /*
             * 嵌套model必传字段校验引发了几个问题需要思考：
             *  1.怎么知道当前属性是嵌套model？ 在嵌套model上使用@CheckRequiredArgs注解标识
             *  2.嵌套model本身是否需要判断是否是必传字段？  需要，因为如果它是一个必传字段，那么就不应该允许传一个null进来
             *  3.如果model本身非必传，该model的必传字段判空是否还有意义？ 分情况，如果它是null，那么它的必传字段的校验也变得没有意义；如果它不是null，那么它的必传字段则是需要校验的
             *  4.经过第1、2步的判断之后，无论model本身是否必传，只要model非null，就递归判断
             * 代码中逻辑，按照以上思考结论实现
             */
            CheckRequiredArgs checkRequiredArgs = field.getAnnotation(CheckRequiredArgs.class);
            if (checkRequiredArgs != null) {
                field.setAccessible(true);
                Object value = field.get(obj);

                // 递归
                if (value instanceof Collection) {
                    if (CollectionUtils.isNotEmpty((Collection) value)) {
                        for (Object element : ((Collection) value)) {
                            String checkResult = requiredArgsNonEmpty(element);
                            if (checkResult != null) {
                                return checkResult;
                            }
                        }
                    }
                } else {
                    String checkResult = requiredArgsNonEmpty(value);
                    if (checkResult != null) {
                        return checkResult;
                    }
                }
            }
        }

        return null;
    }

    /**
     * Field判空
     */
    private static boolean isFieldEmpty(Object value) {
        if (value == null) {
            return true;
        }

        // 集合
        if (value instanceof Collection && CollectionUtils.isEmpty((Collection) value)) {
            return true;
        }

        // 数组
        if (value.getClass().isArray() && ((Object[]) value).length == 0) {
            return true;
        }

        // Map
        if (value instanceof Map && CollectionUtils.isEmpty(((Map) value).keySet())) {
            return true;
        }

        // 字符串
        if (value instanceof String && StringUtils.isBlank(String.valueOf(value))) {
            return true;
        }

        return false;
    }
}

```



## 3. Aop

利用Spring提供的切面编程，完成每个接口的自动扫描，调用上面的工具类，完成校验。代码如下：

```java
package com.inspur.incloud.inetwork.aop;

import com.inspur.incloud.common.OperationResult;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.Arrays;

import static com.inspur.incloud.aop.util.CheckRequiredArgsUtils.check;

/**
 * 基于注解实现入参Model必传字段校验
 */
@Slf4j
@Aspect
@Component
public class CheckRequiredArgsController {

    @Around("execution(public * com.inspur.incloud.inetwork.*.controller..*(..))")
    public Object doAround(ProceedingJoinPoint joinPoint) throws Throwable {
        // 1.校验
        String checkResult = check(joinPoint);

        // 2.未通过校验
        if (checkResult != null) {
            return OperationResult.fail("INETWORK_IN_PARAM_IS_NULL_SPECIFY", Arrays.asList(checkResult));
        }

        // 3.
        return joinPoint.proceed();
    }

}

```

# 三、使用方法

## 1. 引入依赖

在本项目的`pom.xml`中，引入`incloudmanager-aop-util`依赖，需要引入这个依赖是因为*自定义注解*和*工具类*都是在这个项目里定义的。

```xml
<dependency>
    <groupId>com.inspur.incloudmanager</groupId>
    <artifactId>incloudmanager-aop-util</artifactId>
</dependency>
```

## 2. 定义Aop

按照第二章**实现细节**中的[Aop](#3-aop)的实现方法，在本模块定义切面，*修改扫描位置，自定义错误码*。至此，该功能已经开始运转了。

## 3. Model中标识必传字段和嵌套Model

通过`@ApiModelProperty`注解的`required`参数标识字段是否必传。通过自定义注解`@CheckRequiredArgs`标识需要校验的嵌套Model。示例代码如下：

```java
package com.inspur.incloud.inetwork.client.publiccloud.common.model.securityGroup;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel(value = "创建安全组")
public class SecurityGroup4Create implements Serializable {

    private static final long serialVersionUID = 8118716585714099426L;

    @ApiModelProperty(value = "名称", required = true)
    private String name;

    @ApiModelProperty(value = "vdcId", required = true)
    private String vdcId;

    @ApiModelProperty(value = "vpcId", required = true)
    private String vpcId;

    @ApiModelProperty(value = "描述")
    private String description;
    
    @CheckRequiredArgs
    @ApiModelProperty(value = "安全组规则", required = true)
    private SgRule4Create sgRule4Create;

}
```

## 4. Controller层添加自定义注解

`@CheckRequiredArgs`注解需要加在Controller层方法的入参Model上。使用该注解标识的入参Model，会自动进行必传字段校验。示例代码如下：

```java
@Override
@ResponseBody
@OptLog(optTarget = "PUBLICCLOUD_SECURITY_GROUP", optType = "PUBLICCLOUD_SECURITY_GROUP.ADD", optLevel = EventLevel.LOW, isSyn = true)
@PostMapping
public OperationResult<CommonReturnBean> add(@CheckRequiredArgs @RequestBody SecurityGroup4Create sg4Create) throws Exception {
    Lock lock = null;
    try {
        lock = LockUtil.getAcquiredLock(LockUtil.LockType.SECURITYGROUP, sg4Create.getName());
        lock.lock();

        String sgId = getUUID();
        idVal.set(sgId);
        nameListVal.get().add(sg4Create.getName());

        sgService.add(sg4Create, sgId);

        return succ(new CommonReturnBean(sgId));
    } finally {
        LockUtil.releaseLock(lock, log);
    }
}
```
