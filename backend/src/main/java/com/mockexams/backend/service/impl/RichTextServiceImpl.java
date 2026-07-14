package com.mockexams.backend.service.impl;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.mockexams.backend.entity.FileMetadata;
import com.mockexams.backend.entity.FilePurpose;
import com.mockexams.backend.error.BusinessException;
import com.mockexams.backend.repository.FileMetadataRepository;
import com.mockexams.backend.service.PlatformSettingService;
import com.mockexams.backend.service.RichTextService;

@Service
class RichTextServiceImpl implements RichTextService {

	private static final int SUPPORTED_VERSION = 1;
	private static final Set<String> BLOCK_TYPES = Set.of(
			"heading",
			"paragraph",
			"bullet_list",
			"ordered_list",
			"list_item",
			"blockquote",
			"table",
			"table_row",
			"table_cell",
			"table_header");
	private static final Set<String> INLINE_TYPES = Set.of("text", "hard_break", "image", "file");
	private static final Set<String> MARK_TYPES = Set.of("bold", "italic", "underline", "strike", "code", "link");

	private final FileMetadataRepository fileMetadataRepository;
	private final PlatformSettingService platformSettingService;
	private final ObjectMapper objectMapper;

	RichTextServiceImpl(
			FileMetadataRepository fileMetadataRepository,
			PlatformSettingService platformSettingService,
			ObjectMapper objectMapper
	) {
		this.fileMetadataRepository = fileMetadataRepository;
		this.platformSettingService = platformSettingService;
		this.objectMapper = objectMapper;
	}

	@Override
	public SanitizedRichText fromDocument(JsonNode document, boolean required, String fieldName) {
		if (document == null || document.isNull()) {
			if (required) {
				throw validationFailed(fieldName + " is required.");
			}
			return null;
		}
		validateDocumentSize(document, fieldName);
		Set<Long> fileIds = new HashSet<>();
		ObjectNode sanitized = sanitizeDocument(document, fieldName, fileIds);
		validateFileReferences(fileIds, fieldName);
		StringBuilder text = new StringBuilder();
		StringBuilder html = new StringBuilder();
		renderChildren(sanitized.path("content"), text, html);
		String contentText = compactText(text.toString());
		if (required && contentText.isBlank()) {
			throw validationFailed(fieldName + " is required.");
		}
		return new SanitizedRichText(writeJson(sanitized), contentText, html.toString());
	}

	@Override
	public SanitizedRichText fromPlainText(String value, boolean required, String fieldName) {
		String text = value == null ? null : value.trim();
		if ((text == null || text.isBlank()) && required) {
			throw validationFailed(fieldName + " is required.");
		}
		if (text == null || text.isBlank()) {
			return null;
		}
		ObjectNode document = objectMapper.createObjectNode();
		document.put("version", SUPPORTED_VERSION);
		document.put("type", "doc");
		ArrayNode content = document.putArray("content");
		ObjectNode paragraph = content.addObject();
		paragraph.put("type", "paragraph");
		ArrayNode paragraphContent = paragraph.putArray("content");
		ObjectNode textNode = paragraphContent.addObject();
		textNode.put("type", "text");
		textNode.put("text", text);
		validateDocumentSize(document, fieldName);
		return new SanitizedRichText(writeJson(document), text, "<p>" + escapeHtml(text) + "</p>");
	}

	@Override
	public SanitizedRichText fromStored(String document, String text, String renderedHtml, String legacyText) {
		if (document != null) {
			try {
				return fromDocument(objectMapper.readTree(document), false, "Stored rich text");
			}
			catch (JsonProcessingException | BusinessException exception) {
				return fromStoredPlainText(text, legacyText);
			}
		}
		return fromStoredPlainText(text, legacyText);
	}

	private SanitizedRichText fromStoredPlainText(String text, String legacyText) {
		String fallbackText = trimToNull(text) == null ? legacyText : text;
		return fromPlainText(fallbackText, false, "Content");
	}

