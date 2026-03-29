package com.heartless.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PlayerSelectionTest {

    private PlayerSelection selection;

    @BeforeEach
    void setUp() {
        selection = new PlayerSelection("p1", "Alice");
    }

    @Test
    void initialStateIsNone() {
        assertEquals(SubmissionStatus.NONE, selection.getSubmissionStatus());
        assertTrue(selection.getSelectedItems().isEmpty());
        assertNull(selection.getTextInput());
    }

    @Test
    void selectTransitionsToSelected() {
        selection.select(Set.of("Forest"), null);
        assertEquals(SubmissionStatus.SELECTED, selection.getSubmissionStatus());
        assertEquals(Set.of("Forest"), selection.getSelectedItems());
    }

    @Test
    void selectWithMultipleItems() {
        selection.select(Set.of("Forest", "Cave"), null);
        assertEquals(Set.of("Forest", "Cave"), selection.getSelectedItems());
    }

    @Test
    void reSelectOverwritesPrevious() {
        selection.select(Set.of("Forest"), null);
        selection.select(Set.of("Cave"), "some text");
        assertEquals(Set.of("Cave"), selection.getSelectedItems());
        assertEquals("some text", selection.getTextInput());
    }

    @Test
    void submitFromSelectedSucceeds() {
        selection.select(Set.of("Forest"), null);
        selection.submit();
        assertEquals(SubmissionStatus.SUBMITTED, selection.getSubmissionStatus());
    }

    @Test
    void submitFromNoneThrows() {
        assertThrows(IllegalStateException.class, () -> selection.submit());
    }

    @Test
    void cancelSubmitFromSubmittedSucceeds() {
        selection.select(Set.of("Forest"), null);
        selection.submit();
        selection.cancelSubmit();
        assertEquals(SubmissionStatus.SELECTED, selection.getSubmissionStatus());
    }

    @Test
    void cancelSubmitFromSelectedThrows() {
        selection.select(Set.of("Forest"), null);
        assertThrows(IllegalStateException.class, () -> selection.cancelSubmit());
    }

    @Test
    void resetSubmissionSetsToSelected() {
        selection.select(Set.of("Forest"), null);
        selection.submit();
        selection.resetSubmission();
        assertEquals(SubmissionStatus.SELECTED, selection.getSubmissionStatus());
    }

    @Test
    void reSelectAfterSubmitRevertsToSelected() {
        selection.select(Set.of("Forest"), null);
        selection.submit();
        selection.select(Set.of("Cave"), null);
        assertEquals(SubmissionStatus.SELECTED, selection.getSubmissionStatus());
        assertEquals(Set.of("Cave"), selection.getSelectedItems());
    }

    @Test
    void getSelectedItemsReturnsImmutableCopy() {
        selection.select(Set.of("Forest"), null);
        assertThrows(UnsupportedOperationException.class, () -> selection.getSelectedItems().add("Cave"));
    }
}
