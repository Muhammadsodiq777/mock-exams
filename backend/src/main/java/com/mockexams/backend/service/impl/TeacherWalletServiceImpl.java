package com.mockexams.backend.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mockexams.backend.dto.TeacherWalletRDO;
import com.mockexams.backend.dto.TeacherWalletTransactionRDO;
import com.mockexams.backend.entity.Purchase;
import com.mockexams.backend.entity.PurchaseStatuses;
import com.mockexams.backend.entity.TeacherTransaction;
import com.mockexams.backend.entity.TeacherTransactionSources;
import com.mockexams.backend.entity.TeacherTransactionTypes;
import com.mockexams.backend.entity.TeacherWallet;
import com.mockexams.backend.entity.schedules.SpeakingSessionBooking;
import com.mockexams.backend.entity.schedules.SpeakingSessionBookingStatuses;
import com.mockexams.backend.error.BusinessException;
import com.mockexams.backend.mapper.TeacherWalletMapper;
import com.mockexams.backend.repository.PurchaseRepository;
import com.mockexams.backend.repository.TeacherTransactionRepository;
import com.mockexams.backend.repository.TeacherWalletRepository;
import com.mockexams.backend.repository.schedules.SpeakingSessionBookingRepository;
import com.mockexams.backend.service.PlatformSettingService;
import com.mockexams.backend.service.TeacherWalletService;
import com.mockexams.backend.teachers.entity.TeacherProfile;
import com.mockexams.backend.teachers.repository.TeacherProfileRepository;
import com.mockexams.backend.users.entity.User;

@Service
class TeacherWalletServiceImpl implements TeacherWalletService {

	private static final BigDecimal ONE_HUNDRED = new BigDecimal("100.00");

	private final TeacherWalletRepository walletRepository;
	private final TeacherTransactionRepository transactionRepository;
	private final PurchaseRepository purchaseRepository;
	private final SpeakingSessionBookingRepository bookingRepository;
	private final TeacherProfileRepository teacherProfileRepository;
	private final PlatformSettingService platformSettingService;
	private final TeacherWalletMapper mapper;

	TeacherWalletServiceImpl(
			TeacherWalletRepository walletRepository,
			TeacherTransactionRepository transactionRepository,
			PurchaseRepository purchaseRepository,
			SpeakingSessionBookingRepository bookingRepository,
			TeacherProfileRepository teacherProfileRepository,
			PlatformSettingService platformSettingService,
			TeacherWalletMapper mapper
	) {
		this.walletRepository = walletRepository;
		this.transactionRepository = transactionRepository;
		this.purchaseRepository = purchaseRepository;
		this.bookingRepository = bookingRepository;
		this.teacherProfileRepository = teacherProfileRepository;
		this.platformSettingService = platformSettingService;
		this.mapper = mapper;
	}

	@Override
	@Transactional
	public TeacherWalletRDO myWallet(User teacher) {
		TeacherProfile profile = requireTeacherProfile(teacher);
		return mapper.toRDO(getOrCreateWallet(profile, "UZS"));
	}

	@Override
	@Transactional
	public Page<TeacherWalletTransactionRDO> myTransactions(User teacher, Pageable pageable) {
		TeacherProfile profile = requireTeacherProfile(teacher);
		TeacherWallet wallet = getOrCreateWallet(profile, "UZS");
		return transactionRepository.findByWallet_Id(wallet.getId(), pageable).map(mapper::toRDO);
	}

