package com.mockexams.backend.service.impl;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mockexams.backend.config.BackendEnvironmentProperties;
import com.mockexams.backend.dto.PlatformSettingRDO;
import com.mockexams.backend.entity.FilePurpose;
import com.mockexams.backend.entity.PlatformSetting;
import com.mockexams.backend.error.BusinessException;
import com.mockexams.backend.mapper.PlatformSettingMapper;
import com.mockexams.backend.repository.PlatformSettingRepository;
import com.mockexams.backend.service.PlatformSettingService;
import com.mockexams.backend.users.entity.User;

@Service
class PlatformSettingServiceImpl implements PlatformSettingService {

	private static final String MARKETPLACE_COMMISSION = "marketplace.default_commission_percent";
	private static final String PLATFORM_COMMISSION = "platform.default_commission_percent";
	private static final String LIVE_SPEAKING_COMMISSION = "live_speaking.platform_commission_percent";
	private static final String AI_FREE_REQUESTS = "ai.free_monthly_requests";
	private static final String AI_HARD_REQUESTS = "ai.hard_monthly_requests";
	private static final String AI_DEFAULT_MODEL = "ai.default_model";
	private static final String AI_RUBRIC_DEFAULTS = "ai.rubric_defaults";
	private static final String AI_PREMIUM_PRICE = "ai.premium_monthly_price";
	private static final String PAYMENTS_PROVIDER_MODE = "payments.provider_mode";
	private static final String PAYMENTS_PROVIDER_OPTIONS = "payments.provider_options";
	private static final String PAYMENTS_DEFAULT_CURRENCY = "payments.default_currency";
	private static final String WITHDRAWAL_MINIMUM = "wallet.minimum_withdrawal_amount";
	private static final String MODERATION_BEHAVIOR = "moderation.behavior";
	private static final String RICH_TEXT_LIMITS = "rich_text.limits";
	private static final String GOOGLE_CALENDAR_MEET = "google.calendar_meet";
	private static final String LIVE_SPEAKING_CANCELLATION = "live_speaking.cancellation_policy";
	private static final String TESTS_DEFAULT_LANGUAGE = "tests.default_language";

	private static final BigDecimal ONE_HUNDRED = new BigDecimal("100.00");
	private static final Pattern CURRENCY_PATTERN = Pattern.compile("[A-Z]{3}");
	private static final Pattern LANGUAGE_PATTERN = Pattern.compile("[a-z]{2}(-[A-Z]{2})?");
	private static final Set<String> AI_PROVIDERS = Set.of("mock", "openai");
	private static final Set<String> PAYMENT_PROVIDERS = Set.of("mock", "payme", "click", "stripe");
	private static final Set<String> PAYMENT_MODES = Set.of("mock", "sandbox", "live");
	private static final Set<String> GOOGLE_MODES = Set.of("disabled", "platform_calendar", "teacher_oauth");
	private static final Set<String> GOOGLE_OWNERS = Set.of("platform", "teacher");
	private static final Set<FilePurpose> RICH_TEXT_EMBED_PURPOSES = Set.of(
			FilePurpose.PASSAGE_IMAGE,
			FilePurpose.DIAGRAM,
			FilePurpose.DOCUMENT);
	private static final List<String> SECRET_FIELD_TOKENS = List.of(
			"secret",
			"apikey",
			"api_key",
			"token",
			"password",
			"privatekey",
			"private_key",
			"clientsecret",
			"client_secret",
			"webhooksecret",
			"webhook_secret");

	private final PlatformSettingRepository repository;
	private final PlatformSettingMapper mapper;
	private final ObjectMapper objectMapper;
	private final BackendEnvironmentProperties properties;
	private final Clock clock;
	private final Map<String, SettingDefinition> definitions;

	PlatformSettingServiceImpl(
			PlatformSettingRepository repository,
			PlatformSettingMapper mapper,
			ObjectMapper objectMapper,
			BackendEnvironmentProperties properties,
			Clock clock
	) {
		this.repository = repository;
		this.mapper = mapper;
		this.objectMapper = objectMapper;
		this.properties = properties;
		this.clock = clock;
		this.definitions = definitions();
	}

