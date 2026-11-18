package edu.aiplatform.ai.contract;

import static org.assertj.core.api.Assertions.*;
import edu.aiplatform.shared.error.ApiException;
import java.util.*;
import org.junit.jupiter.api.Test;

class McpInputTest {
    @Test void paginationRejectsFractionsOverflowAndStringsInsteadOfCoercingThem() {
        for(Object value:List.of(-1,1.5,"1",Long.MAX_VALUE))
            assertThatThrownBy(()->new McpInput(Map.of("page",value)).readPage()).isInstanceOf(ApiException.class);
        assertThat(new McpInput(Map.of()).readSize()).isEqualTo(20);
        assertThat(McpInput.paginateItems(List.of("a","b"),1,1)).containsEntry("total",2).containsEntry("items",List.of("b"));
    }
}
