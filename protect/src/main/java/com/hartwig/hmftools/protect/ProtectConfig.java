package com.hartwig.hmftools.protect;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Set;

import com.google.common.collect.Sets;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.immutables.value.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Value.Immutable
@Value.Style(passAnnotations = { NotNull.class, Nullable.class })
public interface ProtectConfig {

    String DOID_SEPARATOR = ";";

    // General params needed for every report
    String TUMOR_SAMPLE_ID = "tumor_sample_id";
    String PRIMARY_TUMOR_DOIDS = "primary_tumor_doids";
    String OUTPUT_DIRECTORY = "output_dir";

    // Input files used by the algorithm
    String SERVE_ACTIONABILITY_DIRECTORY = "serve_actionability_dir";
    String DOID_JSON = "doid_json";
    String GERMLINE_REPORTING_TSV = "germline_reporting_tsv";

    // Files containing the actual genomic results for this sample.
    String PURPLE_PURITY_TSV = "purple_purity_tsv";
    String PURPLE_QC_FILE = "purple_qc_file";
    String PURPLE_DRIVER_CATALOG_TSV = "purple_driver_catalog_tsv";
    String PURPLE_SOMATIC_VARIANT_VCF = "purple_somatic_variant_vcf";
    String BACHELOR_TSV = "bachelor_tsv";
    String LINX_FUSION_TSV = "linx_fusion_tsv";
    String LINX_BREAKEND_TSV = "linx_breakend_tsv";
    String LINX_VIRAL_INSERTION_TSV = "linx_viral_insertion_tsv";
    String LINX_DRIVERS_TSV = "linx_drivers_tsv";
    String CHORD_PREDICTION_TXT = "chord_prediction_txt";

    // Some additional optional params and flags
    String LOG_DEBUG = "log_debug";

    @NotNull
    static Options createOptions() {
        Options options = new Options();

        options.addOption(TUMOR_SAMPLE_ID, true, "The sample ID for which PROTECT will run.");
        options.addOption(PRIMARY_TUMOR_DOIDS, true, "A semicolon-separated list of DOIDs representing the primary tumor of patient.");
        options.addOption(OUTPUT_DIRECTORY, true, "Path to where the PROTECT output data will be written to.");

        options.addOption(SERVE_ACTIONABILITY_DIRECTORY, true, "Path towards the SERVE actionability directory.");
        options.addOption(DOID_JSON, true, "Path to JSON file containing the full DOID tree.");
        options.addOption(GERMLINE_REPORTING_TSV, true, "Path towards a TSV containing germline reporting config.");

        options.addOption(PURPLE_PURITY_TSV, true, "Path towards the purple purity TSV.");
        options.addOption(PURPLE_QC_FILE, true, "Path towards the purple qc file.");
        options.addOption(PURPLE_DRIVER_CATALOG_TSV, true, "Path towards the purple driver catalog TSV.");
        options.addOption(PURPLE_SOMATIC_VARIANT_VCF, true, "Path towards the purple somatic variant VCF.");
        options.addOption(BACHELOR_TSV, true, "Path towards the bachelor germline TSV.");
        options.addOption(LINX_FUSION_TSV, true, "Path towards the LINX fusion TSV.");
        options.addOption(LINX_BREAKEND_TSV, true, "Path towards the LINX breakend TSV.");
        options.addOption(LINX_VIRAL_INSERTION_TSV, true, "Path towards the LINX viral insertion TSV.");
        options.addOption(LINX_DRIVERS_TSV, true, "Path towards the LINX driver catalog TSV.");
        options.addOption(CHORD_PREDICTION_TXT, true, "Path towards the CHORD prediction TXT.");

        options.addOption(LOG_DEBUG, false, "If provided, set the log level to debug rather than default.");

        return options;
    }

    @NotNull
    String tumorSampleId();

    @NotNull
    Set<String> primaryTumorDoids();

    @NotNull
    String outputDir();

    @NotNull
    String serveActionabilityDir();

    @NotNull
    String doidJsonFile();

    @NotNull
    String germlineReportingTsv();

    @NotNull
    String purplePurityTsv();

    @NotNull
    String purpleQcFile();

    @NotNull
    String purpleDriverCatalogTsv();

    @NotNull
    String purpleSomaticVariantVcf();

