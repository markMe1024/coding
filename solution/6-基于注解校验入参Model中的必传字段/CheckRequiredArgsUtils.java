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
            if (annotations == null || annotations.length == 0) {
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
     * 校验必传参数非空<br><br>
     * 通过校验，返回null。否则，返回未赋值的必传参数的{@code @ApiModelProperty}注解的{@code value}参数值（如果{@code value}参数值为空，返回该参数名）
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

            // 嵌套model必传参数校验
            /*
             * 嵌套model必传参数校验引发了几个问题需要思考：
             *  1.怎么知道当前属性是嵌套model？ 在嵌套model上使用@CheckRequiredArgs注解标识
             *  2.嵌套model本身是否需要判断是否是必传字段？  需要，因为如果它是一个必传参数，那么就不应该允许传一个null进来
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
