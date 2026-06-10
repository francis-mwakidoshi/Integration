package com.frank.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TextUtilsTest {

    @Test
    void convertsToSentenceCase() {
        assertEquals("Kenya", TextUtils.toSentenceCase("kenya"));
        assertEquals("Tanzania", TextUtils.toSentenceCase("TANZANIA"));
        assertEquals("South africa", TextUtils.toSentenceCase("south africa"));
    }
}
