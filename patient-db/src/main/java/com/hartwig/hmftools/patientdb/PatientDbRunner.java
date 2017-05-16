package com.hartwig.hmftools.patientdb;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.stream.XMLStreamException;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.hartwig.hmftools.common.context.RunContext;
import com.hartwig.hmftools.common.ecrf.CpctEcrfModel;
import com.hartwig.hmftools.common.ecrf.datamodel.EcrfPatient;
import com.hartwig.hmftools.common.exception.EmptyFileException;
import com.hartwig.hmftools.common.exception.HartwigException;
import com.hartwig.hmftools.common.io.reader.FileReader;
import com.hartwig.hmftools.common.slicing.Slicer;
import com.hartwig.hmftools.common.slicing.SlicerFactory;
import com.hartwig.hmftools.common.variant.consensus.ConsensusRule;
import com.hartwig.hmftools.patientdb.data.Patient;
import com.hartwig.hmftools.patientdb.readers.CpctPatientReader;
import com.hartwig.hmftools.patientdb.readers.RunsFolderReader;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.jetbrains.annotations.NotNull;

public final class PatientDbRunner {
    private static final Logger LOGGER = LogManager.getLogger(PatientDbRunner.class);

    private static final String RUNS_DIR = "runs_dir";
    private static final String ECRF_FILE = "ecrf";
    private static final String DB_USER = "db_user";
    private static final String DB_PASS = "db_pass";
    private static final String DB_URL = "db_url";
    private static final String HIGH_CONFIDENCE_BED = "high_confidence_bed";
    private static final String EXTREME_CONFIDENCE_BED = "extreme_confidence_bed";
    private static final String TREATMENT_TYPES_CSV = "treatment_types_csv";
    private static final String LIMS_CSV = "lims_csv";
    private static final String LIMS_OLD_CSV = "lims_old_csv";
    private static final String LIMS_UMCU_CSV = "lims_umcu_csv";

    public static void main(@NotNull final String[] args)
            throws ParseException, IOException, InterruptedException, java.text.ParseException, XMLStreamException,
            SQLException, HartwigException {
        final Options options = createOptions();
        final CommandLine cmd = createCommandLine(args, options);
        final String runsFolderPath = cmd.getOptionValue(RUNS_DIR);
        final String ecrfFilePath = cmd.getOptionValue(ECRF_FILE);
        final String userName = cmd.getOptionValue(DB_USER);
        final String password = cmd.getOptionValue(DB_PASS);
        final String databaseUrl = cmd.getOptionValue(DB_URL);  //e.g. mysql://localhost:port/database";
        final String jdbcUrl = "jdbc:" + databaseUrl;
        final String highConfidenceBed = cmd.getOptionValue(HIGH_CONFIDENCE_BED);
        final String extremeConfidenceBed = cmd.getOptionValue(EXTREME_CONFIDENCE_BED);
        final String treatmentMappingCsv = cmd.getOptionValue(TREATMENT_TYPES_CSV);
        final String limsCsv = cmd.getOptionValue(LIMS_CSV);
        final String limsOldCsv = cmd.getOptionValue(LIMS_OLD_CSV);
        final String limsUmcuCsv = cmd.getOptionValue(LIMS_UMCU_CSV);

        ThreadContext.put("cpctHospitalCode", "default");
        if (Utils.anyNull(runsFolderPath, ecrfFilePath, userName, password, databaseUrl, highConfidenceBed,
                extremeConfidenceBed, treatmentMappingCsv, limsCsv, limsOldCsv, limsUmcuCsv)) {
            final HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("Patient-Db", options);
        } else {
            final File runDirectory = new File(runsFolderPath);
            if (runDirectory.isDirectory()) {
                final List<RunContext> runsContexts = RunsFolderReader.getRunContexts(runDirectory);
                LOGGER.info("Loading ecrf model...");
                final CpctEcrfModel model = CpctEcrfModel.loadFromXML(ecrfFilePath);
                final Slicer highConfidenceSlicer = SlicerFactory.fromBedFile(highConfidenceBed);
                final Slicer extremeConfidenceSlicer = SlicerFactory.fromBedFile(extremeConfidenceBed);
                final ConsensusRule consensusRule = ConsensusRule.fromSlicers(highConfidenceSlicer,
                        extremeConfidenceSlicer);
                final CpctPatientReader cpctPatientReader = new CpctPatientReader(model, consensusRule,
                        readTreatmentMappingFile(treatmentMappingCsv), limsCsv, limsOldCsv, limsUmcuCsv);
                final DatabaseWriter dbWriter = new DatabaseWriter(userName, password, jdbcUrl);
                dbWriter.clearTables();
                final Set<String> cpctPatientIds = runsContexts.stream().map(
                        runContext -> getPatientId(runContext.setName())).filter(
                        setName -> setName.startsWith("CPCT")).collect(Collectors.toSet());
                LOGGER.info("Reading CPCT patient data for " + cpctPatientIds.size() + " patients.");
                for (final String patientId : cpctPatientIds) {
                    final EcrfPatient patient = model.findPatientById(patientId);
                    if (patient == null) {
                        LOGGER.warn("Could not find patient with id: " + patientId + " in ecrf file.");
                    } else {
                        final List<String> sampleIdsForPatient = Lists.newArrayList();
                        runsContexts.forEach(runContext -> {
                            final String sampleId = runContext.tumorSample();
                            if (sampleId.startsWith(patient.patientId()) && !sampleIdsForPatient.contains(sampleId))
                                sampleIdsForPatient.add(sampleId);
                        });
                        LOGGER.info(patient.patientId() + ": Samples: " + sampleIdsForPatient);
                        final Patient cpctPatient = cpctPatientReader.read(patient, sampleIdsForPatient);
                        dbWriter.writePatient(cpctPatient);
                    }
                }
            } else {
                if (!runDirectory.exists()) {
                    LOGGER.warn("dir " + runDirectory + " does not exist.");
                }
                final HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("Patient-Db", options);
            }
        }
    }

