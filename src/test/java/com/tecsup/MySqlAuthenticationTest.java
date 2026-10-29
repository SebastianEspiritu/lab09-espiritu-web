package com.tecsup;

import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@EnabledIfEnvironmentVariable(named = "RUN_MYSQL_TESTS", matches = "true")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MySqlAuthenticationTest extends AuthenticationContract {
    @Autowired JdbcTemplate jdbc;

    @BeforeAll
    void reportDatabaseVersion() {
        System.out.println("Lab09 server version: " + jdbc.queryForObject("SELECT VERSION()", String.class));
        System.out.println("Lab09 server distribution: " + jdbc.queryForObject("SELECT @@version_comment", String.class));
    }
}
