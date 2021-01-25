package com.hartwig.hmftools.ckb.gene;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.hartwig.hmftools.common.utils.json.JsonDatamodelChecker;
import com.hartwig.hmftools.common.utils.json.JsonFunctions;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

public class GeneFactory {

    private static final Logger LOGGER = LogManager.getLogger(GeneFactory.class);

    private GeneFactory() {

    }

    @NotNull
    public static List<Gene> readingGenes(@NotNull String geneDir) throws IOException {
        LOGGER.info("Start reading genes");

        List<Gene> genes = Lists.newArrayList();
        File[] filesGenes = new File(geneDir).listFiles();

        if (filesGenes != null) {
            LOGGER.info("The total files in the genes dir is {}", filesGenes.length);

            for (File gene : filesGenes) {
                JsonParser parser = new JsonParser();
                JsonReader reader = new JsonReader(new FileReader(gene));
                reader.setLenient(true);

                while (reader.peek() != JsonToken.END_DOCUMENT) {
                    JsonObject geneEntryObject = parser.parse(reader).getAsJsonObject();
                    JsonDatamodelChecker geneChecker = GeneDataModelChecker.geneObjectChecker();
                    geneChecker.check(geneEntryObject);

                    genes.add(ImmutableGene.builder()
                            .id(JsonFunctions.string(geneEntryObject, "id"))
                            .geneSymbol(JsonFunctions.string(geneEntryObject, "geneSymbol"))
                            .term(JsonFunctions.stringList(geneEntryObject, "terms"))
                            .entrezId(JsonFunctions.nullableString(geneEntryObject, "entrezId"))
                            .synonym(JsonFunctions.stringList(geneEntryObject, "synonyms"))
                            .chromosome(JsonFunctions.nullableString(geneEntryObject, "chromosome"))
                            .mapLocation(JsonFunctions.nullableString(geneEntryObject, "mapLocation"))
                            .geneDescription(extractGeneDescriptions(geneEntryObject.getAsJsonArray("geneDescriptions")))
                            .canonicalTranscript(JsonFunctions.nullableString(geneEntryObject, "canonicalTranscript"))
                            .geneRole(JsonFunctions.string(geneEntryObject, "geneRole"))
                            .createDate(JsonFunctions.string(geneEntryObject, "createDate"))
                            .updateDate(JsonFunctions.nullableString(geneEntryObject, "updateDate"))
                            .clinicalTrial(extractGeneClinicalTrial(geneEntryObject.getAsJsonArray("clinicalTrials")))
                            .evidence(extractGeneEvidence(geneEntryObject.getAsJsonArray("evidence")))
                            .variant(extractGeneVariant(geneEntryObject.getAsJsonArray("variants")))
                            .molecularProfiles(extarctMolecularProfile(geneEntryObject.getAsJsonArray("molecularProfiles")))
                            .categoryVariants(extractCategoryVariant(geneEntryObject.getAsJsonArray("categoryVariants")))
                            .build());
                }
                reader.close();
            }
        }
        LOGGER.info("Finished reading genes");

        return genes;
    }

    @NotNull
    public static List<GeneDescription> extractGeneDescriptions(@NotNull JsonArray jsonArray) {
        List<GeneDescription> geneDescriptions = Lists.newArrayList();
        JsonDatamodelChecker geneDescriptionChecker = GeneDataModelChecker.geneDescriptionObjectChecker();

        for (JsonElement geneDescription : jsonArray) {
            JsonObject geneDescriptionJsonObject = geneDescription.getAsJsonObject();
            geneDescriptionChecker.check(geneDescriptionJsonObject);

            geneDescriptions.add(ImmutableGeneDescription.builder()
                    .description(JsonFunctions.string(geneDescriptionJsonObject, "description"))
                    .geneReference(extractGeneReferences(geneDescriptionJsonObject.getAsJsonArray("references")))
                    .build());
        }
        return geneDescriptions;
    }

