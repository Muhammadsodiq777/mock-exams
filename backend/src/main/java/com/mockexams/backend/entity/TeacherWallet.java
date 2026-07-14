package com.mockexams.backend.entity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import com.mockexams.backend.teachers.entity.TeacherProfile;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "teacher_wallets")
public class TeacherWallet {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "teacher_profile_id")
	private TeacherProfile teacherProfile;

	@Column(nullable = false, precision = 14, scale = 2)
	private BigDecimal balanceAmount = BigDecimal.ZERO;

	@Column(nullable = false, precision = 14, scale = 2)
	private BigDecimal pendingAmount = BigDecimal.ZERO;

	@Column(nullable = false, length = 3)
	private String currency = "UZS";

	@Column(nullable = false)
	private OffsetDateTime createdAt;

	@Column(nullable = false)
	private OffsetDateTime updatedAt;

	protected TeacherWallet() {
	}

	public static TeacherWallet create(TeacherProfile teacherProfile, String currency) {
		TeacherWallet wallet = new TeacherWallet();
		wallet.teacherProfile = teacherProfile;
		wallet.currency = currency;
		return wallet;
	}

	@PrePersist
	void prePersist() {
		OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
		if (createdAt == null) {
			createdAt = now;
		}
		updatedAt = now;
	}

	@PreUpdate
	void preUpdate() {
		updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
	}

	public void credit(BigDecimal amount) {
		balanceAmount = balanceAmount.add(amount);
	}

	public void reserve(BigDecimal amount) {
		balanceAmount = balanceAmount.subtract(amount);
		pendingAmount = pendingAmount.add(amount);
	}

	public void releaseReservation(BigDecimal amount) {
		pendingAmount = pendingAmount.subtract(amount);
		balanceAmount = balanceAmount.add(amount);
	}

	public void settleReservation(BigDecimal amount) {
		pendingAmount = pendingAmount.subtract(amount);
	}

	public void debit(BigDecimal amount) {
		balanceAmount = balanceAmount.subtract(amount);
	}

	public Long getId() {
		return id;
	}

	public TeacherProfile getTeacherProfile() {
		return teacherProfile;
	}

	public BigDecimal getBalanceAmount() {
		return balanceAmount;
	}

	public BigDecimal getPendingAmount() {
		return pendingAmount;
	}

	public String getCurrency() {
		return currency;
	}

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}

	public OffsetDateTime getUpdatedAt() {
		return updatedAt;
	}
}
