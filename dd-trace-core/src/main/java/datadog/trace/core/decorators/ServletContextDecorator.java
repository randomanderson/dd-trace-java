package datadog.trace.core.decorators;

import datadog.trace.api.Config;
import datadog.trace.core.DDSpanContext;

public class ServletContextDecorator extends AbstractDecorator {

  public ServletContextDecorator() {
    super();
    setMatchingTag("servlet.context");
  }

  @Override
  public boolean shouldSetTag(final DDSpanContext context, final String tag, final Object value) {
    String contextName = String.valueOf(value).trim();
    if (contextName.equals("/")
        || (!context.getServiceName().equals(Config.DEFAULT_SERVICE_NAME)
            && !context.getServiceName().isEmpty())) {
      return true;
    }
    if (contextName.startsWith("/")) {
      if (contextName.length() > 1) {
        contextName = contextName.substring(1);
      }
    }
    if (!contextName.isEmpty()) {
      context.setServiceName(contextName);
    }
    return true;
  }
}
