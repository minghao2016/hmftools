package com.hartwig.hmftools.common.hospital;

import org.immutables.value.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Value.Immutable
@Value.Style(allParameters = true,
             passAnnotations = { NotNull.class, Nullable.class })
public abstract class HospitalDataNew {

    @NotNull
    public abstract String hospitalId();

    @NotNull
    public abstract String hospitalPI();

    @NotNull
    public abstract String requestName();

    @NotNull
    public abstract String requestEmail();

}