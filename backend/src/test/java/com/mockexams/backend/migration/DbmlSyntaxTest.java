package com.mockexams.backend.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class DbmlSyntaxTest {

	private static final Pattern PROJECT_BLOCK = Pattern.compile("Project\\s+([A-Za-z_][A-Za-z0-9_]*)\\s*\\{");
	private static final Pattern ENUM_BLOCK = Pattern.compile("Enum\\s+([A-Za-z_][A-Za-z0-9_]*)\\s*\\{");
	private static final Pattern TABLE_BLOCK = Pattern.compile("Table\\s+([A-Za-z_][A-Za-z0-9_]*)\\s*\\{");
	private static final Pattern FIELD = Pattern.compile(
			"([A-Za-z_][A-Za-z0-9_]*)\\s+([A-Za-z_][A-Za-z0-9_]*(?:\\([^)]*\\))?)(?:\\s+\\[[^]]+])?");
	private static final Pattern INDEX = Pattern.compile("\\(([^)]+)\\)\\s+\\[[^]]+]");
	private static final Pattern REF = Pattern.compile(
			"Ref:\\s+([A-Za-z_][A-Za-z0-9_]*)\\.([A-Za-z_][A-Za-z0-9_]*)\\s+>\\s+([A-Za-z_][A-Za-z0-9_]*)\\.([A-Za-z_][A-Za-z0-9_]*)");
	private static final Set<String> SCALAR_TYPES = Set.of(
			"bigint",
			"smallint",
			"integer",
			"varchar",
			"text",
			"timestamptz",
			"numeric",
			"boolean",
			"date",
			"jsonb");

	@Test
	void platformArchitectureDbmlHasParseableSyntax() throws IOException {
		Path dbmlPath = Path.of(System.getProperty("user.dir"), "../docs/ERD/platform-architecture.dbml").normalize();
		DbmlDocument document = parse(Files.readAllLines(dbmlPath));

		assertThat(document.tables()).containsKeys("users", "tests", "question_options", "student_answers", "reviews");
		assertThat(document.enums()).contains("test_type", "question_type", "payment_status");
		assertThat(document.refCount()).isGreaterThan(0);
	}

	private static DbmlDocument parse(Iterable<String> lines) {
		Map<String, Set<String>> tables = new LinkedHashMap<>();
		Set<String> enums = new LinkedHashSet<>();
		ArrayDeque<Context> contexts = new ArrayDeque<>();
		String currentTable = null;
		String currentEnum = null;
		boolean inMultilineText = false;
		int lineNumber = 0;
		int refCount = 0;

		for (String rawLine : lines) {
			lineNumber++;
			String line = stripComment(rawLine).strip();
			if (line.isBlank()) {
				continue;
			}

			if (containsOddTripleQuoteCount(line)) {
				inMultilineText = !inMultilineText;
				continue;
			}
			if (inMultilineText) {
				continue;
			}

			assertBalanced(line, '[', ']', lineNumber);
			assertBalanced(line, '(', ')', lineNumber);

			if (line.equals("}")) {
				Context closed = requireContext(contexts, lineNumber);
				contexts.pop();
				if (closed == Context.TABLE) {
					currentTable = null;
				}
				if (closed == Context.ENUM) {
					currentEnum = null;
				}
				continue;
			}

			if (line.equals("indexes {")) {
				requireCurrentContext(contexts, Context.TABLE, lineNumber, line);
				contexts.push(Context.INDEXES);
				continue;
			}

			Matcher ref = REF.matcher(line);
			if (ref.matches()) {
				requireTopLevel(contexts, lineNumber, line);
				assertKnownColumn(tables, ref.group(1), ref.group(2), lineNumber, line);
				assertKnownColumn(tables, ref.group(3), ref.group(4), lineNumber, line);
				refCount++;
				continue;
			}

			Matcher projectBlock = PROJECT_BLOCK.matcher(line);
			if (projectBlock.matches()) {
				requireTopLevel(contexts, lineNumber, line);
				contexts.push(Context.PROJECT);
				continue;
			}

			Matcher enumBlock = ENUM_BLOCK.matcher(line);
			if (enumBlock.matches()) {
				requireTopLevel(contexts, lineNumber, line);
				currentEnum = enumBlock.group(1);
				enums.add(currentEnum);
				contexts.push(Context.ENUM);
				continue;
			}

			Matcher tableBlock = TABLE_BLOCK.matcher(line);
			if (tableBlock.matches()) {
				requireTopLevel(contexts, lineNumber, line);
				currentTable = tableBlock.group(1);
				tables.put(currentTable, new LinkedHashSet<>());
				contexts.push(Context.TABLE);
				continue;
			}

			Context context = requireContext(contexts, lineNumber);
			if (context == Context.PROJECT) {
				if (!line.matches("[A-Za-z_][A-Za-z0-9_]*:\\s+.+")) {
					fail(lineNumber, line, "Expected a project property.");
				}
				continue;
			}
			if (context == Context.ENUM) {
				if (!line.matches("[A-Za-z_][A-Za-z0-9_]*")) {
					fail(lineNumber, line, "Expected an enum value.");
				}
				continue;
			}
			if (context == Context.TABLE) {
				Matcher field = FIELD.matcher(line);
				if (!field.matches()) {
					fail(lineNumber, line, "Expected a table field.");
				}
				String fieldName = field.group(1);
				String fieldType = field.group(2).replaceAll("\\(.*", "");
				tables.get(currentTable).add(fieldName);
				if (!SCALAR_TYPES.contains(fieldType) && !enums.contains(fieldType)) {
					fail(lineNumber, line, "Unknown field type: " + fieldType);
				}
				continue;
			}
			if (context == Context.INDEXES) {
				Matcher index = INDEX.matcher(line);
				if (!index.matches()) {
					fail(lineNumber, line, "Expected an index definition.");
				}
				for (String column : index.group(1).split(",")) {
					String columnName = column.strip();
					if (!tables.get(currentTable).contains(columnName)) {
						fail(lineNumber, line, "Index references unknown column: " + columnName);
					}
				}
				continue;
			}

			fail(lineNumber, line, "Unexpected top-level statement.");
		}

		assertThat(contexts).as("DBML blocks are closed").isEmpty();
		return new DbmlDocument(tables, enums, refCount);
	}

	private static String stripComment(String rawLine) {
		int commentStart = rawLine.indexOf("//");
		if (commentStart < 0) {
			return rawLine;
		}
		return rawLine.substring(0, commentStart);
	}

	private static boolean containsOddTripleQuoteCount(String line) {
		int count = 0;
		int index = line.indexOf("'''");
		while (index >= 0) {
			count++;
			index = line.indexOf("'''", index + 3);
		}
		return count % 2 == 1;
	}

	private static void assertBalanced(String line, char open, char close, int lineNumber) {
		int depth = 0;
		for (int i = 0; i < line.length(); i++) {
			char current = line.charAt(i);
			if (current == open) {
				depth++;
			}
			if (current == close) {
				depth--;
			}
			if (depth < 0) {
				fail(lineNumber, line, "Unbalanced " + open + close + " delimiters.");
			}
		}
		if (depth != 0) {
			fail(lineNumber, line, "Unbalanced " + open + close + " delimiters.");
		}
	}

	private static void assertKnownColumn(
			Map<String, Set<String>> tables, String tableName, String columnName, int lineNumber, String line) {
		if (!tables.containsKey(tableName)) {
			fail(lineNumber, line, "Relationship references unknown table: " + tableName);
		}
		if (!tables.get(tableName).contains(columnName)) {
			fail(lineNumber, line, "Relationship references unknown column: " + tableName + "." + columnName);
		}
	}

	private static void requireTopLevel(ArrayDeque<Context> contexts, int lineNumber, String line) {
		if (!contexts.isEmpty()) {
			fail(lineNumber, line, "Expected a top-level declaration.");
		}
	}

	private static Context requireContext(ArrayDeque<Context> contexts, int lineNumber) {
		if (contexts.isEmpty()) {
			fail(lineNumber, "", "Expected a DBML block context.");
		}
		return contexts.peek();
	}

	private static void requireCurrentContext(
			ArrayDeque<Context> contexts, Context expected, int lineNumber, String line) {
		if (contexts.isEmpty() || contexts.peek() != expected) {
			fail(lineNumber, line, "Expected context: " + expected);
		}
	}

	private static void fail(int lineNumber, String line, String message) {
		throw new AssertionError("DBML line " + lineNumber + ": " + message + " [" + line + "]");
	}

	private enum Context {
		PROJECT,
		ENUM,
		TABLE,
		INDEXES
	}

	private record DbmlDocument(Map<String, Set<String>> tables, Set<String> enums, int refCount) {
	}
}
