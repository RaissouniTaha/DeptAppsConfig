package com.everis.deptappsconfig.security.service;

import java.io.IOException;
import java.util.List;

import javax.mail.MessagingException;

import org.springframework.web.multipart.MultipartFile;

import com.everis.deptappsconfig.security.domain.User;
import com.everis.deptappsconfig.security.exception.domain.EmailExistException;
import com.everis.deptappsconfig.security.exception.domain.EmailNotFoundException;
import com.everis.deptappsconfig.security.exception.domain.NotAnImageFileException;
import com.everis.deptappsconfig.security.exception.domain.UserNotFoundException;
import com.everis.deptappsconfig.security.exception.domain.UsernameExistException;

public interface UserService {

	User register(String firstName, String lastName, String username, String email) throws EmailExistException, UserNotFoundException, UsernameExistException, MessagingException;
	
	List<User> getUsers();
	
	User findUserByUsername(String username);
	
	User findUserByEmail(String email);
	
	User addNewUser(String firstName, String lastName, String username, String email, String role, boolean isNotLocked, boolean isActive, MultipartFile profileImage) throws EmailExistException, UserNotFoundException, UsernameExistException, IOException, MessagingException, NotAnImageFileException;
	
	User updateUser(String currentUsername, String newFirstName, String newLastName, String newUsername, String newEmail, String role, boolean isNotLocked, boolean isActive, MultipartFile profileImage) throws EmailExistException, UserNotFoundException, UsernameExistException, IOException, NotAnImageFileException;
	
	void deleteUser(String username) throws IOException;
	
	void resetPassword(String email) throws EmailNotFoundException, MessagingException;
	
	User updateProfileImage(String username, MultipartFile profileImage) throws EmailExistException, UserNotFoundException, UsernameExistException, IOException, NotAnImageFileException;
}
