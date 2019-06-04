package com.hartwig.hmftools.svanalysis.types;

import static java.util.stream.Collectors.toList;

import static com.hartwig.hmftools.svanalysis.types.VisCopyNumberFile.DELIMITER;
import static com.hartwig.hmftools.svanalysis.types.VisCopyNumberFile.HEADER_PREFIX;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.DecimalFormat;
import java.util.List;
import java.util.StringJoiner;

import com.google.common.collect.Lists;
import com.hartwig.hmftools.patientdb.database.hmfpatients.tables.Cluster;

import org.jetbrains.annotations.NotNull;

public class VisGeneExonFile
{
    public final String SampleId;
    public final int ClusterId;
    public final String Gene;
    public final String Transcript;
    public final String Chromosome;
    public final String AnnotationType;
    public final int ExonRank;
    public final long ExonStart;
    public final long ExonEnd;

    // SampleId,ClusterId,Gene,Transcript,Chromosome,AnnotationType,ExonRank,ExonStart,ExonEnd

    public VisGeneExonFile(final String sampleId, int clusterId, final String chromosome, final String gene, final String transcript,
            final String type, int exonRank, long exonStart, long exonEnd)
    {
        SampleId = sampleId;
        ClusterId = clusterId;
        Chromosome = chromosome;
        Gene = gene;
        Transcript = transcript;
        AnnotationType = type;
        ExonRank = exonRank;
        ExonStart = exonStart;
        ExonEnd = exonEnd;
    }

    private static final String FILE_EXTENSION = ".linx.vis_gene_exon.csv";

    @NotNull
    public static String generateFilename(@NotNull final String basePath, @NotNull final String sample)
    {
        return basePath + File.separator + sample + FILE_EXTENSION;
    }

    @NotNull
    public static List<VisGeneExonFile> read(final String filePath) throws IOException
    {
        return fromLines(Files.readAllLines(new File(filePath).toPath()));
    }

    public static void write(@NotNull final String filename, @NotNull List<VisGeneExonFile> cnDataList) throws IOException
    {
        Files.write(new File(filename).toPath(), toLines(cnDataList));
    }

    @NotNull
    static List<String> toLines(@NotNull final List<VisGeneExonFile> cnDataList)
    {
        final List<String> lines = Lists.newArrayList();
        lines.add(header());
        cnDataList.stream().map(x -> toString(x)).forEach(lines::add);
        return lines;
    }

    @NotNull
    static List<VisGeneExonFile> fromLines(@NotNull List<String> lines)
    {
        return lines.stream().filter(x -> !x.startsWith(HEADER_PREFIX)).map(VisGeneExonFile::fromString).collect(toList());
    }

    @NotNull
    private static String header()
    {
        return new StringJoiner(DELIMITER, HEADER_PREFIX,"")
                .add("SampleId")
                .add("ClusterId")
                .add("Gene")
                .add("Transcript")
                .add("Chromosome")
                .add("AnnotationType")
                .add("ExonRank")
                .add("ExonStartr")
                .add("ExonEnd")
                .toString();
    }

    @NotNull
    private static String toString(@NotNull final VisGeneExonFile geData)
    {
        return new StringJoiner(DELIMITER)
                .add(String.valueOf(geData.SampleId))
                .add(String.valueOf(geData.ClusterId))
                .add(String.valueOf(geData.Gene))
                .add(String.valueOf(geData.Transcript))
                .add(String.valueOf(geData.Chromosome))
                .add(String.valueOf(geData.AnnotationType))
                .add(String.valueOf(geData.ExonRank))
                .add(String.valueOf(geData.ExonStart))
                .add(String.valueOf(geData.ExonEnd))
                .toString();
    }

    @NotNull
    private static VisGeneExonFile fromString(@NotNull final String tiData)
    {
        String[] values = tiData.split(DELIMITER);

        int index = 0;

        return new VisGeneExonFile(
                values[index++],
                Integer.valueOf(values[index++]),
                values[index++],
                values[index++],
                values[index++],
                values[index++],
                Integer.valueOf(values[index++]),
                Long.valueOf(values[index++]),
                Long.valueOf(values[index++]));
    }

}
