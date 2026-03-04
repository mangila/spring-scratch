package com.github.mangila.app.shared.chaos;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Chaos {

    /**
     * Probability of failure (0.0 to 1.0).
     * 0.1 means 10% of calls will fail.
     */
    double probability() default 0.1;

    String message() default "Chaos struck: Simulated Method Call Failure";
}
