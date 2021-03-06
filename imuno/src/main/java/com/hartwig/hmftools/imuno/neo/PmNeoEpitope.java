package com.hartwig.hmftools.imuno.neo;

import static java.lang.Math.abs;
import static java.lang.Math.max;

import static com.hartwig.hmftools.common.fusion.CodingBaseData.PHASE_0;
import static com.hartwig.hmftools.common.fusion.CodingBaseData.PHASE_1;
import static com.hartwig.hmftools.common.fusion.CodingBaseData.PHASE_2;
import static com.hartwig.hmftools.common.fusion.FusionCommon.FS_DOWN;
import static com.hartwig.hmftools.common.fusion.FusionCommon.FS_UP;
import static com.hartwig.hmftools.common.fusion.FusionCommon.POS_STRAND;
import static com.hartwig.hmftools.common.fusion.FusionCommon.switchStream;
import static com.hartwig.hmftools.common.fusion.TranscriptUtils.calcCodingBases;
import static com.hartwig.hmftools.common.fusion.TranscriptUtils.tickPhaseForward;
import static com.hartwig.hmftools.common.neo.NeoEpitopeFile.pointMutationInfo;
import static com.hartwig.hmftools.common.neo.NeoEpitopeType.FRAMESHIFT;
import static com.hartwig.hmftools.common.neo.NeoEpitopeType.INFRAME_DELETION;
import static com.hartwig.hmftools.common.neo.NeoEpitopeType.INFRAME_INSERTION;
import static com.hartwig.hmftools.common.utils.sv.StartEndIterator.SE_END;
import static com.hartwig.hmftools.common.utils.sv.StartEndIterator.SE_START;
import static com.hartwig.hmftools.common.utils.sv.SvCommonUtils.NEG_ORIENT;
import static com.hartwig.hmftools.common.utils.sv.SvCommonUtils.POS_ORIENT;
import static com.hartwig.hmftools.common.neo.AminoAcidConverter.reverseStrandBases;
import static com.hartwig.hmftools.imuno.neo.NeoUtils.getAminoAcids;
import static com.hartwig.hmftools.imuno.neo.NeoUtils.getDownstreamCodingBaseExcerpt;
import static com.hartwig.hmftools.imuno.neo.NeoUtils.getDownstreamCodingBases;
import static com.hartwig.hmftools.imuno.neo.NeoUtils.getUpstreamCodingBaseExcerpt;
import static com.hartwig.hmftools.imuno.neo.NeoUtils.getUpstreamCodingBases;
import static com.hartwig.hmftools.imuno.neo.NeoUtils.setTranscriptCodingData;
import static com.hartwig.hmftools.imuno.neo.NeoUtils.setTranscriptContext;

import java.util.List;

import com.hartwig.hmftools.common.ensemblcache.EnsemblDataCache;
import com.hartwig.hmftools.common.ensemblcache.TranscriptData;
import com.hartwig.hmftools.common.fusion.CodingBaseData;
import com.hartwig.hmftools.common.genome.refgenome.RefGenomeInterface;
import com.hartwig.hmftools.common.neo.NeoEpitopeType;

import org.apache.commons.compress.utils.Lists;

import htsjdk.samtools.Cigar;
import htsjdk.samtools.CigarElement;
import htsjdk.samtools.CigarOperator;

public class PmNeoEpitope extends NeoEpitope
{
    private final PointMutationData mPointMutation;
    private final int mIndelBaseDiff;
    private String mWildtypeBases;
    private String mWildtypeAcids;

    public PmNeoEpitope(final PointMutationData pointMutation)
    {
        super();
        mPointMutation = pointMutation;
        mIndelBaseDiff = mPointMutation.Alt.length() - mPointMutation.Ref.length();
        mWildtypeBases = "";
        mWildtypeAcids = "";
    }

    public byte orientation(int fs)
    {
        return (fs == FS_UP) == (TransData[fs].Strand == POS_STRAND) ? POS_ORIENT : NEG_ORIENT;
    }

    public int position(int stream)
    {
        // position for a point mutation is defined on the upstream as the base of the mutation
        // and on the downstream as a first base after the end of the mutated section
        // for negative strand genes, the position is set to the upstream end of the mutated section
        int pmPosition = mPointMutation.Position;
        int modifiedBases = mIndelBaseDiff == 0 ? mPointMutation.Alt.length() : 1;
        int deletedBases = mIndelBaseDiff < 0 ? abs(mIndelBaseDiff) : 0;

        if(posStrand())
        {
            if(stream == FS_UP)
                return pmPosition;
            else
                return pmPosition + deletedBases + modifiedBases;
        }
        else
        {
            // non-INDELs use the start of the mutated bases as the upstream position
            int negPosAdj = mIndelBaseDiff == 0 ? -1 : 0;
            if(stream == FS_UP)
                return pmPosition + deletedBases + modifiedBases + negPosAdj;
            else
                return pmPosition + negPosAdj;
        }
    }

