package com.tool.management.config;

import org.h2.jdbcx.JdbcDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;

/**
 * Resolves DataSource at startup: try MySQL when enabled, otherwise H2 in-memory.
 */
@Configuration
public class DataSourceConfig {

    private static final Logger log = LoggerFactory.getLogger(DataSourceConfig.class);

    public enum DbType { MYSQL, H2 }

    private final DbType activeDbType;
    private final DataSource dataSource;

    public DataSourceConfig(
            @Value("${db.mysql.enabled:true}") boolean mysqlEnabled,
            @Value("${db.mysql.url:}") String mysqlUrl,
            @Value("${db.mysql.username:root}") String mysqlUsername,
            @Value("${db.mysql.password:}") String mysqlPassword,
            @Value("${db.mysql.driver:com.mysql.cj.jdbc.Driver}") String mysqlDriver,
            @Value("${db.h2.url:jdbc:h2:mem:taskdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE}") String h2Url,
            @Value("${db.h2.username:sa}") String h2Username,
            @Value("${db.h2.password:}") String h2Password) {

        if (mysqlEnabled && isMySQLReachable(mysqlUrl, mysqlUsername, mysqlPassword, mysqlDriver)) {
            this.activeDbType = DbType.MYSQL;
            com.mysql.cj.jdbc.MysqlDataSource ds = new com.mysql.cj.jdbc.MysqlDataSource();
            ds.setURL(mysqlUrl);
            ds.setUser(mysqlUsername);
            ds.setPassword(mysqlPassword);
            this.dataSource = ds;
            log.info("Using datasource: MySQL");
        } else {
            this.activeDbType = DbType.H2;
            JdbcDataSource ds = new JdbcDataSource();
            ds.setURL(h2Url);
            ds.setUser(h2Username);
            ds.setPassword(h2Password);
            this.dataSource = ds;
            log.info("Using datasource: H2 In-Memory (fallback)");
        }
    }

    public DbType getActiveDbType() {
        return activeDbType;
    }

    @Bean
    @Primary
    public DataSource dataSource() {
        return dataSource;
    }

    private static boolean isMySQLReachable(String url, String user, String password, String driver) {
        if (url == null || url.isBlank()) {
            return false;
        }
        try {
            Class.forName(driver);
            try (Connection conn = DriverManager.getConnection(url, user, password)) {
                log.info("MySQL is available at: {}", url);
                return true;
            }
        } catch (Exception e) {
            log.warn("MySQL not available ({}). Falling back to H2 in-memory database.", e.getMessage());
            return false;
        }
    }
}