    @NotNull
    private static Options createOptions() {
        final Options options = new Options();
        options.addOption(RUNS_DIR, true, "Path towards the folder containing patient runs.");
        options.addOption(ECRF_FILE, true, "Path towards the cpct ecrf file.");
        options.addOption(DB_USER, true, "Database user name.");
        options.addOption(DB_PASS, true, "Database password.");
        options.addOption(DB_URL, true, "Database url.");
        options.addOption(HIGH_CONFIDENCE_BED, true, "The full path towards the high confidence bed");
        options.addOption(EXTREME_CONFIDENCE_BED, true, "The full path towards the extreme confidence bed");
        options.addOption(TREATMENT_TYPES_CSV, true,
                "Path towards the .csv file that maps treatment names to treatment types");
        options.addOption(LIMS_CSV, true, "Path towards the LIMS .csv file");
        options.addOption(LIMS_OLD_CSV, true, "Path towards the LIMS-old .csv file");
        options.addOption(LIMS_UMCU_CSV, true, "Path towards the LIMS-UMCU .csv file");
        return options;
    }

    @NotNull
    private static CommandLine createCommandLine(@NotNull final String[] args, @NotNull final Options options)
            throws ParseException {
        final CommandLineParser parser = new DefaultParser();
        return parser.parse(options, args);
    }

    @NotNull
    private static String getPatientId(@NotNull final String runName) {
        final String[] names = runName.split("_");
        return names[4];
    }

    @NotNull
    private static Map<String, String> readTreatmentMappingFile(@NotNull final String treatmentMappingCsv)
            throws IOException, EmptyFileException {
        final Map<String, String> treatmentMapping = Maps.newHashMap();
        FileReader.build().readLines(new File(treatmentMappingCsv).toPath()).forEach(line -> {
            final String[] parts = line.split(",");
            if (parts.length == 2) {
                treatmentMapping.put(parts[0].toLowerCase().trim(), parts[1].toLowerCase().trim());
            } else {
                LOGGER.warn("Invalid row found in treatment mapping csv: " + line);
            }
        });
        return treatmentMapping;
    }
}
