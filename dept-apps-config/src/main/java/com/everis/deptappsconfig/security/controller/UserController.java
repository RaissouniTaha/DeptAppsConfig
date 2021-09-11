package com.everis.deptappsconfig.security.controller;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import javax.mail.MessagingException;
import javax.websocket.server.PathParam;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.everis.deptappsconfig.security.constant.FileConstant;
import com.everis.deptappsconfig.security.constant.SecurityConstant;
import com.everis.deptappsconfig.security.domain.HttpResponse;
import com.everis.deptappsconfig.security.domain.User;
import com.everis.deptappsconfig.security.domain.UserPrincipal;
import com.everis.deptappsconfig.security.exception.domain.EmailExistException;
import com.everis.deptappsconfig.security.exception.domain.EmailNotFoundException;
import com.everis.deptappsconfig.security.exception.domain.ExceptionHandling;
import com.everis.deptappsconfig.security.exception.domain.NotAnImageFileException;
import com.everis.deptappsconfig.security.exception.domain.UserNotFoundException;
import com.everis.deptappsconfig.security.exception.domain.UsernameExistException;
import com.everis.deptappsconfig.security.service.UserService;
import com.everis.deptappsconfig.security.utility.JWTTokenProvider;

@RestController
@RequestMapping(path = {"/", "/user"})
public class UserController extends ExceptionHandling {

	private static final String USER_DELETED_SUCCESSFULLY = "User deleted successfully";
	private static final String EMAIL_SENT = "An email with a new password was sent to : ";
	private UserService userService;
	private AuthenticationManager authenticationManager;
	private JWTTokenProvider jWTTokenProvider;
	
	
	@Autowired
	public UserController(UserService userService, AuthenticationManager authenticationManager,
			JWTTokenProvider jWTTokenProvider) {
		this.userService = userService;
		this.authenticationManager = authenticationManager;
		this.jWTTokenProvider = jWTTokenProvider;
	}


	@PostMapping("/login")
	public ResponseEntity<User> login(@RequestBody User user) throws UserNotFoundException, EmailExistException, UsernameExistException{
		authenticate(user.getUsername(), user.getPassword());
		User loginUser = userService.findUserByUsername(user.getUsername());
		UserPrincipal userPrincipal = new UserPrincipal(loginUser);
		HttpHeaders jwtHeader = getJwtHeader(userPrincipal);
		return new ResponseEntity<>(loginUser, jwtHeader, HttpStatus.OK);
	}
	

	@PostMapping("/register")
	public ResponseEntity<User> register(@RequestBody User user) throws UserNotFoundException, EmailExistException, UsernameExistException, MessagingException{
		User newUser = userService.register(user.getFirstName(), user.getLastName(), user.getUsername(), user.getEmail());
		return new ResponseEntity<>(newUser,HttpStatus.OK);
	}
	
	@PostMapping("/add")
	public ResponseEntity<User> addNewUser(@RequestParam("firstName") String firstName,
										   @RequestParam("lastName") String lastName,
										   @RequestParam("username") String username,
										   @RequestParam("email") String email,
										   @RequestParam("role") String role,
										   @RequestParam("isActive") String isActive,
										   @RequestParam("isNotLocked") String isNotLocked,
										   @RequestParam(value = "profileImage", required = false) MultipartFile profileImage) throws EmailExistException, UserNotFoundException, UsernameExistException, IOException, MessagingException, NotAnImageFileException{
		
		User newUser = userService.addNewUser(firstName, lastName, username, email, role,
				Boolean.parseBoolean(isNotLocked), Boolean.parseBoolean(isActive), profileImage);
		return new ResponseEntity<>(newUser, HttpStatus.OK);
	}
	
