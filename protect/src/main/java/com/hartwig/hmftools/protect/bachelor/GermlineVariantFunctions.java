package com.hartwig.hmftools.protect.bachelor;

import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;
import com.hartwig.hmftools.common.variant.Hotspot;
import com.hartwig.hmftools.common.variant.VariantType;
import com.hartwig.hmftools.common.variant.germline.ReportableGermlineVariant;
import com.hartwig.hmftools.protect.germline.GermlineReportingEntry;
import com.hartwig.hmftools.protect.germline.GermlineReportingModel;
import com.hartwig.hmftools.protect.purple.ImmutableReportableVariant;
import com.hartwig.hmftools.protect.purple.ReportableVariant;
import com.hartwig.hmftools.protect.purple.ReportableVariantFactory;
import com.hartwig.hmftools.protect.purple.ReportableVariantSource;

import org.jetbrains.annotations.NotNull;

public final class GermlineVariantFunctions {

    private GermlineVariantFunctions() {
    }

    @NotNull
    public static List<ReportableVariant> reportableGermlineVariants(@NotNull List<ReportableGermlineVariant> variants,
            @NotNull Set<String> genesWithSomaticInactivationEvent, @NotNull GermlineReportingModel germlineReportingModel, boolean hasReliablePurity) {
        List<ReportableVariant> reportableVariants = Lists.newArrayList();

        for (ReportableGermlineVariant variant : variants) {
            GermlineReportingEntry reportingEntry = germlineReportingModel.entryForGene(variant.gene());
            if (reportingEntry != null && isPresentInTumor(variant)) {
                boolean includeVariant;
                String exclusiveHgvsProteinFilter = reportingEntry.exclusiveHgvsProteinFilter();
                if (exclusiveHgvsProteinFilter != null) {
                    includeVariant = variant.hgvsProtein().equals(exclusiveHgvsProteinFilter);
                } else if (reportingEntry.reportBiallelicOnly()) {
                    boolean filterBiallelic = variant.biallelic();

                    boolean filterGermlineVariantInSameGene = false;
                    for (ReportableGermlineVariant otherVariant : variants) {
                        if (variant != otherVariant && otherVariant.gene().equals(variant.gene())) {
                            filterGermlineVariantInSameGene = true;
                        }
                    }

                    boolean filterSomaticEventInSameGene = genesWithSomaticInactivationEvent.contains(variant.gene());

                    includeVariant = filterBiallelic || filterGermlineVariantInSameGene || filterSomaticEventInSameGene;
                } else {
                    includeVariant = true;
                }

                if (includeVariant) {
                    reportableVariants.add(fromGermlineVariant(variant, hasReliablePurity).driverLikelihood(1D).build());
                }
            }
        }

        return reportableVariants;
    }

    @NotNull
    private static ImmutableReportableVariant.Builder fromGermlineVariant(@NotNull ReportableGermlineVariant variant, boolean hasReliablePurity) {
        return ImmutableReportableVariant.builder()
                .type(VariantType.type(variant.ref(), variant.alt()))
                .source(ReportableVariantSource.GERMLINE)
                .gene(variant.gene())
                .chromosome(variant.chromosome())
                .position(variant.position())
                .ref(variant.ref())
                .alt(variant.alt())
                .canonicalCodingEffect(variant.codingEffect())
                .canonicalHgvsCodingImpact(variant.hgvsCoding())
                .canonicalHgvsProteinImpact(variant.hgvsProtein())
                .totalReadCount(variant.totalReadCount())
                .alleleReadCount(variant.alleleReadCount())
                .totalCopyNumber(variant.adjustedCopyNumber())
                .alleleCopyNumber(calcAlleleCopyNumber(variant.adjustedCopyNumber(), variant.adjustedVaf()))
                .hotspot(Hotspot.NON_HOTSPOT)
                .clonalLikelihood(1D)
                .copyNumber(ReportableVariantFactory.copyNumberString(variant.adjustedCopyNumber(),
                        hasReliablePurity))
                .tVafString(ReportableVariantFactory.vafString(calcAlleleCopyNumber(variant.adjustedCopyNumber(), variant.adjustedVaf()),
                        variant.adjustedCopyNumber(),
                        hasReliablePurity))
                .biallelic(variant.biallelic());
    }

    private static boolean isPresentInTumor(@NotNull ReportableGermlineVariant germlineVariant) {
        return calcAlleleCopyNumber(germlineVariant.adjustedCopyNumber(), germlineVariant.adjustedVaf()) >= 0.5;
    }

    private static double calcAlleleCopyNumber(double adjustedCopyNumber, double adjustedVAF) {
        return adjustedCopyNumber * Math.max(0, Math.min(1, adjustedVAF));
    }
}
