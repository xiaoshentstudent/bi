package com.yupi.yubibackend;

import com.yupi.yubibackend.model.entity.User;
import com.yupi.yubibackend.service.UserService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.DigestUtils;

@SpringBootTest
class YubiBackendApplicationTests {


	@Resource
	private UserService userService;
	@Test
	void contextLoads() {
	}

	@Test
	public void main() {
			// Define a password
			String password = "123456";
			// Define a salt
			String SALT = "xiaoshen";
		// Use the md5DigestAsHex method to encrypt the password and salt
		String string = DigestUtils.md5DigestAsHex((password + SALT).getBytes());
		// Print the encrypted string
		System.out.println(string);
	}

	@Test
	public void test(){

		User byId = userService.getById(1L);
		System.out.println(byId);
	}


}
