package com.hartwig.hmftools.lilac

import com.google.common.util.concurrent.ThreadFactoryBuilder
import com.hartwig.hmftools.common.genome.genepanel.HmfGenePanelSupplier
import com.hartwig.hmftools.lilac.LilacApplication.Companion.logger
import com.hartwig.hmftools.lilac.amino.AminoAcidFragmentPipeline
import com.hartwig.hmftools.lilac.candidates.Candidates
import com.hartwig.hmftools.lilac.coverage.*
import com.hartwig.hmftools.lilac.coverage.HlaComplexCoverage.Companion.writeToFile
import com.hartwig.hmftools.lilac.evidence.PhasedEvidenceFactory
import com.hartwig.hmftools.lilac.evidence.PhasedEvidenceValidation
import com.hartwig.hmftools.lilac.hla.HlaAllele
import com.hartwig.hmftools.lilac.hla.HlaContextFactory
import com.hartwig.hmftools.lilac.nuc.NucleotideFragment
import com.hartwig.hmftools.lilac.nuc.NucleotideFragmentFactory
import com.hartwig.hmftools.lilac.nuc.NucleotideGeneEnrichment
import com.hartwig.hmftools.lilac.qc.*
import com.hartwig.hmftools.lilac.read.FragmentAlleles
import com.hartwig.hmftools.lilac.read.SAMRecordReader
import com.hartwig.hmftools.lilac.seq.HlaSequenceFile
import com.hartwig.hmftools.lilac.seq.HlaSequenceFile.reduceToFourDigit
import com.hartwig.hmftools.lilac.seq.HlaSequenceFile.reduceToSixDigit
import com.hartwig.hmftools.lilac.seq.HlaSequenceLoci
import com.hartwig.hmftools.lilac.seq.HlaSequenceLociFile
import org.apache.commons.cli.*
import org.apache.logging.log4j.LogManager
import java.io.IOException
import java.util.concurrent.Executors
import kotlin.system.exitProcess

fun main(args: Array<String>) {

    @Throws(ParseException::class)
    fun createCommandLine(args: Array<String>, options: Options): CommandLine {
        val parser: CommandLineParser = DefaultParser()
        return parser.parse(options, args)
    }

    val options = LilacConfig.createOptions()
    try {
        val cmd = createCommandLine(args, options)
        val config = LilacConfig.createConfig(cmd)
        LilacApplication(config).use { x -> x.run() }
    } catch (e: IOException) {
        logger.warn(e)
        exitProcess(1)
    } catch (e: ParseException) {
        logger.warn(e)
        val formatter = HelpFormatter()
        formatter.printHelp("lilac", options)
        exitProcess(1)
    }

}


class LilacApplication(private val config: LilacConfig) : AutoCloseable, Runnable {
    companion object {
        val logger = LogManager.getLogger(this::class.java)
        const val HLA_A = "HLA-A"
        const val HLA_B = "HLA-B"
        const val HLA_C = "HLA-C"

        val EXCLUDED_ALLELES = setOf(HlaAllele("A*01:81"))

        val A_EXON_BOUNDARIES = setOf(24, 114, 206, 298, 337, 348, 364)
        val B_EXON_BOUNDARIES = setOf(24, 114, 206, 298, 337, 348)
        val C_EXON_BOUNDARIES = setOf(24, 114, 206, 298, 338, 349, 365)

        val ALL_PROTEIN_EXON_BOUNDARIES = (A_EXON_BOUNDARIES + B_EXON_BOUNDARIES + C_EXON_BOUNDARIES)
        val ALL_NUCLEOTIDE_EXON_BOUNDARIES = ALL_PROTEIN_EXON_BOUNDARIES.flatMap { listOf(3 * it, 3 * it + 1, 3 * it + 2) }
    }

    private val startTime = System.currentTimeMillis()
    private val transcripts = HmfGenePanelSupplier.allGenesMap37()

    private val sample = config.sample
    private val minBaseQual = config.minBaseQual
    private val minEvidence = config.minEvidence
    private val minFragmentsPerAllele = config.minFragmentsPerAllele
    private val minFragmentsToRemoveSingle = config.minFragmentsToRemoveSingle
    private val minConfirmedUniqueCoverage = config.minConfirmedUniqueCoverage
    private val resourcesDir = config.resourceDir
    private val outputDir = config.outputDir

    private val namedThreadFactory = ThreadFactoryBuilder().setNameFormat("LILAC-%d").build()
    private val executorService = Executors.newFixedThreadPool(config.threads, namedThreadFactory)
    private val nucleotideGeneEnrichment = NucleotideGeneEnrichment(A_EXON_BOUNDARIES, B_EXON_BOUNDARIES, C_EXON_BOUNDARIES)

