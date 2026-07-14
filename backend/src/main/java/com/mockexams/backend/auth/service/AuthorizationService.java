package com.mockexams.backend.auth.service;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mockexams.backend.error.BusinessException;
import com.mockexams.backend.users.entity.User;
import com.mockexams.backend.users.repository.UserRepository;

@Service
public class AuthorizationService {

	private static final String BEARER_PREFIX = "Bearer ";

	private final JwtAccessTokenService jwtAccessTokenService;
	private final UserRepository userRepository;

	public AuthorizationService(JwtAccessTokenService jwtAccessTokenService, UserRepository userRepository) {
		this.jwtAccessTokenService = jwtAccessTokenService;
		this.userRepository = userRepository;
	}

	@Transactional(readOnly = true)
	public User requireActiveUser(String authorizationHeader) {
		Long userId = jwtAccessTokenService.requireSubject(accessTokenFrom(authorizationHeader));
		User user = userRepository.findWithRolesById(userId)
				.orElseThrow(() -> BusinessException.tokenInvalid("Invalid access token."));
		requireActive(user);
		return user;
	}

	@Transactional(readOnly = true)
	public User requireRole(String authorizationHeader, String... allowedRoleCodes) {
		User user = requireActiveUser(authorizationHeader);
		Set<String> userRoles = user.getUserRoles().stream()
				.map(userRole -> userRole.getRole().getCode())
				.collect(Collectors.toSet());
		boolean hasAllowedRole = Arrays.stream(allowedRoleCodes).anyMatch(userRoles::contains);
		if (!hasAllowedRole) {
			throw BusinessException.forbidden("User does not have the required role.");
		}
		return user;
	}

	private void requireActive(User user) {
		if (!user.canLogin()) {
			throw BusinessException.accountInactive("Account is not allowed to access this resource.");
		}
	}

	private String accessTokenFrom(String authorizationHeader) {
		if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
			throw BusinessException.authenticationRequired("Bearer access token is required.");
		}
		String token = authorizationHeader.substring(BEARER_PREFIX.length()).trim();
		if (token.isBlank()) {
			throw BusinessException.authenticationRequired("Bearer access token is required.");
		}
		return token;
	}
}
