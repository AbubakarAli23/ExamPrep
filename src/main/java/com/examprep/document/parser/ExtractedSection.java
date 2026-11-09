package com.examprep.document.parser;

import java.util.List;

/**
 * A structural unit of extracted text, for example a PDF page or a presentation slide.
 */
public record ExtractedSection(String reference, String text) {

    public static ExtractionResult result(List<ExtractedSection> sections, int pageCount) {
        int characters = sections.stream().mapToInt(section -> section.text().length()).sum();
        return new ExtractionResult(sections, pageCount, characters);
    }
}
