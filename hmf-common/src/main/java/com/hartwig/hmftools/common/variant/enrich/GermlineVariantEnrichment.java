package com.hartwig.hmftools.common.variant.enrich;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.google.common.collect.Multimap;
import com.hartwig.hmftools.common.drivercatalog.panel.DriverGene;
import com.hartwig.hmftools.common.drivercatalog.panel.DriverGenePanel;
import com.hartwig.hmftools.common.genome.chromosome.Chromosome;
import com.hartwig.hmftools.common.genome.region.CanonicalTranscript;
import com.hartwig.hmftools.common.purple.PurityAdjuster;
import com.hartwig.hmftools.common.purple.copynumber.PurpleCopyNumber;
import com.hartwig.hmftools.common.variant.hotspot.VariantHotspot;

import org.jetbrains.annotations.NotNull;

import htsjdk.samtools.reference.IndexedFastaSequenceFile;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.VCFHeader;

public class GermlineVariantEnrichment implements VariantContextEnrichment {

    private final VariantContextEnrichment purityEnrichment;
    private final VariantContextEnrichment refGenomeEnrichment;
    private final VariantContextEnrichment pathogenicEnrichment;
    private final VariantContextEnrichment snpEffEnrichment;
    private final VariantContextEnrichment reportableEnrichment;
    private final VariantContextEnrichment hotspotEnrichment;
    private final VariantContextEnrichment genotypeEnrichment;

    public GermlineVariantEnrichment(@NotNull final String purpleVersion, @NotNull final String referenceSample,
            @NotNull final String tumorSample, @NotNull final IndexedFastaSequenceFile reference,
            @NotNull final PurityAdjuster purityAdjuster, @NotNull final List<PurpleCopyNumber> copyNumbers,
            @NotNull final DriverGenePanel genePanel, @NotNull final List<CanonicalTranscript> transcripts,
            @NotNull final Multimap<Chromosome, VariantHotspot> germlineHotspots, @NotNull final Consumer<VariantContext> consumer) {
        final Set<String> germlineGenes =
                genePanel.driverGenes().stream().filter(DriverGene::reportGermline).map(DriverGene::gene).collect(Collectors.toSet());

        this.reportableEnrichment = new GermlineReportedEnrichment(genePanel.driverGenes(), consumer);
        this.pathogenicEnrichment = new GermlinePathogenicEnrichment(reportableEnrichment);
        this.refGenomeEnrichment = new SomaticRefContextEnrichment(reference, pathogenicEnrichment);

        this.snpEffEnrichment = new SnpEffEnrichment(germlineGenes, transcripts, refGenomeEnrichment);
        this.hotspotEnrichment = new VariantHotspotEnrichment(germlineHotspots, snpEffEnrichment);
        this.purityEnrichment = new GermlinePurityEnrichment(purpleVersion, tumorSample, referenceSample, purityAdjuster, copyNumbers,
                hotspotEnrichment);

        // Genotype must go first!
        this.genotypeEnrichment = new GermlineGenotypeEnrichment(referenceSample, purityEnrichment);
    }

    @Override
    public void accept(@NotNull final VariantContext context) {
        genotypeEnrichment.accept(context);
    }

    @Override
    public void flush() {
        genotypeEnrichment.flush();
        purityEnrichment.flush();
        hotspotEnrichment.flush();
        snpEffEnrichment.flush();
        refGenomeEnrichment.flush();
        pathogenicEnrichment.flush();
        reportableEnrichment.flush();
    }

    @NotNull
    @Override
    public VCFHeader enrichHeader(@NotNull final VCFHeader template) {
        VCFHeader header = purityEnrichment.enrichHeader(template);
        header = hotspotEnrichment.enrichHeader(header);
        header = refGenomeEnrichment.enrichHeader(header);
        header = snpEffEnrichment.enrichHeader(header);
        header = reportableEnrichment.enrichHeader(header);
        header = genotypeEnrichment.enrichHeader(header);
        return pathogenicEnrichment.enrichHeader(header);
    }
}
