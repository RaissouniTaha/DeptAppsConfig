package com.everis.deptappsconfig.security.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.everis.deptappsconfig.security.domain.User;

public interface UserRepository extends JpaRepository<User, Long>{

	public User findByUsername(String username);
	public User findByEmail(String email);
}
