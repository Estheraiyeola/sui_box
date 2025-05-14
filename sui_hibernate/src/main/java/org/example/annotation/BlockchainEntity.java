// src/main/java/org/example/annotation/BlockchainEntity.java
package org.example.annotation;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface BlockchainEntity {
    String module() default "sui_box_module";
    String struct() default "Model";
}
