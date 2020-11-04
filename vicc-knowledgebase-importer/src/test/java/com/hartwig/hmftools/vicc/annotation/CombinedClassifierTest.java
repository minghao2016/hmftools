package com.hartwig.hmftools.vicc.annotation;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class CombinedClassifierTest {

    @Test
    public void canAssessWhetherFeatureIsCombinedEvent() {
        assertTrue(CombinedClassifier.isCombinedEvent("Ex19 del L858R", "EGFR"));

        assertTrue(CombinedClassifier.isCombinedEvent("EGFR inframe deletion (L747),inframe insertion (P753PS)", "EGFR"));
        assertTrue(CombinedClassifier.isCombinedEvent("KIT inframe deletion (416-422),inframe insertion (416-422)", "KIT"));
        assertTrue(CombinedClassifier.isCombinedEvent("CSF3R frameshift variant (D771),frameshift variant (S783)", "CSF3R"));

        assertTrue(CombinedClassifier.isCombinedEvent("BCR-ABL F486S", "ABL"));
        assertTrue(CombinedClassifier.isCombinedEvent("NPM1-ALK  amp", "ALK"));
    }
}