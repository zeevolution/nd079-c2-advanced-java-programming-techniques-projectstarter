package com.udacity.webcrawler.profiler;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * A method interceptor that checks whether {@link Method}s are annotated with the {@link Profiled}
 * annotation. If they are, the method interceptor records how long the method invocation took.
 */
final class ProfilingMethodInterceptor implements InvocationHandler {

  private final Clock clock;
  private final Object targetObject;
  private final ProfilingState state;

  ProfilingMethodInterceptor(Clock clock, Object targetObject, ProfilingState state) {
    this.clock = Objects.requireNonNull(clock);
    this.targetObject = Objects.requireNonNull(targetObject);
    this.state = Objects.requireNonNull(state);
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    final Instant start = clock.instant();

    if (method.getDeclaringClass().equals(Object.class)) {
      try {
        return method.invoke(targetObject, args);
      } catch (InvocationTargetException e) {
        throw e.getTargetException();
      } catch (IllegalAccessException e) {
        throw new RuntimeException(e);
      } finally {
        state.record(targetObject.getClass(), method, Duration.between(start, clock.instant()));
      }
    }

    if (method.getAnnotation(Profiled.class) != null) {
      try {
        method.invoke(targetObject, args);
      } catch (InvocationTargetException e) {
        throw e.getTargetException();
      } catch (IllegalArgumentException e) {
        throw new RuntimeException(e);
      } finally {
        state.record(targetObject.getClass(), method, Duration.between(start, clock.instant()));
      }
    }

    return method.invoke(targetObject, args);
  }
}
