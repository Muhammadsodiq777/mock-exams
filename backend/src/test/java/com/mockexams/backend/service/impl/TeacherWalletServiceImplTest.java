package com.mockexams.backend.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import com.mockexams.backend.dto.TeacherWalletRDO;
import com.mockexams.backend.entity.TeacherWallet;
import com.mockexams.backend.mapper.TeacherWalletMapper;
import com.mockexams.backend.repository.PurchaseRepository;
import com.mockexams.backend.repository.TeacherTransactionRepository;
import com.mockexams.backend.repository.TeacherWalletRepository;
import com.mockexams.backend.repository.schedules.SpeakingSessionBookingRepository;
import com.mockexams.backend.service.PlatformSettingService;
import com.mockexams.backend.teachers.entity.TeacherProfile;
import com.mockexams.backend.teachers.repository.TeacherProfileRepository;
import com.mockexams.backend.users.entity.User;

class TeacherWalletServiceImplTest {

	@Test
	@SuppressWarnings("unchecked")
	void walletCreationRaceFallsBackToExistingWalletDeterministically() {
		TeacherWalletRepository walletRepository = mock(TeacherWalletRepository.class);
		TeacherProfileRepository teacherProfileRepository = mock(TeacherProfileRepository.class);
		TeacherWalletServiceImpl service = new TeacherWalletServiceImpl(
				walletRepository,
				mock(TeacherTransactionRepository.class),
				mock(PurchaseRepository.class),
				mock(SpeakingSessionBookingRepository.class),
				teacherProfileRepository,
				mock(PlatformSettingService.class),
				new TeacherWalletMapper());

		User teacher = mock(User.class);
		when(teacher.getId()).thenReturn(7L);
		TeacherProfile profile = mock(TeacherProfile.class);
		when(profile.getId()).thenReturn(22L);
		when(teacherProfileRepository.findByUserId(7L)).thenReturn(Optional.of(profile));

		OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
		TeacherWallet concurrentlyCreatedWallet = mock(TeacherWallet.class);
		when(concurrentlyCreatedWallet.getId()).thenReturn(9101L);
		when(concurrentlyCreatedWallet.getTeacherProfile()).thenReturn(profile);
		when(concurrentlyCreatedWallet.getBalanceAmount()).thenReturn(new BigDecimal("15000.00"));
		when(concurrentlyCreatedWallet.getPendingAmount()).thenReturn(BigDecimal.ZERO);
		when(concurrentlyCreatedWallet.getCurrency()).thenReturn("UZS");
		when(concurrentlyCreatedWallet.getCreatedAt()).thenReturn(now);
		when(concurrentlyCreatedWallet.getUpdatedAt()).thenReturn(now);

		when(walletRepository.findByTeacherProfile_Id(22L))
				.thenReturn(Optional.empty(), Optional.of(concurrentlyCreatedWallet));
		when(walletRepository.saveAndFlush(any(TeacherWallet.class)))
				.thenThrow(new DataIntegrityViolationException("uq_teacher_wallets_profile"));

		TeacherWalletRDO wallet = service.myWallet(teacher);

		assertThat(wallet.id()).isEqualTo(9101L);
		assertThat(wallet.teacherProfileId()).isEqualTo(22L);
		assertThat(wallet.balanceAmount()).isEqualByComparingTo("15000.00");
		verify(walletRepository, times(1)).saveAndFlush(any(TeacherWallet.class));
		verify(walletRepository, times(2)).findByTeacherProfile_Id(22L);
	}
}
