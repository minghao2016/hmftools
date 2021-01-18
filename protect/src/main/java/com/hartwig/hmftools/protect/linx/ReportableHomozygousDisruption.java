package com.hartwig.hmftools.protect.linx;

import org.immutables.value.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Value.Immutable
@Value.Style(allParameters = true,
             passAnnotations = { NotNull.class, Nullable.class })
public abstract class ReportableHomozygousDisruption {

    @NotNull
    @Value.Derived
    public String genomicEvent() {
        return this.gene() + " homozygous disruption";
    }

    @NotNull
    public abstract String chromosome();

    @NotNull
    public abstract String chromosomeBand();

    @NotNull
    public abstract String gene();
}