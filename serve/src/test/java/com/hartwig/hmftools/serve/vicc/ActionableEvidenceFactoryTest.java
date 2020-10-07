package com.hartwig.hmftools.serve.vicc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.hartwig.hmftools.serve.actionability.EvidenceDirection;

import org.junit.Test;

public class ActionableEvidenceFactoryTest {

    @Test
    public void canReformatDrugs() {
        assertEquals("Imatinib,Imatinib", ActionableEvidenceFactory.reformatDrugs("IMATINIB,IMATINIB"));

        assertNull(ActionableEvidenceFactory.reformatDrugs(null));
    }

    @Test
    public void canReformatField() {
        assertEquals("Field", ActionableEvidenceFactory.reformatField("Field"));
        assertEquals("Field", ActionableEvidenceFactory.reformatField("field"));
        assertEquals("Field", ActionableEvidenceFactory.reformatField("FIELD"));

        assertEquals("F", ActionableEvidenceFactory.reformatField("F"));
        assertEquals("F", ActionableEvidenceFactory.reformatField("f"));
        assertEquals("", ActionableEvidenceFactory.reformatField(""));
        assertNull(ActionableEvidenceFactory.reformatField(null));
    }

    @Test
    public void canResolveDirection() {
        assertEquals(EvidenceDirection.RESPONSIVE, ActionableEvidenceFactory.resolveDirection("Responsive"));
        assertEquals(EvidenceDirection.RESPONSIVE, ActionableEvidenceFactory.resolveDirection("Sensitive"));
        assertEquals(EvidenceDirection.RESISTANT, ActionableEvidenceFactory.resolveDirection("Resistant"));

        assertNull(ActionableEvidenceFactory.resolveDirection(null));
        assertNull(ActionableEvidenceFactory.resolveDirection("Conflicting"));
        assertNull(ActionableEvidenceFactory.resolveDirection("This is no direction"));
    }
}