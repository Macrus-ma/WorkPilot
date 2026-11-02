package com.workpilot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workpilot.mapper.OperationRequestMapper;
import com.workpilot.mapper.TicketActionLogMapper;
import com.workpilot.entity.TicketCategoryEntity;
import com.workpilot.mapper.TicketCategoryMapper;
import com.workpilot.mapper.TicketMapper;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FirstBusinessFlowTest {
    @LocalServerPort
    private int port;
    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private TicketMapper ticketMapper;
    @Autowired
    private TicketActionLogMapper logMapper;
    @Autowired
    private TicketCategoryMapper categoryMapper;
    @Autowired
    private OperationRequestMapper operationMapper;

    @Test
    void customerCanCreateOnlyOwnTicketAndRetrySafely() throws Exception {
        String alice = "alice_" + UUID.randomUUID().toString().substring(0, 8);
        String bob = "bob_" + UUID.randomUUID().toString().substring(0, 8);
        register(alice);
        register(bob);
        String aliceToken = login(alice);
        String bobToken = login(bob);
        MvcResult me = mvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk()).andReturn();
        assertThat(objectMapper.readTree(me.getResponse().getContentAsString())
                .get("loginName").asText()).isEqualTo(alice);
        String requestKey = UUID.randomUUID().toString();
        String body = """
                {"categoryId":1,"title":"无法登录","description":"账号提示错误","priority":"P2"}
                """;

        long beforeTickets = ticketMapper.selectCount(null);
        long beforeLogs = logMapper.selectCount(null);
        MvcResult created = mvc.perform(post("/api/tickets")
                        .header("Authorization", "Bearer " + aliceToken)
                        .header("Idempotency-Key", requestKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated()).andReturn();
        JsonNode createdJson = objectMapper.readTree(created.getResponse().getContentAsString());
        long ticketId = createdJson.get("id").asLong();
        assertThat(createdJson.get("title").asText()).isEqualTo("无法登录");
        assertThat(createdJson.get("status").asText()).isEqualTo("NEW");

        TicketCategoryEntity category = categoryMapper.selectById(1L);
        category.setStatus("INACTIVE");
        categoryMapper.updateById(category);

        long beforeInvalidRequest = operationMapper.selectCount(null);
        mvc.perform(post("/api/tickets")
                        .header("Authorization", "Bearer " + aliceToken)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
        assertThat(operationMapper.selectCount(null)).isEqualTo(beforeInvalidRequest);
        assertThat(ticketMapper.selectCount(null)).isEqualTo(beforeTickets + 1);

        MvcResult replay = mvc.perform(post("/api/tickets")
                        .header("Authorization", "Bearer " + aliceToken)
                        .header("Idempotency-Key", requestKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated()).andReturn();
        assertThat(objectMapper.readTree(replay.getResponse().getContentAsString()).get("id").asLong())
                .isEqualTo(ticketId);
        assertThat(ticketMapper.selectCount(null)).isEqualTo(beforeTickets + 1);
        assertThat(logMapper.selectCount(null)).isEqualTo(beforeLogs + 1);

        MvcResult conflict = mvc.perform(post("/api/tickets")
                        .header("Authorization", "Bearer " + aliceToken)
                        .header("Idempotency-Key", requestKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body.replace("无法登录", "另一个问题")))
                .andExpect(status().isConflict()).andReturn();
        assertThat(objectMapper.readTree(conflict.getResponse().getContentAsString())
                .get("code").asText()).isEqualTo("CONFLICT");
        mvc.perform(get("/api/tickets/{id}", ticketId)
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isNotFound());
        MvcResult bobList = mvc.perform(get("/api/tickets/mine")
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isOk()).andReturn();
        assertThat(objectMapper.readTree(bobList.getResponse().getContentAsString()).size()).isZero();

        mvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk());
        mvc.perform(get("/api/tickets/{id}", ticketId)
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void healthEndpointServesRealHttp() {
        ResponseEntity<String> response = new RestTemplate().getForEntity(
                "http://127.0.0.1:" + port + "/actuator/health", String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).contains("\"status\":\"UP\"");
    }

    @Test
    @WithMockUser(roles = "AGENT")
    void staffRoleCannotUseCustomerTicketApi() throws Exception {
        mvc.perform(get("/api/tickets/categories"))
                .andExpect(status().isForbidden());
    }

    private void register(String loginName) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "loginName", loginName, "password", "Passw0rd!", "displayName", "测试客户"));
        MvcResult response = mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated()).andReturn();
        JsonNode json = objectMapper.readTree(response.getResponse().getContentAsString());
        assertThat(json.get("userType").asText()).isEqualTo("CUSTOMER");
        assertThat(json.has("passwordHash")).isFalse();
    }

    private String login(String loginName) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "loginName", loginName, "password", "Passw0rd!"));
        MvcResult response = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk()).andReturn();
        JsonNode json = objectMapper.readTree(response.getResponse().getContentAsString());
        return json.get("accessToken").asText();
    }

}
