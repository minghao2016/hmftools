package com.hartwig.hmftools.patientreporter.variants.germline;

import com.hartwig.hmftools.common.variant.germline.ReportableGermlineVariant;

import org.immutables.value.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Value.Immutable
@Value.Style(allParameters = true,
             passAnnotations = { NotNull.class, Nullable.class })
public abstract class DriverGermlineVariant {

    @NotNull
    public abstract ReportableGermlineVariant variant();

    public abstract double driverLikelihood();
}