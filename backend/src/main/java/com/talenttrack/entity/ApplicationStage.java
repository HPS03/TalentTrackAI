package com.talenttrack.entity;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Columns of the Jira-style recruitment board. Each stage declares which stages
 * an application may move to, so the workflow is enforced server-side and not
 * only by the drag-and-drop UI.
 */
public enum ApplicationStage {
    APPLIED,
    SCREENING,
    INTERVIEW,
    OFFER,
    HIRED,
    REJECTED;

    private static final Map<ApplicationStage, Set<ApplicationStage>> TRANSITIONS = Map.of(
            APPLIED, EnumSet.of(SCREENING, REJECTED),
            SCREENING, EnumSet.of(APPLIED, INTERVIEW, REJECTED),
            INTERVIEW, EnumSet.of(SCREENING, OFFER, REJECTED),
            OFFER, EnumSet.of(INTERVIEW, HIRED, REJECTED),
            HIRED, EnumSet.noneOf(ApplicationStage.class),
            REJECTED, EnumSet.of(APPLIED, SCREENING)
    );

    public boolean canMoveTo(ApplicationStage target) {
        return this == target || TRANSITIONS.get(this).contains(target);
    }

    public Set<ApplicationStage> allowedTransitions() {
        return TRANSITIONS.get(this);
    }

    public boolean isTerminal() {
        return this == HIRED || this == REJECTED;
    }
}
