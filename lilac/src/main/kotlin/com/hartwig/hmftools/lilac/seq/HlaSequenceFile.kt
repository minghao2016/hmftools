package com.hartwig.hmftools.lilac.seq

import com.hartwig.hmftools.lilac.hla.HlaAllele
import org.apache.logging.log4j.LogManager
import java.io.File

object HlaSequenceFile {
    val logger = LogManager.getLogger(this::class.java)

    fun readFile(filename: String): List<HlaSequence> {
        val entries = LinkedHashMap<String, HlaSequence>()
        for (line in File(filename).readLines()) {
            val trimmed = line.trim()

            if (trimmed.startsWith("*", 1)) {
                val split = trimmed.split(" ")
                val allele = split[0].trim()
                val alleleIndex = line.indexOf(allele)
                val remainder = line.substring(alleleIndex + allele.length).trim().replace(" ", "")
                if (entries.containsKey(allele)) {
                    entries[allele] = entries[allele]!!.copyWithAdditionalSequence(remainder)
                } else {
                    entries[allele] = HlaSequence(allele, remainder)
                }
            }
        }

        return entries.values.toList()
    }

    fun List<HlaSequence>.inflate(): List<HlaSequence> {
        val template = this[0].rawSequence
        return this.map { it.inflate(template) }
    }

    fun List<HlaSequence>.deflate(template: HlaSequence): List<HlaSequence> {
        if (this.isEmpty()) {
            return this
        }
        return listOf(template) + this.filter { it.allele != template.allele }.map { it.deflate(template.sequence) }
    }

    fun List<HlaSequence>.reduceToFourDigit(): List<HlaSequence> {
        return reduce { it.asFourDigit() }
    }

    fun List<HlaSequence>.reduceToSixDigit(): List<HlaSequence> {
        return reduce { it.asSixDigit() }
    }

    private fun List<HlaSequence>.reduce(transform: (HlaAllele) -> HlaAllele): List<HlaSequence> {
        val resultMap = LinkedHashMap<HlaAllele, HlaSequence>()

        for (sequence in this) {
            val fourDigitName = transform(sequence.allele)
            if (!resultMap.containsKey(fourDigitName)) {
                resultMap[fourDigitName] = sequence
            }
        }

        return resultMap.values.toList()
    }

    fun writeDeflatedFile(deflatedFileName: String, boundaries: List<Set<Int>>, template: HlaSequence, sequences: List<HlaSequence>) {
        val outputFile = File(deflatedFileName)
        outputFile.writeText("")

//        val indexes = template.sequence.indices.map { it.toString().padStart(3, '0') }
//        for (i in 0..2) {
//            outputFile.appendText("Index".padEnd(20, ' ') + "\t")
//            outputFile.appendText(indexes.map { it[i] }.joinToString (separator = ""))
//            outputFile.appendText("\n")
//        }

        for (boundary in boundaries) {
            writeBoundary(boundary, deflatedFileName)
        }

        appendFile(deflatedFileName, sequences.deflate(template))
    }

    fun wipeFile(outputFileName: String) {
        val outputFile = File(outputFileName)
        outputFile.writeText("")
    }

    fun writeBoundary(boundaries: Collection<Int>, outputFileName: String) {
        val outputFile = File(outputFileName)
        outputFile.appendText("Boundary".padEnd(20, ' ') + "\t")
        for (i in 0..boundaries.max()!!) {
            if (i in boundaries) {
                outputFile.appendText("|")
            } else {
                outputFile.appendText(" ")
            }
        }

        outputFile.appendText("\n")
    }

    fun appendFile(outputFileName: String, sequences: List<HlaSequence>) {
        if (sequences.isNotEmpty()) {
            val outputFile = File(outputFileName)
            for (sequence in sequences) {
                outputFile.appendText(sequence.toString() + "\n")
            }
        }
    }

    fun writeFile(outputFileName: String, sequences: List<HlaSequence>) {
        if (sequences.isNotEmpty()) {
            val outputFile = File(outputFileName)
            outputFile.writeText("")

            for (sequence in sequences) {
                outputFile.appendText(sequence.toString() + "\n")
            }
        }
    }

}