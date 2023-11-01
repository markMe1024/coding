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
