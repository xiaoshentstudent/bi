package com.yupi.yubibackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yupi.yubibackend.model.dto.user.UserLoginRequest;
import com.yupi.yubibackend.model.dto.user.UserQueryRequest;
import com.yupi.yubibackend.model.dto.user.UserRegisterRequest;
import com.yupi.yubibackend.model.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yupi.yubibackend.model.vo.LoginUserVO;
import com.yupi.yubibackend.model.vo.UserVO;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

/**
* @author 小申同学
* @description 针对表【user(用户)】的数据库操作Service
* @createDate 2025-08-07 16:01:30
*/
public interface UserService extends IService<User> {

	/**
	 * 用户注册 userRegister √
	 * 用户登录 userLogin  √
	 * 获取当前登录用户 getLoginUser √
	 * 判断是否为管理员 isAdmin √
	 * 用户注销 userLogout √
	 * 获取脱敏的已登录用户 getLoginUserVO  √
	 * 获取脱敏后的用户信息 getUserVO  √
	 * 获取查询条件 getQueryWrapper
	 * 密码加盐 getEncryptPassword v
	 *
	 */


	/**
	 * 注册用户
	 *
	 * @param userRegisterRequest 用户注册请求
	 * @return 用户id
	 */
	long userRegister(UserRegisterRequest userRegisterRequest);


	/**
	 * 用户登录
	 *
	 * @param userLoginRequest 用户登录请求
	 * @return 脱敏后登录用户信息
	 */
	LoginUserVO userLogin(UserLoginRequest userLoginRequest,HttpServletRequest request);

	/**
	 * 获取当前登录用户
	 *
	 * @param request 请求
	 * @return 登录用户信息
	 */
	User getLoginUser(HttpServletRequest request);

	/**
	 * 判断是否是管理员
	 *
	 * @param request 请求
	 * @return 是否是管理员
	 */
	boolean isAdmin(HttpServletRequest request);


	/**
	 * 判断是否是管理员
	 *
	 * @param user 用户
	 * @return 是否是管理员
	 */
	boolean isAdmin(User user);


	/**
	 * 用户注销
	 *
	 * @param request 请求
	 * @return 是否注销成功
	 */
	boolean userLogout(HttpServletRequest request);

	/**
	 *
	 * @param request 请求
	 * @return 登录用户信息
	 */
	LoginUserVO getUserLonginVO(HttpServletRequest request);

	/**
	 * 获取脱敏后的用户信息
	 *
	 * @param user 用户
	 * @return 脱敏后的用户信息
	 */
	UserVO getUserVO(User user);

	/**
	 * 获取脱敏的已登录用户
	 *
	 * @param user
	 * @return
	 */
	LoginUserVO getLoginUserVO(User user);

	/**
	 * 获取加密后的密码
	 *
	 * @param password 原始密码
	 * @return 加密后的密码
	 */
	String getEncryptPassword(String password);


	/**
	 * 获取脱敏的用户信息
	 *
	 * @param userList
	 * @return
	 */
	List<UserVO> getUserVO(List<User> userList);

	/**
	 * 获取查询条件
	 *
	 * @param userQueryRequest
	 * @return
	 */
	QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest);

}