    public String chromosome(int stream)
    {
        return mPointMutation.Chromosome;
    }

    public String geneName(int stream) { return mPointMutation.Gene; }

    public NeoEpitopeType variantType()
    {
        // missense, inframe insertion, inframe deletion, frameshift,
        if(isBaseChange())
            return NeoEpitopeType.MISSENSE;

        if((mIndelBaseDiff % 3) == 0)
        {
            if(isInsert())
                return INFRAME_INSERTION;
            else
                return INFRAME_DELETION;
        }

        return FRAMESHIFT;
    }

    public String variantInfo()
    {
        return pointMutationInfo(mPointMutation.Chromosome, mPointMutation.Position, mPointMutation.Ref, mPointMutation.Alt);
    }

    public double copyNumber() { return mPointMutation.CopyNumber; }

    public boolean phaseMatched()
    {
        return (abs(mIndelBaseDiff) % 3) == 0;
    }

    private boolean posStrand() { return TransData[FS_UP].Strand == POS_STRAND; }
    private boolean isInsert() { return mIndelBaseDiff > 0; }
    private boolean isDeletion() { return mIndelBaseDiff < 0; }
    private boolean isBaseChange() { return mIndelBaseDiff == 0; }
    public int unsplicedDistance() { return 0; }
    public int skippedAcceptors() { return 0; }
    public int skippedDonors() { return 0; }
    public void setSkippedSpliceSites(final EnsemblDataCache geneTransCache) {}
    public String wildtypeAcids() { return mWildtypeAcids; }

    public void setTranscriptData(final TranscriptData upTransData, final TranscriptData downTransData)
    {
        final TranscriptData transData = upTransData;

        TransData[FS_UP] = TransData[FS_DOWN] = transData;

        // set the data for the lower part of the mutation
        int lowerStream = transData.Strand == POS_STRAND ? FS_UP : FS_DOWN;

        setTranscriptContext(this, transData, position(lowerStream), lowerStream);

        setTranscriptCodingData(this, transData, position(lowerStream), 0, lowerStream);

        int upperStream = switchStream(lowerStream);

        // for DELs, set the downstream data as well since it can cross exon-boundaries and/or affect coding bases
        setTranscriptContext(this, transData, position(upperStream), upperStream);

        setTranscriptCodingData(this, transData, position(upperStream), 0, upperStream);
    }

    private int getUpstreamStartPhase()
    {
        // get the phase at the start of the mutation, taking strand into account
        return Phases[FS_UP];
    }

    private int getUpstreamOpenCodonBases()
    {
        int phase = getUpstreamStartPhase();

        // return the number of open codon bases up to but not including the PM's position
        if(posStrand() || isBaseChange())
        {
            if(phase == PHASE_1)
                return 0;
            else if(phase == PHASE_2)
                return 1;
            else
                return isBaseChange() ? 2 : 0; // INDEL leaves the mutation base unchanged, so if it ends a codon, it's not novel
        }
        else
        {
            if(phase == PHASE_1)
                return 1;
            else if(phase == PHASE_2)
                return 2;
            else
                return isBaseChange() ? 2 : 0;
        }
    }

    private int getDownstreamOpenCodonBases()
    {
        // determine the phase at the base after the mutation - last base of an MNV/SNV, and next base for an INS or DEL
        int mutationTicks;

        if(posStrand())
        {
            mutationTicks = isDeletion() ? 1 : mPointMutation.Alt.length();
        }
        else
        {
            // need to go to the phase (prior to?) the ALT
            if(isBaseChange())
                mutationTicks = mPointMutation.Alt.length();
            else if(isInsert())
                mutationTicks = mPointMutation.Alt.length() + 1;
            else
                mutationTicks = 2;
        }

        int postMutationPhase = tickPhaseForward(Phases[FS_UP], mutationTicks);

        if(postMutationPhase == PHASE_0)
            return 1;
        else if(postMutationPhase == PHASE_1)
            return 0;
        else
            return 2;
    }

