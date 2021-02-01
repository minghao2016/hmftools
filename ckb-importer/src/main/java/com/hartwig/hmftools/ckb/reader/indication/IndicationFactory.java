package com.hartwig.hmftools.ckb.reader.indication;

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
import com.hartwig.hmftools.ckb.datamodel.common.ClinicalTrialInfo;
import com.hartwig.hmftools.ckb.datamodel.common.EvidenceInfo;
import com.hartwig.hmftools.ckb.datamodel.common.ImmutableClinicalTrialInfo;
import com.hartwig.hmftools.ckb.datamodel.common.ImmutableEvidenceInfo;
import com.hartwig.hmftools.ckb.datamodel.common.ImmutableIndicationInfo;
import com.hartwig.hmftools.ckb.datamodel.common.ImmutableMolecularProfileInfo;
import com.hartwig.hmftools.ckb.datamodel.common.ImmutableReferenceInfo;
import com.hartwig.hmftools.ckb.datamodel.common.ImmutableTherapyInfo;
import com.hartwig.hmftools.ckb.datamodel.common.IndicationInfo;
import com.hartwig.hmftools.ckb.datamodel.common.MolecularProfileInfo;
import com.hartwig.hmftools.ckb.datamodel.common.ReferenceInfo;
import com.hartwig.hmftools.ckb.datamodel.common.TherapyInfo;
import com.hartwig.hmftools.ckb.datamodel.indication.ImmutableIndication;
import com.hartwig.hmftools.ckb.datamodel.indication.Indication;
import com.hartwig.hmftools.common.utils.json.JsonDatamodelChecker;
import com.hartwig.hmftools.common.utils.json.JsonFunctions;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

public class IndicationFactory {

    private static final Logger LOGGER = LogManager.getLogger(IndicationFactory.class);

    private IndicationFactory() {

    }

    @NotNull
    public static List<Indication> readingIndication(@NotNull String indicationDir) throws IOException {
        LOGGER.info("Start reading indications");

        List<Indication> indications = Lists.newArrayList();
        File[] filesIndications = new File(indicationDir).listFiles();

        if (filesIndications != null) {
            LOGGER.info("The total files in the indication dir is {}", filesIndications.length);

            for (File indication : filesIndications) {
                JsonParser parser = new JsonParser();
                JsonReader reader = new JsonReader(new FileReader(indication));
                reader.setLenient(true);

                while (reader.peek() != JsonToken.END_DOCUMENT) {
                    JsonObject indicationEntryObject = parser.parse(reader).getAsJsonObject();
                    JsonDatamodelChecker indicationChecker = IndicationDataModelChecker.indicationObjectChecker();
                    indicationChecker.check(indicationEntryObject);

                    indications.add(ImmutableIndication.builder()
                            .id(JsonFunctions.integer(indicationEntryObject, "id"))
                            .name(JsonFunctions.string(indicationEntryObject, "name"))
                            .source(JsonFunctions.string(indicationEntryObject, "source"))
                            .definition(JsonFunctions.nullableString(indicationEntryObject, "definition"))
                            .currentPreferredTerm(JsonFunctions.nullableString(indicationEntryObject, "currentPreferredTerm"))
                            .lastUpdateDateFromDO(JsonFunctions.nullableString(indicationEntryObject, "lastUpdateDateFromDO"))
                            .altId(JsonFunctions.stringList(indicationEntryObject, "altIds"))
                            .termId(JsonFunctions.string(indicationEntryObject, "termId"))
                            .evidence(extractEvidence(indicationEntryObject.getAsJsonArray("evidence")))
                            .clinicalTrial(extractClinicalTrials(indicationEntryObject.getAsJsonArray("clinicalTrials")))
                            .build());
                }
                reader.close();
            }
        }
        LOGGER.info("Finished reading indications");

        return indications;
    }

    @NotNull
    public static List<EvidenceInfo> extractEvidence(@NotNull JsonArray jsonArray) {
        List<EvidenceInfo> evidences = Lists.newArrayList();
        JsonDatamodelChecker indicationEvidenceChecker = IndicationDataModelChecker.indicationEvidenceObjectChecker();

        for (JsonElement evidence : jsonArray) {
            JsonObject evidenceJsonObject = evidence.getAsJsonObject();
            indicationEvidenceChecker.check(evidenceJsonObject);

            evidences.add(ImmutableEvidenceInfo.builder()
                    .id(JsonFunctions.integer(evidenceJsonObject, "id"))
                    .approvalStatus(JsonFunctions.string(evidenceJsonObject, "approvalStatus"))
                    .evidenceType(JsonFunctions.string(evidenceJsonObject, "evidenceType"))
                    .efficacyEvidence(JsonFunctions.string(evidenceJsonObject, "efficacyEvidence"))
                    .molecularProfile(extractMolecularProfile(evidenceJsonObject.getAsJsonObject("molecularProfile")))
                    .therapy(extractTherapy(evidenceJsonObject.getAsJsonObject("therapy")))
                    .indication(extractIndication(evidenceJsonObject.getAsJsonObject("indication")))
                    .responseType(JsonFunctions.string(evidenceJsonObject, "responseType"))
                    .reference(extractReference(evidenceJsonObject.getAsJsonArray("references")))
                    .ampCapAscoEvidenceLevel(JsonFunctions.string(evidenceJsonObject, "ampCapAscoEvidenceLevel"))
                    .ampCapAscoInferredTier(JsonFunctions.string(evidenceJsonObject, "ampCapAscoInferredTier"))
                    .build());
        }
        return evidences;
    }

