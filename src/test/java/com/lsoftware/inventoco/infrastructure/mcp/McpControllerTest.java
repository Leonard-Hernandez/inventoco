package com.lsoftware.inventoco.infrastructure.mcp;

import com.lsoftware.inventoco.entity.exception.DomainException;
import com.lsoftware.inventoco.infrastructure.config.security.CurrentUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static com.lsoftware.inventoco.usecase.Fixtures.ANA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class McpControllerTest {

    private final CurrentUser currentUser = mock(CurrentUser.class);
    private final InventoryTools inventoryTools = mock(InventoryTools.class);
    private final McpController controller = new McpController(currentUser, inventoryTools, JsonMapper.builder().build());
    private final Jwt jwt = Jwt.withTokenValue("token").header("alg", "RS256").subject("sub-ana")
            .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60)).build();
    private final AtomicReference<String> seenUser = new AtomicReference<>();

    @BeforeEach
    void setUp() {
        when(currentUser.of(jwt)).thenReturn(ANA);
        when(inventoryTools.tools()).thenReturn(List.of(
                new McpTool("echo", "Echo", "Returns its input", Schemas.object().string("text", "Text", true).build(), true,
                        (args, user) -> {
                            seenUser.set(user.authcoSub());
                            return Map.of("echo", args.requiredString("text"));
                        }),
                new McpTool("fails", "Fails", "Always fails", Schemas.object().build(), false,
                        (args, user) -> { throw new DomainException("ITEM_NOT_FOUND", "Item not found"); })));
    }

    @Test
    @SuppressWarnings("unchecked")
    void initializeNegotiatesTheProtocolVersion() {
        var response = (Map<String, Object>) controller.handle(rpc(1, "initialize", Map.of("protocolVersion", "2025-03-26")), jwt).getBody();

        var result = (Map<String, Object>) response.get("result");
        assertThat(result.get("protocolVersion")).isEqualTo("2025-03-26");
        assertThat(result).containsKey("instructions");
    }

    @Test
    void notificationsAreAcceptedWithoutBody() {
        var response = controller.handle(Map.of("jsonrpc", "2.0", "method", "notifications/initialized"), jwt);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response.getBody()).isNull();
    }

    @Test
    @SuppressWarnings("unchecked")
    void toolsAreListedWithTheirReadOnlyHint() {
        var response = (Map<String, Object>) controller.handle(rpc(2, "tools/list", Map.of()), jwt).getBody();

        var tools = (List<Map<String, Object>>) ((Map<String, Object>) response.get("result")).get("tools");
        assertThat(tools).extracting(t -> ((Map<String, Object>) t.get("annotations")).get("readOnlyHint"))
                .containsExactly(true, false);
    }

    @Test
    @SuppressWarnings("unchecked")
    void toolCallsRunAsTheTokensUser() {
        var response = (Map<String, Object>) controller.handle(
                rpc(3, "tools/call", Map.of("name", "echo", "arguments", Map.of("text", "hola"))), jwt).getBody();

        var result = (Map<String, Object>) response.get("result");
        assertThat(result.get("isError")).isEqualTo(false);
        assertThat(((List<Map<String, Object>>) result.get("content")).getFirst().get("text")).isEqualTo("{\"echo\":\"hola\"}");
        assertThat(seenUser.get()).isEqualTo("sub-ana");
    }

    @Test
    @SuppressWarnings("unchecked")
    void domainErrorsComeBackAsToolErrorsTheModelCanRead() {
        var response = (Map<String, Object>) controller.handle(rpc(4, "tools/call", Map.of("name", "fails")), jwt).getBody();

        var result = (Map<String, Object>) response.get("result");
        assertThat(result.get("isError")).isEqualTo(true);
        assertThat(result.toString()).contains("ITEM_NOT_FOUND");
    }

    @Test
    @SuppressWarnings("unchecked")
    void unknownMethodsAreJsonRpcErrors() {
        var response = (Map<String, Object>) controller.handle(rpc(5, "resources/list", Map.of()), jwt).getBody();

        assertThat(((Map<String, Object>) response.get("error")).get("code")).isEqualTo(-32601);
    }

    private static Map<String, Object> rpc(int id, String method, Map<String, Object> params) {
        return Map.of("jsonrpc", "2.0", "id", id, "method", method, "params", params);
    }
}
