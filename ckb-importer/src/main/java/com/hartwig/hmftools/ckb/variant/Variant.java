package com.hartwig.hmftools.ckb.variant;

import java.util.List;

import org.immutables.value.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Value.Immutable
@Value.Style(passAnnotations = { NotNull.class, Nullable.class })
public abstract class Variant {

    @NotNull
    public abstract String id();

    @NotNull
    public abstract String fullName();

    @Nullable
    public abstract String impact();

    @Nullable
    public abstract String proteinEffect();

    @NotNull
    public abstract List<VariantGeneVariantDescription> geneVariantDescription();

    @Nullable
    public abstract String type();

    @NotNull
    public abstract VariantGene gene();

    @NotNull
    public abstract String variant();

    @NotNull
    public abstract String createDate();

    @NotNull
    public abstract String updateDate();

    @Nullable
    public abstract VariantReferenceTranscriptCoordinate referenceTranscriptCoordinates();

    @NotNull
    public abstract List<VariantPartnerGene> partnerGene();

    @NotNull
    public abstract List<VariantCategoryVariantPath> categoryVariantPath();

    @NotNull
    public abstract List<VariantEvidence> evidence();

    @NotNull
    public abstract List<VariantExtendedEvidence> extendedEvidence();

    @NotNull
    public abstract List<VariantMolecularProfile> molecularProfile();

    @NotNull
    public abstract List<VariantAllTranscriptCoordinate> allTranscriptCoordinate();

    @NotNull
    public abstract List<VariantMemberVariant> memberVariant();


}