	@PostMapping("/update")
	public ResponseEntity<User> update(@RequestParam("currentUsername") String currentUsername,
										   @RequestParam("firstName") String firstName,
										   @RequestParam("lastName") String lastName,
										   @RequestParam("username") String username,
										   @RequestParam("email") String email,
										   @RequestParam("role") String role,
										   @RequestParam("isActive") String isActive,
										   @RequestParam("isNotLocked") String isNotLocked,
										   @RequestParam(value = "profileImage", required = false) MultipartFile profileImage) throws EmailExistException, UserNotFoundException, UsernameExistException, IOException, NotAnImageFileException{
		
		User updatedUser = userService.updateUser(currentUsername, firstName, lastName, username, email, role,
				Boolean.parseBoolean(isNotLocked), Boolean.parseBoolean(isActive), profileImage);
		return new ResponseEntity<>(updatedUser, HttpStatus.OK);
	}
	
	@GetMapping("/list")
	public ResponseEntity<List<User>> getAllUsers(){
		List<User> users = userService.getUsers();
		return new ResponseEntity<>(users,HttpStatus.OK);
	}
	
	@GetMapping("/find/{username}")
	public ResponseEntity<User> getUser(@PathVariable("username") String username){
		User user = userService.findUserByUsername(username);
		return new ResponseEntity<>(user,HttpStatus.OK);
	}
	
	@GetMapping("/resetPassword/{email}")
	public ResponseEntity<HttpResponse> resetPassword(@PathVariable("email") String email) throws EmailNotFoundException, MessagingException{
		userService.resetPassword(email);
		return response(HttpStatus.OK, EMAIL_SENT+email);
	}
	
	@DeleteMapping("/delete/{username}")
	@PreAuthorize("hasAnyAuthority('user:delete')")
	public ResponseEntity<HttpResponse> deleteUser(@PathVariable("username") String username) throws IOException{
		userService.deleteUser(username);
		return response(HttpStatus.OK, USER_DELETED_SUCCESSFULLY);
	}
	
	@PostMapping("/updateProfileImage")
	public ResponseEntity<User> updateProfileImage(
										   @RequestParam("username") String username,
										   @RequestParam(value = "profileImage") MultipartFile profileImage) throws EmailExistException, UserNotFoundException, UsernameExistException, IOException, NotAnImageFileException{
		User user = userService.updateProfileImage(username, profileImage);
		return new ResponseEntity<>(user, HttpStatus.OK);
	}
	
	@GetMapping(path = "/image/{username}/{fileName}", produces = MediaType.IMAGE_JPEG_VALUE)
	public byte[] getProfileImage(@PathVariable("username") String username, @PathVariable("fileName") String fileName) throws IOException {
		return Files.readAllBytes(Paths.get(FileConstant.USER_FOLDER + username + FileConstant.FORWARD_SLASH + fileName));
	}
	
	@GetMapping(path = "/image/profile/{username}", produces = MediaType.IMAGE_JPEG_VALUE)
	public byte[] getTempProfileImage(@PathVariable("username") String username) throws IOException {
		URL url = new URL(FileConstant.TEMP_PROFILE_IMAGE_BASE_URL + username);
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		try(InputStream inputStream = url.openStream()){
			int bytesRead;
			byte[] chunk = new byte[1024];
			while( (bytesRead = inputStream.read(chunk)) > 0){
				byteArrayOutputStream.write(chunk, 0, bytesRead);
			}
		}
		return byteArrayOutputStream.toByteArray();
	}
	
	private ResponseEntity<HttpResponse> response(HttpStatus httpStatus, String message) {
		HttpResponse body = new HttpResponse(httpStatus.value(), httpStatus, httpStatus.getReasonPhrase().toUpperCase(), message);
		return new ResponseEntity<>(body,httpStatus);
	}


	private HttpHeaders getJwtHeader(UserPrincipal userPrincipal) {
		HttpHeaders headers = new HttpHeaders();
		headers.add(SecurityConstant.JWT__TOKEN_HEADER, jWTTokenProvider.generateJwtToken(userPrincipal));
		return headers;
	}

	private void authenticate(String username, String password) {

		authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, password));
	}
}
