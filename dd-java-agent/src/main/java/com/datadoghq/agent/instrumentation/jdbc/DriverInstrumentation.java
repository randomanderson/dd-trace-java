package com.datadoghq.agent.instrumentation.jdbc;

import static net.bytebuddy.matcher.ElementMatchers.hasSuperType;
import static net.bytebuddy.matcher.ElementMatchers.isInterface;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.not;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

import com.google.auto.service.AutoService;
import dd.trace.DDAdvice;
import dd.trace.Instrumenter;
import java.sql.Connection;
import java.sql.Driver;
import java.util.Map;
import java.util.Properties;
import java.util.WeakHashMap;
import lombok.Data;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;

@AutoService(Instrumenter.class)
public final class DriverInstrumentation implements Instrumenter {
  public static final Map<Connection, DBInfo> connectionInfo = new WeakHashMap<>();

  @Override
  public AgentBuilder instrument(final AgentBuilder agentBuilder) {
    return agentBuilder
        .type(not(isInterface()).and(hasSuperType(named(Driver.class.getName()))))
        .transform(
            DDAdvice.create()
                .advice(
                    named("connect").and(takesArguments(String.class, Properties.class)),
                    DriverAdvice.class.getName()))
        .asDecorator();
  }

  public static class DriverAdvice {
    @Advice.OnMethodExit(suppress = Throwable.class)
    public static void addDBInfo(
        @Advice.Argument(0) final String url,
        @Advice.Argument(1) final Properties info,
        @Advice.Return final Connection connection) {
      if (url != null) {
        // Remove end of url to prevent passwords from leaking:
        final String sanitizedURL = url.replaceAll("[?;].*", "");
        final String type = url.split(":")[1];
        final String dbUser = info == null ? null : info.getProperty("user");
        connectionInfo.put(connection, new DBInfo(sanitizedURL, type, dbUser));
      }
    }
  }

  @Data
  public static class DBInfo {
    public static DBInfo UNKNOWN = new DBInfo("null", "unknown", null);
    private final String url;
    private final String type;
    private final String user;
  }
}