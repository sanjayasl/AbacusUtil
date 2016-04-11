/*
 * Copyright (c) 2015, Haiyang Li. All rights reserved.
 */

package com.landawn.abacus.logging;

import com.landawn.abacus.exception.AbacusException;
import com.landawn.abacus.util.N;

import android.util.Log;

/**
 * 
 * @since 0.8
 * 
 * @author Haiyang Li
 */
class AndroidLogger extends AbstractLogger {
    private static final int MAX_TAG_SIZE = 23;

    public AndroidLogger(String name) {
        super(name.startsWith("com.landawn.abacus") ? "Abacus"
                : (name.length() > MAX_TAG_SIZE ? name.substring(N.max(name.length() - MAX_TAG_SIZE, name.lastIndexOf('.') + 1)) : name));

        try {
            Class.forName("android.util.Log");
        } catch (ClassNotFoundException e) {
            throw new AbacusException(e);
        }
    }

    @Override
    public boolean isTraceEnabled() {
        return Log.isLoggable(name, Log.VERBOSE);
    }

    @Override
    public void trace(String msg) {
        Log.v(name, msg);
    }

    @Override
    public void trace(String msg, Throwable t) {
        Log.v(name, msg, t);
    }

    @Override
    public boolean isDebugEnabled() {
        return Log.isLoggable(name, Log.DEBUG);
    }

    @Override
    public void debug(String msg) {
        Log.d(name, msg);
    }

    @Override
    public void debug(String msg, Throwable t) {
        Log.d(name, msg, t);
    }

    @Override
    public boolean isInfoEnabled() {
        return Log.isLoggable(name, Log.INFO);
    }

    @Override
    public void info(String msg) {
        Log.i(name, msg);
    }

    @Override
    public void info(String msg, Throwable t) {
        Log.i(name, msg, t);
    }

    @Override
    public boolean isWarnEnabled() {
        return Log.isLoggable(name, Log.WARN);
    }

    @Override
    public void warn(String msg) {
        Log.w(name, msg);
    }

    @Override
    public void warn(String msg, Throwable t) {
        Log.w(name, msg, t);
    }

    @Override
    public boolean isErrorEnabled() {
        return Log.isLoggable(name, Log.ERROR);
    }

    @Override
    public void error(String msg) {
        Log.e(name, msg);
    }

    @Override
    public void error(String msg, Throwable t) {
        Log.e(name, msg, t);
    }
}
