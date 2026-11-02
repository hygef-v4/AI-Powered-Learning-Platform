package edu.aiplatform.quizzes.contract;

import java.util.List;

public record QuizStudentView(QuizView quiz, String instructions, List<QuizQuestionView> questions) {
}
