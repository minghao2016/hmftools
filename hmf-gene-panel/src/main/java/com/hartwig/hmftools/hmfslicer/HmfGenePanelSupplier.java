package com.hartwig.hmftools.hmfslicer;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.collect.SortedSetMultimap;
import com.google.common.io.Resources;
import com.hartwig.hmftools.common.region.hmfslicer.HmfGenomeFileLoader;
import com.hartwig.hmftools.common.region.hmfslicer.HmfGenomeRegion;

import org.jetbrains.annotations.NotNull;

public enum HmfGenePanelSupplier {
    ;

    @NotNull
    public static Set<String> geneSet() throws IOException {
        return Sets.newHashSet(Resources.readLines(Resources.getResource("gene_panel"), Charset.defaultCharset()));
    }

    @NotNull
    public static SortedSetMultimap<String, HmfGenomeRegion> allGeneMap() {
        final InputStream inputStream = HmfGenePanelSupplier.class.getResourceAsStream("/all_genes.tsv");
        return HmfGenomeFileLoader.fromInputStream(inputStream);
    }

    @NotNull
    public static List<HmfGenomeRegion> allGeneList() {
        return toList(allGeneMap());
    }

    @NotNull
    public static SortedSetMultimap<String, HmfGenomeRegion> hmfGeneMap() throws IOException {
        final Set<String> panel = geneSet();
        final SortedSetMultimap<String, HmfGenomeRegion> genes = allGeneMap();
        genes.values().removeIf(v -> !panel.contains(v.gene()));
        return genes;
    }

    @NotNull
    public static List<HmfGenomeRegion> hmfGeneList() throws IOException {
        return toList(hmfGeneMap());
    }

    @NotNull
    private static List<HmfGenomeRegion> toList(@NotNull final SortedSetMultimap<String, HmfGenomeRegion> map) {
        final List<HmfGenomeRegion> result = Lists.newArrayList(map.values());
        Collections.sort(result);
        return result;
    }
}
