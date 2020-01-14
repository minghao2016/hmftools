package com.hartwig.hmftools.linx;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

public class LinxOutput
{
    public final boolean WriteSvData; // all SV table fields to cohort file
    public final boolean WriteVisualisationData;

    public final boolean WriteClusterHistory;
    public final boolean WriteSingleSVClusters;
    public final boolean WriteLinks;

    public final int LogChainingMaxSize;

    private static final String WRITE_ALL = "write_all";
    private static final String WRITE_SV_DATA = "write_sv_data";
    private static final String WRITE_SINGLE_SV_CLUSTERS = "write_single_sv_clusters";
    private static final String WRITE_CLUSTER_HISTORY = "write_cluster_history";
    private static final String WRITE_LINKS = "write_links";
    private static final String WRITE_VISUALISATION_DATA = "write_vis_data";
    private static final String LOG_CHAIN_MAX_SIZE = "log_chain_size";

    public LinxOutput(final CommandLine cmd)
    {
        if(cmd.hasOption(WRITE_ALL))
        {
            WriteSvData = true;
            WriteVisualisationData = true;
            WriteClusterHistory = true;
            WriteSingleSVClusters = true;
            WriteLinks = true;
        }
        else
        {
            WriteVisualisationData = cmd.hasOption(WRITE_VISUALISATION_DATA);
            WriteSvData = cmd.hasOption(WRITE_SV_DATA);
            WriteClusterHistory = cmd.hasOption(WRITE_CLUSTER_HISTORY);
            WriteSingleSVClusters = cmd.hasOption(WRITE_SINGLE_SV_CLUSTERS);
            WriteLinks = cmd.hasOption(WRITE_LINKS);
        }

        LogChainingMaxSize = Integer.parseInt(cmd.getOptionValue(LOG_CHAIN_MAX_SIZE, "0"));
    }

    public static void addCmdLineArgs(Options options)
    {
        options.addOption(WRITE_ALL, false, "Optional: write all batch-run output files");
        options.addOption(WRITE_SV_DATA, false, "Optional: include all SV table fields (batch-mode)");
        options.addOption(WRITE_LINKS, false, "Optional: write chain links (batch-mode)");
        options.addOption(WRITE_CLUSTER_HISTORY, false, "Optional: write clustering history (batch-mode)");
        options.addOption(WRITE_SINGLE_SV_CLUSTERS, false, "Optional: write cluster data for single SV clusters (batch-mode)");
        options.addOption(WRITE_VISUALISATION_DATA, false, "Optional: write files for Circos (batch-mode)");
        options.addOption(LOG_CHAIN_MAX_SIZE, true, "Write file with chaining diagnostics for chains less than this (off by default)");
    }

    public LinxOutput()
    {
        WriteSvData = false;
        WriteVisualisationData = false;
        WriteClusterHistory = false;
        WriteSingleSVClusters = false;
        WriteLinks = false;

        LogChainingMaxSize = 0;
    }
}