	private ObjectNode sanitizeDocument(JsonNode document, String fieldName, Set<Long> fileIds) {
		if (!document.isObject()) {
			throw validationFailed(fieldName + " must be a JSON object.");
		}
		if (document.path("version").asInt(-1) != SUPPORTED_VERSION) {
			throw validationFailed(fieldName + " uses an unsupported rich-text version.");
		}
		if (!"doc".equals(document.path("type").asText(null))) {
			throw validationFailed(fieldName + " must use type doc.");
		}
		ObjectNode sanitized = objectMapper.createObjectNode();
		sanitized.put("version", SUPPORTED_VERSION);
		sanitized.put("type", "doc");
		sanitized.set("content", sanitizeChildren(document.path("content"), fieldName, fileIds));
		return sanitized;
	}

	private ArrayNode sanitizeChildren(JsonNode content, String fieldName, Set<Long> fileIds) {
		ArrayNode sanitized = objectMapper.createArrayNode();
		if (content.isMissingNode() || content.isNull()) {
			return sanitized;
		}
		if (!content.isArray()) {
			throw validationFailed(fieldName + " content must be an array.");
		}
		for (JsonNode child : content) {
			sanitized.add(sanitizeNode(child, fieldName, fileIds));
		}
		return sanitized;
	}

	private ObjectNode sanitizeNode(JsonNode node, String fieldName, Set<Long> fileIds) {
		if (!node.isObject()) {
			throw validationFailed(fieldName + " content nodes must be objects.");
		}
		String type = node.path("type").asText(null);
		if (type == null || (!BLOCK_TYPES.contains(type) && !INLINE_TYPES.contains(type))) {
			throw validationFailed(fieldName + " contains an unsupported rich-text node.");
		}
		return switch (type) {
			case "text" -> sanitizeText(node, fieldName);
			case "hard_break" -> objectWithType("hard_break");
			case "image", "file" -> sanitizeEmbed(node, type, fieldName, fileIds);
			default -> sanitizeBlock(node, type, fieldName, fileIds);
		};
	}

	private ObjectNode sanitizeText(JsonNode node, String fieldName) {
		ObjectNode sanitized = objectWithType("text");
		sanitized.put("text", node.path("text").asText(""));
		ArrayNode marks = sanitizeMarks(node.path("marks"), fieldName);
		if (!marks.isEmpty()) {
			sanitized.set("marks", marks);
		}
		return sanitized;
	}

	private ArrayNode sanitizeMarks(JsonNode marks, String fieldName) {
		ArrayNode sanitizedMarks = objectMapper.createArrayNode();
		if (marks.isMissingNode() || marks.isNull()) {
			return sanitizedMarks;
		}
		if (!marks.isArray()) {
			throw validationFailed(fieldName + " text marks must be an array.");
		}
		for (JsonNode mark : marks) {
			if (!mark.isObject()) {
				throw validationFailed(fieldName + " text marks must be objects.");
			}
			String type = mark.path("type").asText(null);
			if (!MARK_TYPES.contains(type)) {
				throw validationFailed(fieldName + " contains an unsupported text mark.");
			}
			if ("link".equals(type)) {
				String href = mark.path("attrs").path("href").asText(null);
				if (!isSafeHref(href)) {
					continue;
				}
				ObjectNode sanitizedMark = objectWithType(type);
				ObjectNode attrs = sanitizedMark.putObject("attrs");
				attrs.put("href", href.trim());
				String title = trimToNull(mark.path("attrs").path("title").asText(null));
				if (title != null) {
					attrs.put("title", title);
				}
				sanitizedMarks.add(sanitizedMark);
			}
			else {
				sanitizedMarks.add(objectWithType(type));
			}
		}
		return sanitizedMarks;
	}

