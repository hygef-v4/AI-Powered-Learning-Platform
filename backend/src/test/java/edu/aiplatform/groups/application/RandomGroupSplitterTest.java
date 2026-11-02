package edu.aiplatform.groups.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class RandomGroupSplitterTest {

    @ParameterizedTest
    @CsvSource({"7,4,2", "20,5,4", "21,5,5", "1,2,1", "40,20,2"})
    void groupsDifferByAtMostOneAndEveryoneIsPlacedOnce(int students, int maxSize, int expectedGroups) {
        List<UUID> ids = IntStream.range(0, students).mapToObj(i -> UUID.randomUUID()).toList();

        var split = RandomGroupSplitter.split(ids, maxSize, new Random(students));

        assertThat(split).hasSize(expectedGroups);
        var sizes = split.stream().map(s -> s.members().size()).toList();
        assertThat(sizes.stream().mapToInt(Integer::intValue).max().orElse(0)
                - sizes.stream().mapToInt(Integer::intValue).min().orElse(0)).isLessThanOrEqualTo(1);
        assertThat(sizes).allMatch(s -> s <= maxSize);
        assertThat(split.stream().flatMap(s -> s.members().stream()).toList()).containsExactlyInAnyOrderElementsOf(ids);
        split.forEach(s -> assertThat(s.members()).contains(s.leaderId()));
    }

    @ParameterizedTest
    @CsvSource({"1", "21"})
    void sizeOutsideTwoToTwentyIsRejected(int maxSize) {
        assertThatThrownBy(() -> RandomGroupSplitter.split(List.of(UUID.randomUUID()), maxSize, new Random()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
