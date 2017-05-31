package com.hartwig.hmftools.common.purity;

import static com.hartwig.hmftools.common.purity.PurityAdjustment.purityAdjustedCopyNumber;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class PurityAdjustmentTest {

    private static final double EPSILON = 1e-10;

    @Test
    public void testPurityAdjustedCopynumber() {
        assertEquals(1, purityAdjustedCopyNumber(0.85, 1, 0.575), EPSILON);
    }

}
