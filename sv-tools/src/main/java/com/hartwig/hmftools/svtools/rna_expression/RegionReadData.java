package com.hartwig.hmftools.svtools.rna_expression;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.hartwig.hmftools.common.genome.region.GenomeRegion;

import htsjdk.samtools.SAMRecord;

public class RegionReadData
{
    public final GenomeRegion Region;

    public final String Id;

    private String mRefBases;
    private int[] mRefBasesMatched;
    private int mMatchingReads;
    private int mNonAdjacentReads; // reads spanning to unmapped regions where adjacent regions exist
    private final Map<RegionReadData,Integer> mLinkedRegions; // where a read covers this region and another next to it
    private List<RegionReadData> mPreRegions; // references to adjacent regions with a lower position
    private List<RegionReadData> mPostRegions;

    public RegionReadData(final GenomeRegion region, final String id)
    {
        Region = region;
        Id = id;

        mRefBasesMatched = new int[(int)region.bases()+1];
        mRefBases = "";
        mLinkedRegions = Maps.newHashMap();
        mMatchingReads = 0;

        mPreRegions = Lists.newArrayList();
        mPostRegions = Lists.newArrayList();
        mNonAdjacentReads = 0;
    }

    public String chromosome() { return Region.chromosome(); }
    public long start() { return Region.start(); }
    public long end() { return Region.end(); }

    public int[] refBasesMatched() { return mRefBasesMatched; }
    public void addMatchedRead() { ++mMatchingReads; }
    public int matchedReadCount() { return mMatchingReads; }

    public void addNonAdjacentRead() { ++mNonAdjacentReads; }
    public int nonAdjacentReads() { return mNonAdjacentReads; }

    public List<RegionReadData> getPreRegions() { return mPreRegions; }
    public List<RegionReadData> getPostRegions() { return mPostRegions; }

    public final Map<RegionReadData, Integer> getLinkedRegions() { return mLinkedRegions; }

    public void addLinkedRegion(final RegionReadData region)
    {
        Integer linkCount = mLinkedRegions.get(region);

        if(linkCount != null)
            mLinkedRegions.put(region, linkCount + 1);
        else
            mLinkedRegions.put(region, 1);
    }

    public final String refBases() { return mRefBases; }
    public void setRefBases(final String bases) { mRefBases = bases; }

    public int length() { return mRefBases.length(); }

    public double averageDepth()
    {
        int depthTotal = Arrays.stream(mRefBasesMatched).sum();
        return depthTotal/(double)mRefBasesMatched.length;
    }

    public int baseCoverage(int minReadCount)
    {
        // percent of bases covered by a read
        return (int)Arrays.stream(mRefBasesMatched).filter(x -> x >= minReadCount).count();
    }

    public String toString()
    {
        return String.format("%s: %s:%d -> %d", Id, chromosome(), start(), end());
    }
}
