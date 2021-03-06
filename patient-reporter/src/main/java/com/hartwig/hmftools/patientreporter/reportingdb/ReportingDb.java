package com.hartwig.hmftools.patientreporter.reportingdb;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;

import com.google.common.annotations.VisibleForTesting;
import com.hartwig.hmftools.common.lims.cohort.LimsCohortConfig;
import com.hartwig.hmftools.common.reportingdb.ReportingDatabase;
import com.hartwig.hmftools.common.reportingdb.ReportingEntry;
import com.hartwig.hmftools.patientreporter.algo.AnalysedPatientReport;
import com.hartwig.hmftools.patientreporter.algo.GenomicAnalysis;
import com.hartwig.hmftools.patientreporter.cfreport.ReportResources;
import com.hartwig.hmftools.patientreporter.qcfail.QCFailReport;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class ReportingDb {

    private static final Logger LOGGER = LogManager.getLogger(ReportingDb.class);

    private static final String NA_STRING = "N/A";

    private ReportingDb() {
    }

    public static void addAnalysedReportToReportingDb(@NotNull String reportingDbTsv, @NotNull AnalysedPatientReport report)
            throws IOException {
        String sampleId = report.sampleReport().tumorSampleId();
        GenomicAnalysis analysis = report.genomicAnalysis();

        LimsCohortConfig cohort = report.sampleReport().cohort();

        if (requiresSummary(report.sampleReport().cohort()) && report.clinicalSummary().isEmpty()) {
            LOGGER.warn("Skipping addition to reporting db, missing summary for sample '{}'!", sampleId);
        } else if (!cohort.cohortId().isEmpty()) {
            String tumorBarcode = report.sampleReport().tumorSampleBarcode();
            String reportDate = ReportResources.REPORT_DATE;
            String purity = new DecimalFormat("0.00").format(analysis.impliedPurity());

            boolean hasReliableQuality = analysis.hasReliableQuality();
            boolean hasReliablePurity = analysis.hasReliablePurity();

            String reportType;
            if (hasReliablePurity && analysis.impliedPurity() > ReportResources.PURITY_CUTOFF) {
                reportType = "dna_analysis_report";
            } else {
                reportType = "dna_analysis_report_insufficient_tcp";
            }

            if (report.isCorrectedReport()) {
                reportType = reportType + "_corrected";
            }

            addToReportingDb(reportingDbTsv,
                    tumorBarcode,
                    sampleId,
                    cohort,
                    reportType,
                    reportDate,
                    purity,
                    hasReliableQuality,
                    hasReliablePurity);
        }
    }

    @VisibleForTesting
    static boolean requiresSummary(@Nullable LimsCohortConfig cohort) {
        return cohort.reportConclusion();
    }

    private static void addToReportingDb(@NotNull String reportingDbTsv, @NotNull String tumorBarcode, @NotNull String sampleId,
            @NotNull LimsCohortConfig cohort, @NotNull String reportType, @NotNull String reportDate, @NotNull String purity,
            boolean hasReliableQuality, boolean hasReliablePurity) throws IOException {
        boolean present = false;
        for (ReportingEntry entry : ReportingDatabase.read(reportingDbTsv)) {
            if (!present && sampleId.equals(entry.sampleId()) && tumorBarcode.equals(entry.tumorBarcode())
                    && reportType.equals(entry.reportType())) {
                LOGGER.warn("Sample {} has already been reported with report type '{}'!", sampleId, reportType);
                present = true;
            }
        }

        if (!present) {
            LOGGER.info("Adding {} to reporting db at {} with type '{}'", sampleId, reportingDbTsv, reportType);
            String stringToAppend =
                    tumorBarcode + "\t" + sampleId + "\t" + cohort.cohortId() + "\t" + reportDate + "\t" + reportType + "\t" + purity + "\t"
                            + hasReliableQuality + "\t" + hasReliablePurity + "\n";
            appendToTsv(reportingDbTsv, stringToAppend);
        }
    }

    public static void addQCFailReportToReportingDb(@NotNull String reportingDbTsv, @NotNull QCFailReport report) throws IOException {
        String sampleId = report.sampleReport().tumorSampleId();
        LimsCohortConfig cohort = report.sampleReport().cohort();
        String tumorBarcode = report.sampleReport().tumorSampleBarcode();
        String reportDate = ReportResources.REPORT_DATE;

        String reportType = report.isCorrectedReport() ? report.reason().identifier() + "_corrected" : report.reason().identifier();

        if (!cohort.cohortId().isEmpty()) {
            boolean present = false;
            for (ReportingEntry entry : ReportingDatabase.read(reportingDbTsv)) {
                if (!present && sampleId.equals(entry.sampleId()) && tumorBarcode.equals(entry.tumorBarcode())
                        && reportType.equals(entry.reportType()) && reportDate.equals(entry.reportDate())) {
                    LOGGER.warn("Sample {} has already been reported with report type '{}' on {}!", sampleId, reportType, reportDate);
                    present = true;
                }
            }

            if (!present) {
                LOGGER.info("Adding {} to reporting db at {} with type '{}'", sampleId, reportingDbTsv, reportType);
                String stringToAppend =
                        tumorBarcode + "\t" + sampleId + "\t" + cohort.cohortId() + "\t" + reportDate + "\t" + reportType + "\t" + NA_STRING
                                + "\t" + NA_STRING + "\t" + NA_STRING + "\n";
                appendToTsv(reportingDbTsv, stringToAppend);
            }
        }
    }



    private static void appendToTsv(@NotNull String reportDatesTsv, @NotNull String stringToAppend) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(reportDatesTsv, true));
        writer.write(stringToAppend);
        writer.close();
    }
}
