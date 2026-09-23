package com.talenttrack;

import com.talenttrack.entity.ApplicationStage;
import org.junit.jupiter.api.Test;

import static com.talenttrack.entity.ApplicationStage.*;
import static org.assertj.core.api.Assertions.assertThat;

class ApplicationStageTest {

    @Test
    void happyPathTransitionsAreAllowed() {
        assertThat(APPLIED.canMoveTo(SCREENING)).isTrue();
        assertThat(SCREENING.canMoveTo(INTERVIEW)).isTrue();
        assertThat(INTERVIEW.canMoveTo(OFFER)).isTrue();
        assertThat(OFFER.canMoveTo(HIRED)).isTrue();
    }

    @Test
    void skippingStagesIsRejected() {
        assertThat(APPLIED.canMoveTo(INTERVIEW)).isFalse();
        assertThat(APPLIED.canMoveTo(HIRED)).isFalse();
        assertThat(SCREENING.canMoveTo(OFFER)).isFalse();
    }

    @Test
    void hiredIsTerminalAndAnyActiveStageCanBeRejected() {
        for (ApplicationStage s : ApplicationStage.values()) {
            assertThat(HIRED.canMoveTo(s)).isEqualTo(s == HIRED);
            if (!s.isTerminal()) {
                assertThat(s.canMoveTo(REJECTED)).isTrue();
            }
        }
        assertThat(REJECTED.canMoveTo(SCREENING)).isTrue();
    }
}
