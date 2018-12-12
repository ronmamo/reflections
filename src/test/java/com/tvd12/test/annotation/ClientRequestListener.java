package com.tvd12.test.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 
 * Indicates that a class listener a request from client and process that request
 * 
 * @author tavandung12
 *
 */
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE })
public @interface ClientRequestListener {
    
    /**
     * Command of request
     * 
     * @return command of request
     */
	public String command() default "";
	
	/**
	 * Command of request
	 * 
	 * @return the command
	 */
	public String value() default "";
	
	/**
	 * priority of listener, lowest is first
	 * 
	 * @return priority of listener
	 */
	public int priority() default 0;
}
