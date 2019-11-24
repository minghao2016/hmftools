package com.hartwig.hmftools.vicc.reader;

import static com.hartwig.hmftools.vicc.reader.JsonFunctions.jsonArrayToStringList;

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
import com.hartwig.hmftools.vicc.datamodel.Association;
import com.hartwig.hmftools.vicc.datamodel.EnvironmentalContext;
import com.hartwig.hmftools.vicc.datamodel.Evidence;
import com.hartwig.hmftools.vicc.datamodel.EvidenceInfo;
import com.hartwig.hmftools.vicc.datamodel.EvidenceType;
import com.hartwig.hmftools.vicc.datamodel.Feature;
import com.hartwig.hmftools.vicc.datamodel.GeneIdentifier;
import com.hartwig.hmftools.vicc.datamodel.ImmutableAssociation;
import com.hartwig.hmftools.vicc.datamodel.ImmutableEnvironmentalContext;
import com.hartwig.hmftools.vicc.datamodel.ImmutableEvidence;
import com.hartwig.hmftools.vicc.datamodel.ImmutableEvidenceInfo;
import com.hartwig.hmftools.vicc.datamodel.ImmutableEvidenceType;
import com.hartwig.hmftools.vicc.datamodel.ImmutableFeature;
import com.hartwig.hmftools.vicc.datamodel.ImmutableGeneIdentifier;
import com.hartwig.hmftools.vicc.datamodel.ImmutablePhenotype;
import com.hartwig.hmftools.vicc.datamodel.ImmutablePhenotypeType;
import com.hartwig.hmftools.vicc.datamodel.ImmutableSequenceOntology;
import com.hartwig.hmftools.vicc.datamodel.ImmutableTaxonomy;
import com.hartwig.hmftools.vicc.datamodel.ImmutableViccEntry;
import com.hartwig.hmftools.vicc.datamodel.Phenotype;
import com.hartwig.hmftools.vicc.datamodel.PhenotypeType;
import com.hartwig.hmftools.vicc.datamodel.SequenceOntology;
import com.hartwig.hmftools.vicc.datamodel.Taxonomy;
import com.hartwig.hmftools.vicc.datamodel.ViccEntry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.util.Strings;
import org.jetbrains.annotations.NotNull;

public final class ViccJsonReader {

    private static final Logger LOGGER = LogManager.getLogger(ViccJsonReader.class);

    private ViccJsonReader() {
    }

