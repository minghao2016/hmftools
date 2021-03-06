package com.hartwig.hmftools.protect.purple;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import com.hartwig.hmftools.common.drivercatalog.DriverCatalog;
import com.hartwig.hmftools.common.drivercatalog.DriverCatalogFile;
import com.hartwig.hmftools.common.drivercatalog.DriverType;
import com.hartwig.hmftools.common.purple.CheckPurpleQuality;
import com.hartwig.hmftools.common.purple.copynumber.CopyNumberInterpretation;
import com.hartwig.hmftools.common.purple.copynumber.ImmutableReportableGainLoss;
import com.hartwig.hmftools.common.purple.copynumber.ReportableGainLoss;
import com.hartwig.hmftools.common.purple.purity.PurityContext;
import com.hartwig.hmftools.common.purple.purity.PurityContextFile;
import com.hartwig.hmftools.common.purple.qc.PurpleQCStatus;
import com.hartwig.hmftools.common.utils.DataUtil;
import com.hartwig.hmftools.common.variant.SomaticVariant;
import com.hartwig.hmftools.common.variant.SomaticVariantFactory;
import com.hartwig.hmftools.protect.ProtectConfig;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

public final class PurpleDataLoader {

    private static final Logger LOGGER = LogManager.getLogger(PurpleDataLoader.class);

    private PurpleDataLoader() {
    }

    @NotNull
    public static PurpleData load(@NotNull ProtectConfig config) throws IOException {
        return load(config.tumorSampleId(),
                config.purpleQcFile(),
                config.purplePurityTsv(),
                config.purpleDriverCatalogTsv(),
                config.purpleSomaticVariantVcf());
    }

    @NotNull
    public static PurpleData load(@NotNull String sample, @NotNull String qcFile, @NotNull String purityTsv,
            @NotNull String driverCatalogSomaticTsv, @NotNull String somaticVcf) throws IOException {
        LOGGER.info("Loading PURPLE data from {}", new File(purityTsv).getParent());

        PurityContext purityContext = PurityContextFile.readWithQC(qcFile, purityTsv);
        LOGGER.info("  QC status: {}", purityContext.qc().toString());
        LOGGER.info("  Tumor purity: {}", new DecimalFormat("#'%'").format(purityContext.bestFit().purity() * 100));
        LOGGER.info("  Fit method: {}", purityContext.method());
        LOGGER.info("  Average tumor ploidy: {}", purityContext.bestFit().ploidy());
        LOGGER.info("  Whole genome duplication: {}", purityContext.wholeGenomeDuplication() ? "yes" : "no");
        LOGGER.info("  Microsatellite status: {}", purityContext.microsatelliteStatus().display());
        LOGGER.info("  Tumor mutational load status: {}", purityContext.tumorMutationalLoadStatus().display());

        boolean hasReliablePurity = CheckPurpleQuality.checkHasReliablePurity(purityContext);

        List<DriverCatalog> driverCatalog = DriverCatalogFile.read(driverCatalogSomaticTsv);
        LOGGER.info(" Loaded {} driver catalog entries from {}", driverCatalog.size(), driverCatalogSomaticTsv);

        List<ReportableGainLoss> copyNumberAlterations = driverCatalog.stream()
                .filter(x -> x.driver() == DriverType.AMP || x.driver() == DriverType.PARTIAL_AMP || x.driver() == DriverType.DEL)
                .map(PurpleDataLoader::copyNumberAlteration)
                .collect(Collectors.toList());
        LOGGER.info("  Extracted {} reportable copy number alterations from driver catalog", copyNumberAlterations.size());

        List<SomaticVariant> variants = SomaticVariantFactory.passOnlyInstance().fromVCFFile(sample, somaticVcf);
        List<ReportableVariant> reportableVariants = ReportableVariantFactory.reportableSomaticVariants(variants, driverCatalog, hasReliablePurity);
        LOGGER.info(" Loaded {} reportable somatic variants from {}", reportableVariants.size(), somaticVcf);

        // Set copies + tVAF to N/A when those are unreliable
        List<ReportableVariant> reportableVariantsInterpret = Lists.newArrayList();
        for (ReportableVariant variant : reportableVariants) {

            // amplification
            if (purityContext.qc().status().contains(PurpleQCStatus.WARN_HIGH_COPY_NUMBER_NOISE)) {
                for (ReportableGainLoss gain : copyNumberAlterations) {
                    if (variant.gene().equals(gain.gene()) && gain.interpretation() == CopyNumberInterpretation.FULL_GAIN ||
                    gain.interpretation() == CopyNumberInterpretation.PARTIAL_GAIN) {
                        reportableVariantsInterpret.add(variant);
                    } else {
                        reportableVariantsInterpret.add(interpretReportableVariant(variant).build());

                    }
                }
            }

            //deletions
            if (purityContext.qc().status().contains(PurpleQCStatus.WARN_HIGH_COPY_NUMBER_NOISE) || purityContext.qc()
                    .status()
                    .contains(PurpleQCStatus.WARN_DELETED_GENES)) {
                for (ReportableGainLoss loss : copyNumberAlterations) {
                    if (variant.gene().equals(loss.gene()) && loss.interpretation() == CopyNumberInterpretation.FULL_LOSS ||
                            loss.interpretation() == CopyNumberInterpretation.PARTIAL_LOSS) {
                        reportableVariantsInterpret.add(variant);
                    } else {
                        reportableVariantsInterpret.add(interpretReportableVariant(variant).build());
                    }
                }
            }
        }

        return ImmutablePurpleData.builder()
                .purity(purityContext.bestFit().purity())
                .hasReliablePurity(hasReliablePurity)
                .hasReliableQuality(purityContext.qc().pass())
                .ploidy(purityContext.bestFit().ploidy())
                .microsatelliteIndelsPerMb(purityContext.microsatelliteIndelsPerMb())
                .microsatelliteStatus(purityContext.microsatelliteStatus())
                .tumorMutationalBurdenPerMb(purityContext.tumorMutationalBurdenPerMb())
                .tumorMutationalLoad(purityContext.tumorMutationalLoad())
                .tumorMutationalLoadStatus(purityContext.tumorMutationalLoadStatus())
                .somaticVariants(reportableVariantsInterpret)
                .copyNumberAlterations(copyNumberAlterations)
                .build();
    }

    @NotNull
    private static ReportableGainLoss copyNumberAlteration(@NotNull DriverCatalog driver) {
        return ImmutableReportableGainLoss.builder()
                .chromosome(driver.chromosome())
                .chromosomeBand(driver.chromosomeBand())
                .gene(driver.gene())
                .interpretation(CopyNumberInterpretation.fromCNADriver(driver))
                .copies(Math.round(Math.max(0, driver.minCopyNumber())))
                .build();
    }

    @NotNull
    private static ImmutableReportableVariant.Builder interpretReportableVariant(@NotNull ReportableVariant variant) {
        return ImmutableReportableVariant.builder()
                .type(variant.type())
                .source(variant.source())
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
                .copyNumber(DataUtil.NA_STRING)
                .tVafString(DataUtil.NA_STRING)
                .alleleCopyNumber(variant.alleleCopyNumber())
                .hotspot(variant.hotspot())
                .clonalLikelihood(variant.clonalLikelihood())
                .biallelic(variant.biallelic());
    }


}
