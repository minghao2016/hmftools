package com.hartwig.hmftools.serve.docm;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.hartwig.hmftools.serve.hotspot.ProteinKeyFormatter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

public class DocmCurator {

    private static final Logger LOGGER = LogManager.getLogger(DocmCurator.class);

    private static final Set<CurationKey> ENTRY_BLACKLIST = Sets.newHashSet();

    static {
        // Not clear what the "minus" means, so ignoring. Could be DEL?
        ENTRY_BLACKLIST.add(new CurationKey("BRAF", "ENST00000288602", "K601-"));
        ENTRY_BLACKLIST.add(new CurationKey("CTNNB1", "ENST00000349496", "VSHWQQQSYLDSGIHSG22-"));
        ENTRY_BLACKLIST.add(new CurationKey("CTNNB1", "ENST00000349496", "WQQQSYLD25-"));
        ENTRY_BLACKLIST.add(new CurationKey("FLT3", "ENST00000380982", "D835-"));
        ENTRY_BLACKLIST.add(new CurationKey("FLT3", "ENST00000380982", "I836-"));
        ENTRY_BLACKLIST.add(new CurationKey("KIT", "ENST00000288135", "V559-"));
        ENTRY_BLACKLIST.add(new CurationKey("PDGFRA", "ENST00000257290", "RD841-"));
        ENTRY_BLACKLIST.add(new CurationKey("PDGFRA", "ENST00000257290", "DIM842-"));
        ENTRY_BLACKLIST.add(new CurationKey("PDGFRA", "ENST00000257290", "DIMH842-"));
        ENTRY_BLACKLIST.add(new CurationKey("PDGFRA", "ENST00000257290", "IMHD843-"));
        ENTRY_BLACKLIST.add(new CurationKey("PIK3R1", "ENST00000396611", "T576-"));
        ENTRY_BLACKLIST.add(new CurationKey("PIK3R1", "ENST00000396611", "DKRMNS560-"));
        ENTRY_BLACKLIST.add(new CurationKey("PTEN", "ENST00000371953", "K267X"));
        ENTRY_BLACKLIST.add(new CurationKey("RET", "ENST00000355710", "FPEEEKCFC612-"));
        ENTRY_BLACKLIST.add(new CurationKey("RET", "ENST00000355710", "EL632-"));
        ENTRY_BLACKLIST.add(new CurationKey("RET", "ENST00000355710", "DVYE898-"));

        // Unclear what these variants means. Maybe these are INS?
        ENTRY_BLACKLIST.add(new CurationKey("BRAF", "ENST00000288602", "GD593GN"));
        ENTRY_BLACKLIST.add(new CurationKey("ERBB2", "ENST00000269571", "G776CX"));
        ENTRY_BLACKLIST.add(new CurationKey("ERBB2", "ENST00000269571", "G778GSPX"));
        ENTRY_BLACKLIST.add(new CurationKey("NPM1", "ENST00000296930", "W288SX"));
        ENTRY_BLACKLIST.add(new CurationKey("NPM1", "ENST00000296930", "W288PX"));
        ENTRY_BLACKLIST.add(new CurationKey("NPM1", "ENST00000296930", "W288HX"));
        ENTRY_BLACKLIST.add(new CurationKey("WT1", "ENST00000332351", "S381LYG"));

        // Wildcard variants are ignored
        ENTRY_BLACKLIST.add(new CurationKey("APC", "ENST00000257430", "KIGT1310X"));
        ENTRY_BLACKLIST.add(new CurationKey("APC", "ENST00000257430", "KIGTRSA1310X"));
        ENTRY_BLACKLIST.add(new CurationKey("APC", "ENST00000257430", "GP1466X"));
        ENTRY_BLACKLIST.add(new CurationKey("APC", "ENST00000257430", "IDS1557X"));
        ENTRY_BLACKLIST.add(new CurationKey("ATR", "ENST00000350721", "I774X"));
        ENTRY_BLACKLIST.add(new CurationKey("ERBB2", "ENST00000269571", "L755X"));
        ENTRY_BLACKLIST.add(new CurationKey("HERC2", "ENST00000261609", "D759X"));
        ENTRY_BLACKLIST.add(new CurationKey("KIT", "ENST00000288135", "V560X"));
        ENTRY_BLACKLIST.add(new CurationKey("KIT", "ENST00000288135", "P577X"));
        ENTRY_BLACKLIST.add(new CurationKey("PIK3R1", "ENST00000396611", "E439X"));
        ENTRY_BLACKLIST.add(new CurationKey("PIK3R1", "ENST00000396611", "W583X"));

        // Not sure what these mean? Maybe wildcards?
        ENTRY_BLACKLIST.add(new CurationKey("ABCB1", "ENST00000265724", "I1145"));
        ENTRY_BLACKLIST.add(new CurationKey("ETS2", "ENST00000360214", "P341"));
        ENTRY_BLACKLIST.add(new CurationKey("KIT", "ENST00000288135", "L862"));
        ENTRY_BLACKLIST.add(new CurationKey("MGMT", "ENST00000306010", "R22"));

        // Variants that don't exist on the configured transcript TODO verify
        ENTRY_BLACKLIST.add(new CurationKey("BRAF", "ENST00000496384", "F203L"));
        ENTRY_BLACKLIST.add(new CurationKey("BRAF", "ENST00000496384", "T207I"));
        ENTRY_BLACKLIST.add(new CurationKey("BRAF", "ENST00000496384", "G77L"));
        ENTRY_BLACKLIST.add(new CurationKey("BRAF", "ENST00000496384", "G77V"));
    }

    @NotNull
    private final Set<CurationKey> evaluatedCurationKeys = Sets.newHashSet();

    @NotNull
    public List<DocmEntry> curate(@NotNull List<DocmEntry> entries) {
        List<DocmEntry> curatedEntries = Lists.newArrayList();
        for (DocmEntry entry : entries) {
            CurationKey key = new CurationKey(entry.gene(), entry.transcript(), entry.proteinAnnotation());
            evaluatedCurationKeys.add(key);
            if (ENTRY_BLACKLIST.contains(key)) {
                LOGGER.debug("Removing DocmEntry '{}' because of blacklist curation.",
                        ProteinKeyFormatter.toProteinKey(entry.gene(), entry.transcript(), entry.proteinAnnotation()));
            } else {
                curatedEntries.add(entry);
            }
        }
        return curatedEntries;
    }

    public void reportUnusedBlacklistEntries() {
        int unusedKeys = 0;
        for (CurationKey key : ENTRY_BLACKLIST) {
            if (!evaluatedCurationKeys.contains(key)) {
                unusedKeys++;
                LOGGER.warn("Key '{}' hasn't been used during DoCM curation", key);
            }
        }

        LOGGER.debug("Found {} unused DoCM blacklist entries. {} keys have been requested against {} blacklist entries",
                unusedKeys,
                evaluatedCurationKeys.size(),
                ENTRY_BLACKLIST.size());
    }

    private static final class CurationKey {

        @NotNull
        private final String gene;
        @NotNull
        private final String transcript;
        @NotNull
        private final String proteinAnnotation;

        public CurationKey(@NotNull final String gene, @NotNull final String transcript, @NotNull final String proteinAnnotation) {
            this.gene = gene;
            this.transcript = transcript;
            this.proteinAnnotation = proteinAnnotation;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            final CurationKey that = (CurationKey) o;
            return gene.equals(that.gene) && transcript.equals(that.transcript) && proteinAnnotation.equals(that.proteinAnnotation);
        }

        @Override
        public int hashCode() {
            return Objects.hash(gene, transcript, proteinAnnotation);
        }
    }
}
