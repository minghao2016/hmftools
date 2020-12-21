package com.hartwig.hmftools.serve.extraction.codon;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.StringJoiner;

import com.google.common.collect.Lists;
import com.hartwig.hmftools.common.serve.Knowledgebase;
import com.hartwig.hmftools.serve.util.RefGenomeVersion;

import org.jetbrains.annotations.NotNull;

public final class KnownCodonFile {

    private static final String DELIMITER = "\t";
    private static final String KNOWN_CODON_TSV = "KnownCodons.SERVE.tsv";

    private KnownCodonFile() {
    }

    @NotNull
    public static String knownCodonTsvPath(@NotNull String outputDir, @NotNull RefGenomeVersion refGenomeVersion) {
        return refGenomeVersion.addVersionToFilePath(outputDir + File.separator + KNOWN_CODON_TSV);
    }

    public static void write(@NotNull String codonTsv, @NotNull Iterable<KnownCodon> codons) throws IOException {
        List<String> lines = Lists.newArrayList();
        lines.add(header());
        lines.addAll(toLines(codons));
        Files.write(new File(codonTsv).toPath(), lines);
    }

    @NotNull
    private static String header() {
        return new StringJoiner(DELIMITER).add("gene")
                .add("chromosome")
                .add("start")
                .add("end")
                .add("mutationType")
                .add("codonIndex")
                .add("sources")
                .toString();
    }

    @NotNull
    private static List<String> toLines(@NotNull Iterable<KnownCodon> codons) {
        List<String> lines = Lists.newArrayList();
        for (KnownCodon codon : sort(codons)) {
            lines.add(toLine(codon));
        }
        return lines;
    }

    @NotNull
    private static List<KnownCodon> sort(@NotNull Iterable<KnownCodon> codons) {
        // Need to make a copy since the input may be immutable and cannot be sorted!
        List<KnownCodon> sorted = Lists.newArrayList(codons);
        sorted.sort(new KnownCodonComparator());

        return sorted;
    }

    @NotNull
    private static String toLine(@NotNull KnownCodon codon) {
        return new StringJoiner(DELIMITER).add(codon.annotation().gene())
                .add(codon.annotation().chromosome())
                .add(String.valueOf(codon.annotation().start()))
                .add(String.valueOf(codon.annotation().end()))
                .add(codon.annotation().mutationType().toString())
                .add(String.valueOf(codon.annotation().codonIndex()))
                .add(Knowledgebase.commaSeparatedSourceString(codon.sources()))
                .toString();
    }
}