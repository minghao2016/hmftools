package com.hartwig.hmftools.ckb.indication;

import java.util.List;

import org.immutables.value.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Value.Immutable
@Value.Style(passAnnotations = { NotNull.class, Nullable.class })
public abstract class IndicationEvidence {

    @NotNull
    public abstract String id();

    @NotNull
    public abstract String approvalStatus();

    @NotNull
    public abstract String evidenceType();

    @NotNull
    public abstract String efficacyEvidence();

    @NotNull
    public abstract IndicationMolecularProfile molecularProfile();

    @NotNull
    public abstract IndicationTherapy therapy();

    @NotNull
    public abstract IndicationIndication indication();

    @NotNull
    public abstract String responseType();

    @NotNull
    public abstract List<IndicationReference> reference();

    @NotNull
    public abstract String ampCapAscoEvidenceLevel();

    @NotNull
    public abstract String ampCapAscoInferredTier();




}