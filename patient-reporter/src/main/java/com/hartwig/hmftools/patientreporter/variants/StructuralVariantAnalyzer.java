package com.hartwig.hmftools.patientreporter.variants;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.hartwig.hmftools.common.cosmic.CosmicModel;
import com.hartwig.hmftools.common.slicing.HmfSlicer;
import com.hartwig.hmftools.common.variant.structural.StructuralVariant;
import com.hartwig.hmftools.patientreporter.util.PatientReportFormat;
import com.hartwig.hmftools.svannotation.GeneAnnotation;
import com.hartwig.hmftools.svannotation.VariantAnnotation;
import com.hartwig.hmftools.svannotation.VariantAnnotator;
import com.hartwig.hmftools.svannotation.Transcript;

import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

public class StructuralVariantAnalyzer {

    private final VariantAnnotator annotator;
    private final HmfSlicer slicer;
    private final CosmicModel cosmicModel;

    public StructuralVariantAnalyzer(final VariantAnnotator annotator, final HmfSlicer slicer, final CosmicModel cosmicModel) {
        this.annotator = annotator;
        this.slicer = slicer;
        this.cosmicModel = cosmicModel;
    }

    private boolean inHmfPanel(final GeneAnnotation g) {
        return slicer.hmfRegions().stream().anyMatch(r -> r.gene().equals(g.getGeneName()));
    }

    private boolean inCosmic(final GeneAnnotation g) {
        return cosmicModel.getEntrezMap().containsKey(g.getEntrezId());
    }

    private boolean intronicDisruption(final VariantAnnotation sv) {
        for (final GeneAnnotation g : sv.getStart().getGeneAnnotations()) {
            if (sv.getEnd()
                    .getGeneAnnotations()
                    .stream()
                    .filter(o -> o.getCanonical().isIntronic() && g.getCanonical().isIntronic()
                            && o.getCanonical().getExonUpstream() == g.getCanonical().getExonUpstream())
                    .count() > 0) {
                return true;
            }
        }
        return false;
    }

    private String exonDescription(final Transcript t) {
        if (t.isPromoter()) {
            return "Promoter Region";
        } else if (t.isExonic()) {
            return String.format("Exon %d / %d", t.getExonUpstream(), t.getExonMax());
        } else {
            return String.format("Intron %d / %d", t.getExonUpstream(), t.getExonMax() - 1);
        }
    }

    private String exonSelection(final Transcript t, final boolean upstream) {
        return upstream
                ? String.format("Upstream Exon %d / %d", t.getExonUpstream(), t.getExonMax())
                : String.format("Downstream Exon %d / %d", t.getExonDownstream(), t.getExonMax());
    }

