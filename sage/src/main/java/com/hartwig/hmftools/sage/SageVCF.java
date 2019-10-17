package com.hartwig.hmftools.sage;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import com.hartwig.hmftools.common.variant.enrich.SomaticRefContextEnrichment;
import com.hartwig.hmftools.sage.context.AltContext;
import com.hartwig.hmftools.sage.context.ReadContextCounter;

import org.jetbrains.annotations.NotNull;

import htsjdk.samtools.reference.IndexedFastaSequenceFile;
import htsjdk.variant.variantcontext.Allele;
import htsjdk.variant.variantcontext.Genotype;
import htsjdk.variant.variantcontext.GenotypeBuilder;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.variantcontext.VariantContextBuilder;
import htsjdk.variant.variantcontext.writer.Options;
import htsjdk.variant.variantcontext.writer.VariantContextWriter;
import htsjdk.variant.variantcontext.writer.VariantContextWriterBuilder;
import htsjdk.variant.vcf.VCFConstants;
import htsjdk.variant.vcf.VCFFormatHeaderLine;
import htsjdk.variant.vcf.VCFHeader;
import htsjdk.variant.vcf.VCFHeaderLineType;
import htsjdk.variant.vcf.VCFInfoHeaderLine;
import htsjdk.variant.vcf.VCFStandardHeaderLines;

public class SageVCF implements AutoCloseable {

    private final static String PASS = "PASS";
    private final static String SUBPRIME_QUALITY_READ_DEPTH = "SDP";
    private final static String READ_CONTEXT = "RC";
    private final static String READ_CONTEXT_COUNT = "RCC";
    private final static String READ_CONTEXT_QUALITY = "RCQ";

    private final SageConfig config;
    private final VariantContextWriter writer;
    private final SomaticRefContextEnrichment refContextEnrichment;

    SageVCF(@NotNull final IndexedFastaSequenceFile reference, @NotNull final SageConfig config) {
        this.config = config;

        writer = new VariantContextWriterBuilder().setOutputFile(config.outputFile()).modifyOption(Options.INDEX_ON_THE_FLY, true).build();
        refContextEnrichment = new SomaticRefContextEnrichment(reference, writer::add);

        final VCFHeader header = refContextEnrichment.enrichHeader(header(config.reference(), config.tumor()));
        writer.writeHeader(header);
    }

    public void write(@NotNull final List<AltContext> altContexts) {
        final AltContext normal = altContexts.get(0);
        if (normal.altSupport() <= config.maxNormalAltSupport()) {
            refContextEnrichment.accept(create(altContexts));
        }
    }

    @NotNull
    private Genotype createGenotype(@NotNull final List<Allele> alleles, @NotNull final AltContext evidence) {
        ReadContextCounter readContextCounter = evidence.primaryReadContext();

        return new GenotypeBuilder(evidence.sample()).DP(evidence.readDepth())
                .AD(new int[] { evidence.refSupport(), evidence.altSupport() })
                .attribute("SDP", evidence.subprimeReadDepth())
                .attribute("QUAL",
                        new int[] { readContextCounter.quality(), readContextCounter.baseQuality(), readContextCounter.mapQuality() })
                .attribute("RCC", readContextCounter.rcc())
                .attribute("RCQ", readContextCounter.rcq())
                .alleles(alleles)
                .make();
    }

