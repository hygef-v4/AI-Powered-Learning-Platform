package edu.aiplatform.quizzes.contract;

import java.util.List;

/**
 * Đề cho người học, không có đáp án. Quiz sinh đề ({@code generated}) chỉ có câu khi đã bốc cho một lượt; ngoài lúc đó
 * {@code questions} rỗng và {@code questionCount} là số câu theo quy tắc.
 */
public record QuizStudentView(QuizView quiz, String instructions, List<QuizQuestionView> questions, int questionCount,
                              boolean generated) {

    public QuizStudentView(QuizView quiz, String instructions, List<QuizQuestionView> questions) {
        this(quiz, instructions, questions, questions.size(), false);
    }
}
