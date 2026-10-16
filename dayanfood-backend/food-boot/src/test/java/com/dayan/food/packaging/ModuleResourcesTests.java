package com.dayan.food.packaging;

import org.junit.jupiter.api.Test;

class ModuleResourcesTests {
    @Test
    void allBusinessModulesContributeTheirSqlAndComponents() throws Exception {
        ModuleClasspathProbe.verifyResources();
    }
}
