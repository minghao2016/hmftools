package com.hartwig.hmftools.lilac.read

import com.hartwig.hmftools.common.genome.bed.NamedBed
import com.hartwig.hmftools.common.genome.region.*
import com.hartwig.hmftools.lilac.ext.containsIndel
import com.hartwig.hmftools.lilac.sam.SamSlicer
import htsjdk.samtools.SAMRecord
import htsjdk.samtools.SamReaderFactory
import org.apache.logging.log4j.LogManager
import java.io.File
import java.util.function.Consumer
import kotlin.math.max
import kotlin.math.min

class SAMRecordReader(maxDistance: Int, private val transcripts: List<HmfTranscriptRegion>) {
    private val codingRegions = transcripts.map { GenomeRegions.create(it.chromosome(), it.codingStart() - maxDistance, it.codingEnd() + maxDistance) }

    companion object {
        val logger = LogManager.getLogger(this::class.java)
    }

    fun readFromBam(bamFile: String): List<SAMRecordRead> {
        return transcripts.flatMap { readFromBam(it, bamFile) }
    }

    private fun readFromBam(transcript: HmfTranscriptRegion, bamFile: String): List<SAMRecordRead> {
        logger.info("... querying ${transcript.gene()} (${transcript.chromosome()}:${transcript.codingStart()}-${transcript.codingEnd()})")

        val reverseStrand = transcript.strand() == Strand.REVERSE
        val codingRegions = if (reverseStrand) codingRegions(transcript).reversed() else codingRegions(transcript)

        val realignedRegions = mutableListOf<SAMRecordRead>()
        var length = 0
        for (codingRegion in codingRegions) {
            realignedRegions.addAll(realign(transcript.gene(), length, codingRegion, reverseStrand, bamFile))
            length += codingRegion.bases().toInt()
        }
        return realignedRegions
    }

    private fun codingRegions(transcript: HmfTranscriptRegion): List<NamedBed> {
        return CodingRegions.codingRegions(transcript)
    }

    private fun realign(gene: String, hlaCodingRegionOffset: Int, region: GenomeRegion, reverseStrand: Boolean, bamFileName: String): List<SAMRecordRead> {
        val slicer = SamSlicer(1)
        val result = mutableListOf<SAMRecordRead>()
        SamReaderFactory.makeDefault().open(File(bamFileName)).use { samReader ->

            val consumer = Consumer<SAMRecord> { samRecord ->

                if (samRecord.inRangeOfCodingTranscripts() &&  !samRecord.containsIndel()) {
                    if (reverseStrand) {
                        result.add(realignReverseStrand(gene, hlaCodingRegionOffset, region, samRecord))
                    } else {
                        result.add(realignForwardStrand(gene, hlaCodingRegionOffset, region, samRecord))
                    }
                }
            }


            slicer.slice(region, samReader, consumer)
        }
        return result
    }


    private fun realignForwardStrand(gene: String, hlaExonOffset: Int, region: GenomeRegion, samRecord: SAMRecord): SAMRecordRead {
        val hlaExonStartPosition = region.start().toInt()
        val hlaExonEndPosition = region.end().toInt()

        val alignmentStart = samRecord.alignmentStart
        val alignmentEnd = samRecord.alignmentEnd

        val hlaStart = max(alignmentStart, hlaExonStartPosition)
        val hlaEnd = min(alignmentEnd, hlaExonEndPosition)
        val length = hlaEnd - hlaStart + 1

        val readIndex = samRecord.getReadPositionAtReferencePosition(hlaStart) - 1
        val hlaStartIndex = hlaStart - hlaExonStartPosition + hlaExonOffset

        return SAMRecordRead(gene, hlaStartIndex, readIndex, length, false, samRecord)
    }

    private fun realignReverseStrand(gene: String, hlaExonOffset: Int, region: GenomeRegion, samRecord: SAMRecord): SAMRecordRead {
        val hlaExonStartPosition = region.end().toInt()
        val hlaExonEndPosition = region.start().toInt()

        val alignmentStart = samRecord.alignmentStart
        val alignmentEnd = samRecord.alignmentEnd

        val hlaStart = min(alignmentEnd, hlaExonStartPosition)
        val hlaEnd = max(alignmentStart, hlaExonEndPosition)
        val length = hlaStart - hlaEnd + 1

        val readIndex = samRecord.getReadPositionAtReferencePosition(hlaStart) - 1
        val hlaStartIndex = hlaExonStartPosition - hlaStart + hlaExonOffset

        return SAMRecordRead(gene, hlaStartIndex, readIndex, length, true, samRecord)
    }

    private fun SAMRecord.inRangeOfCodingTranscripts(): Boolean {
        val thisInRange = codingRegions.any {it.chromosome() == this.contig && this.alignmentStart >= it.start() && this.alignmentStart <= it.end()}
        val mateInRange = codingRegions.any {it.chromosome() == this.contig && this.mateAlignmentStart >= it.start() && this.mateAlignmentStart <= it.end()}

        return thisInRange && mateInRange
    }

}