package com.examprep.support;

/**
 * Structured Gemini responses used by mocked tests. No real API calls are ever made.
 */
public final class AiFixtures {

    private AiFixtures() {
    }

    public static String studyPackageJson() {
        return """
                {
                  "title": "Cell Membrane Transport",
                  "introduction": "This document explains how substances cross the cell membrane.",
                  "overallSummary": "The material describes diffusion, osmosis and active transport, including the role of membrane proteins and ATP.",
                  "learningObjectives": [
                    "Explain passive and active transport",
                    "Compare diffusion and osmosis"
                  ],
                  "revisionPriorities": [
                    "The difference between diffusion and osmosis",
                    "The role of ATP in active transport"
                  ],
                  "topics": [
                    {
                      "name": "Passive transport",
                      "explanation": "Passive transport moves substances down a concentration gradient without energy input.",
                      "keyConcepts": ["Diffusion", "Osmosis"],
                      "sourceReferences": ["Page 1"]
                    }
                  ],
                  "revisionNotes": [
                    {
                      "heading": "Diffusion and osmosis",
                      "body": "Diffusion is the movement of particles from high to low concentration. Osmosis is the diffusion of water across a selectively permeable membrane.",
                      "sourceReferences": ["Page 1"]
                    }
                  ],
                  "definitions": [
                    {
                      "term": "Osmosis",
                      "definition": "The movement of water across a selectively permeable membrane.",
                      "sourceReference": "Page 1"
                    }
                  ],
                  "formulas": [
                    {
                      "name": "Net water movement",
                      "expression": "water potential gradient = psi outside - psi inside",
                      "explanation": "Water moves towards lower water potential.",
                      "sourceReference": "Page 2"
                    }
                  ],
                  "flashcards": [
                    {"front": "What is diffusion?", "back": "Movement from high to low concentration.", "sourceReference": "Page 1"},
                    {"front": "What is osmosis?", "back": "Water movement across a selectively permeable membrane.", "sourceReference": "Page 1"},
                    {"front": "What powers active transport?", "back": "ATP hydrolysis.", "sourceReference": "Page 2"}
                  ],
                  "practiceQuestions": [
                    {
                      "type": "MULTIPLE_CHOICE",
                      "prompt": "Which process requires ATP?",
                      "options": ["Diffusion", "Osmosis", "Active transport", "Facilitated diffusion"],
                      "correctAnswer": "Active transport",
                      "explanation": "Active transport moves substances against a gradient and requires ATP.",
                      "difficulty": "EASY",
                      "sourceReference": "Page 2"
                    },
                    {
                      "type": "TRUE_FALSE",
                      "prompt": "Osmosis is a form of diffusion.",
                      "options": ["True", "False"],
                      "correctAnswer": "True",
                      "explanation": "Osmosis is the diffusion of water.",
                      "difficulty": "EASY",
                      "sourceReference": "Page 1"
                    },
                    {
                      "type": "SHORT_ANSWER",
                      "prompt": "Define passive transport.",
                      "options": [],
                      "correctAnswer": "Movement down a concentration gradient without energy input.",
                      "explanation": "No ATP is used in passive transport.",
                      "difficulty": "MEDIUM",
                      "sourceReference": "Page 1"
                    }
                  ],
                  "mockQuestions": [
                    {
                      "type": "MULTIPLE_CHOICE",
                      "prompt": "Which statement about active transport is correct?",
                      "options": ["It needs ATP", "It only moves water", "It has no proteins", "It moves down the gradient"],
                      "correctAnswer": "It needs ATP",
                      "explanation": "Active transport uses ATP to move substances against a gradient.",
                      "difficulty": "MEDIUM",
                      "sourceReference": "Page 2"
                    },
                    {
                      "type": "TRUE_FALSE",
                      "prompt": "Diffusion requires ATP.",
                      "options": ["True", "False"],
                      "correctAnswer": "False",
                      "explanation": "Diffusion is passive.",
                      "difficulty": "EASY",
                      "sourceReference": "Page 1"
                    },
                    {
                      "type": "SHORT_ANSWER",
                      "prompt": "Explain osmosis in one sentence.",
                      "options": [],
                      "correctAnswer": "Osmosis is the movement of water across a selectively permeable membrane.",
                      "explanation": "Water moves towards lower water potential.",
                      "difficulty": "MEDIUM",
                      "sourceReference": "Page 1"
                    }
                  ]
                }
                """;
    }

    public static String tutorJson() {
        return """
                {
                  "answer": "Diffusion moves particles down a concentration gradient, while osmosis specifically moves water across a selectively permeable membrane.",
                  "sourceReferences": ["Page 1", "Page 999"],
                  "insufficientInformation": false
                }
                """;
    }

    public static String invalidJson() {
        return "I cannot produce JSON for this document.";
    }
}