    public void extractCodingBases(final RefGenomeInterface refGenome, int requiredAminoAcids)
    {
        // get the number of bases from the mutation or junction as required by the required amino acid count
        // the downstream bases start directly after the mutation (ie after any insert, or deleted bases)

        // if the upstream phase is not 0 (ie the end of a codon) then additionally extract enough bases to cover the required amino
        // acids plus the bases of the partial codon - the downstream bases will provide the remainder to complete the codon

        // phasing already takes any inserted bases (ie from an INDEL) into account, so just adjust for the number of bases required
        int upExtraBases = getUpstreamOpenCodonBases();
        int downExtraBases = getDownstreamOpenCodonBases();

        int upRequiredBases = requiredAminoAcids * 3 + upExtraBases;

        String upCodingBases = "";
        String downCodingBases = "";

        int upPosition = position(FS_UP);
        int downPosition = position(FS_DOWN);

        // adjust the position for getting reference bases to make way for the ALT to be added
        if(posStrand())
        {
            upPosition = mPointMutation.Position - 1;

            if(!isBaseChange() && upExtraBases == 0 && getUpstreamStartPhase() == PHASE_0)
            {
                // since ref base will be the same, require 1 less coding bases upstream
                --upRequiredBases;
            }
        }
        else
        {
            if(isBaseChange())
                upPosition += 1; // since would otherwise cross with the SNV/MNV

            downPosition = mPointMutation.Position - 1; // since down pos on -ve strand is the mutation base
        }

        CodingBaseExcerpt upExcerpt = getUpstreamCodingBaseExcerpt(
                refGenome, TransData[FS_UP], chromosome(FS_UP), upPosition, orientation(FS_UP), upRequiredBases);

        if(upExcerpt == null)
        {
            // at or past end of transcript is invalid
            Valid = false;
            return;
        }

        upCodingBases = upExcerpt.Bases;

        boolean canStartInExon = true; // assumed true for now RegionType[FS_UPSTREAM] == TranscriptRegionType.EXONIC;
        int downRequiredBases = requiredAminoAcids * 3 + downExtraBases;

        CodingBaseExcerpt downExcerpt = getDownstreamCodingBaseExcerpt(
                refGenome, TransData[FS_DOWN], chromosome(FS_DOWN), downPosition, orientation(FS_DOWN),
                downRequiredBases, canStartInExon, false, !phaseMatched());

        if(downExcerpt == null)
        {
            // at or past end of transcript is invalid
            Valid = false;
            return;
        }

        downCodingBases = downExcerpt.Bases;

        if(isBaseChange())
            setWildtypeAminoAcids(refGenome, requiredAminoAcids);

        // the ref bases was excluded and is now replaced by the alt

        int altLength = mPointMutation.Alt.length();
        int upPhase = getUpstreamStartPhase();

        if(posStrand())
        {
            upCodingBases += mPointMutation.Alt;

            if(isBaseChange())
            {
                NovelBaseIndex[FS_UP] = upExtraBases + altLength;
            }
            else
            {
                // if the mutation occurs on the last base of a codon, then then novel codon starts immediately after
                if(upPhase == PHASE_0)
                {
                    if(isDeletion())
                        NovelBaseIndex[FS_UP] = 0;
                    else
                        NovelBaseIndex[FS_UP] = max(altLength - 1, 0);
                }
                else
                {
                    NovelBaseIndex[FS_UP] = upExtraBases + altLength;
                }
            }

            if(phaseMatched())
                NovelBaseIndex[FS_DOWN] = downExtraBases;

            ExtCodingBases[FS_UP] = upExcerpt.Bases + mPointMutation.Alt + downExcerpt.Bases;
        }
        else
        {
            downCodingBases += mPointMutation.Alt;
            NovelBaseIndex[FS_UP] = upExtraBases;

            if(phaseMatched())
            {
                if(isDeletion() && upPhase == PHASE_0)
                    NovelBaseIndex[FS_DOWN] = 0;
                else
                    NovelBaseIndex[FS_DOWN] = downExtraBases + altLength;
            }

            ExtCodingBases[FS_UP] = downExcerpt.Bases + mPointMutation.Alt + upExcerpt.Bases;
        }

        if(!phaseMatched())
        {
            // ensure a codon-rounding base total
            if(isDeletion())
                downRequiredBases = (requiredAminoAcids * 2 + 1) * 3 - upCodingBases.length();

            downExcerpt = getDownstreamCodingBaseExcerpt(
                    refGenome, TransData[FS_DOWN], chromosome(FS_DOWN), downPosition, orientation(FS_DOWN),
                    downRequiredBases, canStartInExon, false, false);
        }

        CodingBaseExcerpt lowerExcerpt = posStrand() ? upExcerpt : downExcerpt;
        CodingBaseExcerpt upperExcerpt = posStrand() ? downExcerpt : upExcerpt;

        // set cigar details just for the upstream slot since it's for a single gene
        ExtPositions[FS_UP][SE_START] = lowerExcerpt.Positions[SE_START];
        ExtPositions[FS_UP][SE_END] = upperExcerpt.Positions[SE_END];

        List<CigarElement> cigarElements = Lists.newArrayList();
        cigarElements.addAll(lowerExcerpt.CigarRef.getCigarElements());

        CigarElement lastElement = cigarElements.get(cigarElements.size() - 1);

        if(isInsert())
        {
            // add the unchanged base
            if(lastElement.getOperator() == CigarOperator.M)
                cigarElements.set(cigarElements.size() - 1, new CigarElement(lastElement.getLength() + 1, CigarOperator.M));
            else
                cigarElements.add(new CigarElement(1, CigarOperator.M));

            cigarElements.add(new CigarElement(altLength - 1, CigarOperator.I));
        }
        else
        {
            // add the unchanged base(s)
            if(lastElement.getOperator() == CigarOperator.M)
                cigarElements.set(cigarElements.size() - 1, new CigarElement(lastElement.getLength() + altLength, CigarOperator.M));
            else
                cigarElements.add(new CigarElement(altLength, CigarOperator.M));

            if(isDeletion())
                cigarElements.add(new CigarElement(abs(mIndelBaseDiff), CigarOperator.D));
        }

        lastElement = cigarElements.get(cigarElements.size() - 1);

        final List<CigarElement> upElements = Lists.newArrayList();
        upElements.addAll(upperExcerpt.CigarRef.getCigarElements());

        if(lastElement.getOperator() == CigarOperator.M && upElements.get(0).getOperator() == CigarOperator.M)
        {
            cigarElements.set(
                    cigarElements.size() - 1, new CigarElement(lastElement.getLength() + upElements.get(0).getLength(), CigarOperator.M));

            upElements.remove(0);
        }

        cigarElements.addAll(upElements);

        Cigar cigar = new Cigar();
        cigarElements.forEach(x -> cigar.add(x));
        ExtCigars[FS_UP] = cigar;

        RawCodingBases[FS_UP] = upCodingBases;
        RawCodingBases[FS_DOWN] = downCodingBases;
    }

