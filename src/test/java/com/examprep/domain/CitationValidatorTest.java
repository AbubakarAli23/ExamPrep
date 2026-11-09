package com.examprep.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.examprep.ai.validation.CitationValidator;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CitationValidatorTest {

    @Test
    void acceptsSplitChunkBaseReferencesAndRejectsFabrications() {
        Set<String> allowed = CitationValidator.allowedReferences(
                List.of("Page 2", "Page 3 (part 1)", "Slide 7"));

        assertTrue(allowed.contains("page 3"));
        assertEquals(List.of("Page 2", "Slide 7"),
                CitationValidator.filter(List.of("Page 2", "Page 2", "Page 999", "Slide 7", "Appendix B"),
                        allowed));
    }

    @Test
    void normalisesWhitespaceAndCase() {
        Set<String> allowed = CitationValidator.allowedReferences(List.of("Section: Cell Biology"));
        assertEquals("SECTION: cell biology", CitationValidator.firstValid("  SECTION:   cell biology ", allowed));
    }

    @Test
    void rejectsEverythingWhenThereAreNoSourceReferences() {
        assertTrue(CitationValidator.filter(List.of("Page 1"), Set.of()).isEmpty());
        assertFalse(CitationValidator.allowedReferences(List.of("", "  ")).contains(""));
    }
}
