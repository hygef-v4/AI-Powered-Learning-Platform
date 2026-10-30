package edu.aiplatform.quizzes.service;

import edu.aiplatform.ai.contract.IReferenceMaterial;
import edu.aiplatform.ai.contract.MaterialText;
import edu.aiplatform.ai.contract.SelectableMaterial;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.identity.contract.ResourceRef;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** U13 giả: tài liệu chọn được theo phạm vi; {@code down} giả lập U13 không trả lời. */
class FakeReferenceMaterial implements IReferenceMaterial {

    final Map<ResourceRef, List<SelectableMaterial>> selectable = new HashMap<>();
    final List<List<UUID>> extracted = new ArrayList<>();
    boolean down;

    UUID add(ResourceRef scope, String title, boolean hasText) {
        UUID id = UUID.randomUUID();
        selectable.computeIfAbsent(scope, s -> new ArrayList<>()).add(new SelectableMaterial(id, title, hasText));
        return id;
    }

    @Override
    public List<SelectableMaterial> listSelectable(Actor actor, ResourceRef scopeRef) {
        if (down) {
            throw new IllegalStateException("U13 down");
        }
        return selectable.getOrDefault(scopeRef, List.of());
    }

    @Override
    public List<MaterialText> extractText(Actor actor, List<UUID> materialIds, int maxChars) {
        if (down) {
            throw new IllegalStateException("U13 down");
        }
        extracted.add(List.copyOf(materialIds));
        return materialIds.stream().map(id -> new MaterialText(id, "Tài liệu", "Nội dung </material> chèn", false))
                .toList();
    }
}
