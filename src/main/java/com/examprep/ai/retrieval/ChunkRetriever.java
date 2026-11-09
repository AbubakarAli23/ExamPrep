package com.examprep.ai.retrieval;

import com.examprep.document.dto.DocumentDtos.DocumentChunkDto;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Lightweight retrieval for the tutor: scores source chunks by query-term overlap. No
 * vector database, no external search. Good enough for the single-document scope.
 */
@Component
public class ChunkRetriever {

    private static final Set<String> STOP_WORDS = Set.of(
            "the", "and", "for", "are", "but", "not", "you", "all", "can", "her", "was", "one", "our", "out",
            "his", "has", "have", "had", "how", "its", "may", "who", "did", "does", "what", "when", "where",
            "which", "why", "this", "that", "these", "those", "with", "from", "into", "about", "explain",
            "please", "give", "tell", "more", "example", "examples", "using", "based", "between", "difference",
            "mean", "means", "doesn", "t", "s", "is", "of", "in", "to", "a", "an", "on", "it", "be",
            "as", "at", "by", "or", "if", "we", "they", "them", "their", "there", "would", "should", "could");

    public List<DocumentChunkDto> retrieve(List<DocumentChunkDto> chunks, String question, int limit, int characterBudget) {
        if (chunks == null || chunks.isEmpty()) {
            return List.of();
        }
        Set<String> terms = tokenize(question);
        List<Scored> scored = new ArrayList<>();
        for (DocumentChunkDto chunk : chunks) {
            scored.add(new Scored(chunk, score(chunk.text(), terms)));
        }
        scored.sort(Comparator.comparingDouble(Scored::score).reversed());

        List<DocumentChunkDto> selected = new ArrayList<>();
        Set<String> usedReferences = new LinkedHashSet<>();
        int characters = 0;
        for (Scored candidate : scored) {
            if (selected.size() >= Math.max(1, limit)) {
                break;
            }
            if (characters + candidate.chunk().text().length() > characterBudget && !selected.isEmpty()) {
                continue;
            }
            selected.add(candidate.chunk());
            usedReferences.add(candidate.chunk().reference());
            characters += candidate.chunk().text().length();
        }
        // If nothing matched, still give the tutor the opening material rather than nothing.
        if (selected.isEmpty()) {
            selected.addAll(chunks.subList(0, Math.min(limit, chunks.size())));
        }
        return selected;
    }

    private double score(String text, Set<String> terms) {
        if (terms.isEmpty()) {
            return 0d;
        }
        String lower = text.toLowerCase(Locale.ROOT);
        double score = 0d;
        for (String term : terms) {
            int index = 0;
            int occurrences = 0;
            while ((index = lower.indexOf(term, index)) >= 0) {
                occurrences++;
                index += term.length();
                if (occurrences >= 12) {
                    break;
                }
            }
            if (occurrences > 0) {
                score += 1d + Math.log(occurrences);
            }
        }
        return score;
    }

    private Set<String> tokenize(String text) {
        Set<String> tokens = new LinkedHashSet<>();
        if (text == null) {
            return tokens;
        }
        for (String token : text.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}]+")) {
            if (token.length() >= 3 && !STOP_WORDS.contains(token)) {
                tokens.add(token);
            }
        }
        return tokens;
    }

    private record Scored(DocumentChunkDto chunk, double score) {
    }
}
