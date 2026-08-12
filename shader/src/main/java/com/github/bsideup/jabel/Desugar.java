package com.github.bsideup.jabel;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.TYPE, ElementType.RECORD_COMPONENT, ElementType.CONSTRUCTOR})
public @interface Desugar {
}
