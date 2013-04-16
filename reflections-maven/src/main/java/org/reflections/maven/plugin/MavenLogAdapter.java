package org.reflections.maven.plugin;

import org.apache.maven.plugin.logging.Log;
import org.slf4j.Logger;
import org.slf4j.helpers.MarkerIgnoringBase;

import java.text.MessageFormat;

class MavenLogAdapter extends MarkerIgnoringBase implements Logger {
    private Log log;

    MavenLogAdapter(Log log) { this.log = log; }
    public String getName() { return log.getClass().getName(); }
    public boolean isDebugEnabled() { return log.isDebugEnabled(); }
    public void debug(String format, Object arg1, Object arg2) { if (log.isDebugEnabled()) log.debug(MessageFormat.format(format, arg1, arg2)); }
    public void debug(String format, Object arg) { if (log.isDebugEnabled()) log.debug(MessageFormat.format(format, arg)); }
    public void debug(String format, Object[] argArray) { if (log.isDebugEnabled()) log.debug(MessageFormat.format(format, argArray)); }
    public void debug(String msg, Throwable t) { log.debug(msg, t); }
    public void debug(String msg) { log.debug(msg); }
    public boolean isErrorEnabled() { return log.isErrorEnabled(); }
    public void error(String format, Object arg1, Object arg2) { if (log.isErrorEnabled()) log.error(MessageFormat.format(format, arg1, arg2)); }
    public void error(String format, Object arg) { if (log.isErrorEnabled()) log.error(MessageFormat.format(format, arg)); }
    public void error(String format, Object[] argArray) { if (log.isErrorEnabled()) log.error(MessageFormat.format(format, argArray)); }
    public void error(String msg, Throwable t) { log.error(msg, t); }
    public void error(String msg) { log.error(msg); }
    public boolean isInfoEnabled() { return log.isInfoEnabled(); }
    public void info(String format, Object arg1, Object arg2) { if (log.isInfoEnabled()) log.info(MessageFormat.format(format, arg1, arg2)); }
    public void info(String format, Object arg) { if (log.isInfoEnabled()) log.info(MessageFormat.format(format, arg)); }
    public void info(String format, Object[] argArray) { if (log.isInfoEnabled()) log.info(MessageFormat.format(format, argArray)); }
    public void info(String msg, Throwable t) { log.info(msg, t); }
    public void info(String msg) { log.info(msg); }
    public boolean isTraceEnabled() { return log.isDebugEnabled(); }
    public void trace(String format, Object arg1, Object arg2) { if (log.isDebugEnabled()) log.debug(MessageFormat.format(format, arg1, arg2)); }
    public void trace(String format, Object arg) { if (log.isDebugEnabled()) log.debug(MessageFormat.format(format, arg)); }
    public void trace(String format, Object[] argArray) { if (log.isDebugEnabled()) log.debug(MessageFormat.format(format, argArray)); }
    public void trace(String msg, Throwable t) { log.debug(msg, t); }
    public void trace(String msg) { log.debug(msg); }
    public boolean isWarnEnabled() { return log.isWarnEnabled(); }
    public void warn(String format, Object arg1, Object arg2) { if (log.isWarnEnabled()) log.warn(MessageFormat.format(format, arg1, arg2)); }
    public void warn(String format, Object arg) { if (log.isWarnEnabled()) log.warn(MessageFormat.format(format, arg)); }
    public void warn(String format, Object[] argArray) { if (log.isWarnEnabled()) log.warn(MessageFormat.format(format, argArray)); }
    public void warn(String msg, Throwable t) { log.warn(msg, t); }
    public void warn(String msg) { log.warn(msg); }
}
