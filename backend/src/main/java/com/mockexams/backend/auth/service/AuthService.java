package com.mockexams.backend.auth.service;

import com.mockexams.backend.auth.dto.AuthSessionRDO;
import com.mockexams.backend.auth.dto.CurrentUserRDO;
import com.mockexams.backend.auth.dto.LoginCDO;
import com.mockexams.backend.auth.dto.RegisterStudentCDO;
import com.mockexams.backend.auth.dto.RegisterTeacherCDO;
import com.mockexams.backend.users.entity.User;

public interface AuthService {

	AuthSessionRDO registerStudent(RegisterStudentCDO command);

	AuthSessionRDO registerTeacher(RegisterTeacherCDO command);

	AuthSessionRDO login(LoginCDO command);

	CurrentUserRDO currentUser(User user);
}
