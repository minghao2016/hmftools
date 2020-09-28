package com.hartwig.hmftools.serve.vicc.hotspot;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Maps;
import com.hartwig.hmftools.common.variant.hotspot.VariantHotspot;
import com.hartwig.hmftools.serve.hotspot.ProteinResolver;
import com.hartwig.hmftools.vicc.datamodel.Feature;
import com.hartwig.hmftools.vicc.datamodel.ViccEntry;
import com.hartwig.hmftools.vicc.util.EventAnnotation;

import org.jetbrains.annotations.NotNull;

public class HotspotExtractor {

    @NotNull
    private final ProteinResolver proteinResolver;

    public HotspotExtractor(@NotNull final ProteinResolver proteinResolver) {
        this.proteinResolver = proteinResolver;
    }

    @NotNull
    public Map<Feature, List<VariantHotspot>> extractHotspots(@NotNull ViccEntry viccEntry) {
        Map<Feature, List<VariantHotspot>> allHotspotsPerFeature = Maps.newHashMap();
        for (Feature feature : viccEntry.features()) {
            if (feature.eventAnnotation().equals(EventAnnotation.HOTSPOT)) {
                allHotspotsPerFeature.put(feature,
                        proteinResolver.extractHotspotsFromProteinAnnotation(feature.geneSymbol(),
                                viccEntry.transcriptId(),
                                feature.proteinAnnotation()));
            }
        }

        return allHotspotsPerFeature;
    }

}