    override fun run() {
        logger.info("Starting LILAC with parameters:")
        logger.info("     minFragmentsPerAllele = $minFragmentsPerAllele")
        logger.info("minFragmentsToRemoveSingle = $minFragmentsToRemoveSingle")
        logger.info("               minBaseQual = $minBaseQual")
        logger.info("               minEvidence = $minEvidence")
        logger.info("         minUniqueCoverage = $minConfirmedUniqueCoverage")

        // Context
        val hlaContextFactory = HlaContextFactory(A_EXON_BOUNDARIES, B_EXON_BOUNDARIES, C_EXON_BOUNDARIES)
        val hlaAContext = hlaContextFactory.hlaA()
        val hlaBContext = hlaContextFactory.hlaB()
        val hlaCContext = hlaContextFactory.hlaC()

        logger.info("Reading nucleotide files")
        val aNucleotideSequences = nucleotideLoci("${resourcesDir}/A_nuc.txt")
        val bNucleotideSequences = nucleotideLoci("${resourcesDir}/B_nuc.txt")
        val cNucleotideSequences = nucleotideLoci("${resourcesDir}/C_nuc.txt")
        val nucleotideSequences = aNucleotideSequences + bNucleotideSequences + cNucleotideSequences

        logger.info("Reading protein files")
        val aAminoAcidSequences = aminoAcidLoci("${resourcesDir}/A_prot.txt")
        val bAminoAcidSequences = aminoAcidLoci("${resourcesDir}/B_prot.txt")
        val cAminoAcidSequences = aminoAcidLoci("${resourcesDir}/C_prot.txt")
        val aminoAcidSequences = aAminoAcidSequences + bAminoAcidSequences + cAminoAcidSequences
        val aminoAcidSequencesWithInserts = aminoAcidSequences.filter { it.containsInserts() }
        val aminoAcidSequencesWithDeletes = aminoAcidSequences.filter { it.containsDeletes() }

        logger.info("Querying records from reference bam ${config.referenceBam}")
        val nucleotideFragmentFactory = NucleotideFragmentFactory(config.minBaseQual, aminoAcidSequencesWithInserts, aminoAcidSequencesWithDeletes)
        val transcripts = listOf(transcripts[HLA_A]!!, transcripts[HLA_B]!!, transcripts[HLA_C]!!)
        val bamReader = SAMRecordReader(1000, config.refGenome, transcripts, nucleotideFragmentFactory)
        val referenceNucleotideFragments = bamReader.readFromBam(config.referenceBam).enrichGenes()

        val tumorNucleotideFragments = if (config.tumorBam.isNotEmpty()) {
            logger.info("Querying records from tumor bam ${config.tumorBam}")
            bamReader.readFromBam(config.tumorBam).enrichGenes()
        } else {
            listOf()
        }

//        val tumorNucleotideFragments = listOf<NucleotideFragment>()

        logger.info("Enriching reference bam records")
        val aminoAcidPipeline = AminoAcidFragmentPipeline(config, referenceNucleotideFragments, tumorNucleotideFragments)
        val aFragments = aminoAcidPipeline.phasingFragments(hlaAContext)
        val bFragments = aminoAcidPipeline.phasingFragments(hlaBContext)
        val cFragments = aminoAcidPipeline.phasingFragments(hlaCContext)

        // Phasing
        logger.info("Phasing bam records")
        val phasedEvidenceFactory = PhasedEvidenceFactory(minFragmentsPerAllele, config.minFragmentsToRemoveSingle, config.minEvidence)
        val aPhasedEvidence = phasedEvidenceFactory.evidence(hlaAContext, aFragments)
        val bPhasedEvidence = phasedEvidenceFactory.evidence(hlaBContext, bFragments)
        val cPhasedEvidence = phasedEvidenceFactory.evidence(hlaCContext, cFragments)

        // Validate phasing against expected sequences
        val expectedSequences = aminoAcidSequences.filter { it.allele in config.expectedAlleles }
        PhasedEvidenceValidation.validateExpected("A", aPhasedEvidence, expectedSequences)
        PhasedEvidenceValidation.validateExpected("B", bPhasedEvidence, expectedSequences)
        PhasedEvidenceValidation.validateExpected("C", cPhasedEvidence, expectedSequences)

        // Candidates
        val candidateFactory = Candidates(config, nucleotideSequences, aminoAcidSequences)
        val aCandidates = candidateFactory.candidates(hlaAContext, aFragments, aPhasedEvidence)
        val bCandidates = candidateFactory.candidates(hlaBContext, bFragments, bPhasedEvidence)
        val cCandidates = candidateFactory.candidates(hlaCContext, cFragments, cPhasedEvidence)
        val candidates = aCandidates + bCandidates + cCandidates

        // Coverage
        val highQualityAminoAcidFragments = aminoAcidPipeline.referenceCoverageFragments()
        val aminoAcidCounts = SequenceCount.aminoAcids(minEvidence, highQualityAminoAcidFragments)
        val aminoAcidHeterozygousLoci = aminoAcidCounts.heterozygousLoci()
        val nucleotideCounts = SequenceCount.nucleotides(minEvidence, highQualityAminoAcidFragments)
        val nucleotideHeterozygousLoci = nucleotideCounts.heterozygousLoci() intersect ALL_NUCLEOTIDE_EXON_BOUNDARIES

        val candidateAlleles = candidates.map { it.allele }
        val candidateAlleleSpecificProteins = candidateAlleles.map { it.asFourDigit() }

        val aminoAcidCandidates = aminoAcidSequences.filter { it.allele in candidateAlleles }
        val nucleotideCandidates = nucleotideSequences.filter { it.allele.asFourDigit() in candidateAlleleSpecificProteins }

        val referenceFragmentAlleles = FragmentAlleles.create(highQualityAminoAcidFragments,
                aminoAcidHeterozygousLoci, aminoAcidCandidates, nucleotideHeterozygousLoci, nucleotideCandidates)
        val referenceCoverageFactory = HlaComplexCoverageFactory(executorService, referenceFragmentAlleles)

        val tumorFragmentAlleles = FragmentAlleles.create(aminoAcidPipeline.tumorCoverageFragments(),
                aminoAcidHeterozygousLoci, aminoAcidCandidates, nucleotideHeterozygousLoci, nucleotideCandidates)
        val tumorCoverageFactory = HlaComplexCoverageFactory(executorService, tumorFragmentAlleles)


        logger.info("Identifying uniquely identifiable groups and proteins [total,unique,shared,wide]")
        val groupCoverage = referenceCoverageFactory.groupCoverage(candidateAlleles)
        val confirmedGroups = groupCoverage.confirmUnique()
        val discardedGroups = groupCoverage.alleleCoverage.filter { it.uniqueCoverage > 0 && it !in confirmedGroups }.sortedDescending()
        logger.info("... confirmed ${confirmedGroups.size} unique groups: " + confirmedGroups.joinToString(", "))
        if (discardedGroups.isNotEmpty()) {
            logger.info("... found ${discardedGroups.size} insufficiently unique groups: " + discardedGroups.joinToString(", "))
        }

        val candidatesAfterConfirmedGroups = candidateAlleles.filterWithConfirmedGroups(confirmedGroups.map { it.allele })
        val proteinCoverage = referenceCoverageFactory.proteinCoverage(candidatesAfterConfirmedGroups)
        val confirmedProtein = proteinCoverage.confirmUnique()
        val discardedProtein = proteinCoverage.alleleCoverage.filter { it.uniqueCoverage > 0 && it !in confirmedGroups }.sortedDescending()
        logger.info("... confirmed ${confirmedProtein.size} unique proteins: " + confirmedProtein.joinToString(", "))
        if (discardedProtein.isNotEmpty()) {
            logger.info("... found ${discardedProtein.size} insufficiently unique proteins: " + discardedProtein.joinToString(", "))
        }

        val complexes = HlaComplex.complexes(confirmedGroups.alleles(), confirmedProtein.alleles(), candidates.map { it.allele })
        logger.info("Calculating coverage of ${complexes.size} complexes")

        val referenceComplexCoverage = referenceCoverageFactory.complexCoverage(complexes)
        val tumorComplexCoverage = tumorCoverageFactory.complexCoverage(complexes)

        if (expectedSequences.isNotEmpty()) {
            val expectedCoverage = referenceCoverageFactory.proteinCoverage(expectedSequences.map { it.allele })
            logger.info("Expected coverage: $expectedCoverage")
        }

        if (referenceComplexCoverage.isNotEmpty()) {
            val tumorRankedComplexes = HlaComplexCoverageRanking().candidateRanking(tumorComplexCoverage)
            val referenceRankedComplexes = HlaComplexCoverageRanking().candidateRanking(referenceComplexCoverage)
            val winningComplex = referenceRankedComplexes[0]
            val winningAlleles = winningComplex.alleleCoverage.alleles()
            val winningSequences = candidates.filter { candidate -> candidate.allele in winningAlleles }


            tumorRankedComplexes.writeToFile("$outputDir/$sample.tumor.coverage.txt")
            referenceRankedComplexes.writeToFile("$outputDir/$sample.reference.coverage.txt")

            logger.info(HlaComplexCoverage.header())
            for (winningCandidate in referenceRankedComplexes) {
                logger.info(winningCandidate)
            }

            logger.info("${config.sample} - REF - ${referenceRankedComplexes.size} CANDIDATES, WINNING ALLELES: ${winningComplex.alleleCoverage.map { it.allele }}")

            if (tumorRankedComplexes.isNotEmpty()) {
                logger.info("TUMOR WINNERS:")
                for (winningCandidate in tumorRankedComplexes) {
                    logger.info(winningCandidate)
                }
                logger.info("${config.sample} - TUMOR - ${tumorRankedComplexes.size} CANDIDATES, WINNING ALLELES: ${tumorRankedComplexes[0].alleleCoverage.map { it.allele }}")
            }

            val aminoAcidQC = AminoAcidQC.create(winningSequences, aminoAcidCounts)
            val haplotypeQC = HaplotypeQC.create(3, winningSequences, aPhasedEvidence + bPhasedEvidence + cPhasedEvidence, aminoAcidCounts)
            val bamQC = BamQC.create(bamReader)
            val coverageQC = CoverageQC.create(referenceFragmentAlleles.size, winningComplex)
            val lilacQC = LilacQC(aminoAcidQC, bamQC, coverageQC, haplotypeQC)

            logger.info("QC Stats:")
            logger.info("   ${lilacQC.header().joinToString(",")}")
            logger.info("   ${lilacQC.body().joinToString(",")}")

            lilacQC.writefile("$outputDir/$sample.qc.txt")
        }

        logger.info("Writing output to $outputDir")
        val deflatedSequenceTemplate = aminoAcidSequences.first { it.allele == HlaAllele("A*01:01:01:01") }
        val candidateToWrite = (candidates + expectedSequences + deflatedSequenceTemplate).distinct().sortedBy { it.allele }
        HlaSequenceLociFile.write("$outputDir/$sample.candidates.deflate.txt", A_EXON_BOUNDARIES, B_EXON_BOUNDARIES, C_EXON_BOUNDARIES, candidateToWrite)
        aminoAcidCounts.writeVertically("$outputDir/$sample.aminoacids.count.txt")
        nucleotideCounts.writeVertically("$outputDir/$sample.nucleotides.count.txt")

    }