	@Override
	@Transactional
	public Optional<TeacherWalletTransactionRDO> recordMarketplacePurchaseEarning(Long purchaseId) {
		TeacherTransaction existing = transactionRepository
				.findByPurchaseIdAndTransactionType(purchaseId, TeacherTransactionTypes.EARNING)
				.orElse(null);
		if (existing != null) {
			return Optional.of(mapper.toRDO(existing));
		}
		Purchase purchase = requirePurchase(purchaseId);
		if (!PurchaseStatuses.GRANTED.equals(purchase.getStatus()) || purchase.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
			return Optional.empty();
		}
		TeacherProfile profile = purchase.getListing().getTeacherProfile();
		if (profile == null) {
			throw teacherProfileMissing();
		}
		String sourceReference = earningSourceReference(purchase);
		existing = transactionRepository
				.findBySourceTypeAndSourceReferenceAndTransactionType(
						"marketplace_purchase",
						sourceReference,
						TeacherTransactionTypes.EARNING)
				.orElse(null);
		if (existing != null) {
			return Optional.of(mapper.toRDO(existing));
		}
		TeacherWallet wallet = getOrCreateWallet(profile, normalizeCurrency(purchase.getCurrency()));
		requireWalletCurrency(wallet, purchase.getCurrency());

		BigDecimal gross = money(purchase.getAmount());
		BigDecimal commission = commissionAmount(gross);
		BigDecimal net = gross.subtract(commission);
		TeacherTransaction transaction = TeacherTransaction.earning(
				wallet,
				purchase,
				commission,
				net,
				sourceReference,
				"Marketplace purchase earning");
		wallet.credit(net);
		TeacherTransaction saved = transactionRepository.save(transaction);
		return Optional.of(mapper.toRDO(saved));
	}

	@Override
	@Transactional
	public Optional<TeacherWalletTransactionRDO> recordLiveSpeakingBookingEarning(Long bookingId, Long paymentId) {
		if (bookingId == null || bookingId < 1 || paymentId == null || paymentId < 1) {
			throw validationFailed("Booking and payment ids are required.");
		}
		String sourceReference = "payment:" + paymentId;
		TeacherTransaction existing = transactionRepository
				.findBySourceTypeAndSourceReferenceAndTransactionType(
						TeacherTransactionSources.LIVE_SPEAKING_BOOKING,
						sourceReference,
						TeacherTransactionTypes.EARNING)
				.orElse(null);
		if (existing != null) {
			return Optional.of(mapper.toRDO(existing));
		}
		SpeakingSessionBooking booking = bookingRepository.findById(bookingId).orElseThrow(this::bookingNotFound);
		if (!SpeakingSessionBookingStatuses.BOOKED.equals(booking.getStatus())
				|| booking.getPriceAmount().compareTo(BigDecimal.ZERO) <= 0) {
			return Optional.empty();
		}
		TeacherWallet wallet = getOrCreateWallet(booking.getTeacherProfile(), normalizeCurrency(booking.getCurrency()));
		requireWalletCurrency(wallet, booking.getCurrency());

		BigDecimal gross = money(booking.getPriceAmount());
		BigDecimal commission = liveSpeakingCommissionAmount(gross);
		BigDecimal net = gross.subtract(commission);
		TeacherTransaction transaction = TeacherTransaction.liveSpeakingEarning(
				wallet,
				booking,
				paymentId,
				commission,
				net,
				sourceReference,
				"Live speaking booking earning");
		wallet.credit(net);
		return Optional.of(mapper.toRDO(transactionRepository.save(transaction)));
	}

	@Override
	@Transactional
	public TeacherWalletRDO reserveWithdrawal(Long teacherProfileId, BigDecimal amount, String currency, String sourceReference) {
		TeacherWallet wallet = walletForTeacherProfile(teacherProfileId, currency);
		BigDecimal reservationAmount = positiveMoney(amount);
		requireWalletCurrency(wallet, currency);
		requireAvailable(wallet, reservationAmount);
		wallet.reserve(reservationAmount);
		return mapper.toRDO(wallet);
	}

	@Override
	@Transactional
	public TeacherWalletRDO releaseWithdrawalReservation(Long teacherProfileId, BigDecimal amount, String currency) {
		TeacherWallet wallet = walletForTeacherProfile(teacherProfileId, currency);
		BigDecimal releaseAmount = positiveMoney(amount);
		requireWalletCurrency(wallet, currency);
		if (wallet.getPendingAmount().compareTo(releaseAmount) < 0) {
			throw insufficientBalance();
		}
		wallet.releaseReservation(releaseAmount);
		return mapper.toRDO(wallet);
	}

