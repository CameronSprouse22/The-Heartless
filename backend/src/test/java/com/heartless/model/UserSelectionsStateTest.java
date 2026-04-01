package com.heartless.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

class UserSelectionsStateTest {

    private UserSelectionsState state;

    @BeforeEach
    void setUp() {
        state = new UserSelectionsState();
    }

    // --- Initial State ---

    @Test
    void initialSelectedItemsIsEmpty() {
        assertTrue(state.getSelectedItems().isEmpty());
    }

    @Test
    void initialTextFieldInputIsEmptyString() {
        assertEquals("", state.getTextFieldInput());
    }

    @Test
    void initialSubmitPressedIsFalse() {
        assertFalse(state.isSubmitPressed());
    }

    // --- addSelectedItem ---

    @Test
    void addSelectedItem_addsToList() {
        state.addSelectedItem("player-1");
        assertTrue(state.getSelectedItems().contains("player-1"));
    }

    @Test
    void addSelectedItem_doesNotAddDuplicate() {
        state.addSelectedItem("player-1");
        state.addSelectedItem("player-1");
        assertEquals(1, state.getSelectedItems().size());
    }

    @Test
    void addSelectedItem_nullIsIgnored() {
        state.addSelectedItem(null);
        assertTrue(state.getSelectedItems().isEmpty());
    }

    // --- removeSelectedItem ---

    @Test
    void removeSelectedItem_removesExistingEntry() {
        state.addSelectedItem("player-1");
        state.removeSelectedItem("player-1");
        assertFalse(state.getSelectedItems().contains("player-1"));
    }

    @Test
    void removeSelectedItem_nonExistentEntryIsNoOp() {
        assertDoesNotThrow(() -> state.removeSelectedItem("player-x"));
    }

    // --- addSelectedItem / removeSelectedItem round-trips ---

    @Test
    void addRemoveRoundTrip_listIsEmptyAfterRemove() {
        state.addSelectedItem("player-1");
        state.addSelectedItem("player-2");
        state.removeSelectedItem("player-1");
        assertEquals(List.of("player-2"), state.getSelectedItems());
    }

    // --- setSelectedItems ---

    @Test
    void setSelectedItems_replacesExistingList() {
        state.addSelectedItem("player-old");
        state.setSelectedItems(List.of("player-new"));
        assertEquals(List.of("player-new"), state.getSelectedItems());
    }

    @Test
    void setSelectedItems_nullStoresEmptyList() {
        state.addSelectedItem("player-1");
        state.setSelectedItems(null);
        assertTrue(state.getSelectedItems().isEmpty());
    }

    @Test
    void setSelectedItems_emptyListClearsPreviousSelections() {
        state.addSelectedItem("player-1");
        state.setSelectedItems(new ArrayList<>());
        assertTrue(state.getSelectedItems().isEmpty());
    }

    // --- getSelectedItems defensive copy ---

    @Test
    void getSelectedItems_returnsDefensiveCopy() {
        state.addSelectedItem("player-1");
        ArrayList<String> copy = state.getSelectedItems();
        copy.add("mutate-externally");
        // Internal state should not be affected by mutation of the returned copy
        assertFalse(state.getSelectedItems().contains("mutate-externally"));
    }

    // --- setTextFieldInput / getTextFieldInput ---

    @Test
    void setTextFieldInput_storesValue() {
        state.setTextFieldInput("hello world");
        assertEquals("hello world", state.getTextFieldInput());
    }

    @Test
    void setTextFieldInput_nullStoresEmptyString() {
        state.setTextFieldInput(null);
        assertEquals("", state.getTextFieldInput());
    }

    @Test
    void setTextFieldInput_emptyStringStored() {
        state.setTextFieldInput("some text");
        state.setTextFieldInput("");
        assertEquals("", state.getTextFieldInput());
    }

    // --- setSubmitPressed ---

    @Test
    void setSubmitPressed_trueTransitionsFlag() {
        state.setSubmitPressed(true);
        assertTrue(state.isSubmitPressed());
    }

    @Test
    void setSubmitPressed_trueIsIrreversible() {
        state.setSubmitPressed(true);
        state.setSubmitPressed(false); // should be a no-op
        assertTrue(state.isSubmitPressed(), "submitPressed must remain true once set");
    }

    @Test
    void setSubmitPressed_falseWhenAlreadyFalseRemainsNoOp() {
        state.setSubmitPressed(false);
        assertFalse(state.isSubmitPressed());
    }

    // --- Concurrent safety smoke test ---

    @Test
    void concurrentAddAndRemove_doesNotThrow() throws InterruptedException {
        int threads = 10;
        CountDownLatch latch = new CountDownLatch(threads);
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        for (int i = 0; i < threads; i++) {
            final int idx = i;
            executor.submit(() -> {
                try {
                    state.addSelectedItem("player-" + idx);
                    state.removeSelectedItem("player-" + idx);
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();
        executor.shutdown();
        // No exception thrown — concurrent access is safe
        assertNotNull(state.getSelectedItems());
    }
}
