package com.hartwig.hmftools.vicc.datamodel;

import org.immutables.value.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Value.Immutable
@Value.Style(passAnnotations = { NotNull.class, Nullable.class })
public abstract class CivicPublicationDate {

    @NotNull
    public abstract String year();

    @Nullable
    public abstract String day();

    @Nullable
    public abstract String month();
}