    private fun nucleotideLoci(inputFilename: String): List<HlaSequenceLoci> {
        val sequences = HlaSequenceFile.readFile(inputFilename)
                .filter { it.allele !in EXCLUDED_ALLELES }
//                .filter { it.allele.gene in setOf("A", "B") ||  it.allele in listOf(HlaAllele("C*01:02:01:01"), HlaAllele("C*17:01:01:02")) }
                .reduceToSixDigit()
        return HlaSequenceLoci.create(sequences)
                .filter { it.sequences.isNotEmpty() }
    }

    private fun aminoAcidLoci(inputFilename: String): List<HlaSequenceLoci> {
        val sequences = HlaSequenceFile.readFile(inputFilename)
                .filter { it.allele !in EXCLUDED_ALLELES }
                .reduceToFourDigit()
                .map { if (it.rawSequence.endsWith("X")) it else it.copyWithAdditionalSequence("X") }

        return HlaSequenceLoci.create(sequences)
                .filter { it.sequences.isNotEmpty() }
    }


    override fun close() {
        executorService.shutdown()
        logger.info("Finished in ${(System.currentTimeMillis() - startTime) / 1000} seconds")
    }

    private fun List<HlaAllele>.filterWithConfirmedGroups(confirmedGroups: List<HlaAllele>): List<HlaAllele> {
        val a = confirmedGroups.filter { it.gene == "A" }
        val b = confirmedGroups.filter { it.gene == "B" }
        val c = confirmedGroups.filter { it.gene == "C" }
        val map = mapOf(Pair("A", a), Pair("B", b), Pair("C", c))

        return this.filter { map[it.gene]!!.size < 2 || map[it.gene]!!.contains(it.asAlleleGroup()) }
    }

    private fun HlaComplexCoverage.confirmUnique(): List<HlaAlleleCoverage> {
        val unique = this.alleleCoverage.filter { it.uniqueCoverage >= config.minConfirmedUniqueCoverage }.sortedDescending()
        val a = unique.filter { it.allele.gene == "A" }.take(2)
        val b = unique.filter { it.allele.gene == "B" }.take(2)
        val c = unique.filter { it.allele.gene == "C" }.take(2)

        return (a + b + c).sortedDescending()
    }


    private fun List<HlaAlleleCoverage>.alleles(): List<HlaAllele> {
        return this.map { it.allele }
    }

    private fun List<NucleotideFragment>.enrichGenes(): List<NucleotideFragment> {
        return nucleotideGeneEnrichment.enrich(this)
    }

}