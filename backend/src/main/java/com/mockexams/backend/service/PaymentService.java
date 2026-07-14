package com.mockexams.backend.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.mockexams.backend.dto.PaymentCDO;
import com.mockexams.backend.dto.PaymentRDO;
import com.mockexams.backend.dto.PromoQuoteRDO;
import com.mockexams.backend.users.entity.User;

public interface PaymentService {

	PaymentRDO createPayment(User student, PaymentCDO request);

	PromoQuoteRDO promoQuote(User student, Long listingId, String promoCode);

	PaymentRDO studentPayment(User student, Long paymentId);

	List<PaymentRDO> studentPayments(User student);

	PaymentRDO adminPayment(Long paymentId);

	Page<PaymentRDO> adminPayments(String status, Pageable pageable);

	PaymentRDO handleProviderWebhook(String rawBody, String signature);
}