    @NotNull
    public static List<GeneReference> extractGeneReferences(@NotNull JsonArray jsonArray) {
        List<GeneReference> references = Lists.newArrayList();
        JsonDatamodelChecker geneReferenceChecker = GeneDataModelChecker.geneReferenceObjectChecker();

        for (JsonElement geneReference : jsonArray) {
            JsonObject geneReferenceJsonObject = geneReference.getAsJsonObject();
            geneReferenceChecker.check(geneReferenceJsonObject);

            references.add(ImmutableGeneReference.builder()
                    .id(JsonFunctions.string(geneReferenceJsonObject, "id"))
                    .pubmedId(JsonFunctions.nullableString(geneReferenceJsonObject, "pubMedId"))
                    .title(JsonFunctions.nullableString(geneReferenceJsonObject, "title"))
                    .url(JsonFunctions.nullableString(geneReferenceJsonObject, "url"))
                    .build());

        }
        return references;
    }

    @NotNull
    public static List<GeneClinicalTrial> extractGeneClinicalTrial(@NotNull JsonArray jsonArray) {
        List<GeneClinicalTrial> clinicalTrials = Lists.newArrayList();
        JsonDatamodelChecker geneClinicalTrialChecker = GeneDataModelChecker.geneClinicalTrialObjectChecker();

        for (JsonElement geneClinicalTrial : jsonArray) {
            JsonObject geneClinicalTrialJsonObject = geneClinicalTrial.getAsJsonObject();
            geneClinicalTrialChecker.check(geneClinicalTrialJsonObject);

            clinicalTrials.add(ImmutableGeneClinicalTrial.builder()
                    .nctId(JsonFunctions.string(geneClinicalTrialJsonObject, "nctId"))
                    .title(JsonFunctions.string(geneClinicalTrialJsonObject, "title"))
                    .phase(JsonFunctions.string(geneClinicalTrialJsonObject, "phase"))
                    .recruitment(JsonFunctions.string(geneClinicalTrialJsonObject, "recruitment"))
                    .geneTherapy(extractGeneTherapy(geneClinicalTrialJsonObject.getAsJsonArray("therapies")))
                    .build());
        }
        return clinicalTrials;
    }

    @NotNull
    public static List<GeneTherapy> extractGeneTherapy(@NotNull JsonArray jsonArray) {
        List<GeneTherapy> geneTherapies = Lists.newArrayList();
        JsonDatamodelChecker geneTherapiesChecker = GeneDataModelChecker.geneTherapiesObjectChecker();

        for (JsonElement geneTherapy : jsonArray) {
            JsonObject geneTherapyJsonObject = geneTherapy.getAsJsonObject();
            geneTherapiesChecker.check(geneTherapyJsonObject);

            geneTherapies.add(ImmutableGeneTherapy.builder()
                    .id(JsonFunctions.string(geneTherapyJsonObject, "id"))
                    .therapyName(JsonFunctions.string(geneTherapyJsonObject, "therapyName"))
                    .synonyms(JsonFunctions.nullableString(geneTherapyJsonObject, "synonyms"))
                    .build());
        }
        return geneTherapies;
    }