	private ObjectNode sanitizeEmbed(JsonNode node, String type, String fieldName, Set<Long> fileIds) {
		JsonNode attrs = node.path("attrs");
		if (!attrs.isObject()) {
			throw validationFailed(fieldName + " embeds require attrs.");
		}
		if (attrs.hasNonNull("src") || attrs.hasNonNull("url")) {
			throw validationFailed(fieldName + " embeds must reference uploaded files.");
		}
		Long fileId = longOrNull(attrs.path("fileId"));
		if (fileId == null) {
			throw validationFailed(fieldName + " embeds require fileId.");
		}
		fileIds.add(fileId);
		ObjectNode sanitized = objectWithType(type);
		ObjectNode sanitizedAttrs = sanitized.putObject("attrs");
		sanitizedAttrs.put("fileId", fileId);
		String alt = trimToNull(attrs.path("alt").asText(null));
		if (alt != null) {
			sanitizedAttrs.put("alt", alt);
		}
		String title = trimToNull(attrs.path("title").asText(null));
		if (title != null) {
			sanitizedAttrs.put("title", title);
		}
		return sanitized;
	}

	private ObjectNode sanitizeBlock(JsonNode node, String type, String fieldName, Set<Long> fileIds) {
		ObjectNode sanitized = objectWithType(type);
		if ("heading".equals(type)) {
			int level = node.path("attrs").path("level").asInt(2);
			if (level < 1 || level > 6) {
				throw validationFailed(fieldName + " heading level must be between 1 and 6.");
			}
			ObjectNode attrs = sanitized.putObject("attrs");
			attrs.put("level", level);
		}
		sanitized.set("content", sanitizeChildren(node.path("content"), fieldName, fileIds));
		return sanitized;
	}

	private void validateFileReferences(Set<Long> fileIds, String fieldName) {
		if (fileIds.isEmpty()) {
			return;
		}
		Set<FilePurpose> allowedPurposes = platformSettingService.richTextAllowedEmbedPurposes();
		List<FileMetadata> files = fileMetadataRepository.findAllById(fileIds);
		Map<Long, FileMetadata> filesById = files.stream().collect(Collectors.toMap(FileMetadata::getId, file -> file));
		for (Long fileId : fileIds) {
			FileMetadata file = filesById.get(fileId);
			if (file == null || !allowedPurposes.contains(file.getPurpose())) {
				throw validationFailed(fieldName + " references an unknown or unsupported file.");
			}
		}
	}

	private void validateDocumentSize(JsonNode document, String fieldName) {
		try {
			if (objectMapper.writeValueAsBytes(document).length > platformSettingService.richTextMaxDocumentBytes()) {
				throw validationFailed(fieldName + " exceeds the maximum rich-text document size.");
			}
		}
		catch (JsonProcessingException exception) {
			throw validationFailed(fieldName + " could not be serialized.");
		}
	}

	private void renderChildren(JsonNode content, StringBuilder text, StringBuilder html) {
		if (!content.isArray()) {
			return;
		}
		for (JsonNode child : content) {
			renderNode(child, text, html);
		}
	}

	private void renderNode(JsonNode node, StringBuilder text, StringBuilder html) {
		String type = node.path("type").asText("");
		switch (type) {
			case "heading" -> renderWrappedBlock(node, text, html, "h" + node.path("attrs").path("level").asInt(2));
			case "paragraph" -> renderWrappedBlock(node, text, html, "p");
			case "bullet_list" -> renderWrappedBlock(node, text, html, "ul");
			case "ordered_list" -> renderWrappedBlock(node, text, html, "ol");
			case "list_item" -> renderWrappedBlock(node, text, html, "li");
			case "blockquote" -> renderWrappedBlock(node, text, html, "blockquote");
			case "table" -> renderWrappedBlock(node, text, html, "table");
			case "table_row" -> renderWrappedBlock(node, text, html, "tr");
			case "table_cell" -> renderWrappedBlock(node, text, html, "td");
			case "table_header" -> renderWrappedBlock(node, text, html, "th");
			case "text" -> renderText(node, text, html);
			case "hard_break" -> {
				text.append(' ');
				html.append("<br>");
			}
			case "image" -> renderImage(node, text, html);
			case "file" -> renderFile(node, text, html);
			default -> {
			}
		}
	}

	private void renderWrappedBlock(JsonNode node, StringBuilder text, StringBuilder html, String tag) {
		int before = text.length();
		html.append('<').append(tag).append('>');
		renderChildren(node.path("content"), text, html);
		html.append("</").append(tag).append('>');
		if (text.length() > before) {
			text.append(' ');
		}
	}

