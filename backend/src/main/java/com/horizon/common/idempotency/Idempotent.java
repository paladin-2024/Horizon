package com.horizon.common.idempotency;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a POST controller method as idempotent: the request must carry an {@code Idempotency-Key}
 * header, the same key with the same request replays the stored response, the same key with a
 * different request is rejected with 422, and a request still in flight is answered with 409.
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {}
