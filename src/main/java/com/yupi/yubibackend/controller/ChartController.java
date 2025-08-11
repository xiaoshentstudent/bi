package com.yupi.yubibackend.controller;


import cn.hutool.core.io.FileUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.gson.Gson;
import com.yupi.yubibackend.ai.AiCodeGeneratorService;
import com.yupi.yubibackend.ai.AiCodeGeneratorServiceFactory;
import com.yupi.yubibackend.annotation.AuthCheck;
import com.yupi.yubibackend.common.BaseResponse;
import com.yupi.yubibackend.common.DeleteRequest;
import com.yupi.yubibackend.common.ResultUtils;
import com.yupi.yubibackend.constant.CommonConstant;
import com.yupi.yubibackend.constant.UserConstant;
import com.yupi.yubibackend.exception.BusinessException;
import com.yupi.yubibackend.exception.ErrorCode;
import com.yupi.yubibackend.exception.ThrowUtils;
import com.yupi.yubibackend.manager.RedisLimiterManager;
import com.yupi.yubibackend.model.dto.chart.*;
import com.yupi.yubibackend.model.entity.Chart;
import com.yupi.yubibackend.model.entity.User;
import com.yupi.yubibackend.model.vo.AiResponse;
import com.yupi.yubibackend.model.vo.LoginUserVO;
import com.yupi.yubibackend.service.ChartService;
import com.yupi.yubibackend.service.UserService;
import com.yupi.yubibackend.utils.ExcelUtils;
import com.yupi.yubibackend.utils.SqlUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.formula.functions.T;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/chart")
@Slf4j
public class ChartController {

	@Resource
	private ChartService chartService;

	@Resource
	private UserService userService;

	@Resource
	private AiCodeGeneratorService aiCodeGeneratorService;

	@Resource
	private RedisLimiterManager redisLimiterManager;

	private final static Gson GSON = new Gson();

	// region 增删改查

	/**
	 * 创建
	 *
	 * @param chartAddRequest
	 * @param request
	 * @return
	 */
	@PostMapping("/add")
	public BaseResponse<Long> addChart(@RequestBody ChartAddRequest chartAddRequest, HttpServletRequest request) {
		if (chartAddRequest == null) {
			throw new BusinessException(ErrorCode.PARAMS_ERROR);
		}
		Chart chart = new Chart();
		BeanUtils.copyProperties(chartAddRequest, chart);
		User loginUser = userService.getLoginUser(request);
		chart.setUserId(loginUser.getId());
		boolean result = chartService.save(chart);
		ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
		long newChartId = chart.getId();
		return ResultUtils.success(newChartId);
	}

	/**
	 * 删除
	 *
	 * @param deleteRequest
	 * @param request
	 * @return
	 */
	@PostMapping("/delete")
	public BaseResponse<Boolean> deleteChart(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
		if (deleteRequest == null || deleteRequest.getId() <= 0) {
			throw new BusinessException(ErrorCode.PARAMS_ERROR);
		}
		User user = userService.getLoginUser(request);
		long id = deleteRequest.getId();
		// 判断是否存在
		Chart oldChart = chartService.getById(id);
		ThrowUtils.throwIf(oldChart == null, ErrorCode.NOT_FOUND_ERROR);
		// 仅本人或管理员可删除
		if (!oldChart.getUserId().equals(user.getId()) && !userService.isAdmin(request)) {
			throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
		}
		boolean b = chartService.removeById(id);
		return ResultUtils.success(b);
	}

	/**
	 * 更新（仅管理员）
	 *
	 * @param chartUpdateRequest
	 * @return
	 */
	@PostMapping("/update")
	@AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
	public BaseResponse<Boolean> updateChart(@RequestBody ChartUpdateRequest chartUpdateRequest) {
		if (chartUpdateRequest == null || chartUpdateRequest.getId() <= 0) {
			throw new BusinessException(ErrorCode.PARAMS_ERROR);
		}
		Chart chart = new Chart();
		BeanUtils.copyProperties(chartUpdateRequest, chart);
		long id = chartUpdateRequest.getId();
		// 判断是否存在
		Chart oldChart = chartService.getById(id);
		ThrowUtils.throwIf(oldChart == null, ErrorCode.NOT_FOUND_ERROR);
		boolean result = chartService.updateById(chart);
		return ResultUtils.success(result);
	}

