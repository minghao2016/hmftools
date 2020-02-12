package com.hartwig.hmftools.knowledgebasegenerator.compassionateUsePrograms;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.List;

import com.google.common.io.Resources;
import com.hartwig.hmftools.knowledgebasegenerator.compassionateuse.CompassionateUseProgramFile;
import com.hartwig.hmftools.knowledgebasegenerator.compassionateuse.CompassionateUsePrograms;

import org.junit.Ignore;
import org.junit.Test;

public class CompassionateUseProgramFileTest {

    private static final String COMPASSIONATE_USE_PROGRAMS_TSV = Resources.getResource("compassionateUsePrograms/compassionateUseProgramsFile.tsv").getPath();

    @Test
    @Ignore //TODO fix test
    public void canReadTestCompassionateUseProgramFile() throws IOException {
        List<CompassionateUsePrograms> compassionateUsePrograms = CompassionateUseProgramFile.read(COMPASSIONATE_USE_PROGRAMS_TSV);

        assertEquals(1, compassionateUsePrograms.size());

        CompassionateUsePrograms compassionateUsePrograms1 = compassionateUsePrograms.get(0);
        assertEquals("AC", compassionateUsePrograms1.CBGsite());
        assertEquals("-", compassionateUsePrograms1.variants());
        assertEquals("CBG", compassionateUsePrograms1.source());
        assertEquals("Trametinib", compassionateUsePrograms1.drug());
        assertEquals("inhibitor", compassionateUsePrograms1.drugType());
        assertEquals("Lung", compassionateUsePrograms1.cancerType());
        assertEquals("B", compassionateUsePrograms1.level());
        assertEquals("Responsive", compassionateUsePrograms1.direction());
        assertEquals("link", compassionateUsePrograms1.link());
    }
}
