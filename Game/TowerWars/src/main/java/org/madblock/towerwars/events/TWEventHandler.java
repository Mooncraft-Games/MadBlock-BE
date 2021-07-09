package org.madblock.towerwars.events;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface TWEventHandler {

    Priority callPriority() default Priority.REGULAR;

    enum Priority {
        LOWEST, // Called last
        LOW,
        REGULAR,
        HIGH,
        HIGHEST // Called first
    }

}
