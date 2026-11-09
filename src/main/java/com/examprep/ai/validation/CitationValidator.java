package com.examprep.ai.validation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Validates citations returned by the model against the references that actually exist in
 * the source document. Anything else is discarded instead of being displayed.
 */
public final class CitationValidator {

    private CitationValidator() {
    }

    /**
     * Builds the set of acceptable references, including the base label of a split chunk
     * ("Page 3 (part 2)" also accepts "Page 3").
     */
    public static Set<String> allowedReferences(Collection<String> chunkReferences) {
        Set<String> allowed = new LinkedHashSet<>();
        for (String reference : chunkReferences) {
            if (reference == null || reference.isBlank()) {
                continue;
            }
            allowed.add(normalize(reference));
            int part = reference.indexOf(" (part ");
            if (part > 0) {
                allowed.add(normalize(reference.substring(0, part)));
            }
        }
        return allowed;
    }

    public static List<String> filter(Collection<String> references, Set<String> allowed) {
        List<String> valid = new ArrayList<>();
        if (references == null) {
            return valid;
        }
        for (String reference : references) {
            if (reference == null) {
                continue;
            }
            String cleaned = clean(reference);
            if (allowed.contains(normalize(cleaned)) && !valid.contains(cleaned)) {
                valid.add(cleaned);
            }
        }
        return valid;
    }

    public static String firstValid(String reference, Set<String> allowed) {
        List<String> filtered = filter(reference == null ? List.of() : List.of(reference), allowed);
        return filtered.isEmpty() ? null : filtered.get(0);
    }

    private static String clean(String reference) {
        return reference.replace('\u00A0', ' ').trim().replaceAll("\\s{2,}", " ");
    }

    public static String normalize(String reference) {
        return reference.replace('\u00A0', ' ').trim().replaceAll("\\s{2,}", " ").toLowerCase(Locale.ROOT);
    }
}
