package com.hartwig.hmftools.patientreporter.jsonoutput;

import java.util.List;

import com.google.common.collect.Lists;
import com.hartwig.hmftools.common.lims.LimsGermlineReportingLevel;
import com.hartwig.hmftools.patientreporter.algo.AnalysedPatientReport;
import com.hartwig.hmftools.patientreporter.algo.ImmutableAnalysedPatientReport;
import com.hartwig.hmftools.patientreporter.algo.ImmutableGenomicAnalysis;
import com.hartwig.hmftools.protect.purple.ImmutableReportableVariant;
import com.hartwig.hmftools.protect.purple.ReportableVariant;
import com.hartwig.hmftools.protect.purple.ReportableVariantSource;

import org.jetbrains.annotations.NotNull;

public class ReportData {

    @NotNull
    public static AnalysedPatientReport overrulePatientReportData(@NotNull AnalysedPatientReport report) {
        List<ReportableVariant> filteredVariantsOverruleVariantSource = Lists.newArrayList();
        for (ReportableVariant variant : report.genomicAnalysis().reportableVariants()) {
            if (report.sampleReport().germlineReportingLevel() == LimsGermlineReportingLevel.REPORT_WITHOUT_NOTIFICATION) {
                filteredVariantsOverruleVariantSource.add(overruleVariant(variant).build());
            }
        }

        return ImmutableAnalysedPatientReport.builder()
                .sampleReport(report.sampleReport())
                .qsFormNumber(report.qsFormNumber())
                .clinicalSummary(report.clinicalSummary())
                .genomicAnalysis(createOverruleGenomicAnalysis(report, filteredVariantsOverruleVariantSource))
                .circosPath(report.circosPath())
                .comments(report.comments())
                .isCorrectedReport(report.isCorrectedReport())
                .signaturePath(report.signaturePath())
                .logoRVAPath(report.logoRVAPath())
                .logoCompanyPath(report.logoCompanyPath())
                .pipelineVersion(report.pipelineVersion())
                .build();
    }

    @NotNull
    private static ImmutableGenomicAnalysis createOverruleGenomicAnalysis(@NotNull AnalysedPatientReport report,
            @NotNull List<ReportableVariant> filteredVariantsOverruleVariantSource) {
        return ImmutableGenomicAnalysis.builder()
                .from(report.genomicAnalysis())
                .reportableVariants(filteredVariantsOverruleVariantSource)
                .viralInsertions(report.genomicAnalysis().viralInsertions())
                .tumorSpecificEvidence(report.genomicAnalysis().tumorSpecificEvidence())
                .clinicalTrials(report.genomicAnalysis().clinicalTrials())
                .offLabelEvidence(report.genomicAnalysis().offLabelEvidence())
                .build();
    }

    @NotNull
    private static ImmutableReportableVariant.Builder overruleVariant(@NotNull ReportableVariant variant) {
        return ImmutableReportableVariant.builder()
                .type(variant.type())
                .source(ReportableVariantSource.SOMATIC)
                .gene(variant.gene())
                .chromosome(variant.chromosome())
                .position(variant.position())
                .ref(variant.ref())
                .alt(variant.alt())
                .canonicalCodingEffect(variant.canonicalCodingEffect())
                .canonicalHgvsCodingImpact(variant.canonicalHgvsCodingImpact())
                .canonicalHgvsProteinImpact(variant.canonicalHgvsProteinImpact())
                .totalReadCount(variant.totalReadCount())
                .alleleReadCount(variant.alleleReadCount())
                .totalCopyNumber(variant.totalCopyNumber())
                .alleleCopyNumber(variant.alleleCopyNumber())
                .hotspot(variant.hotspot())
                .clonalLikelihood(variant.clonalLikelihood())
                .driverLikelihood(variant.driverLikelihood())
                .copyNumber(variant.copyNumber())
                .tVafString(variant.tVafString())
                .biallelic(variant.biallelic());
    }
}