	private void renderText(JsonNode node, StringBuilder text, StringBuilder html) {
		String value = node.path("text").asText("");
		text.append(value);
		String rendered = escapeHtml(value);
		if (node.path("marks").isArray()) {
			for (JsonNode mark : node.path("marks")) {
				rendered = openMark(mark) + rendered + closeMark(mark);
			}
		}
		html.append(rendered);
	}

	private String openMark(JsonNode mark) {
		return switch (mark.path("type").asText("")) {
			case "bold" -> "<strong>";
			case "italic" -> "<em>";
			case "underline" -> "<u>";
			case "strike" -> "<s>";
			case "code" -> "<code>";
			case "link" -> {
				String href = escapeHtmlAttribute(mark.path("attrs").path("href").asText(""));
				String title = trimToNull(mark.path("attrs").path("title").asText(null));
				yield title == null
						? "<a href=\"" + href + "\" rel=\"nofollow noopener noreferrer\">"
						: "<a href=\"" + href + "\" title=\"" + escapeHtmlAttribute(title)
								+ "\" rel=\"nofollow noopener noreferrer\">";
			}
			default -> "";
		};
	}

	private String closeMark(JsonNode mark) {
		return switch (mark.path("type").asText("")) {
			case "bold" -> "</strong>";
			case "italic" -> "</em>";
			case "underline" -> "</u>";
			case "strike" -> "</s>";
			case "code" -> "</code>";
			case "link" -> "</a>";
			default -> "";
		};
	}

	private void renderImage(JsonNode node, StringBuilder text, StringBuilder html) {
		long fileId = node.path("attrs").path("fileId").asLong();
		String alt = node.path("attrs").path("alt").asText("");
		if (!alt.isBlank()) {
			text.append(alt).append(' ');
		}
		html.append("<figure data-file-id=\"").append(fileId).append("\"><img data-file-id=\"")
				.append(fileId).append("\" alt=\"").append(escapeHtmlAttribute(alt)).append("\"></figure>");
	}

	private void renderFile(JsonNode node, StringBuilder text, StringBuilder html) {
		long fileId = node.path("attrs").path("fileId").asLong();
		String title = trimToNull(node.path("attrs").path("title").asText(null));
		String label = title == null ? "File " + fileId : title;
		text.append(label).append(' ');
		html.append("<a data-file-id=\"").append(fileId).append("\">")
				.append(escapeHtml(label)).append("</a>");
	}

	private ObjectNode objectWithType(String type) {
		ObjectNode object = objectMapper.createObjectNode();
		object.put("type", type);
		return object;
	}

	private String writeJson(JsonNode node) {
		try {
			return objectMapper.writeValueAsString(node);
		}
		catch (JsonProcessingException exception) {
			throw validationFailed("Rich text document could not be serialized.");
		}
	}

	private boolean isSafeHref(String href) {
		String value = trimToNull(href);
		if (value == null) {
			return false;
		}
		String lower = value.toLowerCase(Locale.ROOT);
		return lower.startsWith("https://") || lower.startsWith("http://") || lower.startsWith("mailto:");
	}

	private Long longOrNull(JsonNode value) {
		if (!value.canConvertToLong()) {
			return null;
		}
		long longValue = value.asLong();
		return longValue > 0 ? longValue : null;
	}

	private String compactText(String value) {
		return value == null ? "" : value.replaceAll("\\s+", " ").trim();
	}

	private String trimToNull(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value.trim();
	}

	private String escapeHtml(String value) {
		return value
				.replace("&", "&amp;")
				.replace("<", "&lt;")
				.replace(">", "&gt;");
	}

	private String escapeHtmlAttribute(String value) {
		return escapeHtml(value)
				.replace("\"", "&quot;")
				.replace("'", "&#39;");
	}

	private BusinessException validationFailed(String message) {
		return new BusinessException("VALIDATION_FAILED", message, HttpStatus.BAD_REQUEST);
	}
}
