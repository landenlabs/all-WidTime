package com.landenlabs.widtime.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ListMovesTest {

    private static List<String> list(String... items) {
        return new ArrayList<>(Arrays.asList(items));
    }

    private static Set<String> sel(String... items) {
        return new HashSet<>(Arrays.asList(items));
    }

    @Test
    public void moveUpSingle() {
        List<String> l = list("a", "b", "c");
        assertTrue(ListMoves.moveUp(l, sel("c")));
        assertEquals(list("a", "c", "b"), l);
    }

    @Test
    public void moveUpBlockKeepsShape() {
        List<String> l = list("a", "b", "c", "d");
        ListMoves.moveUp(l, sel("c", "d"));
        assertEquals(list("a", "c", "d", "b"), l);
    }

    @Test
    public void moveUpSeparatedItemsEachMove() {
        List<String> l = list("a", "b", "c", "d");
        ListMoves.moveUp(l, sel("b", "d"));
        assertEquals(list("b", "a", "d", "c"), l);
    }

    @Test
    public void moveUpAtTopDoesNothing() {
        List<String> l = list("a", "b", "c");
        assertFalse(ListMoves.moveUp(l, sel("a", "b")));
        assertEquals(list("a", "b", "c"), l);
    }

    @Test
    public void moveUpStuckBlockOnlyLaterItemMoves() {
        List<String> l = list("a", "b", "c", "d");
        ListMoves.moveUp(l, sel("a", "d"));
        assertEquals(list("a", "b", "d", "c"), l);
    }

    @Test
    public void moveDownBlockKeepsShape() {
        List<String> l = list("a", "b", "c", "d");
        ListMoves.moveDown(l, sel("a", "b"));
        assertEquals(list("c", "a", "b", "d"), l);
    }

    @Test
    public void moveDownAtBottomDoesNothing() {
        List<String> l = list("a", "b", "c");
        assertFalse(ListMoves.moveDown(l, sel("c")));
        assertEquals(list("a", "b", "c"), l);
    }

    @Test
    public void emptySelectionDoesNothing() {
        List<String> l = list("a", "b");
        assertFalse(ListMoves.moveUp(l, sel()));
        assertFalse(ListMoves.moveDown(l, sel()));
        assertEquals(list("a", "b"), l);
    }
}
