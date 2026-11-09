package com.examprep.document.parser;

import java.util.List;

public record ExtractionResult(List<ExtractedSection> sections, int pageCount, int characterCount) {
}
