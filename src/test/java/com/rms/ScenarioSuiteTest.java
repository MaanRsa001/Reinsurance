package com.rms;

import org.junit.jupiter.api.Test;

/** Plain-JUnit entry points for the scenario checks (real service logic against in-memory fake repositories; no database needed). */
class ScenarioSuiteTest {
    @Test
    void coverHierarchyAllocationRulesAndChainValidation() {
        CoverHierarchyScenarioTest.run();
    }

    @Test
    void treatyCreateUpdateAmendRenewDelete() throws Exception {
        TreatyServiceScenarioTest.run();
    }

    @Test
    void deterministicExecutionChain() {
        AllocationChainScenarioTest.run();
    }
}