	@Override
	@Transactional(readOnly = true)
	public List<PlatformSettingRDO> listSettings() {
		Map<String, PlatformSetting> storedByKey = repository.findAllByOrderBySettingKeyAsc().stream()
				.collect(Collectors.toMap(PlatformSetting::getSettingKey, setting -> setting));
		return definitions.values().stream()
				.sorted(Comparator.comparing(SettingDefinition::key))
				.map(definition -> {
					PlatformSetting setting = storedByKey.get(definition.key());
					if (setting == null) {
						return mapper.toRDO(definition.key(), defaultValue(definition.key()));
					}
					return mapper.toRDO(setting);
				})
				.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public PlatformSettingRDO getSetting(String key) {
		SettingDefinition definition = requireDefinition(key);
		return repository.findBySettingKey(definition.key())
				.map(mapper::toRDO)
				.orElseGet(() -> mapper.toRDO(definition.key(), defaultValue(definition.key())));
	}

	@Override
	@Transactional
	public PlatformSettingRDO updateSetting(User admin, String key, JsonNode value) {
		SettingDefinition definition = requireDefinition(key);
		JsonNode normalized = validate(definition, value, true);
		String storedValue = write(normalized);
		PlatformSetting setting = repository.findBySettingKey(definition.key())
				.orElseGet(() -> new PlatformSetting(definition.key(), storedValue));
		setting.updateValue(storedValue, admin.getId(), OffsetDateTime.now(clock));
		return mapper.toRDO(repository.save(setting));
	}

	@Override
	@Transactional(readOnly = true)
	public BigDecimal marketplaceCommissionPercent() {
		return decimalValue(MARKETPLACE_COMMISSION, "value");
	}

	@Override
	@Transactional(readOnly = true)
	public BigDecimal platformCommissionPercent() {
		return decimalValue(PLATFORM_COMMISSION, "value");
	}

	@Override
	@Transactional(readOnly = true)
	public BigDecimal liveSpeakingPlatformCommissionPercent() {
		return decimalValue(LIVE_SPEAKING_COMMISSION, "value");
	}

	@Override
	@Transactional(readOnly = true)
	public int aiFreeMonthlyRequestsLimit() {
		return intValue(AI_FREE_REQUESTS, "value");
	}

	@Override
	@Transactional(readOnly = true)
	public Integer aiHardMonthlyRequestsLimit() {
		return intValue(AI_HARD_REQUESTS, "value");
	}

	@Override
	@Transactional(readOnly = true)
	public String aiDefaultModel() {
		return textValue(AI_DEFAULT_MODEL, "model");
	}

	@Override
	@Transactional(readOnly = true)
	public String aiWritingRubricVersion() {
		return textValue(AI_RUBRIC_DEFAULTS, "writingRubricVersion");
	}

	@Override
	@Transactional(readOnly = true)
	public String aiSpeakingRubricVersion() {
		return textValue(AI_RUBRIC_DEFAULTS, "speakingRubricVersion");
	}

	@Override
	@Transactional(readOnly = true)
	public BigDecimal withdrawalMinimumAmount() {
		return decimalValue(WITHDRAWAL_MINIMUM, "value");
	}

	@Override
	@Transactional(readOnly = true)
	public int richTextMaxDocumentBytes() {
		return intValue(RICH_TEXT_LIMITS, "maxDocumentBytes");
	}

	@Override
	@Transactional(readOnly = true)
	public Set<FilePurpose> richTextAllowedEmbedPurposes() {
		JsonNode node = settingValue(RICH_TEXT_LIMITS).path("allowedEmbedPurposes");
		if (!node.isArray()) {
			throw platformSettingInvalid(RICH_TEXT_LIMITS);
		}
		List<FilePurpose> purposes = new ArrayList<>();
		for (JsonNode purpose : node) {
			purposes.add(FilePurpose.from(purpose.asText()));
		}
		return Set.copyOf(purposes);
	}

	@Override
	@Transactional(readOnly = true)
	public Duration liveSpeakingStudentCancellationWindow() {
		return Duration.ofHours(intValue(LIVE_SPEAKING_CANCELLATION, "studentCancellationWindowHours"));
	}

	@Override
	@Transactional(readOnly = true)
	public boolean googleIntegrationEnabled() {
		return settingValue(GOOGLE_CALENDAR_MEET).path("enabled").asBoolean(false);
	}

	@Override
	@Transactional(readOnly = true)
	public String googleIntegrationMode() {
		return textValue(GOOGLE_CALENDAR_MEET, "mode");
	}

	private Map<String, SettingDefinition> definitions() {
		Map<String, SettingDefinition> result = new LinkedHashMap<>();
		add(result, MARKETPLACE_COMMISSION, "{\"value\": 25.00}", this::validatePercentValue);
		add(result, PLATFORM_COMMISSION, "{\"value\": 25.00}", this::validatePercentValue);
		add(result, LIVE_SPEAKING_COMMISSION, "{\"value\": 25.00}", this::validatePercentValue);
		add(result, AI_FREE_REQUESTS, "{\"value\": 20, \"period\": \"monthly\"}", this::validateMonthlyLimit);
		add(result, AI_HARD_REQUESTS, "{\"value\": 50, \"period\": \"monthly\"}", this::validateMonthlyLimit);
		add(result, AI_DEFAULT_MODEL, "{\"provider\": \"openai\", \"model\": \"gpt-4.1-mini\"}", this::validateAiModel);
		add(result, AI_RUBRIC_DEFAULTS,
				"{\"writingRubricVersion\": \"ielts-writing-v1\", \"speakingRubricVersion\": \"ielts-speaking-v1\"}",
				this::validateRubricDefaults);
		add(result, AI_PREMIUM_PRICE, "{\"amount\": 0.00, \"currency\": \"UZS\"}", this::validateMoneyAmount);
		add(result, PAYMENTS_PROVIDER_MODE,
				"{\"enabled\": false, \"provider\": \"mock\", \"mode\": \"mock\"}", this::validatePaymentMode);
		add(result, PAYMENTS_PROVIDER_OPTIONS,
				"{\"defaultProvider\": \"mock\", \"allowedProviders\": [\"mock\"]}", this::validatePaymentOptions);
		add(result, PAYMENTS_DEFAULT_CURRENCY, "{\"value\": \"UZS\"}", this::validateCurrencyValue);
		add(result, WITHDRAWAL_MINIMUM, "{\"value\": 10000.00, \"currency\": \"UZS\"}", this::validateMoneyValue);
		add(result, MODERATION_BEHAVIOR,
				"{\"autoHideThreshold\": 3, \"manualReviewRequired\": true}", this::validateModerationBehavior);
		add(result, RICH_TEXT_LIMITS,
				"{\"maxDocumentBytes\": 65536, \"allowedEmbedPurposes\": [\"passage_image\", \"diagram\", \"document\"]}",
				this::validateRichTextLimits);
		add(result, GOOGLE_CALENDAR_MEET,
				"{\"enabled\": false, \"mode\": \"disabled\", \"calendarOwner\": \"platform\"}",
				this::validateGoogleCalendarMeet);
		add(result, LIVE_SPEAKING_CANCELLATION,
				"{\"studentCancellationWindowHours\": 24, \"teacherCancellationWindowHours\": 2}",
				this::validateLiveSpeakingCancellation);
		add(result, TESTS_DEFAULT_LANGUAGE, "{\"value\": \"en\"}", this::validateLanguageValue);
		return Map.copyOf(result);
	}

	private void add(Map<String, SettingDefinition> definitions, String key, String defaultJson,
			Consumer<JsonNode> validator) {
		definitions.put(key, new SettingDefinition(key, defaultJson, validator));
	}

	private SettingDefinition requireDefinition(String key) {
		String normalized = trimToNull(key);
		SettingDefinition definition = normalized == null ? null : definitions.get(normalized);
		if (definition == null) {
			throw new BusinessException("UNKNOWN_SETTING", "Platform setting is not supported.", HttpStatus.NOT_FOUND);
		}
		return definition;
	}

	private JsonNode validate(SettingDefinition definition, JsonNode value, boolean requireExternalConfig) {
		if (value == null || value.isNull() || !value.isObject()) {
			throw validationFailed("Setting value must be a JSON object.");
		}
		rejectSecretFields(value);
		definition.validator().accept(value);
		if (requireExternalConfig && GOOGLE_CALENDAR_MEET.equals(definition.key())) {
			requireGoogleConfigWhenEnabled(value);
		}
		return value.deepCopy();
	}

	private JsonNode settingValue(String key) {
		SettingDefinition definition = requireDefinition(key);
		JsonNode value = repository.findBySettingKey(definition.key())
				.map(PlatformSetting::getSettingValue)
				.map(this::readJson)
				.orElseGet(() -> defaultValue(definition.key()));
		try {
			return validate(definition, value, false);
		}
		catch (BusinessException ex) {
			throw platformSettingInvalid(definition.key());
		}
	}

	private JsonNode defaultValue(String key) {
		return readJson(requireDefinition(key).defaultJson());
	}

	private JsonNode readJson(String value) {
		try {
			JsonNode root = objectMapper.readTree(value);
			if (root.isTextual()) {
				return objectMapper.readTree(root.asText());
			}
			return root;
		}
		catch (JsonProcessingException ex) {
			throw new BusinessException("PLATFORM_SETTING_INVALID", "Platform setting value is invalid.",
					HttpStatus.CONFLICT);
		}
	}

	private String write(JsonNode value) {
		try {
			return objectMapper.writeValueAsString(value);
		}
		catch (JsonProcessingException ex) {
			throw validationFailed("Setting value could not be serialized.");
		}
	}

	private void validatePercentValue(JsonNode value) {
		requireOnlyFields(value, "value");
		BigDecimal percent = requiredDecimal(value, "value");
		if (percent.compareTo(BigDecimal.ZERO) < 0 || percent.compareTo(ONE_HUNDRED) > 0) {
			throw validationFailed("Percent value must be between 0 and 100.");
		}
	}

	private void validateMonthlyLimit(JsonNode value) {
		requireOnlyFields(value, "value", "period");
		int requests = requiredInt(value, "value");
		if (requests < 0) {
			throw validationFailed("Monthly request limit cannot be negative.");
		}
		if (!"monthly".equals(text(value, "period"))) {
			throw validationFailed("AI usage period must be monthly.");
		}
	}

	private void validateAiModel(JsonNode value) {
		requireOnlyFields(value, "provider", "model");
		String provider = lowerText(value, "provider");
		if (!AI_PROVIDERS.contains(provider)) {
			throw validationFailed("Unsupported AI provider.");
		}
		requireText(value, "model", 120);
	}

	private void validateRubricDefaults(JsonNode value) {
		requireOnlyFields(value, "writingRubricVersion", "speakingRubricVersion");
		requireText(value, "writingRubricVersion", 80);
		requireText(value, "speakingRubricVersion", 80);
	}

	private void validateMoneyAmount(JsonNode value) {
		requireOnlyFields(value, "amount", "currency");
		BigDecimal amount = requiredDecimal(value, "amount");
		if (amount.compareTo(BigDecimal.ZERO) < 0) {
			throw validationFailed("Amount cannot be negative.");
		}
		requireCurrency(value, "currency");
	}

	private void validateMoneyValue(JsonNode value) {
		requireOnlyFields(value, "value", "currency");
		BigDecimal amount = requiredDecimal(value, "value");
		if (amount.compareTo(BigDecimal.ZERO) < 0) {
			throw validationFailed("Amount cannot be negative.");
		}
		if (value.hasNonNull("currency")) {
			requireCurrency(value, "currency");
		}
	}

	private void validatePaymentMode(JsonNode value) {
		requireOnlyFields(value, "enabled", "provider", "mode");
		requireBoolean(value, "enabled");
		String provider = lowerText(value, "provider");
		String mode = lowerText(value, "mode");
		if (!PAYMENT_PROVIDERS.contains(provider)) {
			throw validationFailed("Unsupported payment provider.");
		}
		if (!PAYMENT_MODES.contains(mode)) {
			throw validationFailed("Unsupported payment mode.");
		}
		if ("mock".equals(provider) != "mock".equals(mode)) {
			throw validationFailed("Mock payment provider must use mock mode.");
		}
	}

	private void validatePaymentOptions(JsonNode value) {
		requireOnlyFields(value, "defaultProvider", "allowedProviders");
		String defaultProvider = lowerText(value, "defaultProvider");
		JsonNode providers = value.path("allowedProviders");
		if (!providers.isArray() || providers.isEmpty()) {
			throw validationFailed("Payment provider options must include allowed providers.");
		}
		List<String> allowed = new ArrayList<>();
		for (JsonNode provider : providers) {
			String normalized = lowerText(provider);
			if (!PAYMENT_PROVIDERS.contains(normalized)) {
				throw validationFailed("Unsupported payment provider.");
			}
			allowed.add(normalized);
		}
		if (!allowed.contains(defaultProvider)) {
			throw validationFailed("Default payment provider must be allowed.");
		}
	}

	private void validateCurrencyValue(JsonNode value) {
		requireOnlyFields(value, "value");
		requireCurrency(value, "value");
	}

	private void validateModerationBehavior(JsonNode value) {
		requireOnlyFields(value, "autoHideThreshold", "manualReviewRequired");
		int threshold = requiredInt(value, "autoHideThreshold");
		if (threshold < 0 || threshold > 100) {
			throw validationFailed("Moderation threshold must be between 0 and 100.");
		}
		requireBoolean(value, "manualReviewRequired");
	}

	private void validateRichTextLimits(JsonNode value) {
		requireOnlyFields(value, "maxDocumentBytes", "allowedEmbedPurposes");
		int maxDocumentBytes = requiredInt(value, "maxDocumentBytes");
		if (maxDocumentBytes < 1024 || maxDocumentBytes > 1_048_576) {
			throw validationFailed("Rich-text document size must be between 1024 and 1048576 bytes.");
		}
		JsonNode purposes = value.path("allowedEmbedPurposes");
		if (!purposes.isArray() || purposes.isEmpty()) {
			throw validationFailed("Rich-text embed purposes are required.");
		}
		for (JsonNode purposeNode : purposes) {
			FilePurpose purpose = FilePurpose.from(text(purposeNode));
			if (!RICH_TEXT_EMBED_PURPOSES.contains(purpose)) {
				throw validationFailed("Rich-text embed purpose is not allowed.");
			}
		}
	}

	private void validateGoogleCalendarMeet(JsonNode value) {
		requireOnlyFields(value, "enabled", "mode", "calendarOwner");
		boolean enabled = requireBoolean(value, "enabled");
		String mode = lowerText(value, "mode");
		String owner = lowerText(value, "calendarOwner");
		if (!GOOGLE_MODES.contains(mode)) {
			throw validationFailed("Unsupported Google Calendar/Meet mode.");
		}
		if (!GOOGLE_OWNERS.contains(owner)) {
			throw validationFailed("Unsupported Google calendar owner.");
		}
		if (enabled && "disabled".equals(mode)) {
			throw validationFailed("Enabled Google integration requires a non-disabled mode.");
		}
		if ("platform_calendar".equals(mode) && !"platform".equals(owner)) {
			throw validationFailed("Platform calendar mode requires platform owner.");
		}
		if ("teacher_oauth".equals(mode) && !"teacher".equals(owner)) {
			throw validationFailed("Teacher OAuth mode requires teacher owner.");
		}
	}

	private void validateLiveSpeakingCancellation(JsonNode value) {
		requireOnlyFields(value, "studentCancellationWindowHours", "teacherCancellationWindowHours");
		int studentWindow = requiredInt(value, "studentCancellationWindowHours");
		int teacherWindow = requiredInt(value, "teacherCancellationWindowHours");
		if (studentWindow < 0 || studentWindow > 168 || teacherWindow < 0 || teacherWindow > 168) {
			throw validationFailed("Cancellation windows must be between 0 and 168 hours.");
		}
	}

	private void validateLanguageValue(JsonNode value) {
		requireOnlyFields(value, "value");
		String language = text(value, "value");
		if (!LANGUAGE_PATTERN.matcher(language).matches()) {
			throw validationFailed("Language must be a supported locale code.");
		}
	}

	private void rejectSecretFields(JsonNode value) {
		if (value.isObject()) {
			value.fieldNames().forEachRemaining(fieldName -> {
				String normalized = fieldName.toLowerCase(Locale.ROOT).replace("-", "").replace(".", "");
				if (SECRET_FIELD_TOKENS.stream().anyMatch(normalized::contains)) {
					throw validationFailed("Setting values cannot include secret fields.");
				}
				rejectSecretFields(value.path(fieldName));
			});
		}
		else if (value.isArray()) {
			for (JsonNode child : value) {
				rejectSecretFields(child);
			}
		}
	}

	private void requireGoogleConfigWhenEnabled(JsonNode value) {
		if (!value.path("enabled").asBoolean(false)) {
			return;
		}
		String mode = lowerText(value, "mode");
		if ("platform_calendar".equals(mode) && (!realSecret(properties.google().clientId())
				|| !realSecret(properties.google().clientSecret())
				|| !realSecret(properties.google().platformCalendarId()))) {
			throw integrationConfigurationMissing();
		}
		if ("teacher_oauth".equals(mode) && (!realSecret(properties.google().clientId())
				|| !realSecret(properties.google().clientSecret()))) {
			throw integrationConfigurationMissing();
		}
	}

	private boolean realSecret(String value) {
		String normalized = trimToNull(value);
		return normalized != null && !normalized.toLowerCase(Locale.ROOT).startsWith("dev-only-");
	}

	private void requireOnlyFields(JsonNode value, String... fields) {
		Set<String> allowed = Set.of(fields);
		value.fieldNames().forEachRemaining(fieldName -> {
			if (!allowed.contains(fieldName)) {
				throw validationFailed("Setting value contains unsupported fields.");
			}
		});
	}

	private BigDecimal requiredDecimal(JsonNode value, String field) {
		JsonNode node = value.path(field);
		try {
			if (node.isNumber()) {
				return node.decimalValue();
			}
			if (node.isTextual()) {
				return new BigDecimal(node.asText());
			}
		}
		catch (NumberFormatException ex) {
			throw validationFailed("Setting numeric value is invalid.");
		}
		throw validationFailed("Setting numeric value is required.");
	}

	private int requiredInt(JsonNode value, String field) {
		JsonNode node = value.path(field);
		if (!node.canConvertToInt() || node.decimalValue().stripTrailingZeros().scale() > 0) {
			throw validationFailed("Setting integer value is required.");
		}
		return node.asInt();
	}

	private boolean requireBoolean(JsonNode value, String field) {
		JsonNode node = value.path(field);
		if (!node.isBoolean()) {
			throw validationFailed("Setting boolean value is required.");
		}
		return node.asBoolean();
	}

	private String requireText(JsonNode value, String field, int maxLength) {
		String text = text(value, field);
		if (text.length() > maxLength) {
			throw validationFailed("Setting text value is too long.");
		}
		return text;
	}

	private String text(JsonNode value, String field) {
		return text(value.path(field));
	}

	private String text(JsonNode value) {
		if (!value.isTextual() || value.asText().isBlank()) {
			throw validationFailed("Setting text value is required.");
		}
		return value.asText().trim();
	}

	private String lowerText(JsonNode value, String field) {
		return lowerText(value.path(field));
	}

	private String lowerText(JsonNode value) {
		return text(value).toLowerCase(Locale.ROOT);
	}

	private void requireCurrency(JsonNode value, String field) {
		String currency = text(value.path(field)).toUpperCase(Locale.ROOT);
		if (!CURRENCY_PATTERN.matcher(currency).matches()) {
			throw validationFailed("Currency must be a 3-letter code.");
		}
	}

	private BigDecimal decimalValue(String key, String field) {
		return requiredDecimal(settingValue(key), field);
	}

	private int intValue(String key, String field) {
		return requiredInt(settingValue(key), field);
	}

	private String textValue(String key, String field) {
		return text(settingValue(key), field);
	}

	private String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isBlank() ? null : trimmed;
	}

	private BusinessException validationFailed(String message) {
		return new BusinessException("VALIDATION_FAILED", message, HttpStatus.BAD_REQUEST);
	}

	private BusinessException platformSettingInvalid(String key) {
		return new BusinessException(
				"PLATFORM_SETTING_INVALID",
				"Platform setting " + key + " is invalid.",
				HttpStatus.CONFLICT);
	}

	private BusinessException integrationConfigurationMissing() {
		return new BusinessException(
				"INTEGRATION_CONFIGURATION_MISSING",
				"Required external integration configuration is missing.",
				HttpStatus.CONFLICT);
	}

	private record SettingDefinition(String key, String defaultJson, Consumer<JsonNode> validator) {
	}
}
