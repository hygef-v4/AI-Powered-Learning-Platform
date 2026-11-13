package edu.aiplatform.quizzes.service;

import static org.assertj.core.api.Assertions.assertThat;

import edu.aiplatform.quizzes.model.Difficulty;
import edu.aiplatform.quizzes.model.DrawRule;
import edu.aiplatform.quizzes.repository.IQuestionRepository.PoolEntry;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class QuizPaperPlannerTest {

    private final UUID quiz = UUID.randomUUID();

    private DrawRule rule(int position, String tag, Difficulty difficulty, int count) {
        return new DrawRule(quiz, position, tag, difficulty, count, BigDecimal.ONE);
    }

    private static PoolEntry entry(String hash, Difficulty difficulty, String... tags) {
        UUID id = UUID.randomUUID();
        return new PoolEntry(id, id, List.of(tags), difficulty, hash);
    }

    private static List<PoolEntry> pool(String tag, int size) {
        return new ArrayList<>(IntStream.range(0, size)
                .mapToObj(i -> entry(tag + i, Difficulty.EASY, tag)).toList());
    }

    @Test
    void picksExactCountsPerTagFromTheExampleInTheFeedback() {
        List<PoolEntry> all = new ArrayList<>(pool("t1", 6));
        all.addAll(pool("t2", 6));
        all.addAll(pool("t3", 9));
        var plan = QuizPaperPlanner.plan(List.of(rule(1, "t1", null, 4), rule(2, "t2", null, 5), rule(3, "t3", null, 7)),
                all, Set.of(), 7L);
        assertThat(plan.picked(1)).isEqualTo(4);
        assertThat(plan.picked(2)).isEqualTo(5);
        assertThat(plan.picked(3)).isEqualTo(7);
        assertThat(plan.picks().get(1)).allSatisfy(e -> assertThat(e.tags()).contains("t1"));
    }

    @Test
    void differentSeedsGiveDifferentPapersAndSameSeedRepeats() {
        List<PoolEntry> all = pool("t", 40);
        List<DrawRule> rules = List.of(rule(1, "t", null, 10));
        var first = QuizPaperPlanner.plan(rules, all, Set.of(), 1L).picks().get(1);
        assertThat(QuizPaperPlanner.plan(rules, all, Set.of(), 1L).picks().get(1)).isEqualTo(first);
        assertThat(QuizPaperPlanner.plan(rules, all, Set.of(), 2L).picks().get(1)).isNotEqualTo(first);
    }

    @Test
    void overlappingTagsAreMatchedInsteadOfReportingAFalseShortage() {
        PoolEntry both = entry("x", Difficulty.EASY, "a", "b");
        PoolEntry onlyA = entry("y", Difficulty.EASY, "a");
        // Mọi thứ tự bốc đều phải đủ: quy tắc b chỉ có thể dùng câu mang cả hai thẻ.
        for (long seed = 0; seed < 20; seed++) {
            var plan = QuizPaperPlanner.plan(List.of(rule(1, "a", null, 1), rule(2, "b", null, 1)), List.of(both, onlyA),
                    Set.of(), seed);
            assertThat(plan.picks().get(2)).containsExactly(both);
            assertThat(plan.picks().get(1)).containsExactly(onlyA);
        }
    }

    @Test
    void aQuestionIsNeverUsedTwiceAndShortageIsReported() {
        PoolEntry both = entry("x", Difficulty.EASY, "a", "b");
        var plan = QuizPaperPlanner.plan(List.of(rule(1, "a", null, 1), rule(2, "b", null, 1)), List.of(both), Set.of(), 3L);
        assertThat(plan.picked(1) + plan.picked(2)).isEqualTo(1);
    }

    @Test
    void identicalContentCountsOnceAndDifficultyAndCaseAreRespected() {
        var duplicates = QuizPaperPlanner.plan(List.of(rule(1, "a", null, 2)),
                List.of(entry("same", Difficulty.EASY, "a"), entry("same", Difficulty.EASY, "a")), Set.of(), 1L);
        assertThat(duplicates.picked(1)).isEqualTo(1);
        var hard = QuizPaperPlanner.plan(List.of(rule(1, " CHƯƠNG 1 ", Difficulty.HARD, 5)),
                List.of(entry("h", Difficulty.HARD, "chương 1"), entry("e", Difficulty.EASY, "chương 1")), Set.of(), 1L);
        assertThat(hard.picks().get(1)).singleElement().satisfies(e -> assertThat(e.contentHash()).isEqualTo("h"));
    }

    @Test
    void prefersQuestionsNotSeenInEarlierAttempts() {
        List<PoolEntry> all = pool("t", 20);
        Set<UUID> seen = new HashSet<>(all.subList(0, 10).stream().map(PoolEntry::lineageId).toList());
        for (long seed = 0; seed < 20; seed++) {
            var plan = QuizPaperPlanner.plan(List.of(rule(1, "t", null, 10)), all, seen, seed);
            assertThat(plan.picks().get(1)).noneMatch(e -> seen.contains(e.lineageId()));
        }
        var reuse = QuizPaperPlanner.plan(List.of(rule(1, "t", null, 15)), all, seen, 5L);
        assertThat(reuse.picked(1)).as("hết câu mới thì dùng lại câu đã gặp").isEqualTo(15);
    }
}