	@Override
	@Transactional
	public TeacherWalletTransactionRDO recordApprovedWithdrawal(
			Long teacherProfileId,
			BigDecimal amount,
			String currency,
			String sourceReference
	) {
		String normalizedSourceReference = requireSourceReference(sourceReference);
		TeacherTransaction existing = existingWithdrawalTransaction(normalizedSourceReference);
		if (existing != null) {
			return mapper.toRDO(existing);
		}
		// Acquire the wallet's pessimistic write lock, then re-check idempotency before settling.
		// Two concurrent approvals for the same withdrawal can both pass the check above before
		// either commits; the lock below serializes them so only the winner reaches this point
		// first. The loser, once unblocked, must see the winner's already-committed transaction and
		// fail as a business error (its withdrawal request is no longer approvable) instead of racing
		// into the unique constraint on insert and surfacing a generic conflict/500.
		TeacherWallet wallet = walletForTeacherProfile(teacherProfileId, currency);
		if (existingWithdrawalTransaction(normalizedSourceReference) != null) {
			throw withdrawalAlreadySettled();
		}
		BigDecimal withdrawalAmount = positiveMoney(amount);
		requireWalletCurrency(wallet, currency);
		if (wallet.getPendingAmount().compareTo(withdrawalAmount) < 0) {
			throw insufficientBalance();
		}
		wallet.settleReservation(withdrawalAmount);
		TeacherTransaction transaction = TeacherTransaction.withdrawal(
				wallet,
				withdrawalAmount,
				normalizeCurrency(currency),
				normalizedSourceReference,
				"Approved withdrawal");
		return mapper.toRDO(transactionRepository.save(transaction));
	}

	private TeacherTransaction existingWithdrawalTransaction(String normalizedSourceReference) {
		return transactionRepository
				.findBySourceTypeAndSourceReferenceAndTransactionType(
						"withdrawal_request",
						normalizedSourceReference,
						TeacherTransactionTypes.WITHDRAWAL)
				.orElse(null);
	}

	@Override
	@Transactional
	public Optional<TeacherWalletTransactionRDO> recordMarketplacePurchaseRefund(Long purchaseId, String sourceReference) {
		Purchase purchase = requirePurchase(purchaseId);
		String normalizedSourceReference = requireSourceReference(sourceReference);
		TeacherTransaction existingRefund = transactionRepository
				.findBySourceTypeAndSourceReferenceAndTransactionType(
						"refund",
						normalizedSourceReference,
						TeacherTransactionTypes.REFUND)
				.orElse(null);
		if (existingRefund != null) {
			return Optional.of(mapper.toRDO(existingRefund));
		}
		TeacherTransaction earning = transactionRepository
				.findByPurchaseIdAndTransactionType(purchaseId, TeacherTransactionTypes.EARNING)
				.orElse(null);
		if (earning == null) {
			return Optional.empty();
		}
		TeacherWallet wallet = walletForTeacherProfile(purchase.getListing().getTeacherProfile().getId(), purchase.getCurrency());
		requireWalletCurrency(wallet, purchase.getCurrency());
		requireAvailable(wallet, earning.getNetAmount());
		wallet.debit(earning.getNetAmount());
		TeacherTransaction refund = TeacherTransaction.refund(
				wallet,
				purchase,
				earning,
				normalizedSourceReference,
				"Marketplace purchase refund");
		return Optional.of(mapper.toRDO(transactionRepository.save(refund)));
	}

	private TeacherProfile requireTeacherProfile(User teacher) {
		if (teacher == null || teacher.getId() == null) {
			throw teacherProfileMissing();
		}
		return teacherProfileRepository.findByUserId(teacher.getId()).orElseThrow(this::teacherProfileMissing);
	}

	private TeacherWallet walletForTeacherProfile(Long teacherProfileId, String currency) {
		TeacherProfile profile = teacherProfileRepository.findById(teacherProfileId).orElseThrow(this::teacherProfileMissing);
		return getOrCreateWallet(profile, normalizeCurrency(currency));
	}

	private TeacherWallet getOrCreateWallet(TeacherProfile profile, String currency) {
		return walletRepository.findByTeacherProfile_Id(profile.getId())
				.orElseGet(() -> createWallet(profile, currency));
	}

