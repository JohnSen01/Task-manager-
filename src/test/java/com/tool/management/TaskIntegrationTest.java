package com.tool.management;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests against the real Spring Boot HTTP stack + H2.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TaskIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private static Long createdTaskId;

    private String tasksUrl() {
        return "http://localhost:" + port + "/tasks";
    }

    @Test
    @Order(0)
    @DisplayName("GET /health → 200 UP")
    void health_returns200() throws Exception {
        ResponseEntity<String> resp = restTemplate.getForEntity("http://localhost:" + port + "/health", String.class);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        JsonNode node = objectMapper.readTree(resp.getBody());
        assertEquals("UP", node.get("status").asText());
    }

    @Test
    @Order(1)
    @DisplayName("POST /tasks → 201 Created")
    void createTask_returns201() throws Exception {
        String body = """
                {"title":"Fix bug #42","description":"NPE in production","priority":"HIGH"}
                """;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> resp = restTemplate.postForEntity(
                tasksUrl(), new HttpEntity<>(body, headers), String.class);

        assertEquals(HttpStatus.CREATED, resp.getStatusCode());
        JsonNode node = objectMapper.readTree(resp.getBody());
        createdTaskId = node.get("id").asLong();
        assertEquals("Fix bug #42", node.get("title").asText());
        assertEquals("TODO", node.get("status").asText());
    }

    @Test
    @Order(2)
    @DisplayName("GET /tasks → 200 with list")
    void getAllTasks_returns200() throws Exception {
        ResponseEntity<String> resp = restTemplate.getForEntity(tasksUrl(), String.class);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        JsonNode node = objectMapper.readTree(resp.getBody());
        assertTrue(node.isArray());
        assertTrue(node.size() > 0);
    }

    @Test
    @Order(3)
    @DisplayName("GET /tasks/{id} → 200 with task")
    void getTaskById_returns200() throws Exception {
        assumeTaskCreated();
        ResponseEntity<String> resp = restTemplate.getForEntity(tasksUrl() + "/" + createdTaskId, String.class);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        JsonNode node = objectMapper.readTree(resp.getBody());
        assertEquals(createdTaskId, node.get("id").asLong());
    }

    @Test
    @Order(4)
    @DisplayName("PUT /tasks/{id} → status transition TODO → IN_PROGRESS")
    void updateTask_validTransition_returns200() throws Exception {
        assumeTaskCreated();
        String body = """
                {"status":"IN_PROGRESS"}
                """;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> resp = restTemplate.exchange(
                tasksUrl() + "/" + createdTaskId,
                HttpMethod.PUT,
                new HttpEntity<>(body, headers),
                String.class);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        JsonNode node = objectMapper.readTree(resp.getBody());
        assertEquals("IN_PROGRESS", node.get("status").asText());
    }

    @Test
    @Order(5)
    @DisplayName("PUT /tasks/{id} → invalid transition returns 409 Conflict")
    void updateTask_invalidTransition_returns409() {
        assumeTaskCreated();
        String body = """
                {"status":"TODO"}
                """;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> resp = restTemplate.exchange(
                tasksUrl() + "/" + createdTaskId,
                HttpMethod.PUT,
                new HttpEntity<>(body, headers),
                String.class);

        assertEquals(HttpStatus.CONFLICT, resp.getStatusCode());
    }

    @Test
    @Order(6)
    @DisplayName("DELETE /tasks/{id} → 204 No Content")
    void deleteTask_returns204() {
        assumeTaskCreated();
        ResponseEntity<String> resp = restTemplate.exchange(
                tasksUrl() + "/" + createdTaskId,
                HttpMethod.DELETE,
                null,
                String.class);
        assertEquals(HttpStatus.NO_CONTENT, resp.getStatusCode());
    }

    @Test
    @Order(7)
    @DisplayName("GET /tasks/{id} after delete → 404 Not Found")
    void getDeletedTask_returns404() {
        assumeTaskCreated();
        ResponseEntity<String> resp = restTemplate.getForEntity(tasksUrl() + "/" + createdTaskId, String.class);
        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
    }

    private void assumeTaskCreated() {
        Assumptions.assumeTrue(createdTaskId != null, "Previous test must have created a task");
    }
}