    @NotNull
    public static List<ViccEntry> readViccKnowledgebaseJsonFile(@NotNull String jsonPath) throws IOException {
        List<ViccEntry> entries = Lists.newArrayList();

        JsonParser parser = new JsonParser();
        JsonReader reader = new JsonReader(new FileReader(jsonPath));
        reader.setLenient(true);

        while (reader.peek() != JsonToken.END_DOCUMENT) {
            JsonObject viccEntryObject = parser.parse(reader).getAsJsonObject();
            ViccDatamodelCheckerFactory.viccEntryChecker().check(viccEntryObject);

            ImmutableViccEntry.Builder viccEntryBuilder = ImmutableViccEntry.builder();
            viccEntryBuilder.source(viccEntryObject.getAsJsonPrimitive("source").getAsString());
            viccEntryBuilder.genes(jsonArrayToStringList(viccEntryObject.getAsJsonArray("genes")));
            viccEntryBuilder.geneIdentifiers(createGeneIdentifiers(viccEntryObject.getAsJsonArray("gene_identifiers")));

            // SAGE records have no "feature names" while all other knowledgebases do have it.
            if (viccEntryObject.has("feature_names")) {
                JsonElement featureNames = viccEntryObject.get("feature_names");
                if (featureNames.isJsonArray()) {
                    viccEntryBuilder.featureNames(jsonArrayToStringList(featureNames.getAsJsonArray()));
                } else if (featureNames.isJsonPrimitive()) {
                    viccEntryBuilder.featureNames(Lists.newArrayList(featureNames.getAsJsonPrimitive().getAsString()));
                }
            }

            viccEntryBuilder.features(createFeatures(viccEntryObject.getAsJsonArray("features")));
            viccEntryBuilder.association(createAssociation(viccEntryObject.getAsJsonObject("association")));
            viccEntryBuilder.tags(jsonArrayToStringList(viccEntryObject.getAsJsonArray("tags")));
            viccEntryBuilder.devTags(jsonArrayToStringList(viccEntryObject.getAsJsonArray("dev_tags")));

            if (viccEntryObject.has("cgi")) {
                viccEntryBuilder.KbSpecificObject(CgiObjectFactory.create(viccEntryObject.getAsJsonObject("cgi")));
            } else if (viccEntryObject.has("brca")) {
                viccEntryBuilder.KbSpecificObject(BRCAObjectFactory.create(viccEntryObject.getAsJsonObject("brca")));
            } else if (viccEntryObject.has("sage")) {
                viccEntryBuilder.KbSpecificObject(SageObjectFactory.create(viccEntryObject.getAsJsonObject("sage")));
            } else if (viccEntryObject.has("pmkb")) {
                viccEntryBuilder.KbSpecificObject(PmkbObjectFactory.create(viccEntryObject.getAsJsonObject("pmkb")));
            } else if (viccEntryObject.has("oncokb")) {
                viccEntryBuilder.KbSpecificObject(OncokbObjectFactory.create(viccEntryObject.getAsJsonObject("oncokb")));
            } else if (viccEntryObject.has("jax")) {
                viccEntryBuilder.KbSpecificObject(JaxObjectFactory.create(viccEntryObject.getAsJsonObject("jax")));
            } else if (viccEntryObject.has("jax_trials")) {
                viccEntryBuilder.KbSpecificObject(JaxTrialsObjectFactory.create(viccEntryObject.getAsJsonObject("jax_trials")));
            } else if (viccEntryObject.has("molecularmatch")) {
                viccEntryBuilder.KbSpecificObject(MolecularMatchObjectFactory.create(viccEntryObject.getAsJsonObject("molecularmatch")));
            } else if (viccEntryObject.has("molecularmatch_trials")) {
                viccEntryBuilder.KbSpecificObject(MolecularMatchTrialsObjectFactory.create(viccEntryObject.getAsJsonObject(
                        "molecularmatch_trials")));
            } else if (viccEntryObject.has("civic")) {
                viccEntryBuilder.KbSpecificObject(CivicObjectFactory.create(viccEntryObject.getAsJsonObject("civic")));
            } else {
                LOGGER.warn("Could not resolve kb specific object for {}", viccEntryObject);
            }

            entries.add(viccEntryBuilder.build());
        }

        reader.close();

        return entries;
    }

    @NotNull
    private static List<GeneIdentifier> createGeneIdentifiers(@NotNull JsonArray geneIdentifierArray) {
        List<GeneIdentifier> geneIdentifierList = Lists.newArrayList();

        for (JsonElement geneIdentifierElement : geneIdentifierArray) {
            JsonObject geneIdentifierObject = geneIdentifierElement.getAsJsonObject();
            ViccDatamodelCheckerFactory.geneIdentifierChecker().check(geneIdentifierObject);

            geneIdentifierList.add(ImmutableGeneIdentifier.builder()
                    .symbol(geneIdentifierObject.getAsJsonPrimitive("symbol").getAsString())
                    .entrezId(geneIdentifierObject.getAsJsonPrimitive("entrez_id").getAsString())
                    .ensemblGeneId(!geneIdentifierObject.get("ensembl_gene_id").isJsonNull() ? geneIdentifierObject.getAsJsonPrimitive(
                            "ensembl_gene_id").getAsString() : null)
                    .build());
        }

        return geneIdentifierList;
    }