	private TeacherWallet createWallet(TeacherProfile profile, String currency) {
		try {
			return walletRepository.saveAndFlush(TeacherWallet.create(profile, normalizeCurrency(currency)));
		}
		catch (DataIntegrityViolationException ex) {
			return walletRepository.findByTeacherProfile_Id(profile.getId()).orElseThrow(() -> ex);
		}
	}

	private Purchase requirePurchase(Long purchaseId) {
		if (purchaseId == null || purchaseId < 1) {
			throw validationFailed("Purchase id is required.");
		}
		return purchaseRepository.findById(purchaseId).orElseThrow(this::purchaseNotFound);
	}

	private BigDecimal commissionAmount(BigDecimal gross) {
		return money(gross.multiply(platformSettingService.marketplaceCommissionPercent())
				.divide(ONE_HUNDRED, 4, RoundingMode.HALF_UP));
	}

	private BigDecimal liveSpeakingCommissionAmount(BigDecimal gross) {
		return money(gross.multiply(platformSettingService.liveSpeakingPlatformCommissionPercent())
				.divide(ONE_HUNDRED, 4, RoundingMode.HALF_UP));
	}

	private String earningSourceReference(Purchase purchase) {
		if (purchase.getPaymentId() != null) {
			return "payment:" + purchase.getPaymentId();
		}
		return "purchase:" + purchase.getId();
	}

	private void requireWalletCurrency(TeacherWallet wallet, String currency) {
		String normalizedCurrency = normalizeCurrency(currency);
		if (!wallet.getCurrency().equals(normalizedCurrency)) {
			throw new BusinessException(
					"WALLET_CURRENCY_MISMATCH",
					"Wallet currency does not match the source transaction currency.",
					HttpStatus.CONFLICT);
		}
	}

	private void requireAvailable(TeacherWallet wallet, BigDecimal amount) {
		if (wallet.getBalanceAmount().compareTo(amount) < 0) {
			throw insufficientBalance();
		}
	}

	private BigDecimal positiveMoney(BigDecimal amount) {
		if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
			throw validationFailed("Amount must be greater than zero.");
		}
		return money(amount);
	}

	private BigDecimal money(BigDecimal amount) {
		return amount.setScale(2, RoundingMode.HALF_UP);
	}

	private String normalizeCurrency(String currency) {
		String normalized = trimToNull(currency);
		if (normalized == null || normalized.length() != 3) {
			throw validationFailed("Currency is invalid.");
		}
		return normalized.toUpperCase(Locale.ROOT);
	}

	private String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isBlank() ? null : trimmed;
	}

	private String requireSourceReference(String sourceReference) {
		String normalized = trimToNull(sourceReference);
		if (normalized == null) {
			throw validationFailed("Source reference is required.");
		}
		return normalized;
	}

	private BusinessException validationFailed(String message) {
		return new BusinessException("VALIDATION_FAILED", message, HttpStatus.BAD_REQUEST);
	}

	private BusinessException teacherProfileMissing() {
		return new BusinessException("TEACHER_PROFILE_NOT_FOUND", "Teacher profile was not found.", HttpStatus.NOT_FOUND);
	}

	private BusinessException purchaseNotFound() {
		return new BusinessException("PURCHASE_NOT_FOUND", "Purchase was not found.", HttpStatus.NOT_FOUND);
	}

	private BusinessException bookingNotFound() {
		return new BusinessException("SCHEDULE_BOOKING_NOT_FOUND", "Schedule booking was not found.", HttpStatus.NOT_FOUND);
	}

	private BusinessException insufficientBalance() {
		return new BusinessException(
				"WALLET_INSUFFICIENT_BALANCE",
				"Wallet balance is insufficient for this operation.",
				HttpStatus.CONFLICT);
	}

	private BusinessException withdrawalAlreadySettled() {
		return new BusinessException(
				"WALLET_LEDGER_ALREADY_SETTLED",
				"A ledger transaction for this withdrawal was already recorded by a concurrent approval.",
				HttpStatus.CONFLICT);
	}

}
