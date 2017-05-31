package com.hartwig.hmftools.common.region;

import com.hartwig.hmftools.common.chromosome.Chromosomes;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface GenomeRegion extends Comparable<GenomeRegion> {

    @NotNull
    String chromosome();

    long start();

    long end();

    @Nullable
    String annotation();

    default long bases() {
        return 1 + end() - start();
    }

    @Override
    default int compareTo(@NotNull final GenomeRegion other) {

        if (chromosome().equals(other.chromosome())) {
            if (start() < other.start()) {
                return -1;
            } else if (start() == other.start()) {
                return 0;
            }
            return 1;
        }

        return Integer.compare(Chromosomes.asInt(chromosome()), Chromosomes.asInt(other.chromosome()));
    }

    default boolean overlaps(@NotNull final GenomeRegion other) {
        return other.chromosome().equals(chromosome()) && other.end() > start() && other.start() < end();
    }
}
