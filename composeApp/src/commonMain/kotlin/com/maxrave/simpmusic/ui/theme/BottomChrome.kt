package com.maxrave.simpmusic.ui.theme

import androidx.compose.ui.unit.dp

/**
 * The phone's floating bottom chrome, in one place: the mini player, the bottom bar it sits on and
 * the gaps between them. The bars draw these sizes and the pages have to clear them, so both sides
 * read the same numbers here instead of each keeping a copy.
 */
object BottomChrome {
    /** Outer height of the mini player card (`MiniPlayer` is always given exactly this). */
    val MiniPlayerHeight = 56.dp

    /** Height of the tab capsule; the search button beside it is [BottomBarButtonSize]. */
    val BottomBarHeight = 64.dp

    /** The circular search button, the collapsed pill's inner blob and the flat bar's indicator. */
    val BottomBarButtonSize = 56.dp

    /** Vertical gap between the mini player and the bar it floats above. */
    val MiniPlayerToBarGap = 12.dp

    /** Padding the bar keeps below itself, above the system navigation bar. */
    val BottomBarBottomPadding = 8.dp

    /**
     * Everything the bottom chrome stacks above the system navigation bar when the mini player is
     * showing: 56 + 12 + 64 + 8 = 140dp. The flat bar is 8dp shorter (its mini player carries 4dp of
     * its own bottom padding and it adds 4dp on top), so this is the taller of the two.
     */
    val StackHeight = MiniPlayerHeight + MiniPlayerToBarGap + BottomBarHeight + BottomBarBottomPadding
}
