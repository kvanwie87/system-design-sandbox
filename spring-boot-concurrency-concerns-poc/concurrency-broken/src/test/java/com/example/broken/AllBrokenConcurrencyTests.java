package com.example.broken;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

/**
 * Summary test suite for the broken concurrency module.
 * Runs all concurrency demonstrations that prove broken behavior.
 */
@Suite
@SelectClasses({
        RaceConditionTest.class,
        LostUpdateTest.class,
        DeadlockTest.class,
        SharedFieldTest.class,
        CheckThenActTest.class,
        CompoundOperationTest.class,
        ThreadStarvationTest.class
})
class AllBrokenConcurrencyTests {
}