    @NotNull
    public static List<GeneEvidence> extractGeneEvidence(@NotNull JsonArray jsonArray) {
        List<GeneEvidence> geneEvidences = Lists.newArrayList();
        JsonDatamodelChecker geneEvidenceChecker = GeneDataModelChecker.geneEvidenceObjectChecker();

        for (JsonElement geneEvidence : jsonArray) {
            JsonObject geneEvidenceObject = geneEvidence.getAsJsonObject();
            geneEvidenceChecker.check(geneEvidenceObject);

            geneEvidences.add(ImmutableGeneEvidence.builder()
                    .id(JsonFunctions.string(geneEvidenceObject, "id"))
                    .approvalStatus(JsonFunctions.string(geneEvidenceObject, "approvalStatus"))
                    .evidenceType(JsonFunctions.string(geneEvidenceObject, "evidenceType"))
                    .efficacyEvidence(JsonFunctions.string(geneEvidenceObject, "efficacyEvidence"))
                    .molecularProfile(extractGeneMolecularProfileObject(geneEvidenceObject.getAsJsonObject("molecularProfile")))
                    .therapy(extractGeneTherapyObject(geneEvidenceObject.getAsJsonObject("therapy")))
                    .indication(extractGeneIndicationObject(geneEvidenceObject.getAsJsonObject("indication")))
                    .responseType(JsonFunctions.string(geneEvidenceObject, "responseType"))
                    .reference(extractGeneReferences(geneEvidenceObject.getAsJsonArray("references")))
                    .ampCapAscoEvidenceLevel(JsonFunctions.string(geneEvidenceObject, "ampCapAscoEvidenceLevel"))
                    .ampCapAscoInferredTier(JsonFunctions.string(geneEvidenceObject, "ampCapAscoInferredTier"))
                    .build());
        }
        return geneEvidences;
    }

    @NotNull
    public static GeneMolecularProfile extractGeneMolecularProfileObject(@NotNull JsonObject jsonObject) {
        JsonDatamodelChecker geneMolecularProfileChecker = GeneDataModelChecker.geneMolecularProfileObjectChecker();
        geneMolecularProfileChecker.check(jsonObject);

        return ImmutableGeneMolecularProfile.builder()
                .id(JsonFunctions.string(jsonObject, "id"))
                .profileName(JsonFunctions.string(jsonObject, "profileName"))
                .profileTreatmentApproache(null) // is not present in array for evidence
                .build();
    }

    @NotNull
    public static GeneTherapy extractGeneTherapyObject(@NotNull JsonObject jsonObject) {
        JsonDatamodelChecker geneTherapyChecker = GeneDataModelChecker.geneTherapyObjectChecker();
        geneTherapyChecker.check(jsonObject);

        return ImmutableGeneTherapy.builder()
                .id(JsonFunctions.string(jsonObject, "id"))
                .therapyName(JsonFunctions.string(jsonObject, "therapyName"))
                .synonyms(JsonFunctions.nullableString(jsonObject, "synonyms"))
                .build();
    }

    @NotNull
    public static GeneIndication extractGeneIndicationObject(@NotNull JsonObject jsonObject) {
        JsonDatamodelChecker geneIndicationChecker = GeneDataModelChecker.geneIndicationObjectChecker();
        geneIndicationChecker.check(jsonObject);

        return ImmutableGeneIndication.builder()
                .id(JsonFunctions.string(jsonObject, "id"))
                .name(JsonFunctions.string(jsonObject, "name"))
                .source(JsonFunctions.string(jsonObject, "source"))
                .build();
    }

    @NotNull
    public static List<GeneVariant> extractGeneVariant(@NotNull JsonArray jsonArray) {
        List<GeneVariant> geneVariants = Lists.newArrayList();
        JsonDatamodelChecker geneVariantChecker = GeneDataModelChecker.geneVariantObjectChecker();

        for (JsonElement geneVariant : jsonArray) {
            JsonObject geneVariantObject = geneVariant.getAsJsonObject();
            geneVariantChecker.check(geneVariantObject);

            geneVariants.add(ImmutableGeneVariant.builder()
                    .id(JsonFunctions.string(geneVariantObject, "id"))
                    .fullName(JsonFunctions.string(geneVariantObject, "fullName"))
                    .impact(JsonFunctions.nullableString(geneVariantObject, "impact"))
                    .proteinEffect(JsonFunctions.nullableString(geneVariantObject, "proteinEffect"))
                    .variantDescription(extractVariantDescription(geneVariantObject.getAsJsonArray("geneVariantDescriptions")))
                    .build());
        }
        return geneVariants;
    }

