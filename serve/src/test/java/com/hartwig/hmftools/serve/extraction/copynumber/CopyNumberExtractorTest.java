package com.hartwig.hmftools.serve.extraction.copynumber;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.google.common.collect.Lists;
import com.hartwig.hmftools.common.serve.classification.EventType;
import com.hartwig.hmftools.serve.extraction.util.GeneChecker;
import com.hartwig.hmftools.serve.extraction.util.GeneCheckerTestFactory;

import org.junit.Test;

public class CopyNumberExtractorTest {

    private static final GeneChecker V37_GENE_CHECKER = GeneCheckerTestFactory.buildForV37();

    @Test
    public void canExtractCopyNumbersAmp() {
        CopyNumberExtractor copyNumberExtractor = new CopyNumberExtractor(V37_GENE_CHECKER, Lists.newArrayList());
        KnownCopyNumber amp = copyNumberExtractor.extract("AKT1", EventType.AMPLIFICATION);

        assertEquals("AKT1", amp.gene());
        assertEquals(CopyNumberType.AMPLIFICATION, amp.type());
    }

    @Test
    public void canFilterAmpOnUnknownGene() {
        CopyNumberExtractor copyNumberExtractor = new CopyNumberExtractor(V37_GENE_CHECKER, Lists.newArrayList());
        assertNull(copyNumberExtractor.extract("NOT-A-GENE", EventType.AMPLIFICATION));
    }

    @Test
    public void canExtractCopyNumbersDel() {
        CopyNumberExtractor copyNumberExtractor = new CopyNumberExtractor(V37_GENE_CHECKER, Lists.newArrayList());
        KnownCopyNumber del = copyNumberExtractor.extract("PTEN", EventType.DELETION);

        assertEquals("PTEN", del.gene());
        assertEquals(CopyNumberType.DELETION, del.type());
    }

    @Test
    public void canFilterDelOnUnknownGene() {
        CopyNumberExtractor copyNumberExtractor = new CopyNumberExtractor(V37_GENE_CHECKER, Lists.newArrayList());
        assertNull(copyNumberExtractor.extract("NOT-A-GENE", EventType.DELETION));
    }
}