    @NotNull
    public static MolecularProfileInfo extractMolecularProfile(@NotNull JsonObject jsonObject) {
        JsonDatamodelChecker indicationEvidenceMolecularProfileChecker = IndicationDataModelChecker.indicationEvidenceMolecularProfileObjectChecker();
        indicationEvidenceMolecularProfileChecker.check(jsonObject);

        return ImmutableMolecularProfileInfo.builder()
                .id(JsonFunctions.integer(jsonObject, "id"))
                .profileName(JsonFunctions.string(jsonObject, "profileName"))
                .build();
    }

    @NotNull
    public static TherapyInfo extractTherapy(@NotNull JsonObject jsonObject) {
        JsonDatamodelChecker indicationEvidenceTherapyChecker = IndicationDataModelChecker.indicationEvidenceTherapyObjectChecker();
        indicationEvidenceTherapyChecker.check(jsonObject);

        return ImmutableTherapyInfo.builder()
                .id(JsonFunctions.integer(jsonObject, "id"))
                .therapyName(JsonFunctions.string(jsonObject, "therapyName"))
                .synonyms(JsonFunctions.nullableString(jsonObject, "synonyms"))
                .build();
    }

    @NotNull
    public static IndicationInfo extractIndication(@NotNull JsonObject jsonObject) {
        JsonDatamodelChecker indicationEvidenceIndicationChecker = IndicationDataModelChecker.indicationEvidenceIndicationObjectChecker();
        indicationEvidenceIndicationChecker.check(jsonObject);

        return ImmutableIndicationInfo.builder()
                .id(JsonFunctions.integer(jsonObject, "id"))
                .name(JsonFunctions.string(jsonObject, "name"))
                .source(JsonFunctions.string(jsonObject, "source"))
                .build();
    }

    @NotNull
    public static List<ReferenceInfo> extractReference(@NotNull JsonArray jsonArray) {
        List<ReferenceInfo> references = Lists.newArrayList();
        JsonDatamodelChecker indicationEvidenceReferenceChecker = IndicationDataModelChecker.indicationEvidenceReferenceObjectChecker();

        for (JsonElement reference : jsonArray) {
            JsonObject referenceJsonObject = reference.getAsJsonObject();
            indicationEvidenceReferenceChecker.check(referenceJsonObject);

            references.add(ImmutableReferenceInfo.builder()
                    .id(JsonFunctions.integer(referenceJsonObject, "id"))
                    .pubMedId(JsonFunctions.nullableString(referenceJsonObject, "pubMedId"))
                    .title(JsonFunctions.nullableString(referenceJsonObject, "title"))
                    .url(JsonFunctions.nullableString(referenceJsonObject, "url"))
                    .build());
        }
        return references;
    }

    @NotNull
    public static List<ClinicalTrialInfo> extractClinicalTrials(@NotNull JsonArray jsonArray) {
        List<ClinicalTrialInfo> clinicalTrials = Lists.newArrayList();
        JsonDatamodelChecker indicationClinicalTrialChecker = IndicationDataModelChecker.indicationClinicaltrialObjectChecker();

        for (JsonElement clinicalTrial : jsonArray) {
            JsonObject clinicalTrialJsonObject = clinicalTrial.getAsJsonObject();
            indicationClinicalTrialChecker.check(clinicalTrialJsonObject);

            clinicalTrials.add(ImmutableClinicalTrialInfo.builder()
                    .nctId(JsonFunctions.string(clinicalTrialJsonObject, "nctId"))
                    .title(JsonFunctions.string(clinicalTrialJsonObject, "title"))
                    .phase(JsonFunctions.string(clinicalTrialJsonObject, "phase"))
                    .recruitment(JsonFunctions.string(clinicalTrialJsonObject, "recruitment"))
                    .therapy(extractTherapyList(clinicalTrialJsonObject.getAsJsonArray("therapies")))
                    .build());
        }
        return clinicalTrials;
    }

    @NotNull
    public static List<TherapyInfo> extractTherapyList(@NotNull JsonArray jsonArray) {
        List<TherapyInfo> therapies = Lists.newArrayList();
        JsonDatamodelChecker indicationClinicalTrialTherapiesChecker = IndicationDataModelChecker.indicationEvidenceTherapyObjectChecker();

        for (JsonElement therapy : jsonArray) {
            JsonObject therapyJsonObject = therapy.getAsJsonObject();
            indicationClinicalTrialTherapiesChecker.check(therapyJsonObject);

            therapies.add(ImmutableTherapyInfo.builder()
                    .id(JsonFunctions.integer(therapyJsonObject, "id"))
                    .therapyName(JsonFunctions.string(therapyJsonObject, "therapyName"))
                    .synonyms(JsonFunctions.nullableString(therapyJsonObject, "synonyms"))
                    .build());
        }
        return therapies;

    }
}