package de.hymods.flexiisgoggles.zoom;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ZoomStepsTest {

    @Test
    void stepsRunFromOneAndAHalfToSixteen() {
        assertEquals(16, ZoomSteps.count());
        assertEquals(1.5f, ZoomSteps.value(0));
        assertEquals(2f, ZoomSteps.value(1));
        assertEquals(12f, ZoomSteps.value(11));
        assertEquals(16f, ZoomSteps.value(99));
    }

    @Test
    void valuesSnapToTheNearestStep() {
        assertEquals(0, ZoomSteps.index(1.4f));
        assertEquals(1, ZoomSteps.index(2.2f));
        assertEquals(4, ZoomSteps.index(5f));
        assertEquals(5f, ZoomSteps.snap(5.3f));
    }

    @Test
    void theHighestStepUnderACap() {
        assertEquals(0, ZoomSteps.maxIndex(1.5f));
        assertEquals(2, ZoomSteps.maxIndex(3f));
        assertEquals(2, ZoomSteps.maxIndex(3.9f));
        assertEquals(11, ZoomSteps.maxIndex(12f));
        assertEquals(15, ZoomSteps.maxIndex(16f));
        assertEquals(0, ZoomSteps.maxIndex(1f));
    }

    @Test
    void labelsAndRootIds() {
        assertEquals("1.5", ZoomSteps.label(1.5f));
        assertEquals("8", ZoomSteps.label(8f));
        assertEquals("Root_FlexiisGoggles_Zoom_1_5x", ZoomAssets.root(1.5f));
        assertEquals("Root_FlexiisGoggles_Zoom_12x", ZoomAssets.root(12f));
    }
}
