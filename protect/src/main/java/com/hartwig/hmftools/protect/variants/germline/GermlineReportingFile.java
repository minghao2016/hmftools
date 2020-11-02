package com.hartwig.hmftools.protect.variants.germline;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import com.google.common.collect.Lists;

import org.jetbrains.annotations.NotNull;

public final class GermlineReportingFile {

    private static final String SEPARATOR = "\t";

    private GermlineReportingFile() {
    }

    @NotNull
    public static GermlineReportingModel buildFromTsv(@NotNull String germlineReportingTsv) throws IOException {
        List<String> lines = Files.readAllLines(new File(germlineReportingTsv).toPath());

        List<GermlineReporting> germlineReportingEntries = Lists.newArrayList();

        // Skip header
        for (String line : lines.subList(1, lines.size())) {
            String[] parts = line.split(SEPARATOR);

            germlineReportingEntries.add(ImmutableGermlineReporting.builder()
                    .gene(parts[0])
                    .notifyClinicalGeneticist(Boolean.parseBoolean(parts[1]))
                    .reportBiallelicOnly(parts[1].equalsIgnoreCase("Biallelic"))
                    .reportableSpecificVariant(parts.length > 3 ? parts[3] : null)
                    .build());
        }

        return new GermlineReportingModel(germlineReportingEntries);
    }
}


