package com.hartwig.hmftools.serve.checkertool;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

public class CheckCodonRanges {
    private static final Logger LOGGER = LogManager.getLogger(CheckCodonRanges.class);

    public CheckCodonRanges() {

    }

    public void run(@NotNull String event, @NotNull String gene) {
        LOGGER.info("Event check codon ranges {} of gene {}", event, gene);

    }
}