    @NotNull
    private static List<Feature> createFeatures(@NotNull JsonArray featureArray) {
        List<Feature> featureList = Lists.newArrayList();
        ViccDatamodelChecker featureChecker = ViccDatamodelCheckerFactory.featureChecker();

        for (JsonElement featureElement : featureArray) {
            JsonObject featureObject = featureElement.getAsJsonObject();
            featureChecker.check(featureObject);

            featureList.add(ImmutableFeature.builder()
                    .name(featureObject.has("name") ? featureObject.getAsJsonPrimitive("name").getAsString() : null)
                    .biomarkerType(featureObject.has("biomarker_type")
                            ? featureObject.getAsJsonPrimitive("biomarker_type").getAsString()
                            : null)
                    .referenceName(featureObject.has("referenceName")
                            ? featureObject.getAsJsonPrimitive("referenceName").getAsString()
                            : null)
                    .chromosome(featureObject.has("chromosome") ? featureObject.getAsJsonPrimitive("chromosome").getAsString() : null)
                    .start(featureObject.has("start") && !featureObject.get("start").isJsonNull()
                            ? featureObject.getAsJsonPrimitive("start").getAsString()
                            : null)
                    .end(featureObject.has("end") && !featureObject.get("end").isJsonNull() ? featureObject.getAsJsonPrimitive("end")
                            .getAsString() : null)
                    .ref(featureObject.has("ref") && !featureObject.get("ref").isJsonNull() ? featureObject.getAsJsonPrimitive("ref")
                            .getAsString() : null)
                    .alt(featureObject.has("alt") && !featureObject.get("alt").isJsonNull() ? featureObject.getAsJsonPrimitive("alt")
                            .getAsString() : null)
                    // TODO Read provenance!
                    .provenance(Lists.newArrayList())
                    .provenanceRule(featureObject.has("provenance_rule")
                            ? featureObject.getAsJsonPrimitive("provenance_rule").getAsString()
                            : null)
                    .geneSymbol(featureObject.has("geneSymbol") && !featureObject.get("geneSymbol").isJsonNull()
                            ? featureObject.getAsJsonPrimitive("geneSymbol").getAsString()
                            : null)
                    .synonyms(featureObject.has("synonyms") ? jsonArrayToStringList(featureObject.getAsJsonArray("synonyms")) : null)
                    .entrezId(featureObject.has("entrez_id") ? featureObject.getAsJsonPrimitive("entrez_id").getAsString() : null)
                    .sequenceOntology(featureObject.has("sequence_ontology") ? createSequenceOntology(featureObject.getAsJsonObject(
                            "sequence_ontology")) : null)
                    .links(featureObject.has("links") ? jsonArrayToStringList(featureObject.getAsJsonArray("links")) : null)
                    .description(featureObject.has("description") ? featureObject.getAsJsonPrimitive("description").getAsString() : null)
                    // TODO Add 'attributes'
                    // TODO Add 'info'
                    .build());
        }

        return featureList;
    }

    @NotNull
    private static SequenceOntology createSequenceOntology(@NotNull JsonObject objectSequenceOntology) {
        ViccDatamodelCheckerFactory.sequenceOntologyChecker().check(objectSequenceOntology);

        return ImmutableSequenceOntology.builder()
                .hierarchy(objectSequenceOntology.has("hierarchy")
                        ? jsonArrayToStringList(objectSequenceOntology.getAsJsonArray("hierarchy"))
                        : null)
                .soid(objectSequenceOntology.getAsJsonPrimitive("soid").getAsString())
                .parentSoid(objectSequenceOntology.getAsJsonPrimitive("parent_soid").getAsString())
                .name(objectSequenceOntology.getAsJsonPrimitive("name").getAsString())
                .parentName(objectSequenceOntology.getAsJsonPrimitive("parent_name").getAsString())
                .build();
    }

