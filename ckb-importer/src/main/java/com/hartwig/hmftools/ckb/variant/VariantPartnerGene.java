package com.hartwig.hmftools.ckb.variant;

import com.hartwig.hmftools.ckb.common.GeneInfo;

import org.immutables.value.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Value.Immutable
@Value.Style(passAnnotations = { NotNull.class, Nullable.class })
public abstract class VariantPartnerGene {

    @NotNull
    public abstract GeneInfo gene();
}
