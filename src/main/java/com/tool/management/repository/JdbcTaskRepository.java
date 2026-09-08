package com.tool.management.repository;

import com.tool.management.domain.Task;
import com.tool.management.domain.TaskPriority;
import com.tool.management.domain.TaskStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC-based TaskRepository that works with both H2 and MySQL.
 * Detects the database dialect at startup and adapts DDL and sequence-reset accordingly.
 * Demonstrates raw SQL / JDBC skills without ORM magic.
 */
@Repository
public class JdbcTaskRepository implements TaskRepository {

    private static final Logger log = LoggerFactory.getLogger(JdbcTaskRepository.class);
    private final DataSource dataSource;

    /** True when connected to MySQL, false for H2 */
    private final boolean isMysql;

    public JdbcTaskRepository(DataSource dataSource) {
        this.dataSource = dataSource;
        this.isMysql = detectMysql();
        initSchema();
    }

    // ──────────────────────── Dialect Detection ────────────────────────

    private boolean detectMysql() {
        try (Connection conn = dataSource.getConnection()) {
            String name = conn.getMetaData().getDatabaseProductName().toLowerCase();
            boolean mysql = name.contains("mysql");
            log.info("Detected database: {}", name);
            return mysql;
        } catch (SQLException e) {
            throw new RepositoryException("Failed to detect database type", e);
        }
    }

    // ──────────────────────── Schema Init ────────────────────────

    private void initSchema() {
        // MySQL uses TEXT instead of CLOB; both support AUTO_INCREMENT
        String descType = isMysql ? "TEXT" : "CLOB";
        String sql = """
                CREATE TABLE IF NOT EXISTS tasks (
                    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
                    title       VARCHAR(255)  NOT NULL,
                    description %s,
                    status      VARCHAR(50)   NOT NULL,
                    priority    VARCHAR(50)   NOT NULL,
                    due_date    TIMESTAMP     NULL,
                    created_at  TIMESTAMP     NOT NULL,
                    updated_at  TIMESTAMP     NOT NULL
                )
                """.formatted(descType);
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            log.info("Database schema initialized ({}).", isMysql ? "MySQL" : "H2");
        } catch (SQLException e) {
            throw new RepositoryException("Failed to initialize schema", e);
        }
    }

    // ──────────────────────── CRUD ────────────────────────

    @Override
    public Task save(Task task) {
        if (task.getId() == null) {
            return insert(task);
        } else {
            return update(task);
        }
    }

    private Task insert(Task task) {
        String sql = """
                INSERT INTO tasks (title, description, status, priority, due_date, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            setTaskParams(ps, task);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return task.toBuilder().id(keys.getLong(1)).build();
                }
            }
            throw new RepositoryException("Insert did not return generated key");
        } catch (SQLException e) {
            throw new RepositoryException("Failed to insert task", e);
        }
    }

    private Task update(Task task) {
        String sql = """
                UPDATE tasks
                SET title=?, description=?, status=?, priority=?, due_date=?, created_at=?, updated_at=?
                WHERE id=?
                """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            setTaskParams(ps, task);
            ps.setLong(8, task.getId());
            int rows = ps.executeUpdate();
            if (rows == 0) throw new RepositoryException("Task not found for update: " + task.getId());
            return task;
        } catch (SQLException e) {
            throw new RepositoryException("Failed to update task", e);
        }
    }

    @Override
    public Optional<Task> findById(long id) {
        String sql = "SELECT * FROM tasks WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RepositoryException("Failed to find task by id", e);
        }
        return Optional.empty();
    }

    @Override
    public List<Task> findAll() {
        return query("SELECT * FROM tasks ORDER BY id");
    }

    @Override
    public List<Task> findByStatus(TaskStatus status) {
        return queryWithParams("SELECT * FROM tasks WHERE status = ? ORDER BY id", status.name());
    }

    @Override
    public List<Task> findByPriority(TaskPriority priority) {
        return queryWithParams("SELECT * FROM tasks WHERE priority = ? ORDER BY id", priority.name());
    }

    @Override
    public List<Task> findByStatusAndPriority(TaskStatus status, TaskPriority priority) {
        return queryWithParams(
                "SELECT * FROM tasks WHERE status = ? AND priority = ? ORDER BY id",
                status.name(), priority.name());
    }

    @Override
    public boolean deleteById(long id) {
        String sql = "DELETE FROM tasks WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            boolean deleted = ps.executeUpdate() > 0;
            if (deleted) {
                resetSequenceIfEmpty(conn);
            }
            return deleted;
        } catch (SQLException e) {
            throw new RepositoryException("Failed to delete task", e);
        }
    }

    /**
     * Demo/dev only: resets AUTO_INCREMENT to 1 when the table is empty so repeated
     * create/delete cycles start at id=1. Do not use in production — reused IDs can
     * collide with values cached by external systems.
     *
     * H2:    ALTER TABLE tasks ALTER COLUMN id RESTART WITH 1
     * MySQL: ALTER TABLE tasks AUTO_INCREMENT = 1
     */
    private void resetSequenceIfEmpty(Connection conn) throws SQLException {
        String countSql = "SELECT COUNT(*) FROM tasks";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(countSql)) {
            if (rs.next() && rs.getInt(1) == 0) {
                String resetSql = isMysql
                        ? "ALTER TABLE tasks AUTO_INCREMENT = 1"
                        : "ALTER TABLE tasks ALTER COLUMN id RESTART WITH 1";
                stmt.execute(resetSql);
                log.info("All tasks deleted — ID sequence reset to 1.");
            }
        }
    }

    // ──────────────────────── Helpers ────────────────────────

    private List<Task> query(String sql) {
        List<Task> tasks = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) tasks.add(mapRow(rs));
        } catch (SQLException e) {
            throw new RepositoryException("Query failed", e);
        }
        return tasks;
    }

    private List<Task> queryWithParams(String sql, String... params) {
        List<Task> tasks = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setString(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) tasks.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RepositoryException("Query with param failed", e);
        }
        return tasks;
    }

    private void setTaskParams(PreparedStatement ps, Task task) throws SQLException {
        ps.setString(1, task.getTitle());
        ps.setString(2, task.getDescription());
        ps.setString(3, task.getStatus().name());
        ps.setString(4, task.getPriority().name());
        ps.setTimestamp(5, task.getDueDate() != null ? Timestamp.valueOf(task.getDueDate()) : null);
        ps.setTimestamp(6, Timestamp.valueOf(task.getCreatedAt()));
        ps.setTimestamp(7, Timestamp.valueOf(task.getUpdatedAt()));
    }

    private Task mapRow(ResultSet rs) throws SQLException {
        Timestamp due = rs.getTimestamp("due_date");
        return Task.builder()
                .id(rs.getLong("id"))
                .title(rs.getString("title"))
                .description(rs.getString("description"))
                .status(TaskStatus.valueOf(rs.getString("status")))
                .priority(TaskPriority.valueOf(rs.getString("priority")))
                .dueDate(due != null ? due.toLocalDateTime() : null)
                .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
                .updatedAt(rs.getTimestamp("updated_at").toLocalDateTime())
                .build();
    }
}