    @NotNull
    private static Association createAssociation(@NotNull JsonObject associationObject) {
        ViccDatamodelCheckerFactory.associationChecker().check(associationObject);

        return ImmutableAssociation.builder()
                .variantName(associationObject.has("variant_name") && associationObject.get("variant_name").isJsonArray()
                        ? Strings.EMPTY
                        : associationObject.has("variant_name") && associationObject.get("variant_name").isJsonPrimitive()
                                ? associationObject.getAsJsonPrimitive("variant_name").getAsString()
                                : null)
                .evidence(createEvidence(associationObject.getAsJsonArray("evidence")))
                .evidenceLevel(associationObject.has("evidence_level") ? associationObject.getAsJsonPrimitive("evidence_level")
                        .getAsString() : null)
                .evidenceLabel(
                        associationObject.has("evidence_label") && !associationObject.get("evidence_label").isJsonNull() ? associationObject
                                .getAsJsonPrimitive("evidence_label")
                                .getAsString() : null)
                .responseType(associationObject.has("response_type") && !associationObject.get("response_type").isJsonNull()
                        ? associationObject.getAsJsonPrimitive("response_type").getAsString()
                        : null)
                .drugLabels(associationObject.has("drug_labels") ? associationObject.getAsJsonPrimitive("drug_labels").getAsString() : null)
                .sourceLink(associationObject.has("source_link") ? associationObject.getAsJsonPrimitive("source_link").getAsString() : null)
                .publicationUrls(associationObject.has("publication_url") && associationObject.get("publication_url").isJsonPrimitive()
                        ? Lists.newArrayList(associationObject.getAsJsonPrimitive("publication_url").getAsString())
                        : associationObject.has("publication_url") && associationObject.get("publication_url").isJsonArray()
                                ? Lists.newArrayList(associationObject.getAsJsonArray("publication_url").getAsString())
                                : null)
                .phenotype(associationObject.has("phenotype") ? createPhenotype(associationObject.getAsJsonObject("phenotype")) : null)
                .description(associationObject.getAsJsonPrimitive("description").getAsString())
                .environmentalContexts(associationObject.has("environmentalContexts")
                        ? createEnvironmentalContexts(associationObject.getAsJsonArray("environmentalContexts"))
                        : null)
                .oncogenic(associationObject.has("oncogenic") ? associationObject.getAsJsonPrimitive("oncogenic").getAsString() : null)
                .build();
    }

    @NotNull
    private static List<Evidence> createEvidence(@NotNull JsonArray evidenceArray) {
        List<Evidence> evidenceList = Lists.newArrayList();
        ViccDatamodelChecker evidenceChecker = ViccDatamodelCheckerFactory.evidenceChecker();

        for (JsonElement evidenceElement : evidenceArray) {
            JsonObject evidenceObject = evidenceElement.getAsJsonObject();
            evidenceChecker.check(evidenceObject);

            evidenceList.add(ImmutableEvidence.builder()
                    .info(!evidenceObject.get("info").isJsonNull() ? createEvidenceInfo(evidenceObject.getAsJsonObject("info")) : null)
                    .evidenceType(createEvidenceType(evidenceObject.getAsJsonObject("evidenceType")))
                    .description(!evidenceObject.get("description").isJsonNull() ? evidenceObject.getAsJsonPrimitive("description")
                            .getAsString() : null)
                    .build());
        }
        return evidenceList;
    }

    @NotNull
    private static EvidenceInfo createEvidenceInfo(@NotNull JsonObject evidenceInfoObject) {
        ViccDatamodelCheckerFactory.evidenceInfoChecker().check(evidenceInfoObject);

        return ImmutableEvidenceInfo.builder()
                .publications(jsonArrayToStringList(evidenceInfoObject.getAsJsonArray("publications")))
                .build();
    }

    @NotNull
    private static EvidenceType createEvidenceType(@NotNull JsonObject evidenceTypeObject) {
        ViccDatamodelCheckerFactory.evidenceTypeChecker().check(evidenceTypeObject);

        return ImmutableEvidenceType.builder()
                .sourceName(evidenceTypeObject.getAsJsonPrimitive("sourceName").getAsString())
                .id(evidenceTypeObject.has("id") ? evidenceTypeObject.getAsJsonPrimitive("id").getAsString() : null)
                .build();
    }

