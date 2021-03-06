package com.hartwig.hmftools.serve;

import java.io.File;
import java.nio.file.Files;
import java.util.Set;

import com.google.common.collect.Sets;
import com.hartwig.hmftools.common.fusion.KnownFusionCache;
import com.hartwig.hmftools.serve.util.RefGenomeVersion;
import com.hartwig.hmftools.vicc.datamodel.ViccSource;

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
public interface ServeConfig {

    String NOT_APPLICABLE = "N/A";

    // Input sources to SERVE
    String USE_VICC = "use_vicc";
    String VICC_JSON = "vicc_json";
    String VICC_SOURCES = "vicc_sources";
    String USE_ICLUSION = "use_iclusion";
    String ICLUSION_TRIAL_TSV = "iclusion_trial_tsv";
    String USE_DOCM = "use_docm";
    String DOCM_TSV = "docm_tsv";
    String USE_HARTWIG_COHORT = "use_hartwig_cohort";
    String HARTWIG_COHORT_TSV = "hartwig_cohort_tsv";
    String USE_HARTWIG_CURATED = "use_hartwig_curated";
    String HARTWIG_CURATED_TSV = "hartwig_curated_tsv";

    // Config for curation of evidence
    String MISSING_DOIDS_MAPPING_TSV = "missing_doids_mapping_tsv";

    // Additional config for knowledge generation
    String REF_GENOME_VERSION = "ref_genome_version";
    String REF_GENOME_FASTA_FILE = "ref_genome_fasta_file";
    String DRIVER_GENE_TSV = "driver_gene_tsv";
    String KNOWN_FUSION_FILE = KnownFusionCache.KNOWN_FUSIONS_FILE;

    // All output from SERVE will be written to this dir
    String OUTPUT_DIR = "output_dir";

    // Options to help with debugging / testing
    String SKIP_HOTSPOT_RESOLVING = "skip_hotspot_resolving";
    String LOG_DEBUG = "log_debug";

    @NotNull
    static Options createOptions() {
        Options options = new Options();

        options.addOption(USE_VICC, false, "If provided, VICC will be used as a source in SERVE");
        options.addOption(VICC_JSON, true, "Path to the VICC JSON knowledgebase");
        options.addOption(VICC_SOURCES, true, "Comma-separated list of (lowercase) VICC sources to include");
        options.addOption(USE_ICLUSION, false, "If provided, iClusion will be used as a source in SERVE");
        options.addOption(ICLUSION_TRIAL_TSV, true, "Path to the iClusion input trial TSV");
        options.addOption(USE_DOCM, false, "If provided, DoCM will be used as a source in SERVE");
        options.addOption(DOCM_TSV, true, "Path to the DoCM knowledgebase input TSV");
        options.addOption(USE_HARTWIG_COHORT, false, "If provided, Hartwig Cohort will be used as a source in SERVE");
        options.addOption(HARTWIG_COHORT_TSV, true, "Path to the Hartwig Cohort input TSV");
        options.addOption(USE_HARTWIG_CURATED, false, "If provided, Hartwig Curated will be used as a source in SERVE");
        options.addOption(HARTWIG_CURATED_TSV, true, "Path to the Hartwig Curated input TSV");

        options.addOption(MISSING_DOIDS_MAPPING_TSV, true, "Path to the mapping TSV containing entries for missing DOIDs");

        options.addOption(REF_GENOME_VERSION, true, "Ref version. Should be 'hgXX'");
        options.addOption(REF_GENOME_FASTA_FILE, true, "Path to the ref genome fasta file");
        options.addOption(DRIVER_GENE_TSV, true, "Path to driver gene TSV");
        options.addOption(KNOWN_FUSION_FILE, true, "Path to the known fusion file");

        options.addOption(OUTPUT_DIR, true, "Dir which will hold all SERVE output files");

        options.addOption(SKIP_HOTSPOT_RESOLVING, false, "If present, skips hotspot resolving");
        options.addOption(LOG_DEBUG, false, "If present, switches the logging to DEBUG mode");

        return options;
    }

