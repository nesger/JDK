/*
 * Copyright (c) 2015, 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */
/*
 */


package org.graalvm.compiler.core.test.deopt;

import java.util.concurrent.CountDownLatch;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;

import org.graalvm.compiler.core.common.GraalOptions;
import org.graalvm.compiler.core.test.GraalCompilerTest;

import jdk.vm.ci.code.InstalledCode;

public final class SafepointRethrowDeoptTest extends GraalCompilerTest {

    private static final Object RETURN_VALUE = "1 2 3";
    private static final RuntimeException BREAK_EX = new RuntimeException();
    private static final RuntimeException CONTINUE_EX = new RuntimeException();
    private static volatile int terminate;
    private static volatile int entered;

    public static Object execute() {
        entered = 1;
        for (;;) {
            try {
                if (terminate != 0) {
                    throw BREAK_EX;
                } else {
                    throw CONTINUE_EX;
                }
            } catch (RuntimeException e) {
                if (e == BREAK_EX) {
                    break;
                } else if (e == CONTINUE_EX) {
                    continue;
                }
                throw e;
            }
        }
        return RETURN_VALUE;
    }

    @Test
    public void test() {
        Assume.assumeTrue(GraalOptions.GenLoopSafepoints.getValue(getInitialOptions()));
        synchronized (SafepointRethrowDeoptTest.class) {
            // needs static fields
            terminate = 1;

            InstalledCode installed = getCode(getResolvedJavaMethod("execute"));

            terminate = 0;
            entered = 0;
            CountDownLatch cdl = new CountDownLatch(1);
            Thread t1 = new Thread(() -> {
                try {
                    cdl.await();
                    while (entered == 0) {
                        /* spin */
                    }
                    installed.invalidate();
                } catch (InterruptedException e) {
                    Assert.fail("interrupted");
                } finally {
                    terminate = 1;
                }
            });
            Thread t2 = new Thread(() -> {
                cdl.countDown();
                Object result;
                try {
                    result = installed.executeVarargs();
                } catch (Exception e) {
                    e.printStackTrace();
                    Assert.fail("exception");
                    return;
                }
                Assert.assertEquals(RETURN_VALUE, result);
            });

            t1.start();
            t2.start();
            try {
                t1.join();
                t2.join();
            } catch (InterruptedException e) {
                Assert.fail("interrupted");
            }
        }
    }
}
