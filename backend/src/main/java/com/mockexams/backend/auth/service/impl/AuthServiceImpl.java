package com.mockexams.backend.auth.service.impl;

import java.util.Locale;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mockexams.backend.auth.dto.AuthSessionRDO;
import com.mockexams.backend.auth.dto.CurrentUserRDO;
import com.mockexams.backend.auth.dto.LoginCDO;
import com.mockexams.backend.auth.dto.RegisterStudentCDO;
import com.mockexams.backend.auth.dto.RegisterTeacherCDO;
import com.mockexams.backend.auth.mapper.CurrentUserMapper;
import com.mockexams.backend.auth.service.AuthService;
import com.mockexams.backend.auth.service.JwtAccessTokenService;
import com.mockexams.backend.auth.service.PasswordHasher;
import com.mockexams.backend.error.BusinessException;
import com.mockexams.backend.teachers.entity.TeacherProfile;
import com.mockexams.backend.teachers.repository.TeacherProfileRepository;
import com.mockexams.backend.users.RoleCodes;
import com.mockexams.backend.users.entity.Role;
import com.mockexams.backend.users.entity.User;
import com.mockexams.backend.users.entity.UserRole;
import com.mockexams.backend.users.repository.RoleRepository;
import com.mockexams.backend.users.repository.UserRepository;
import com.mockexams.backend.users.repository.UserRoleRepository;

@Service
class AuthServiceImpl implements AuthService {

	private static final String TOKEN_TYPE = "Bearer";

	private final UserRepository userRepository;
	private final RoleRepository roleRepository;
	private final UserRoleRepository userRoleRepository;
	private final TeacherProfileRepository teacherProfileRepository;
	private final PasswordHasher passwordHasher;
	private final JwtAccessTokenService jwtAccessTokenService;
	private final CurrentUserMapper currentUserMapper;

	AuthServiceImpl(
			UserRepository userRepository,
			RoleRepository roleRepository,
			UserRoleRepository userRoleRepository,
			TeacherProfileRepository teacherProfileRepository,
			PasswordHasher passwordHasher,
			JwtAccessTokenService jwtAccessTokenService,
			CurrentUserMapper currentUserMapper
	) {
		this.userRepository = userRepository;
		this.roleRepository = roleRepository;
		this.userRoleRepository = userRoleRepository;
		this.teacherProfileRepository = teacherProfileRepository;
		this.passwordHasher = passwordHasher;
		this.jwtAccessTokenService = jwtAccessTokenService;
		this.currentUserMapper = currentUserMapper;
	}

	@Override
	@Transactional
	public AuthSessionRDO registerStudent(RegisterStudentCDO command) {
		User user = createUser(command.email(), command.password(), command.fullName(), command.phone(),
				command.preferredLanguage());
		assignRole(user, RoleCodes.STUDENT);
		return createSession(user.getId());
	}

	@Override
	@Transactional
	public AuthSessionRDO registerTeacher(RegisterTeacherCDO command) {
		User user = createUser(command.email(), command.password(), command.fullName(), command.phone(),
				command.preferredLanguage());
		assignRole(user, RoleCodes.TEACHER);
		teacherProfileRepository.save(new TeacherProfile(
				user,
				command.displayName().trim(),
				trimToNull(command.headline()),
				trimToNull(command.bio())));
		return createSession(user.getId());
	}

	@Override
	@Transactional(readOnly = true)
	public AuthSessionRDO login(LoginCDO command) {
		User user = userRepository.findByEmailIgnoreCase(normalizeEmail(command.email()))
				.orElseThrow(() -> BusinessException.authenticationRequired("Invalid email or password."));
		if (!passwordHasher.matches(command.password(), user.getPasswordHash())) {
			throw BusinessException.authenticationRequired("Invalid email or password.");
		}
		requireLoginAllowed(user);
		return createSession(user.getId());
	}

	@Override
	@Transactional(readOnly = true)
	public CurrentUserRDO currentUser(User user) {
		return currentUserMapper.toRDO(user, teacherProfileRepository.findByUserId(user.getId()));
	}

	private User createUser(String email, String password, String fullName, String phone, String preferredLanguage) {
		String normalizedEmail = normalizeEmail(email);
		if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
			throw duplicateEmail();
		}
		try {
			User user = new User(
					normalizedEmail,
					passwordHasher.hash(password),
					fullName.trim(),
					trimToNull(phone),
					trimToNull(preferredLanguage));
			return userRepository.saveAndFlush(user);
		}
		catch (DataIntegrityViolationException ex) {
			throw duplicateEmail();
		}
	}

	private void assignRole(User user, String roleCode) {
		Role role = roleRepository.findByCode(roleCode)
				.orElseThrow(() -> new IllegalStateException("Missing required role: " + roleCode));
		UserRole userRole = userRoleRepository.saveAndFlush(new UserRole(user, role));
		user.getUserRoles().add(userRole);
	}

	private AuthSessionRDO createSession(Long userId) {
		User user = userRepository.findWithRolesById(userId)
				.orElseThrow(() -> new IllegalStateException("User disappeared after registration."));
		JwtAccessTokenService.IssuedAccessToken accessToken = jwtAccessTokenService.issue(user.getId(), user.getEmail());
		CurrentUserRDO currentUser = currentUserMapper.toRDO(
				user,
				teacherProfileRepository.findByUserId(user.getId()));
		return new AuthSessionRDO(accessToken.token(), TOKEN_TYPE, accessToken.expiresAt(), currentUser);
	}

	private void requireLoginAllowed(User user) {
		if (!user.canLogin()) {
			throw BusinessException.accountInactive("Account is not allowed to log in.");
		}
	}

	private BusinessException duplicateEmail() {
		return new BusinessException("EMAIL_ALREADY_REGISTERED", "Email is already registered.", HttpStatus.CONFLICT);
	}

	private String normalizeEmail(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}

	private String trimToNull(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value.trim();
	}
}
