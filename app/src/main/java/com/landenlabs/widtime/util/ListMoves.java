package com.landenlabs.widtime.util;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Pure (Android-free) move-up/move-down for a multi-selection, as in a file manager: every
 * selected item moves one place, a selected block keeps its shape, and an item already at the
 * end (or stuck behind a selected item at the end) stays put.
 */
public final class ListMoves {

    private ListMoves() {
    }

    /** Moves each selected item one place toward index 0. Returns true if anything moved. */
    public static <T> boolean moveUp(List<T> list, Set<T> selected) {
        boolean moved = false;
        for (int i = 1; i < list.size(); i++) {
            if (selected.contains(list.get(i)) && !selected.contains(list.get(i - 1))) {
                Collections.swap(list, i, i - 1);
                moved = true;
            }
        }
        return moved;
    }

    /** Moves each selected item one place toward the end. Returns true if anything moved. */
    public static <T> boolean moveDown(List<T> list, Set<T> selected) {
        boolean moved = false;
        for (int i = list.size() - 2; i >= 0; i--) {
            if (selected.contains(list.get(i)) && !selected.contains(list.get(i + 1))) {
                Collections.swap(list, i, i + 1);
                moved = true;
            }
        }
        return moved;
    }
}
