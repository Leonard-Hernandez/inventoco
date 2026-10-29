package com.lsoftware.inventoco.infrastructure.mcp;

import com.lsoftware.inventoco.entity.profile.model.UserProfile;

import java.util.Map;
import java.util.function.BiFunction;

/**
 * One tool the assistant can call. inputSchema is a JSON Schema object; handler receives the
 * call arguments and the user the token belongs to, and returns any value, sent back as JSON.
 */
public record McpTool(String name, String title, String description, Map<String, Object> inputSchema,
                      boolean readOnly, BiFunction<McpArgs, UserProfile, Object> handler) {

    public Map<String, Object> descriptor() {
        return Map.of(
                "name", name,
                "title", title,
                "description", description,
                "inputSchema", inputSchema,
                "annotations", Map.of("readOnlyHint", readOnly, "destructiveHint", false, "openWorldHint", false));
    }
}
