package com.examprep.study.repository;

import com.examprep.study.domain.QuestionSection;
import com.examprep.study.domain.QuizQuestion;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuizQuestionRepository extends JpaRepository<QuizQuestion, UUID> {

    List<QuizQuestion> findByStudyPackageIdAndSectionOrderByPositionAsc(UUID studyPackageId, QuestionSection section);

    long countByStudyPackageIdAndSection(UUID studyPackageId, QuestionSection section);

    void deleteByStudyPackageId(UUID studyPackageId);
}
