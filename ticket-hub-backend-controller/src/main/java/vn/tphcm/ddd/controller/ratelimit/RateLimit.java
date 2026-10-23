/*
 * @ (#) RateLimit.java       1.0     10/13/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.controller.ratelimit;
/*
 * @author: Luong Tan Dat
 * @date: 10/13/2026
 */

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {
    int limit();

    int windowSeconds() default 60;
}