    @NotNull
    private VariantContext create(@NotNull final List<AltContext> altContexts) {
        assert (altContexts.size() > 1);

        final AltContext normal = altContexts.get(0);
        final AltContext firstTumor = altContexts.get(1);

        final Allele ref = Allele.create(normal.ref(), true);
        final Allele alt = Allele.create(normal.alt(), false);
        final List<Allele> alleles = Lists.newArrayList(ref, alt);

        final List<Genotype> genotypes = altContexts.stream().map(x -> createGenotype(alleles, x)).collect(Collectors.toList());

        final VariantContextBuilder builder = new VariantContextBuilder().chr(normal.chromosome())
                .start(normal.position())

                //                .attribute(VCFConstants.ALLELE_FREQUENCY_KEY, round(tumorEvidence.vaf()))
                //                .attribute("MAP_Q", tumorEvidence.altMapQuality())
                //                .attribute("MAP_BASE_Q", tumorEvidence.altMinQuality())
                //                .attribute("AVG_DISTANCE_RECORD", tumorEvidence.avgAltDistanceFromRecordStart())
                //                .attribute("AVG_DISTANCE_ALIGNMENT", tumorEvidence.avgAltMinDistanceFromAlignment())
                //                .attribute(READ_CONTEXT, tumorEvidence.readContext())
                //                .attribute(READ_CONTEXT_COUNT, tumorEvidence.readContextCount())
                //                .attribute(READ_CONTEXT_COUNT_OTHER, tumorEvidence.readContextCountOther())
                .attribute("RC", normal.primaryReadContext().toString())
                .attribute("RDIF", normal.primaryReadContext().readContext().distanceCigar())
                .attribute("RDIS", normal.primaryReadContext().readContext().distance())
                .computeEndFromAlleles(alleles, (int) normal.position())
                .source("SAGE")
                .genotypes(genotypes)
                .alleles(alleles);

        final VariantContext context = builder.make();
        context.getCommonInfo().setLog10PError(firstTumor.primaryReadContext().quality() / -10d);
        return context;
    }

    private static double round(double number) {
        double multiplier = Math.pow(10, 3);
        return Math.round(number * multiplier) / multiplier;
    }

    @NotNull
    private static VCFHeader header(@NotNull final String normalSample, @NotNull final List<String> tumorSamples) {
        final List<String> allSamples = Lists.newArrayList(normalSample);
        allSamples.addAll(tumorSamples);

        VCFHeader header = new VCFHeader(Collections.emptySet(), allSamples);
        header.addMetaDataLine(VCFStandardHeaderLines.getFormatLine((VCFConstants.GENOTYPE_KEY)));
        header.addMetaDataLine(VCFStandardHeaderLines.getFormatLine((VCFConstants.GENOTYPE_ALLELE_DEPTHS)));
        header.addMetaDataLine(VCFStandardHeaderLines.getFormatLine((VCFConstants.DEPTH_KEY)));
        header.addMetaDataLine(new VCFFormatHeaderLine(SUBPRIME_QUALITY_READ_DEPTH,
                1,
                VCFHeaderLineType.Integer,
                "Subprime quality read depth"));

        header.addMetaDataLine(VCFStandardHeaderLines.getInfoLine((VCFConstants.ALLELE_FREQUENCY_KEY)));

        header.addMetaDataLine(new VCFInfoHeaderLine(READ_CONTEXT, 1, VCFHeaderLineType.String, "TODO"));
        header.addMetaDataLine(new VCFFormatHeaderLine(READ_CONTEXT_COUNT, 5, VCFHeaderLineType.Integer, "[Full, Partial, Realigned, J-, J+]"));
        header.addMetaDataLine(new VCFFormatHeaderLine(READ_CONTEXT_QUALITY,
                3,
                VCFHeaderLineType.Integer,
                "[ImproperPairedRead, InconsistentChromosome, ExcessInferredSize]"));

        header.addMetaDataLine(new VCFFormatHeaderLine("QUAL", 3, VCFHeaderLineType.Integer, "[MinBaseMapQual, BaseQual, MapQual]"));
        header.addMetaDataLine(new VCFFormatHeaderLine("DIST", 2, VCFHeaderLineType.Integer, "[AvgRecordDistance, AvgAlignmentDistance]"));
        header.addMetaDataLine(new VCFInfoHeaderLine("RDIF", 1, VCFHeaderLineType.String, "Difference from ref"));
        header.addMetaDataLine(new VCFInfoHeaderLine("RDIS", 1, VCFHeaderLineType.Integer, "Distance from ref"));

        return header;
    }

    @Override
    public void close() {
        writer.close();
    }

}
