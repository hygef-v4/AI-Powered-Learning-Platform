package edu.aiplatform.quizzes.service;

import edu.aiplatform.quizzes.model.DrawRule;
import edu.aiplatform.quizzes.repository.IQuestionRepository.PoolEntry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Phân bổ câu cho quy tắc sinh đề (BR-U10-17). Một câu có thể mang nhiều thẻ nên bốc tham lam có thể báo thiếu sai, vì
 * vậy ghép cặp mỗi chỗ trống với một câu: mỗi câu chỉ dùng một lần, không lặp nội dung, ưu tiên câu chưa gặp. Cùng
 * {@code seed} và dữ liệu cho cùng kết quả.
 */
final class QuizPaperPlanner {

    private QuizPaperPlanner() {
    }

    /** {@code picks}: câu của từng quy tắc theo {@code position}; thiếu thì danh sách ngắn hơn số câu cần. */
    record Plan(Map<Integer, List<PoolEntry>> picks) {

        int picked(int position) {
            return picks.getOrDefault(position, List.of()).size();
        }
    }

    static Plan plan(List<DrawRule> rules, List<PoolEntry> pool, Set<java.util.UUID> seenLineages, long seed) {
        Random random = new Random(seed);
        List<PoolEntry> distinct = distinctContent(pool, random);
        List<DrawRule> slots = new ArrayList<>();
        Map<Integer, List<PoolEntry>> candidates = new HashMap<>();
        for (DrawRule rule : rules) {
            List<PoolEntry> matching = new ArrayList<>(distinct.stream().filter(e -> matches(rule, e)).toList());
            Collections.shuffle(matching, random);
            matching.sort(Comparator.comparing((PoolEntry e) -> seenLineages.contains(e.lineageId())));
            candidates.put(rule.position(), matching);
            for (int i = 0; i < rule.questionCount(); i++) {
                slots.add(rule);
            }
        }
        Map<PoolEntry, Integer> owner = new HashMap<>();
        for (int slot = 0; slot < slots.size(); slot++) {
            assign(slot, slots, candidates, owner, new HashSet<>());
        }
        Map<Integer, List<PoolEntry>> picks = new LinkedHashMap<>();
        rules.forEach(rule -> picks.put(rule.position(), new ArrayList<>()));
        owner.forEach((entry, slot) -> picks.get(slots.get(slot).position()).add(entry));
        picks.values().forEach(list -> {
            list.sort(Comparator.comparing(e -> e.id().toString()));
            Collections.shuffle(list, random);
        });
        return new Plan(picks);
    }

    /** Đường tăng của ghép cặp: lấy câu trống, hoặc đẩy chỗ đang giữ câu đó sang câu khác. */
    private static boolean assign(int slot, List<DrawRule> slots, Map<Integer, List<PoolEntry>> candidates,
                                  Map<PoolEntry, Integer> owner, Set<PoolEntry> visited) {
        for (PoolEntry entry : candidates.get(slots.get(slot).position())) {
            if (!visited.add(entry)) {
                continue;
            }
            Integer holder = owner.get(entry);
            if (holder == null || assign(holder, slots, candidates, owner, visited)) {
                owner.put(entry, slot);
                return true;
            }
        }
        return false;
    }

    private static List<PoolEntry> distinctContent(List<PoolEntry> pool, Random random) {
        List<PoolEntry> shuffled = new ArrayList<>(pool);
        shuffled.sort(Comparator.comparing(e -> e.id().toString()));
        Collections.shuffle(shuffled, random);
        Set<String> hashes = new HashSet<>();
        return shuffled.stream().filter(e -> e.contentHash() == null || hashes.add(e.contentHash())).toList();
    }

    static boolean matches(DrawRule rule, PoolEntry entry) {
        String tag = rule.tag().strip().toLowerCase(Locale.ROOT);
        return (rule.difficulty() == null || rule.difficulty() == entry.difficulty())
                && entry.tags().stream().anyMatch(t -> t.strip().toLowerCase(Locale.ROOT).equals(tag));
    }
}
