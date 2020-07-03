package com.hartwig.hmftools.common.utils.sv;

import static com.hartwig.hmftools.common.utils.sv.StartEndIterator.SE_END;
import static com.hartwig.hmftools.common.utils.sv.StartEndIterator.SE_START;

public class SvRegion
{
    public final String Chromosome;
    public final int[] Positions;

    public SvRegion(final String chromosome, final int[] positions)
    {
        Chromosome = chromosome;
        Positions = positions;
    }

    public SvRegion(final String chromosome, final int posStart, final int posEnd)
    {
        Chromosome = chromosome;
        Positions = new int[] { posStart, posEnd };
    }

    public int start() { return Positions[SE_START]; }
    public int end() { return Positions[SE_END]; }

    public void setPosition(int position, int index) { Positions[index] = position; }
    public void setStart(int pos) { setPosition(pos, SE_START); }
    public void setEnd(int pos) { setPosition(pos, SE_END); }

    public int baseLength() { return length() + 1; }
    public int length() { return Positions[SE_END] - Positions[SE_START]; }

    public boolean overlaps(final SvRegion other)
    {
        return positionsOverlap(Positions[SE_START], Positions[SE_END], other.Positions[SE_START], other.Positions[SE_END]);
    }

    // utility methods relating to position comparisons
    public static boolean positionsOverlap(int posStart1, int posEnd1, int posStart2, int posEnd2)
    {
        return !(posStart1 > posEnd2 || posEnd1 < posStart2);
    }

    public static boolean positionWithin(int position, int otherPosStart, int otherPosEnd)
    {
        return (position >= otherPosStart && position <= otherPosEnd);
    }

    public static boolean positionsWithin(int innerStart, int innerEnd, int outerStart, int outerEnd)
    {
        return (innerStart < innerEnd && innerStart >= outerStart && innerEnd <= outerEnd);
    }


}
