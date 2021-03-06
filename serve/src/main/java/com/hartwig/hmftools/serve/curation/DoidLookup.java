package com.hartwig.hmftools.serve.curation;

import java.util.Map;
import java.util.Set;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Sets;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DoidLookup {

    private static final Logger LOGGER = LogManager.getLogger(DoidLookup.class);

    @NotNull
    private final Map<String, Set<String>> cancerTypeToDoidsMapping;
    private final Set<String> evaluatedCancerTypes = Sets.newHashSet();

    DoidLookup(@NotNull final Map<String, Set<String>> cancerTypeToDoidsMapping) {
        this.cancerTypeToDoidsMapping = cancerTypeToDoidsMapping;
    }

    @Nullable
    public Set<String> lookupDoidsForCancerType(@NotNull String cancerType) {
        evaluatedCancerTypes.add(cancerType);
        return cancerTypeToDoidsMapping.get(cancerType);
    }

    public void reportUnusedMappings() {
        int unusedCancerTypeCount = 0;
        for (String cancerType : cancerTypeToDoidsMapping.keySet()) {
            if (!evaluatedCancerTypes.contains(cancerType)) {
                unusedCancerTypeCount++;
                LOGGER.warn("Key '{}' hasn't been used during DOID cancer type mapping", cancerType);
            }
        }

        LOGGER.debug("Found {} unused DOID mapping entries. {} keys have been requested against {} entries",
                unusedCancerTypeCount,
                evaluatedCancerTypes.size(),
                cancerTypeToDoidsMapping.keySet().size());
    }

    @NotNull
    @VisibleForTesting
    Map<String, Set<String>> cancerTypeToDoidsMapping() {
        return cancerTypeToDoidsMapping;
    }
}
