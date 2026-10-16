package edu.aiplatform.ai.contract;

import edu.aiplatform.identity.contract.Role;
import java.util.Set;

/** {@code parameterSchema} là JSON Schema của tham số; tool {@code WRITE} cần credential {@code READ_WRITE}. */
public record McpToolDefinition(String name, String description, String parameterSchema, McpToolKind kind,
                                Set<Role> allowedRoles) {
}
