package edu.aiplatform.materials.service;

import edu.aiplatform.ai.contract.*;
import edu.aiplatform.identity.contract.Actor;
import java.util.*;
import org.springframework.stereotype.Component;

@Component
public class MaterialMcpQueryTools implements IMcpToolProvider {
    private final MaterialQueryService materialQueryService;

    public MaterialMcpQueryTools(MaterialQueryService materialQueryService) {
        this.materialQueryService = materialQueryService;
    }

    @Override
    public List<McpToolDefinition> tools() {
        return List.of(
                McpSchemas.read(
                        "list_materials",
                        "Tìm học liệu được phép; kiểm textStatus trước khi chọn làm nguồn.",
                        McpSchemas.SCOPE
                                + ","
                                + McpSchemas.PAGE
                                + ",\"query\":"
                                + McpSchemas.TEXT));
    }

    @Override
    public McpToolResult call(Actor actor, String name, Map<String, Object> args) {
        McpInput.requireTeaching(actor);
        if (!"list_materials".equals(name)) throw McpInput.createInvalidArguments();
        var in = new McpInput(args);
        int page = in.readPage(), size = in.readSize();
        if (in.has("classId") == in.has("subjectId")) throw McpInput.createInvalidArguments();
        String query = in.readText("query", "").toLowerCase(Locale.ROOT);
        var rows =
                in.has("classId")
                        ? materialQueryService.listClassMaterials(
                                actor, in.readUuid("classId"), false, null)
                        : materialQueryService.listSubjectMaterials(actor, in.readUuid("subjectId"));
        var items =
                rows.stream()
                        .filter(m -> m.title().toLowerCase(Locale.ROOT).contains(query))
                        .map(
                                m ->
                                        Map.of(
                                                "materialId",
                                                m.id(),
                                                "title",
                                                m.title(),
                                                "kind",
                                                m.kind(),
                                                "status",
                                                m.status(),
                                                "versionNo",
                                                m.currentVersionNo(),
                                                "textStatus",
                                                m.current().textStatus()))
                        .toList();
        return new McpToolResult(false, McpInput.paginateItems(items, page, size));
    }
}
