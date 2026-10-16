package edu.aiplatform.ai.contract;

import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.identity.contract.ResourceRef;
import java.util.List;
import java.util.UUID;

/**
 * U06 khai báo, U13 cài: người soạn chọn tài liệu tham khảo được phép và lấy chữ đưa vào prompt; U07, U10 gọi.
 * Chưa có U13 thì trả rỗng.
 */
public interface IReferenceMaterial {

    List<SelectableMaterial> listSelectable(Actor actor, ResourceRef scopeRef);

    List<MaterialText> extractText(Actor actor, List<UUID> materialIds, int maxChars);
}
