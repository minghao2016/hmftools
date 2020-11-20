package com.hartwig.hmftools.vicc.annotation;

import java.util.List;
import java.util.Set;

import com.google.common.collect.Sets;
import com.hartwig.hmftools.common.serve.classification.EventClassifier;
import com.hartwig.hmftools.common.serve.classification.ExclusiveEventClassifier;

import org.jetbrains.annotations.NotNull;

class SignatureClassifier implements EventClassifier {

    private static final Set<String> SIGNATURES = Sets.newHashSet("Microsatellite Instability-High");

    @NotNull
    public static EventClassifier create(@NotNull List<EventClassifier> excludingEventClassifiers) {
        return new ExclusiveEventClassifier(excludingEventClassifiers, new SignatureClassifier());
    }

    private SignatureClassifier() {
    }

    @Override
    public boolean matches(@NotNull String gene, @NotNull String event) {
        return SIGNATURES.contains(event);
    }
}
