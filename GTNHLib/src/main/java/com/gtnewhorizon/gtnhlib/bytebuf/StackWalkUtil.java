/*
 * Copyright LWJGL. All rights reserved. License terms: https://www.lwjgl.org/license
 */
package com.gtnewhorizon.gtnhlib.bytebuf;

import java.util.Arrays;
import java.util.Objects;
import java.util.Iterator;

import org.jetbrains.annotations.Nullable;

/**
 * Stack-walking utilities.
 *
 * <p>
 * On Java 9 these methods are implemented using {@code java.lang.StackWalker}, which has much lower overhead.
 * </p>
 */
final class StackWalkUtil {
    private static final StackWalker STACK_WALKER = Runtime.version().feature() >= 17
            ? StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
            : null;

    private StackWalkUtil() {}

    public static int classVersion() {
        return Runtime.version().feature() >= 17 ? 17 : 8;
    }

    static StackTraceElement[] stackWalkArray(Object[] a) {
        if (Runtime.version().feature() >= 17) {
            return Arrays.stream(((StackWalker.StackFrame[]) a)).map(StackWalker.StackFrame::toStackTraceElement)
                    .toArray(StackTraceElement[]::new);
        }
        return (StackTraceElement[]) a;
    }

    static Object stackWalkGetMethod(Class<?> after) {
        if (Runtime.version().feature() >= 17) {
            return STACK_WALKER.walk(stream -> {
                Iterator<StackWalker.StackFrame> iter = stream.iterator();
                iter.next();
                iter.next();

                StackWalker.StackFrame frame;
                do {
                    frame = iter.next();
                } while (frame.getDeclaringClass() == after && iter.hasNext());

                return frame;
            });
        }
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();

        for (int i = 3; i < stackTrace.length; i++) {
            if (!stackTrace[i].getClassName().startsWith(after.getName())) {
                return stackTrace[i];
            }
        }

        throw new IllegalStateException();
    }

    private static boolean isSameMethod(StackTraceElement a, StackTraceElement b) {
        return isSameMethod(a, b, b.getMethodName());
    }

    private static boolean isSameMethod(StackTraceElement a, StackTraceElement b, String methodName) {
        return a.getMethodName().equals(methodName) && a.getClassName().equals(b.getClassName())
                && Objects.equals(a.getFileName(), b.getFileName());
    }

    private static boolean isSameMethod(StackWalker.StackFrame a, StackWalker.StackFrame b) {
        return isSameMethod(a, b, b.getMethodName());
    }

    private static boolean isSameMethod(StackWalker.StackFrame a, StackWalker.StackFrame b, String methodName) {
        return a.getDeclaringClass() == b.getDeclaringClass() && a.getMethodName().equals(methodName);
    }

    private static boolean isAutoCloseable(StackTraceElement element, StackTraceElement pushed) {
        // Java 9 try-with-resources: synthetic $closeResource
        if (isSameMethod(element, pushed, "$closeResource")) {
            return true;
        }

        // Kotlin T.use: kotlin.AutoCloseable::closeFinally
        if ("closeFinally".equals(element.getMethodName()) && "AutoCloseable.kt".equals(element.getFileName())) {
            return true;
        }

        return false;
    }

    private static boolean isAutoCloseable(StackWalker.StackFrame element, StackWalker.StackFrame pushed) {
        if (isSameMethod(element, pushed, "$closeResource")) {
            return true;
        }

        return "kotlin.jdk7.AutoCloseableKt".equals(element.getClassName())
                && "closeFinally".equals(element.getMethodName());
    }

    static @Nullable Object stackWalkCheckPop(Class<?> after, Object pushedObj) {
        if (Runtime.version().feature() >= 17) {
            StackWalker.StackFrame pushed = (StackWalker.StackFrame) pushedObj;

            return StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).walk(stream -> {
                Iterator<StackWalker.StackFrame> iter = stream.iterator();
                iter.next();
                iter.next();

                StackWalker.StackFrame element;
                do {
                    element = iter.next();
                } while (element.getDeclaringClass() == after && iter.hasNext());

                if (isSameMethod(element, pushed)) {
                    return null;
                }

                if (iter.hasNext() && isAutoCloseable(element, pushed)) {
                    element = iter.next();
                    if (isSameMethod(element, pushed)) {
                        return null;
                    }
                }

                return element;
            });
        }
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();

        for (int i = 3; i < stackTrace.length; i++) {
            StackTraceElement element = stackTrace[i];
            if (element.getClassName().startsWith(after.getName())) {
                continue;
            }

            StackTraceElement pushed = (StackTraceElement) pushedObj;
            if (isSameMethod(element, pushed)) {
                return null;
            }

            if (isAutoCloseable(element, pushed) && i + 1 < stackTrace.length) {
                // Some runtimes use a separate method to call AutoCloseable::close in try-with-resources blocks.
                // That method suppresses any exceptions thrown by close if necessary.
                // When that happens, the pop is 1 level deeper than expected.
                element = stackTrace[i + 1];
                if (isSameMethod(pushed, stackTrace[i + 1])) {
                    return null;
                }
            }

            return element;
        }

        throw new IllegalStateException();
    }

    static Object[] stackWalkGetTrace() {
        if (Runtime.version().feature() >= 17) {
            return StackWalker.getInstance().walk(stream -> stream.skip(2)
                    .dropWhile(frame -> frame.getClassName().startsWith("org.lwjgl.system.Memory"))
                    .toArray(StackWalker.StackFrame[]::new));
        }
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();

        int i = 3;
        for (; i < stackTrace.length; i++) {
            if (!stackTrace[i].getClassName().startsWith("org.lwjgl.system.Memory")) {
                break;
            }
        }

        return Arrays.copyOfRange(stackTrace, i, stackTrace.length);
    }

}
