package com.mockexams.backend.entity;

import java.util.Set;

public final class MarketplaceListingTypes {

	public static final String SINGLE_TEST = "single_test";
	public static final String TEST_PACK = "test_pack";
	public static final String WRITING_COURSE = "writing_course";
	public static final String SPEAKING_QUESTION_PACK = "speaking_question_pack";
	public static final String VOCABULARY_PACK = "vocabulary_pack";
	public static final String FULL_MOCK_EXAM = "full_mock_exam";
	public static final String SUBSCRIPTION_PLAN = "subscription_plan";

	private static final Set<String> SUPPORTED = Set.of(
			SINGLE_TEST,
			TEST_PACK,
			WRITING_COURSE,
			SPEAKING_QUESTION_PACK,
			VOCABULARY_PACK,
			FULL_MOCK_EXAM,
			SUBSCRIPTION_PLAN);

	private static final Set<String> DIRECT_TEST_TYPES = Set.of(SINGLE_TEST, FULL_MOCK_EXAM);

	private MarketplaceListingTypes() {
	}

	public static boolean isSupported(String listingType) {
		return SUPPORTED.contains(listingType);
	}

	public static boolean isDirectTestListing(String listingType) {
		return DIRECT_TEST_TYPES.contains(listingType);
	}
}
