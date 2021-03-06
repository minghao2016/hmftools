package com.hartwig.hmftools.lilac.qc

import com.hartwig.hmftools.lilac.coverage.HlaComplexCoverage
import java.text.DecimalFormat

data class CoverageQC(val aTypes: Int, val bTypes: Int, val cTypes: Int, val totalFragments: Int, val uniqueFragments: Int, val sharedFragments: Int, val wildcardFragments: Int) {

    val fittedFragments = uniqueFragments + sharedFragments + wildcardFragments
    val unusedFragments = totalFragments - fittedFragments
    val percentUnique = 1.0 * uniqueFragments / fittedFragments
    val percentShared = 1.0 * sharedFragments / fittedFragments
    val percentWildcard = 1.0 * wildcardFragments / fittedFragments

    companion object {
        fun create(totalFragments: Int, winner: HlaComplexCoverage): CoverageQC {
            val alleles = winner.alleleCoverage.map { it.allele }
            val aTypes = alleles.filter { it.gene == "A" }.distinct().size
            val bTypes = alleles.filter { it.gene == "B" }.distinct().size
            val cTypes = alleles.filter { it.gene == "C" }.distinct().size

            return CoverageQC(aTypes, bTypes, cTypes, totalFragments, winner.uniqueCoverage, winner.sharedCoverage, winner.wildCoverage)
        }

        private val percentFormatter = DecimalFormat("00.0%")
        private fun Double.percent(): String {
            return percentFormatter.format(this)
        }
    }

    fun header(): List<String> {
        return listOf("aTypes", "bTypes", "cTypes", "unusedFragments", "fittedFragments", "percentUnique", "percentShared", "percentWildcard")
    }

    fun body(): List<String> {
        return listOf(aTypes.toString(), bTypes.toString(), cTypes.toString(), unusedFragments.toString(), fittedFragments.toString(), percentUnique.percent(), percentShared.percent(), percentWildcard.percent())
    }

    override fun toString(): String {


        return "CoverageQC(aTypes=$aTypes, bTypes=$bTypes, cTypes=$cTypes, unusedFragments=$unusedFragments, uniqueFragments=$uniqueFragments, sharedFragments=$sharedFragments, wildcardFragments=$wildcardFragments, fittedFragments=$fittedFragments, percentUnique=${percentUnique.percent()}, percentShared=${percentShared.percent()}, percentWildcard=${percentWildcard.percent()})"
    }


}