    @NotNull
    private static List<EnvironmentalContext> createEnvironmentalContexts(@NotNull JsonArray environmentalContextArray) {
        List<EnvironmentalContext> environmentalContextList = Lists.newArrayList();
        ViccDatamodelChecker environmentalContextChecker = ViccDatamodelCheckerFactory.environmentalContextChecker();

        for (JsonElement environmentalContextElement : environmentalContextArray) {
            JsonObject environmentalContextObject = environmentalContextElement.getAsJsonObject();
            environmentalContextChecker.check(environmentalContextObject);

            environmentalContextList.add(ImmutableEnvironmentalContext.builder()
                    .term(environmentalContextObject.has("term")
                            ? environmentalContextObject.getAsJsonPrimitive("term").getAsString()
                            : null)
                    .description(environmentalContextObject.getAsJsonPrimitive("description").getAsString())
                    .taxonomy(environmentalContextObject.has("taxonomy") ? createTaxonomy(environmentalContextObject.getAsJsonObject(
                            "taxonomy")) : null)
                    .source(environmentalContextObject.has("source")
                            ? environmentalContextObject.getAsJsonPrimitive("source").getAsString()
                            : null)
                    .usanStem(environmentalContextObject.has("usan_stem") ? environmentalContextObject.getAsJsonPrimitive("usan_stem")
                            .getAsString() : null)
                    .approvedCountries(environmentalContextObject.has("approved_countries") ? jsonArrayToStringList(
                            environmentalContextObject.getAsJsonArray("approved_countries")) : Lists.newArrayList())
                    .toxicity(environmentalContextObject.has("toxicity") ? environmentalContextObject.getAsJsonPrimitive("toxicity")
                            .getAsString() : null)
                    .id(environmentalContextObject.has("id") && !environmentalContextObject.get("id").isJsonNull()
                            ? environmentalContextObject.getAsJsonPrimitive("id").getAsString()
                            : null)
                    .build());
        }
        return environmentalContextList;
    }

    @NotNull
    private static Taxonomy createTaxonomy(@NotNull JsonObject taxonomyObject) {
        ViccDatamodelCheckerFactory.taxonomyChecker().check(taxonomyObject);

        return ImmutableTaxonomy.builder()
                .kingdom(taxonomyObject.getAsJsonPrimitive("kingdom").getAsString())
                .directParent(taxonomyObject.getAsJsonPrimitive("direct-parent").getAsString())
                .classs(taxonomyObject.getAsJsonPrimitive("class").getAsString())
                .subClass(taxonomyObject.has("subclass") ? taxonomyObject.getAsJsonPrimitive("subclass").getAsString() : null)
                .superClass(taxonomyObject.getAsJsonPrimitive("superclass").getAsString())
                .build();
    }

    @NotNull
    private static Phenotype createPhenotype(@NotNull JsonObject phenotypeObject) {
        ViccDatamodelCheckerFactory.phenotypeChecker().check(phenotypeObject);

        return ImmutablePhenotype.builder()
                .type(phenotypeObject.has("type") ? createPhenotypeType(phenotypeObject.getAsJsonObject("type")) : null)
                .description(phenotypeObject.getAsJsonPrimitive("description").getAsString())
                .family(phenotypeObject.getAsJsonPrimitive("family").getAsString())
                .id(phenotypeObject.has("id") ? phenotypeObject.getAsJsonPrimitive("id").getAsString() : null)
                .build();
    }

    @NotNull
    private static PhenotypeType createPhenotypeType(@NotNull JsonObject phenotypeTypeObject) {
        ViccDatamodelCheckerFactory.phenotypeTypeChecker().check(phenotypeTypeObject);

        return ImmutablePhenotypeType.builder()
                .source(!phenotypeTypeObject.get("source").isJsonNull()
                        ? phenotypeTypeObject.getAsJsonPrimitive("source").getAsString()
                        : null)
                .term(phenotypeTypeObject.getAsJsonPrimitive("term").getAsString())
                .id(phenotypeTypeObject.getAsJsonPrimitive("id").getAsString())
                .build();
    }
}