    @NotNull
    public static List<GeneVariantDescription> extractVariantDescription(@NotNull JsonArray jsonArray) {
        List<GeneVariantDescription> geneVariantDescriptions = Lists.newArrayList();
        JsonDatamodelChecker geneVariantDescriptionChecker = GeneDataModelChecker.geneVariantDescriptionObjectChecker();

        for (JsonElement variantDescription : jsonArray) {
            JsonObject variantDescriptionObject = variantDescription.getAsJsonObject();
            geneVariantDescriptionChecker.check(variantDescriptionObject);

            geneVariantDescriptions.add(ImmutableGeneVariantDescription.builder()
                    .description(JsonFunctions.string(variantDescriptionObject, "description"))
                    .reference(extractGeneReferences(variantDescriptionObject.getAsJsonArray("references")))
                    .build());
        }
        return geneVariantDescriptions;
    }

    @NotNull
    public static List<GeneMolecularProfile> extarctMolecularProfile(@NotNull JsonArray jsonArray) {
        List<GeneMolecularProfile> molecularProfiles = Lists.newArrayList();
        JsonDatamodelChecker geneMolecularProfileChecker = GeneDataModelChecker.geneMolecularProfileObjectChecker();

        for (JsonElement molecularProfile : jsonArray) {
            JsonObject molecularProfileObject = molecularProfile.getAsJsonObject();
            geneMolecularProfileChecker.check(molecularProfileObject);

            molecularProfiles.add(ImmutableGeneMolecularProfile.builder()
                    .id(JsonFunctions.string(molecularProfileObject, "id"))
                    .profileName(JsonFunctions.string(molecularProfileObject, "profileName"))
                    .profileTreatmentApproache(extractProfileTreatmentApproach(molecularProfileObject.getAsJsonArray("profileTreatmentApproaches")))
                    .build());
        }
        return molecularProfiles;
    }

    @NotNull
    public static List<GeneProfileTreatmentApproache> extractProfileTreatmentApproach(@NotNull JsonArray jsonArray) {
        List<GeneProfileTreatmentApproache> geneProfileTreatmentApproaches = Lists.newArrayList();
        JsonDatamodelChecker geneProfileTreatmentApprochChecker = GeneDataModelChecker.geneProfileTreatmentApproachObjectChecker();

        for (JsonElement profileTreatmentApproach : jsonArray) {
            JsonObject profileTreatmentApproachObject = profileTreatmentApproach.getAsJsonObject();
            geneProfileTreatmentApprochChecker.check(profileTreatmentApproachObject);

            geneProfileTreatmentApproaches.add(ImmutableGeneProfileTreatmentApproache.builder()
                    .id(JsonFunctions.string(profileTreatmentApproachObject, "id"))
                    .name(JsonFunctions.string(profileTreatmentApproachObject, "name"))
                    .profileName(JsonFunctions.string(profileTreatmentApproachObject, "profileName"))
                    .build());
        }
        return geneProfileTreatmentApproaches;
    }

    @NotNull
    public static List<GeneCategoryVariant> extractCategoryVariant(@NotNull JsonArray jsonArray) {
        List<GeneCategoryVariant> geneCategoryVariants = Lists.newArrayList();
        JsonDatamodelChecker geneProfileTreatmentApprochChecker = GeneDataModelChecker.geneCategoryVariantObjectChecker();

        for (JsonElement geneCategoryVariant : jsonArray) {
            JsonObject geneCategoryVariantObject = geneCategoryVariant.getAsJsonObject();
            geneProfileTreatmentApprochChecker.check(geneCategoryVariantObject);

            geneCategoryVariants.add(ImmutableGeneCategoryVariant.builder()
                    .id(JsonFunctions.string(geneCategoryVariantObject, "id"))
                    .fullName(JsonFunctions.string(geneCategoryVariantObject, "fullName"))
                    .impact(JsonFunctions.nullableString(geneCategoryVariantObject, "impact"))
                    .proteinEffect(JsonFunctions.nullableString(geneCategoryVariantObject, "proteinEffect"))
                    .geneVariantDescription(extractVariantDescription(geneCategoryVariantObject.getAsJsonArray("geneVariantDescriptions")))
                    .build());
        }
        return geneCategoryVariants;
    }


}