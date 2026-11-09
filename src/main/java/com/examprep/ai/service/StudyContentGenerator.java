package com.examprep.ai.service;

import com.examprep.document.dto.DocumentDtos.DocumentContext;
import com.examprep.study.domain.content.StudyContent;
import com.examprep.study.dto.StudyPreferences;
import java.util.function.Consumer;

public interface StudyContentGenerator {

    StudyContent.GeneratedPackage generate(
            DocumentContext document,
            StudyPreferences preferences,
            Consumer<String> progress);
}