	/**
	 * 智能分析
	 *
	 * @param multipartFile
	 * @param genChartByAiRequest
	 * @param request
	 * @return
	 */
	@PostMapping("/gen")
	public BaseResponse<String> genChartByAi(@RequestPart("file") MultipartFile multipartFile,
											 GenChartByAiRequest genChartByAiRequest, HttpServletRequest request) {
		String name = genChartByAiRequest.getName();
		String goal = genChartByAiRequest.getGoal();
		String chartType = genChartByAiRequest.getChartType();
		// 校验
		// todo  如果分析目标为空，就抛出请求参数错误异常，并给出提示
		ThrowUtils.throwIf(StringUtils.isBlank(goal), ErrorCode.PARAMS_ERROR, "目标为空");
		// 如果名称不为空，并且名称长度大于100，就抛出异常，并给出提示
		ThrowUtils.throwIf(StringUtils.isNotBlank(name) && name.length() > 100, ErrorCode.PARAMS_ERROR, "名称过长");


		/**
		 * 校验文件
		 * 校验文件大小和文件后缀名称
		 */
		long fileSize = multipartFile.getSize();
		String originalFilename = multipartFile.getOriginalFilename();

		final long ONE_MB = 1024 * 1024;
		// 如果文件大于 5 MB ，就抛出异常，并给出提示
		ThrowUtils.throwIf(fileSize > 5 * ONE_MB, ErrorCode.PARAMS_ERROR, "文件过大");
		// 如果文件后缀名称不为空，并且文件后缀名称不是 .xlsx 或 .xls ，就抛出异常，并给出提示
		String suffix = FileUtil.getSuffix(originalFilename);
		List<String> validFileSuffixList = Arrays.asList("png", "jpg", "svg", "webp", "jpeg");
		ThrowUtils.throwIf(!validFileSuffixList.contains(suffix), ErrorCode.PARAMS_ERROR, "文件格式错误");

		User loginUser = userService.getLoginUser(request);

		redisLimiterManager.doRateLimit("genChartByAi" +  loginUser.getId());


		String csvData = ExcelUtils.excelToCsv(multipartFile);
		Long userId = userService.getLoginUser(request).getId();
		// 构造用户信息
		StringBuilder userInput = new StringBuilder();
		userInput.append("分析需求:").append("\n");
		// 拼接分析目标
		String userGoal = goal;
		// 如果图表类型为空
		if (StringUtils.isNotBlank(chartType)) {
			userGoal += "请使用" + chartType;
		}
		userInput.append(userGoal).append("\n");
		userInput.append("原始数据:").append("\n");
		userInput.append(csvData).append("\n");


		String result = aiCodeGeneratorService.generateHtmlCode(userInput);

		// 对返回结果进行拆分
		String[] split = result.split("【【【【【");
		// 拆分后进行校验
		if (split.length < 3) {
			throw new BusinessException(ErrorCode.PARAMS_ERROR, "生成失败");
		}
		String genChart = split[1].intern();
		String genResult = split[2].intern();
		// 保存到数据库
		Chart chart = new Chart();
		chart.setName(name);
		chart.setGoal(goal);
		chart.setChartData(csvData);
		chart.setChartType(chartType);
		chart.setGenChart(genChart);
		chart.setGenResult(genResult);
		chart.setUserId(userId);
		boolean save = chartService.save(chart);
		if (!save) {
			throw new BusinessException(ErrorCode.OPERATION_ERROR, "保存失败");
		}
		// 构造返回结果，供前端调用
		AiResponse aiResponse = new AiResponse();
		aiResponse.setGenChart(genChart);
		aiResponse.setGenResult(genResult);
		aiResponse.setChartId(chart.getId());
		return ResultUtils.success(aiResponse.toString());

	}


