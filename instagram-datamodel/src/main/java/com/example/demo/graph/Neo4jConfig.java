package com.example.demo.graph;

import org.neo4j.driver.Driver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.core.DatabaseSelection;
import org.springframework.data.neo4j.core.DatabaseSelectionProvider;
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@EnableNeo4jRepositories(
		basePackages = "com.example.demo.graph.repository",
		transactionManagerRef = "neo4jTransactionManager"
)
public class Neo4jConfig {

	@Bean
	public DatabaseSelectionProvider databaseSelectionProvider() {
		return () -> DatabaseSelection.byName("neo4j");
	}

	@Bean
	public PlatformTransactionManager neo4jTransactionManager(Driver driver,
			DatabaseSelectionProvider databaseSelectionProvider) {
		return new Neo4jTransactionManager(driver, databaseSelectionProvider);
	}
}
