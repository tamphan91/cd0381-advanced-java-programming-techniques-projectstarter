package com.udacity.webcrawler.profiler;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Objects;

/**
 * A method interceptor that checks whether {@link Method}s are annotated with the {@link Profiled}
 * annotation. If they are, the method interceptor records how long the method invocation took.
 */
final class ProfilingMethodInterceptor implements InvocationHandler {

    private final Clock clock;
    private final Object delegate;
    private final ProfilingState state;
    private final ZonedDateTime startTime;

    // TODO: You will need to add more instance fields and constructor arguments to this class.
    ProfilingMethodInterceptor(Clock clock, Object delegate, ProfilingState state, ZonedDateTime startTime) {
        this.clock = Objects.requireNonNull(clock);
        this.delegate = delegate;
        this.state = state;
        this.startTime = startTime;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Object invoked;
        Instant start = null;
        boolean profiled = method.getAnnotation(Profiled.class) != null;
        if (profiled) {
            start = clock.instant();
        }
        try {
            invoked = method.invoke(delegate, args);
        } catch (InvocationTargetException ex) {
            throw ex.getTargetException();
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } finally {
            if (profiled) {
                Duration duration = Duration.between(start, clock.instant());
                state.record(delegate.getClass(), method, duration);
            }
        }

        return invoked;
    }
}
