package com.mockexams.backend.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;

class FlywayMigrationSchemaTest {

	private static final String POSTGRES_MIGRATION_PROPERTY = "mockexams.migration.postgres.enabled";

	private static final List<String> TARGET_TABLES = List.of(
			"users",
			"roles",
			"user_roles",
			"teacher_profiles",
			"files",
			"tests",
			"test_sections",
			"listening_segments",
			"questions",
			"question_options",
			"question_answers",
			"test_attempts",
			"student_answers",
			"test_results",
			"band_conversions",
			"marketplace_listings",
			"marketplace_listing_items",
			"purchases",
			"reviews",
			"payments",
			"teacher_wallets",
			"teacher_transactions",
			"ai_feedbacks",
			"platform_settings");

	@Test
	void freshH2PostgreSqlModeDatabaseMigratesToTargetSchema() throws SQLException {
		String databaseName = "mock_exams_migration_test_" + UUID.randomUUID().toString().replace("-", "");
		String jdbcUrl = "jdbc:h2:mem:" + databaseName
				+ ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1";

		migrateAndAssertTargetSchema(jdbcUrl, "sa", "");
	}

	@Test
	void freshPostgresDatabaseMigratesToTargetSchema() throws SQLException {
		assumeTrue(Boolean.getBoolean(POSTGRES_MIGRATION_PROPERTY),
				() -> "Set -D" + POSTGRES_MIGRATION_PROPERTY + "=true to run the Docker-backed PostgreSQL migration test.");

		try (PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
				.withDatabaseName("mock_exams_migration_test")
				.withUsername("mock_exams")
				.withPassword("mock_exams")) {
			postgres.start();

			migrateAndAssertTargetSchema(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
		}
	}

	private static void migrateAndAssertTargetSchema(String jdbcUrl, String username, String password)
			throws SQLException {
		Flyway.configure()
				.dataSource(jdbcUrl, username, password)
				.locations("classpath:db/migration")
				.load()
				.migrate();

		try (Connection connection = DriverManager.getConnection(jdbcUrl, username, password)) {
			assertTablesExist(connection);
			assertForeignKeyExists(connection, "user_roles", "user_id", "users", "id");
			assertForeignKeyExists(connection, "user_roles", "role_id", "roles", "id");
			assertForeignKeyExists(connection, "teacher_profiles", "user_id", "users", "id");
			assertForeignKeyExists(connection, "tests", "created_by_user_id", "users", "id");
			assertForeignKeyExists(connection, "test_sections", "test_id", "tests", "id");
			assertForeignKeyExists(connection, "test_sections", "audio_file_id", "files", "id");
			assertForeignKeyExists(connection, "listening_segments", "section_id", "test_sections", "id");
			assertForeignKeyExists(connection, "questions", "section_id", "test_sections", "id");
			assertForeignKeyExists(connection, "questions", "listening_segment_id", "listening_segments", "id");
			assertForeignKeyExists(connection, "question_options", "question_id", "questions", "id");
			assertForeignKeyExists(connection, "question_answers", "question_id", "questions", "id");
			assertForeignKeyExists(connection, "test_attempts", "test_id", "tests", "id");
			assertForeignKeyExists(connection, "test_results", "attempt_id", "test_attempts", "id");
			assertForeignKeyExists(connection, "marketplace_listings", "teacher_profile_id", "teacher_profiles", "id");
			assertForeignKeyExists(connection, "marketplace_listings", "test_id", "tests", "id");
			assertForeignKeyExists(connection, "marketplace_listing_items", "listing_id", "marketplace_listings", "id");
			assertForeignKeyExists(connection, "marketplace_listing_items", "test_id", "tests", "id");
			assertForeignKeyExists(connection, "marketplace_listing_items", "file_id", "files", "id");
			assertForeignKeyExists(connection, "purchases", "student_user_id", "users", "id");
			assertForeignKeyExists(connection, "purchases", "listing_id", "marketplace_listings", "id");
			assertForeignKeyExists(connection, "payments", "student_user_id", "users", "id");
			assertForeignKeyExists(connection, "teacher_wallets", "teacher_profile_id", "teacher_profiles", "id");
			assertForeignKeyExists(connection, "teacher_transactions", "wallet_id", "teacher_wallets", "id");
			assertForeignKeyExists(connection, "teacher_transactions", "purchase_id", "purchases", "id");
			assertForeignKeyExists(connection, "teacher_transactions", "payment_id", "payments", "id");
			assertForeignKeyExists(connection, "ai_feedbacks", "student_user_id", "users", "id");
			assertForeignKeyExists(connection, "student_answers", List.of("selected_option_id", "question_id"),
					"question_options", List.of("id", "question_id"));

			assertUniqueConstraintExists(connection, "user_roles", List.of("user_id", "role_id"));
			assertUniqueConstraintExists(connection, "test_sections", List.of("test_id", "section_order"));
			assertUniqueConstraintExists(connection, "listening_segments", List.of("section_id", "segment_order"));
			assertUniqueConstraintExists(connection, "question_options", List.of("id", "question_id"));
			assertUniqueConstraintExists(connection, "reviews", List.of("student_user_id", "test_id"));
			assertUniqueConstraintExists(connection, "reviews", List.of("student_user_id", "teacher_profile_id"));
			assertUniqueConstraintExists(connection, "test_results", List.of("attempt_id"));
			assertUniqueConstraintExists(connection, "marketplace_listing_items", List.of("listing_id", "item_order"));
			assertUniqueConstraintExists(connection, "teacher_wallets", List.of("teacher_profile_id"));

			assertIntegrityRulesAreEnforced(connection);
		}
	}

	private static void assertIntegrityRulesAreEnforced(Connection connection) throws SQLException {
		execute(connection, """
				insert into users (id, email, password_hash, full_name)
				values
				  (1001, 'student@example.test', 'hash', 'Student Example'),
				  (1002, 'teacher@example.test', 'hash', 'Teacher Example')
				""");
		execute(connection, """
				insert into teacher_profiles (id, user_id, display_name)
				values (2001, 1002, 'Teacher Example')
				""");
		execute(connection, """
				insert into tests (id, title, type, duration_minutes, created_by_user_id)
				values (3001, 'Reading sample', 'reading', 60, 1002)
				""");
		execute(connection, """
				insert into test_sections (id, test_id, title, section_order)
				values (4001, 3001, 'Reading section', 1)
				""");
		execute(connection, """
				insert into questions (id, section_id, question_type, question_text, question_order)
				values
				  (5001, 4001, 'multiple_choice', 'Question one', 1),
				  (5002, 4001, 'multiple_choice', 'Question two', 2)
				""");
		execute(connection, """
				insert into question_options (id, question_id, option_label, option_text, option_order)
				values
				  (6001, 5001, 'A', 'Question one option', 1),
				  (6002, 5002, 'A', 'Question two option', 1)
				""");
		execute(connection, """
				insert into test_attempts (id, test_id, student_user_id)
				values (7001, 3001, 1001)
				""");

		assertThatThrownBy(() -> execute(connection, """
				insert into student_answers (id, attempt_id, question_id, selected_option_id)
				values (7101, 7001, 5001, 6002)
				""")).isInstanceOf(SQLException.class);

		execute(connection, """
				insert into reviews (id, student_user_id, teacher_profile_id, rating)
				values (8001, 1001, 2001, 5)
				""");
		assertThatThrownBy(() -> execute(connection, """
				insert into reviews (id, student_user_id, teacher_profile_id, rating)
				values (8002, 1001, 2001, 4)
				""")).isInstanceOf(SQLException.class);

		execute(connection, """
				insert into reviews (id, student_user_id, test_id, rating)
				values (8003, 1001, 3001, 5)
				""");
		assertThatThrownBy(() -> execute(connection, """
				insert into reviews (id, student_user_id, test_id, rating)
				values (8004, 1001, 3001, 4)
				""")).isInstanceOf(SQLException.class);
		assertThatThrownBy(() -> execute(connection, """
				insert into reviews (id, student_user_id, teacher_profile_id, test_id, rating)
				values (8005, 1001, 2001, 3001, 4)
				""")).isInstanceOf(SQLException.class);
	}

	private static void execute(Connection connection, String sql) throws SQLException {
		try (var statement = connection.createStatement()) {
			statement.executeUpdate(sql);
		}
	}

	private static void assertTablesExist(Connection connection) throws SQLException {
		Set<String> tableNames = new LinkedHashSet<>();
		try (var statement = connection.prepareStatement("""
				select table_name
				from information_schema.tables
				where table_schema = 'public'
				""");
				ResultSet resultSet = statement.executeQuery()) {
			while (resultSet.next()) {
				tableNames.add(resultSet.getString("table_name"));
			}
		}

		assertThat(tableNames).containsAll(TARGET_TABLES);
	}

	private static void assertForeignKeyExists(
			Connection connection,
			String tableName,
			String columnName,
			String referencedTableName,
			String referencedColumnName)
			throws SQLException {
		assertForeignKeyExists(connection, tableName, List.of(columnName), referencedTableName,
				List.of(referencedColumnName));
	}

	private static void assertForeignKeyExists(
			Connection connection,
			String tableName,
			List<String> columnNames,
			String referencedTableName,
			List<String> referencedColumnNames)
			throws SQLException {
		DatabaseMetaData metadata = connection.getMetaData();
		Map<String, List<ForeignKeyColumn>> foreignKeys = new HashMap<>();
		try (ResultSet resultSet = metadata.getImportedKeys(connection.getCatalog(), null, tableName)) {
			while (resultSet.next()) {
				String importedTableName = resultSet.getString("FKTABLE_NAME");
				String importedReferencedTableName = resultSet.getString("PKTABLE_NAME");
				if (!tableName.equalsIgnoreCase(importedTableName)
						|| !referencedTableName.equalsIgnoreCase(importedReferencedTableName)) {
					continue;
				}

				String foreignKeyName = resultSet.getString("FK_NAME");
				foreignKeys.computeIfAbsent(foreignKeyName, ignored -> new ArrayList<>())
						.add(new ForeignKeyColumn(
								resultSet.getShort("KEY_SEQ"),
								resultSet.getString("FKCOLUMN_NAME").toLowerCase(),
								resultSet.getString("PKCOLUMN_NAME").toLowerCase()));
			}
		}

		List<String> expectedColumnNames = columnNames.stream().map(String::toLowerCase).toList();
		List<String> expectedReferencedColumnNames = referencedColumnNames.stream().map(String::toLowerCase).toList();
		boolean exists = foreignKeys.values().stream()
				.map(columns -> columns.stream()
						.sorted(Comparator.comparingInt(ForeignKeyColumn::keySequence))
						.toList())
				.anyMatch(columns -> columns.stream().map(ForeignKeyColumn::columnName).toList()
						.equals(expectedColumnNames)
						&& columns.stream().map(ForeignKeyColumn::referencedColumnName).toList()
								.equals(expectedReferencedColumnNames));

		assertThat(exists)
				.as("%s.%s has a foreign key to %s.%s", tableName, columnNames, referencedTableName,
						referencedColumnNames)
				.isTrue();
	}

	private record ForeignKeyColumn(int keySequence, String columnName, String referencedColumnName) {
	}

	private static void assertUniqueConstraintExists(Connection connection, String tableName, List<String> columnNames)
			throws SQLException {
		List<List<String>> uniqueConstraints = new ArrayList<>();
		try (var statement = connection.prepareStatement("""
				select tc.constraint_name, kcu.column_name
				from information_schema.table_constraints tc
				join information_schema.key_column_usage kcu
				  on tc.constraint_schema = kcu.constraint_schema
				 and tc.constraint_name = kcu.constraint_name
				where tc.constraint_schema = 'public'
				  and tc.table_name = ?
				  and tc.constraint_type in ('PRIMARY KEY', 'UNIQUE')
				order by tc.constraint_name, kcu.ordinal_position
				""")) {
			statement.setString(1, tableName);

			String currentConstraint = null;
			List<String> currentColumns = new ArrayList<>();
			try (ResultSet resultSet = statement.executeQuery()) {
				while (resultSet.next()) {
					String constraintName = resultSet.getString("constraint_name");
					if (!constraintName.equals(currentConstraint)) {
						if (!currentColumns.isEmpty()) {
							uniqueConstraints.add(currentColumns);
						}
						currentConstraint = constraintName;
						currentColumns = new ArrayList<>();
					}
					currentColumns.add(resultSet.getString("column_name"));
				}
			}
			if (!currentColumns.isEmpty()) {
				uniqueConstraints.add(currentColumns);
			}
		}

		assertThat(uniqueConstraints).as("%s has unique constraint on %s", tableName, columnNames).contains(columnNames);
	}
}