    boolean useVicc();

    @NotNull
    String viccJson();

    @NotNull
    Set<ViccSource> viccSources();

    boolean useIclusion();

    @NotNull
    String iClusionTrialTsv();

    boolean useDocm();

    @NotNull
    String docmTsv();

    boolean useHartwigCohort();

    @NotNull
    String hartwigCohortTsv();

    boolean useHartwigCurated();

    @NotNull
    String hartwigCuratedTsv();

    @NotNull
    String missingDoidsMappingTsv();

    @NotNull
    RefGenomeVersion refGenomeVersion();

    @NotNull
    String refGenomeFastaFile();

    @NotNull
    String driverGeneTsv();

    @NotNull
    String knownFusionFile();

    @NotNull
    String outputDir();

    boolean skipHotspotResolving();

    @NotNull
    static ServeConfig createConfig(@NotNull CommandLine cmd) throws ParseException {
        if (cmd.hasOption(LOG_DEBUG)) {
            Configurator.setRootLevel(Level.DEBUG);
        }

        boolean useVicc = cmd.hasOption(USE_VICC);
        boolean useIclusion = cmd.hasOption(USE_ICLUSION);
        boolean useDocm = cmd.hasOption(USE_DOCM);
        boolean useHartwigCohort = cmd.hasOption(USE_HARTWIG_COHORT);
        boolean useHartwigCurated = cmd.hasOption(USE_HARTWIG_CURATED);

        return ImmutableServeConfig.builder()
                .useVicc(useVicc)
                .viccJson(useVicc ? nonOptionalFile(cmd, VICC_JSON) : NOT_APPLICABLE)
                .viccSources(useVicc ? readViccSources(cmd) : Sets.newHashSet())
                .useIclusion(useIclusion)
                .iClusionTrialTsv(useIclusion ? nonOptionalFile(cmd, ICLUSION_TRIAL_TSV) : NOT_APPLICABLE)
                .useDocm(useDocm)
                .docmTsv(useDocm ? nonOptionalFile(cmd, DOCM_TSV) : NOT_APPLICABLE)
                .useHartwigCohort(useHartwigCohort)
                .hartwigCohortTsv(useHartwigCohort ? nonOptionalFile(cmd, HARTWIG_COHORT_TSV) : NOT_APPLICABLE)
                .useHartwigCurated(useHartwigCurated)
                .hartwigCuratedTsv(useHartwigCurated ? nonOptionalFile(cmd, HARTWIG_CURATED_TSV) : NOT_APPLICABLE)
                .missingDoidsMappingTsv(nonOptionalFile(cmd, MISSING_DOIDS_MAPPING_TSV))
                .refGenomeVersion(RefGenomeVersion.fromIdentifier(nonOptionalValue(cmd, REF_GENOME_VERSION)))
                .refGenomeFastaFile(nonOptionalFile(cmd, REF_GENOME_FASTA_FILE))
                .driverGeneTsv(nonOptionalFile(cmd, DRIVER_GENE_TSV))
                .knownFusionFile(nonOptionalFile(cmd, KNOWN_FUSION_FILE))
                .outputDir(nonOptionalDir(cmd, OUTPUT_DIR))
                .skipHotspotResolving(cmd.hasOption(SKIP_HOTSPOT_RESOLVING))
                .build();
    }

    @NotNull
    static Set<ViccSource> readViccSources(@NotNull CommandLine cmd) throws ParseException {
        Set<ViccSource> viccSources = Sets.newHashSet();
        String[] sources = nonOptionalValue(cmd, VICC_SOURCES).split(",");
        for (String source : sources) {
            viccSources.add(ViccSource.fromViccKnowledgebaseString(source));
        }
        return viccSources;
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