    private void setWildtypeAminoAcids(final RefGenomeInterface refGenome, int requiredAminoAcids)
    {
        final TranscriptData transData = TransData[FS_UP];
        boolean posStrand = transData.Strand == POS_STRAND;

        final CodingBaseData cbData = calcCodingBases(transData, mPointMutation.Position);
        int upOpenCodonBases = cbData.Phase == PHASE_0 ? 0 : cbData.Phase;

        int upRequiredBases = requiredAminoAcids * 3 + upOpenCodonBases;

        byte upOrient = posStrand ? POS_ORIENT : NEG_ORIENT;
        String upBases = getUpstreamCodingBases(refGenome, transData, chromosome(FS_UP), mPointMutation.Position, upOrient, upRequiredBases);

        byte downOrient = posStrand ? NEG_ORIENT : POS_ORIENT;
        int downPosition = posStrand ? mPointMutation.Position + 1 : mPointMutation.Position - 1;
        int downRequiredBases = requiredAminoAcids * 3 - upOpenCodonBases;

        String downBases = getDownstreamCodingBases(
                refGenome, transData, chromosome(FS_UP), downPosition, downOrient, downRequiredBases,
                true, false, false);

        if(!posStrand)
        {
            upBases = reverseStrandBases(upBases);
            downBases = reverseStrandBases(downBases);
        }

        if(upOpenCodonBases > 0)
        {
            downBases = upBases.substring(upBases.length() - upOpenCodonBases) + downBases;
            upBases = upBases.substring(0, upBases.length() - upOpenCodonBases);
        }

        mWildtypeBases = trimIncompleteCodons(upBases) + downBases;

        final String upstreamAcids = getAminoAcids(upBases, false);
        final String downstreamAcids = getAminoAcids(downBases, true);
        mWildtypeAcids = upstreamAcids + downstreamAcids;
    }

    public String toString()
    {
        return String.format("pointMut(%s: %s:%d %s -> %s)",
                mPointMutation.Gene, mPointMutation.Chromosome,  mPointMutation.Position,
                mPointMutation.Ref, mPointMutation.Alt);
    }

}
