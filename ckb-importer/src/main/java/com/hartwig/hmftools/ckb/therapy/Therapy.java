package com.hartwig.hmftools.ckb.therapy;

import java.util.List;

import org.immutables.value.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Value.Immutable
@Value.Style(passAnnotations = { NotNull.class, Nullable.class })
public abstract class Therapy {

    @NotNull
    public abstract String id();

    @NotNull
    public abstract String therapyName();

    @Nullable
    public abstract String synonyms();

    @NotNull
    public abstract List<TherapyTherapyDescription> therapyDescription();

    @NotNull
    public abstract String createDate();

    @Nullable
    public abstract String updateDate();

    @NotNull
    public abstract List<TherapyEvidence> evidence();

    @NotNull
    public abstract List<TherapyClinicalTrial> clinicalTrial();

    @NotNull
    public abstract List<TherapyDrug> drug();

    @NotNull
    public abstract List<TherapyGlobalApprovalStatus> globalApprovalStatus();


}