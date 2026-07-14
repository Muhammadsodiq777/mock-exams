package com.mockexams.backend.mapper;

import org.springframework.stereotype.Component;

import com.mockexams.backend.dto.PromoCodeRDO;
import com.mockexams.backend.entity.PromoCode;

@Component
public class PromoCodeMapper {

	public PromoCodeRDO toRDO(PromoCode promoCode) {
		return new PromoCodeRDO(
				promoCode.getId(),
				promoCode.getTeacherProfile() == null ? null : promoCode.getTeacherProfile().getId(),
				promoCode.getCode(),
				promoCode.getDiscountPercent(),
				promoCode.getDiscountAmount(),
				promoCode.getCurrency(),
				promoCode.getStartsAt(),
				promoCode.getExpiresAt(),
				promoCode.getUsageLimit(),
				promoCode.getUsedCount(),
				promoCode.isActive(),
				promoCode.getCreatedAt(),
				promoCode.getUpdatedAt());
	}
}
