package com.zenandops.cmdb;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class CmdbServiceTest {

    @Test
    void contextLoads() {
        assertTrue(true, "CMDB service context should load");
    }
}