    @NotNull
    String bachelorTsv();

    @NotNull
    String linxFusionTsv();

    @NotNull
    String linxBreakendTsv();

    @NotNull
    String linxViralInsertionTsv();

    @NotNull
    String linxDriversTsv();

    @NotNull
    String chordPredictionTxt();

    @NotNull
    static ProtectConfig createConfig(@NotNull CommandLine cmd) throws ParseException, IOException {
        if (cmd.hasOption(LOG_DEBUG)) {
            Configurator.setRootLevel(Level.DEBUG);
        }

        return ImmutableProtectConfig.builder()
                .tumorSampleId(nonOptionalValue(cmd, TUMOR_SAMPLE_ID))
                .primaryTumorDoids(toStringSet(optionalValue(cmd, PRIMARY_TUMOR_DOIDS), DOID_SEPARATOR))
                .outputDir(outputDir(cmd, OUTPUT_DIRECTORY))
                .serveActionabilityDir(nonOptionalDir(cmd, SERVE_ACTIONABILITY_DIRECTORY))
                .doidJsonFile(nonOptionalFile(cmd, DOID_JSON))
                .germlineReportingTsv(nonOptionalFile(cmd, GERMLINE_REPORTING_TSV))
                .purplePurityTsv(nonOptionalFile(cmd, PURPLE_PURITY_TSV))
                .purpleQcFile(nonOptionalFile(cmd, PURPLE_QC_FILE))
                .purpleDriverCatalogTsv(nonOptionalFile(cmd, PURPLE_DRIVER_CATALOG_TSV))
                .purpleSomaticVariantVcf(nonOptionalFile(cmd, PURPLE_SOMATIC_VARIANT_VCF))
                .bachelorTsv(nonOptionalFile(cmd, BACHELOR_TSV))
                .linxFusionTsv(nonOptionalFile(cmd, LINX_FUSION_TSV))
                .linxBreakendTsv(nonOptionalFile(cmd, LINX_BREAKEND_TSV))
                .linxViralInsertionTsv(nonOptionalFile(cmd, LINX_VIRAL_INSERTION_TSV))
                .linxDriversTsv(nonOptionalFile(cmd, LINX_DRIVERS_TSV))
                .chordPredictionTxt(nonOptionalFile(cmd, CHORD_PREDICTION_TXT))
                .build();
    }

    @NotNull
    static Iterable<String> toStringSet(@Nullable String paramValue, @NotNull String separator) {
        return paramValue != null ? Sets.newHashSet(paramValue.split(separator)) : Sets.newHashSet();
    }

    @Nullable
    static String optionalValue(@NotNull CommandLine cmd, @NotNull String param) {
        if (cmd.hasOption(param)) {
            return cmd.getOptionValue(param);
        } else {
            return null;
        }
    }

    @NotNull
    static String nonOptionalValue(@NotNull CommandLine cmd, @NotNull String param) throws ParseException {
        String value = cmd.getOptionValue(param);
        if (value == null) {
            throw new ParseException("Parameter must be provided: " + param);
        }

        return value;
    }

    @NotNull
    static String nonOptionalDir(@NotNull CommandLine cmd, @NotNull String param) throws ParseException {
        String value = nonOptionalValue(cmd, param);

        if (!pathExists(value) || !pathIsDirectory(value)) {
            throw new ParseException("Parameter '" + param + "' must be an existing directory: " + value);
        }

        return value;
    }

    @NotNull
    static String outputDir(@NotNull CommandLine cmd, @NotNull String param) throws ParseException, IOException {
        String value = nonOptionalValue(cmd, param);
        final File outputDir = new File(value);
        if (!outputDir.exists() && !outputDir.mkdirs()) {
            throw new IOException("Unable to write to directory " + value);
        }
        return value;
    }

    @NotNull
    static String nonOptionalFile(@NotNull CommandLine cmd, @NotNull String param) throws ParseException {
        String value = nonOptionalValue(cmd, param);

        if (!pathExists(value)) {
            throw new ParseException("Parameter '" + param + "' must be an existing file: " + value);
        }

        return value;
    }

    static boolean pathExists(@NotNull String path) {
        return Files.exists(new File(path).toPath());
    }

    static boolean pathIsDirectory(@NotNull String path) {
        return Files.isDirectory(new File(path).toPath());
    }
}