	/**
	 * 分页获取列表（封装类）
	 *
	 * @param chartQueryRequest
	 * @param request
	 * @return
	 */
	@PostMapping("/list/page")
	public BaseResponse<Page<Chart>> listChartByPage(@RequestBody ChartQueryRequest chartQueryRequest,
													 HttpServletRequest request) {
		long current = chartQueryRequest.getCurrent();
		long size = chartQueryRequest.getPageSize();
		// 限制爬虫
		ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
		Page<Chart> chartPage = chartService.page(new Page<>(current, size),
				getQueryWrapper(chartQueryRequest));
		return ResultUtils.success(chartPage);
	}

	/**
	 * 分页获取当前用户创建的资源列表
	 *
	 * @param chartQueryRequest
	 * @param request
	 * @return
	 */
	@PostMapping("/my/list/page")
	public BaseResponse<Page<Chart>> listMyChartByPage(@RequestBody ChartQueryRequest chartQueryRequest,
													   HttpServletRequest request) {
		if (chartQueryRequest == null) {
			throw new BusinessException(ErrorCode.PARAMS_ERROR);
		}
		User loginUser = userService.getLoginUser(request);
		chartQueryRequest.setUserId(loginUser.getId());
		long current = chartQueryRequest.getCurrent();
		long size = chartQueryRequest.getPageSize();
		// 限制爬虫
		ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
		Page<Chart> chartPage = chartService.page(new Page<>(current, size),
				getQueryWrapper(chartQueryRequest));
		return ResultUtils.success(chartPage);
	}


	// endregion

	/**
	 * 编辑（用户）
	 *
	 * @param chartEditRequest
	 * @param request
	 * @return
	 */
	@PostMapping("/edit")
	public BaseResponse<Boolean> editChart(@RequestBody ChartEditRequest chartEditRequest, HttpServletRequest request) {
		if (chartEditRequest == null || chartEditRequest.getId() <= 0) {
			throw new BusinessException(ErrorCode.PARAMS_ERROR);
		}
		Chart chart = new Chart();
		BeanUtils.copyProperties(chartEditRequest, chart);
		User loginUser = userService.getLoginUser(request);
		long id = chartEditRequest.getId();
		// 判断是否存在
		Chart oldChart = chartService.getById(id);
		ThrowUtils.throwIf(oldChart == null, ErrorCode.NOT_FOUND_ERROR);
		// 仅本人或管理员可编辑
		if (!oldChart.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
			throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
		}
		boolean result = chartService.updateById(chart);
		return ResultUtils.success(result);
	}

	/**
	 * 获取查询包装类
	 *
	 * @param chartQueryRequest
	 * @return
	 */
	private QueryWrapper<Chart> getQueryWrapper(ChartQueryRequest chartQueryRequest) {
		QueryWrapper<Chart> queryWrapper = new QueryWrapper<>();
		if (chartQueryRequest == null) {
			return queryWrapper;
		}
		Long id = chartQueryRequest.getId();
		String name = chartQueryRequest.getName();
		String goal = chartQueryRequest.getGoal();
		String chartType = chartQueryRequest.getChartType();
		Long userId = chartQueryRequest.getUserId();
		String sortField = chartQueryRequest.getSortField();
		String sortOrder = chartQueryRequest.getSortOrder();


		queryWrapper.eq(id != null && id > 0, "id", id);
		queryWrapper.like(StringUtils.isNotBlank(name), "name", name);
		queryWrapper.eq(StringUtils.isNotBlank(goal), "goal", goal);
		queryWrapper.eq(StringUtils.isNotBlank(chartType), "chartType", chartType);
		queryWrapper.eq(ObjectUtils.isNotEmpty(userId), "userId", userId);
		queryWrapper.eq("isDelete", false);
		queryWrapper.orderBy(SqlUtils.validSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
				sortField);
		return queryWrapper;
	}
}