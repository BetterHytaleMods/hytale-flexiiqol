package de.hymods.flexiisgoggles.zoom;

/** Who gets R at the moment: the zoom or the game. */
public final class ZoomRules {
    private ZoomRules() {
    }

    /** Why R does or does not zoom right now. */
    public enum Hold {
        ZOOM,
        /** The zoom is switched off, or its root interaction did not load. */
        OFF,
        /** Another plugin or the game mode has put its own interaction on R, or locked every input (spectators). */
        TAKEN,
        /** A block is in hand, and R also rotates blocks. */
        BLOCK,
        /** The rune in ability slot 3 can be cast with the held weapon, and R casts it. */
        RUNE
    }

    public static Hold decide(boolean zoomOn, boolean taken, boolean holdingBlock, boolean runeReady) {
        if (!zoomOn) {
            return Hold.OFF;
        }
        if (taken) {
            return Hold.TAKEN;
        }
        if (runeReady) {
            return Hold.RUNE;
        }
        if (holdingBlock) {
            return Hold.BLOCK;
        }
        return Hold.ZOOM;
    }
}
