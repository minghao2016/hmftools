package com.hartwig.hmftools.patientreporter.actionability;

import java.util.List;
import java.util.stream.Collectors;

import com.hartwig.hmftools.common.protect.ProtectEvidence;
import com.hartwig.hmftools.common.serve.Knowledgebase;

import org.jetbrains.annotations.NotNull;

public final class ReportableEvidenceItemFactory {

    private ReportableEvidenceItemFactory() {
    }

    @NotNull
    public static List<ProtectEvidence> extractNonTrials(@NotNull List<ProtectEvidence> evidenceItems) {
        return evidenceItems.stream()
                .filter(evidenceItem -> !evidenceItem.sources().contains(Knowledgebase.ICLUSION))
                .collect(Collectors.toList());
    }
}