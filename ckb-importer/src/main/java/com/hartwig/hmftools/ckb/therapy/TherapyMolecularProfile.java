package com.hartwig.hmftools.ckb.therapy;

import org.immutables.value.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Value.Immutable
@Value.Style(passAnnotations = { NotNull.class, Nullable.class })
public abstract class TherapyMolecularProfile {

    @NotNull
    public abstract String id();

    @NotNull
    public abstract String profileName();

}