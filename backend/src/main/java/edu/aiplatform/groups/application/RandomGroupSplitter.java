package edu.aiplatform.groups.application;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.random.RandomGenerator;

/**
 * Chia ngẫu nhiên sinh viên chưa có nhóm (BR-U12-03): sĩ số tối đa 2–20, các nhóm mới chênh nhau tối đa một người,
 * trưởng nhóm chọn ngẫu nhiên. Không đụng nhóm đã có; kết quả chỉ là bản xem trước.
 */
public final class RandomGroupSplitter {

    public static final int MIN_SIZE = 2;
    public static final int MAX_SIZE = 20;

    private RandomGroupSplitter() {
    }

    public record Split(List<UUID> members, UUID leaderId) {
    }

    public static List<Split> split(List<UUID> students, int maxSize, RandomGenerator random) {
        if (maxSize < MIN_SIZE || maxSize > MAX_SIZE) {
            throw new IllegalArgumentException("maxSize");
        }
        if (students.isEmpty()) {
            return List.of();
        }
        List<UUID> shuffled = new ArrayList<>(students);
        Collections.shuffle(shuffled, random instanceof Random r ? r : new Random(random.nextLong()));
        int groups = (shuffled.size() + maxSize - 1) / maxSize;
        int base = shuffled.size() / groups;
        int extra = shuffled.size() % groups;
        List<Split> result = new ArrayList<>(groups);
        int from = 0;
        for (int i = 0; i < groups; i++) {
            int size = base + (i < extra ? 1 : 0);
            List<UUID> members = List.copyOf(shuffled.subList(from, from + size));
            from += size;
            result.add(new Split(members, members.get(random.nextInt(members.size()))));
        }
        return result;
    }
}
