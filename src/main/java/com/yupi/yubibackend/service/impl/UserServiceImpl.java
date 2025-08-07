package com.yupi.yubibackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.xiaoymin.knife4j.core.util.CollectionUtils;
import com.yupi.yubibackend.constant.CommonConstant;
import com.yupi.yubibackend.exception.BusinessException;
import com.yupi.yubibackend.exception.ErrorCode;
import com.yupi.yubibackend.model.dto.user.UserLoginRequest;
import com.yupi.yubibackend.model.dto.user.UserQueryRequest;
import com.yupi.yubibackend.model.dto.user.UserRegisterRequest;
import com.yupi.yubibackend.model.enums.UserRoleEnum;
import com.yupi.yubibackend.model.vo.LoginUserVO;
import com.yupi.yubibackend.model.vo.UserVO;
import com.yupi.yubibackend.service.UserService;
import com.yupi.yubibackend.mapper.UserMapper;
import com.yupi.yubibackend.model.entity.User;
import com.yupi.yubibackend.utils.SqlUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;


import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static com.yupi.yubibackend.constant.UserConstant.USER_LOGIN_STATE;


/**
* @author 小申同学
* @description 针对表【user(用户)】的数据库操作Service实现
* @createDate 2025-08-07 16:01:30
*/
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

	@Override
	public long userRegister(UserRegisterRequest userRegisterRequest) {
		if (userRegisterRequest == null) {
			throw new BusinessException(ErrorCode.PARAMS_ERROR);
		}
		String userAccount = userRegisterRequest.getUserAccount();
		String userPassword = userRegisterRequest.getUserPassword();
		String checkPassword = userRegisterRequest.getCheckPassword();
		if (userAccount.length() < 4) {
			throw new BusinessException(ErrorCode.PARAMS_ERROR,"用户名称过短");
		}
		if (userPassword.length() < 8|| checkPassword.length() < 8) {
			throw new BusinessException(ErrorCode.PARAMS_ERROR,"用户密码过短");
		}
		if (!userPassword.equals(checkPassword)) {
			throw new BusinessException(ErrorCode.PARAMS_ERROR,"两次密码不一致");
		}
		// 加锁 防止同时注册账号出现重复现象
		synchronized (userAccount.intern()) {
			// 账号不能重复
			QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
			userQueryWrapper.eq("userAccount",userAccount);
			Long count = this.baseMapper.selectCount(userQueryWrapper);
			if (count > 0) {
				throw new BusinessException(ErrorCode.PARAMS_ERROR,"用户账号重复");
			}
			// 密码加密
			String encryptPassword = getEncryptPassword(userPassword);
			// 构造创建用户
			User user = new User();
			user.setUserAccount(userAccount);
			user.setUserPassword(encryptPassword);
			user.setUserRole(UserRoleEnum.USER.getValue());
			user.setCreateTime(new Date());
			boolean save = this.save(user);
			if (!save) {
				throw new BusinessException(ErrorCode.PARAMS_ERROR,"用户注册失败");
			}
			return user.getId();
		}
	}

	@Override
	public LoginUserVO userLogin(UserLoginRequest userLoginRequest,HttpServletRequest request) {
		String userAccount = userLoginRequest.getUserAccount();
		String userPassword = userLoginRequest.getUserPassword();
		// 1. 校验
		if (StringUtils.isAnyBlank(userAccount, userPassword)) {
			throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
		}
		if (userAccount.length() < 4) {
			throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号错误");
		}
		if (userPassword.length() < 8) {
			throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码错误");
		}
		// 判断账号密码是否正确
		String encryptPassword = getEncryptPassword(userPassword);
		QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
		userQueryWrapper.eq("userAccount",userAccount);
		userQueryWrapper.eq("userPassword",encryptPassword);
		User user = this.baseMapper.selectOne(userQueryWrapper);
		// 判断账号是否存在
		if (user == null) {
			throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在");
		}
		// 记录登录态
		request.getSession().setAttribute(USER_LOGIN_STATE,user);
		// 返回脱敏后的用户信息
		return null;
	}

	@Override
	public User getLoginUser(HttpServletRequest request) {
		// 先判断是否已登录
		Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
		User currentUser = (User) userObj;
		if (currentUser == null || currentUser.getId() == null) {
			throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
		}
		// 从数据库查询（追求性能的话可以注释，直接走缓存）
		long userId = currentUser.getId();
		currentUser = this.getById(userId);
		if (currentUser == null) {
			throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
		}
		return currentUser;
	}

	@Override
	public boolean isAdmin(HttpServletRequest request) {
		// 仅管理员可查询
		Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
		User user = (User) userObj;
		return isAdmin(user);
	}

	@Override
	public boolean isAdmin(User user) {
		return user != null && UserRoleEnum.ADMIN.getValue().equals(user.getUserRole());
	}

	@Override
	public boolean userLogout(HttpServletRequest request) {
		if (request.getSession().getAttribute(USER_LOGIN_STATE) == null){
			throw new BusinessException(ErrorCode.PARAMS_ERROR);
		}
		request.getSession().removeAttribute(USER_LOGIN_STATE);
		return true;
	}

	@Override
	public LoginUserVO getUserLonginVO(HttpServletRequest request) {
		if (request == null){
			throw new BusinessException(ErrorCode.PARAMS_ERROR);
		}
		User user = (User) request.getSession().getAttribute(USER_LOGIN_STATE);
		if (user == null){
			throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
		}
		return getLoginUserVO(user);
	}

	@Override
	public UserVO getUserVO(User user) {
		if (user == null){
			throw new BusinessException(ErrorCode.PARAMS_ERROR);
		}
		UserVO userVO = new UserVO();
		BeanUtils.copyProperties(user,userVO);
		return userVO;
	}

	@Override
	public LoginUserVO getLoginUserVO(User user) {
		if (user == null){
			throw new BusinessException(ErrorCode.PARAMS_ERROR);
		}
		LoginUserVO loginUserVO = new LoginUserVO();
		BeanUtils.copyProperties(user,loginUserVO);
		return loginUserVO;
	}

	@Override
	public String getEncryptPassword(String password) {
		if (password == null) {
			return null;
		}
		String SALT = "xiaoshen";
		return DigestUtils.md5DigestAsHex((password + SALT).getBytes());
	}


	@Override
	public List<UserVO> getUserVO(List<User> userList) {
		if (CollectionUtils.isEmpty(userList)) {
			return new ArrayList<>();
		}
		return userList.stream().map(this::getUserVO).collect(Collectors.toList());
	}

	@Override
	public QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest) {
		if (userQueryRequest == null) {
			throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
		}
		Long id = userQueryRequest.getId();
		String unionId = userQueryRequest.getUnionId();
		String mpOpenId = userQueryRequest.getMpOpenId();
		String userName = userQueryRequest.getUserName();
		String userProfile = userQueryRequest.getUserProfile();
		String userRole = userQueryRequest.getUserRole();
		String sortField = userQueryRequest.getSortField();
		String sortOrder = userQueryRequest.getSortOrder();
		QueryWrapper<User> queryWrapper = new QueryWrapper<>();
		queryWrapper.eq(id != null, "id", id);
		queryWrapper.eq(StringUtils.isNotBlank(unionId), "unionId", unionId);
		queryWrapper.eq(StringUtils.isNotBlank(mpOpenId), "mpOpenId", mpOpenId);
		queryWrapper.eq(StringUtils.isNotBlank(userRole), "userRole", userRole);
		queryWrapper.like(StringUtils.isNotBlank(userProfile), "userProfile", userProfile);
		queryWrapper.like(StringUtils.isNotBlank(userName), "userName", userName);
		queryWrapper.orderBy(SqlUtils.validSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
				sortField);
		return queryWrapper;
	}
}




