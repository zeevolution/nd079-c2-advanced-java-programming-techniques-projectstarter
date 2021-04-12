package com.udacity.webcrawler.profiler;

import javax.inject.Inject;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.Objects;

import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;

/**
 * Concrete implementation of the {@link Profiler}.
 */
final class ProfilerImpl implements Profiler {

  private final Clock clock;
  private final ProfilingState state = new ProfilingState();
  private final ZonedDateTime startTime;

  @Inject
  ProfilerImpl(Clock clock) {
    this.clock = Objects.requireNonNull(clock);
    this.startTime = ZonedDateTime.now(clock);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T wrap(Class<T> klass, T delegate) {
    Objects.requireNonNull(klass);

    boolean isProfiled = false;
    for (Method method : klass.getMethods()) {
      if (method.isAnnotationPresent(Profiled.class)) {
        isProfiled = true;
        break;
      }
    }

    if (!isProfiled) {
      throw new IllegalArgumentException();
    }

    Object proxy  = Proxy.newProxyInstance(
            klass.getClassLoader(),
            new Class<?>[]{klass},
            new ProfilingMethodInterceptor(clock, delegate, state));

    return (T) proxy;
  }

  @Override
  public void writeData(Path path) throws IOException {
    try (FileWriter fileWriter = new FileWriter(path.toString(), true)) {
      BufferedWriter writer = new BufferedWriter(fileWriter);
      this.writeData(writer);
      writer.close();
    }
  }

  @Override
  public void writeData(Writer writer) throws IOException {
    writer.write("Run at " + RFC_1123_DATE_TIME.format(startTime));
    writer.write(System.lineSeparator());
    state.write(writer);
    writer.write(System.lineSeparator());
  }
}
