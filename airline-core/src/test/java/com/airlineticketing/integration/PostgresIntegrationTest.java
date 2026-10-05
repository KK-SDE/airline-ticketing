package com.airlineticketing.integration;

import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.testcontainers.DockerClientFactory;

@SpringBootTest
@Import(PostgresTestConfig.class)
@EnabledIf("com.airlineticketing.integration.PostgresIntegrationTest#dockerAvailable")
abstract class PostgresIntegrationTest {

    static boolean dockerAvailable() {
        return DockerClientFactory.instance().isDockerAvailable();
    }
}
