package com.example.fixed;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

/**
 * Summary test suite for the fixed concurrency module.
 * Runs all concurrency demonstrations that prove correct behavior.
 */
@Suite
@SelectClasses({
        RaceConditionFixedTest.class,
        LostUpdateFixedTest.class,
        DeadlockFixedTest.class,
        SharedFieldFixedTest.class,
        CheckThenActFixedTest.class,
        CompoundOperationFixedTest.class,
        ThreadStarvationFixedTest.class
})
class AllFixedConcurrencyTests {
}
