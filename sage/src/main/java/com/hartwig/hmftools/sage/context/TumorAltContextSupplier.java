package com.hartwig.hmftools.sage.context;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.function.Supplier;

import com.google.common.collect.Lists;
import com.hartwig.hmftools.common.region.GenomeRegion;
import com.hartwig.hmftools.sage.SageConfig;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SamReaderFactory;


public class TumorAltContextSupplier implements Supplier<List<AltContext>> {

    private static final Logger LOGGER = LogManager.getLogger(TumorAltContextSupplier.class);

    private final String sample;
    private final SageConfig config;
    private final String bamFile;
    private final List<AltContext> altContexts = Lists.newArrayList();
    private final ContextSelector<AltContext> consumerSelector;
    private final TumorRefContextCandidates candidates;
    private final RefContextConsumer refContextConsumer;

    private final GenomeRegion bounds;

    public TumorAltContextSupplier(final SageConfig config, final String sample, @NotNull final GenomeRegion bounds,
            @NotNull final String bamFile, @NotNull final RefSequence refGenome) {
        this.config = config;
        this.sample = sample;
        this.bamFile = bamFile;
        this.consumerSelector = new ContextSelector<>(altContexts);
        this.candidates = new TumorRefContextCandidates(sample);
        this.bounds = bounds;
        this.refContextConsumer = new RefContextConsumer(true, config, bounds, refGenome, this.candidates);

    }

    private void processFirstPass(final SAMRecord samRecord) {
        refContextConsumer.accept(samRecord);
    }

    private void processSecondPass(final SAMRecord samRecord) {
        consumerSelector.select(samRecord.getAlignmentStart(), samRecord.getAlignmentEnd(), x -> x.primaryReadContext().accept(samRecord));
    }

    @Override
    public List<AltContext> get() {

        if (bounds.start() == 1) {
            LOGGER.info("Beginning processing of {} chromosome {} ", sample, bounds.chromosome());
        }

        LOGGER.info("Tumor candidates {} position {}:{}", sample, bounds.chromosome(), bounds.start());

        try {
            final SamReader tumorReader = SamReaderFactory.makeDefault().open(new File(bamFile));
            new SageSamSlicer(0, Lists.newArrayList(bounds)).slice(tumorReader, this::processFirstPass);

            // Add all valid alt contexts
            for (final RefContext refContext : candidates.refContexts()) {
                for (final AltContext altContext : refContext.alts()) {
                    if (altContext.altSupport() >= config.minTumorAltSupport()) {
                        altContext.setPrimaryReadCounterFromInterim();
                        altContexts.add(altContext);
                    }
                }
            }

            new SageSamSlicer(config.minMapQuality(), Lists.newArrayList(bounds)).slice(tumorReader, this::processSecondPass);

            tumorReader.close();
        } catch (IOException e) {
            throw new CompletionException(e);
        }

        return altContexts;
    }

}