    private List<StructuralVariantAnalysis.GeneFusion> processFusions(final List<VariantAnnotation> annotations) {

        final List<Pair<Transcript, Transcript>> fusions = Lists.newArrayList();

        for (final VariantAnnotation sv : annotations) {

            final List<Pair<Transcript, Transcript>> svFusions = Lists.newArrayList();

            for (final GeneAnnotation g : sv.getStart().getGeneAnnotations()) {

                final boolean g_upstream = g.getStrand() * g.getBreakend().getOrientation() > 0;
                if (!inCosmic(g)) {
                    continue;
                }

                for (final GeneAnnotation o : sv.getEnd().getGeneAnnotations()) {

                    if (!inCosmic(o)) {
                        continue;
                    }
                    if (g.getGeneName().equals(o.getGeneName())) {
                        continue;
                    }

                    final boolean o_upstream = o.getStrand() * o.getBreakend().getOrientation() > 0;

                    // can't both be upstream
                    if (g_upstream == o_upstream) {
                        continue;
                    }

                    for (final Transcript t1 : g.getTranscripts()) {
                        if (!t1.isIntronic()) {
                            continue;
                        }

                        for (final Transcript t2 : o.getTranscripts()) {
                            if (!t2.isIntronic()) {
                                continue;
                            }

                            if (g_upstream
                                    ? t1.getExonUpstreamPhase() == t2.getExonDownstreamPhase()
                                    : t1.getExonDownstreamPhase() == t2.getExonUpstreamPhase()) {
                                svFusions.add(Pair.of(t1, t2));
                            }

                        }
                    }
                }
            }

            Optional<Pair<Transcript, Transcript>> fusion =
                    svFusions.stream().filter(p -> p.getLeft().isCanonical() && p.getRight().isCanonical()).findFirst();

            if (fusion.isPresent()) {
                fusions.add(fusion.get());
                continue;
            }

            fusion = svFusions.stream()
                    .filter(p -> p.getLeft().isCanonical() || p.getRight().isCanonical())
                    .sorted(Comparator.comparingInt(a -> a.getLeft().getExonMax() + a.getRight().getExonMax()))
                    .reduce((a, b) -> b); // get longest

            if (fusion.isPresent()) {
                fusions.add(fusion.get());
                continue;
            }

            svFusions.stream()
                    .sorted(Comparator.comparingInt(a -> a.getLeft().getExonMax() + a.getRight().getExonMax()))
                    .reduce((a, b) -> b) // get longest
                    .ifPresent(fusions::add);
        }

        // transform results to reported details

        final List<StructuralVariantAnalysis.GeneFusion> result = Lists.newArrayList();
        for (final Pair<Transcript, Transcript> fusion : fusions) {

            final boolean left_upstream = fusion.getLeft().getStrand() * fusion.getLeft().getGeneAnnotation().getBreakend().getOrientation() > 0;

            final GeneAnnotation upstream = left_upstream ? fusion.getLeft().getGeneAnnotation() : fusion.getRight().getGeneAnnotation();
            final GeneAnnotation downstream = left_upstream ? fusion.getRight().getGeneAnnotation() : fusion.getLeft().getGeneAnnotation();

            final StructuralVariantAnalysis.GeneFusion details = new StructuralVariantAnalysis.GeneFusion();
            details.Type = upstream.getBreakend().getStructuralVariant().getVariant().type().toString();

            details.GeneStart = upstream.getGeneName();
            details.Start = upstream.getBreakend().getPositionString();
            details.GeneContextStart = exonSelection(fusion.getLeft(), true);

            details.GeneEnd = downstream.getGeneName();
            details.End = downstream.getBreakend().getPositionString();
            details.GeneContextEnd = exonSelection(fusion.getRight(), false);

            result.add(details);
            annotations.remove(fusion.getLeft().getGeneAnnotation().getBreakend().getStructuralVariant());
        }

        return result;
    }

    private List<StructuralVariantAnalysis.GeneDisruption> processDisruptions(final List<VariantAnnotation> annotations) {

        final List<GeneAnnotation> geneAnnotations = Lists.newArrayList();
        for (final VariantAnnotation sv : annotations) {

            if (intronicDisruption(sv)) {
                continue;
            }

            geneAnnotations.addAll(sv.getStart().getGeneAnnotations());
            geneAnnotations.addAll(sv.getEnd().getGeneAnnotations());
        }

        final ArrayListMultimap<String, GeneAnnotation> geneMap = ArrayListMultimap.create();
        for (final GeneAnnotation g : geneAnnotations) {
            if (!inHmfPanel(g)) {
                continue;
            }
            geneMap.put(g.getGeneName(), g);
        }

        final List<StructuralVariantAnalysis.GeneDisruption> disruptions = Lists.newArrayList();
        for (final String geneName : geneMap.keySet()) {
            for (final GeneAnnotation g : geneMap.get(geneName)) {

                final StructuralVariantAnalysis.GeneDisruption disruption = new StructuralVariantAnalysis.GeneDisruption();
                disruption.GeneName = geneName;
                disruption.Location = g.getBreakend().getPositionString();
                disruption.GeneContext = exonDescription(g.getCanonical());
                disruption.Orientation = g.getBreakend().getOrientation() > 0 ? "5\"" : "3\"";
                disruption.Partner = g.getOtherBreakend().getPositionString();
                disruption.HGVS = "TODO";
                disruption.Type = g.getBreakend().getStructuralVariant().getVariant().type().toString();
                disruption.VAF = PatientReportFormat.formatNullablePercent(g.getBreakend().getAlleleFrequency());

                disruptions.add(disruption);
                annotations.remove(g.getBreakend().getStructuralVariant());
            }
        }

        return disruptions;
    }

    @NotNull
    public StructuralVariantAnalysis run(@NotNull final List<StructuralVariant> variants) {

        final List<VariantAnnotation> annotations = annotator.annotateVariants(variants);
        final List<StructuralVariantAnalysis.GeneFusion> fusions = processFusions(annotations);
        final List<StructuralVariantAnalysis.GeneDisruption> disruptions = processDisruptions(annotations);

        return new StructuralVariantAnalysis(annotations, fusions, disruptions);
    }
}
