package com.lsoftware.inventoco.infrastructure.mcp;

import com.lsoftware.inventoco.entity.exception.DomainException;
import com.lsoftware.inventoco.entity.profile.model.UserProfile;
import com.lsoftware.inventoco.infrastructure.config.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * MCP server (Streamable HTTP, stateless, JSON responses) at https://host/inventoco-api/mcp.
 * Any user can connect with their authco token (MCP clients get one through dynamic client
 * registration); every tool works on that user's inventory only.
 */
@Slf4j
@RestController
@RequestMapping("/mcp")
@RequiredArgsConstructor
@Tag(name = "MCP", description = "Inventory assistant: stock, prices, shopping list and forecasts")
@SecurityRequirement(name = "bearerAuth")
public class McpController {

    static final List<String> PROTOCOL_VERSIONS = List.of("2025-06-18", "2025-03-26", "2024-11-05");

    private static final String INSTRUCTIONS = """
            Tools over the user's own home or business inventory: items and their stock, consumption, \
            purchases, prices per supplier, prices other users shared publicly, a shopping list and forecasts \
            of when each item runs out. Find items with list_items and use their ids. Quantities can be given \
            in any unit of the item's kind (kg, g, lb, l, ml, unit, dozen...). Amounts are in the user's \
            currency (get_profile). Dates without a time zone are read in the user's time zone. Before recording \
            something, make sure of the item, quantity and unit; never invent prices.""";

    private final CurrentUser currentUser;
    private final InventoryTools inventoryTools;
    private final JsonMapper json;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "JSON-RPC message (or batch) of the MCP protocol")
    public ResponseEntity<Object> handle(@RequestBody Object body, @AuthenticationPrincipal Jwt jwt) {
        UserProfile user = currentUser.of(jwt);
        if (body instanceof List<?> batch) {
            List<Object> responses = new ArrayList<>();
            for (Object message : batch) {
                Map<String, Object> response = dispatch(message, user);
                if (response != null) responses.add(response);
            }
            return responses.isEmpty() ? ResponseEntity.accepted().build() : ResponseEntity.ok(responses);
        }
        Map<String, Object> response = dispatch(body, user);
        return response == null ? ResponseEntity.accepted().build() : ResponseEntity.ok(response);
    }

    /** No server-initiated stream: the spec allows 405 here. */
    @GetMapping
    public ResponseEntity<Void> stream() {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).build();
    }

    @SuppressWarnings("unchecked")
    Map<String, Object> dispatch(Object raw, UserProfile user) {
        if (!(raw instanceof Map<?, ?> message) || !"2.0".equals(message.get("jsonrpc"))) {
            return error(null, -32600, "Invalid JSON-RPC request");
        }
        Object id = message.get("id");
        String method = String.valueOf(message.get("method"));
        // Notifications (no id) and responses get no answer
        if (id == null) return null;
        Map<String, Object> params = message.get("params") instanceof Map<?, ?> p ? (Map<String, Object>) p : Map.of();
        try {
            return switch (method) {
                case "initialize" -> result(id, initialize(params));
                case "ping" -> result(id, Map.of());
                case "tools/list" -> result(id, Map.of("tools", inventoryTools.tools().stream().map(McpTool::descriptor).toList()));
                case "tools/call" -> result(id, call(params, user));
                default -> error(id, -32601, "Method not found: " + method);
            };
        } catch (RuntimeException e) {
            log.warn("MCP {} failed", method, e);
            return error(id, -32603, "Internal error");
        }
    }

    private Map<String, Object> initialize(Map<String, Object> params) {
        Object requested = params.get("protocolVersion");
        String version = PROTOCOL_VERSIONS.contains(requested) ? (String) requested : PROTOCOL_VERSIONS.getFirst();
        return Map.of(
                "protocolVersion", version,
                "capabilities", Map.of("tools", Map.of("listChanged", false)),
                "serverInfo", Map.of("name", "inventoco", "title", "Inventoco", "version", "1.0.0"),
                "instructions", INSTRUCTIONS);
    }

    /** Tool errors go back as results with isError, so the model can read and fix them. */
    @SuppressWarnings("unchecked")
    private Map<String, Object> call(Map<String, Object> params, UserProfile user) {
        String name = String.valueOf(params.get("name"));
        Map<String, Object> arguments = params.get("arguments") instanceof Map<?, ?> a ? (Map<String, Object>) a : Map.of();
        McpTool tool = inventoryTools.tools().stream().filter(t -> t.name().equals(name)).findFirst().orElse(null);
        if (tool == null) return toolError("Unknown tool: " + name);
        try {
            Object value = tool.handler().apply(new McpArgs(arguments), user);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("content", List.of(Map.of("type", "text", "text", json.writeValueAsString(value))));
            result.put("isError", false);
            return result;
        } catch (DomainException e) {
            return toolError(e.getCode() + ": " + e.getMessage());
        } catch (IllegalArgumentException e) {
            return toolError("INVALID_ARGUMENT: " + e.getMessage());
        }
    }

    private static Map<String, Object> toolError(String message) {
        return Map.of("content", List.of(Map.of("type", "text", "text", message)), "isError", true);
    }

    private static Map<String, Object> result(Object id, Object result) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("jsonrpc", "2.0");
        m.put("id", id);
        m.put("result", result);
        return m;
    }

    private static Map<String, Object> error(Object id, int code, String message) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("jsonrpc", "2.0");
        m.put("id", id);
        m.put("error", Map.of("code", code, "message", message));
        return m;
    }
}
