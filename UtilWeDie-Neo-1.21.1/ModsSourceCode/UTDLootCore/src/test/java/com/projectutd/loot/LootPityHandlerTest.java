package com.projectutd.loot;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class LootPityHandlerTest {
    @Test
    void tier4SoftCurveMatchesThePreservedKubeJsPolicy() {
        assertEquals(0.0D, PityPolicy.tier4SoftChance(6));
        assertEquals(0.10D, PityPolicy.tier4SoftChance(7));
        assertEquals(0.20D, PityPolicy.tier4SoftChance(8));
        assertEquals(0.35D, PityPolicy.tier4SoftChance(9));
        assertEquals(0.0D, PityPolicy.tier4SoftChance(10));
    }

    @Test
    void tier5SoftCurveMatchesThePreservedKubeJsPolicy() {
        assertEquals(0.0D, PityPolicy.tier5SoftChance(14));
        assertEquals(0.01D, PityPolicy.tier5SoftChance(15));
        assertEquals(0.02D, PityPolicy.tier5SoftChance(16));
        assertEquals(0.03D, PityPolicy.tier5SoftChance(17));
        assertEquals(0.05D, PityPolicy.tier5SoftChance(18));
        assertEquals(0.08D, PityPolicy.tier5SoftChance(19));
        assertEquals(0.0D, PityPolicy.tier5SoftChance(20));
    }

    @Test
    void hardGuaranteesBeginAfterTheConfiguredNumberOfMisses() {
        assertFalse(PityPolicy.tier4HardGuarantee(9));
        assertTrue(PityPolicy.tier4HardGuarantee(10));
        assertFalse(PityPolicy.tier5HardGuarantee(19));
        assertTrue(PityPolicy.tier5HardGuarantee(20));
    }
}
