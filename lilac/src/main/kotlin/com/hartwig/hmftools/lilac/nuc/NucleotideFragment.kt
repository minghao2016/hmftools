package com.hartwig.hmftools.lilac.nuc

import com.hartwig.hmftools.common.codon.Codons
import com.hartwig.hmftools.lilac.read.Fragment
import com.hartwig.hmftools.lilac.read.SAMRecordRead

open class NucleotideFragment(val id: String, private val nucleotideLoci: List<Int>, private val nucleotides: List<Char>, val genes: Set<String>) {

    companion object {

        fun fromReads(minBaseQual: Int, reads: List<SAMRecordRead>): List<NucleotideFragment> {
            return reads.groupBy { it.samRecord.readName }.map { fromReadPairs(minBaseQual, it.value) }
        }

        private fun fromReadPairs(minBaseQual: Int, reads: List<SAMRecordRead>): NucleotideFragment {
            fun nucleotide(index: Int): Char {
                for (read in reads) {
                    if (read.nucleotideIndices().contains(index)) {
                        return read.nucleotide(index)
                    }
                }
                throw IllegalArgumentException("Fragment does not contain nucleotide at location $index")
            }

            val nucleotideIndices = reads
                    .flatMap { it.nucleotideIndices(minBaseQual).toList() }
                    .distinct()
                    .sorted()

            val nucleotides = nucleotideIndices.map { nucleotide(it) }
            val id = reads[0].samRecord.readName + " -> " + reads[0].samRecord.alignmentStart
            val genes = mutableSetOf<String>()
            genes.add(reads[0].gene)
            if (reads.size > 1) {
                genes.add(reads[1].gene)
            }

            return NucleotideFragment(id, nucleotideIndices, nucleotides, genes)
        }
    }

    fun isEmpty(): Boolean {
        return nucleotideLoci.isEmpty()
    }

    fun isNotEmpty(): Boolean {
        return !isEmpty()
    }

    fun containsNucleotide(index: Int): Boolean {
        return nucleotideLoci.contains(index)
    }

    fun containsAllNucleotides(vararg indices: Int): Boolean {
        return indices.all { containsNucleotide(it) }
    }

    fun nucleotides(vararg indices: Int): String {
        return indices.map { nucleotide(it) }.joinToString("")
    }

    fun nucleotide(loci: Int): Char {
        return nucleotides[nucleotideLoci.indexOf(loci)]
    }

    fun nucleotides(): List<Char> = nucleotides

    fun nucleotideIndices(): List<Int> = nucleotideLoci

    fun toAminoAcidFragment(): Fragment {
        fun aminoAcid(index: Int): Char {
            val first = nucleotide(index * 3)
            val second = nucleotide(index * 3 + 1)
            val third = nucleotide(index * 3 + 2)
            return Codons.aminoAcid(first.toString() + second + third)
        }

        val aminoAcidIndices = nucleotideLoci
                .filter { it % 3 == 0 }
                .filter { nucleotideLoci.contains(it + 1) && nucleotideLoci.contains(it + 2) }
                .map { it / 3 }

        val aminoAcids = aminoAcidIndices.map { aminoAcid(it) }

        return Fragment(id, nucleotideLoci, nucleotides, genes, aminoAcidIndices, aminoAcids)
    }

    fun enrich(index: Int, nucleotide: Char): NucleotideFragment {
        assert(!containsNucleotide(index))
        return NucleotideFragment(id, nucleotideLoci + index, nucleotides + nucleotide, genes)
    }

}