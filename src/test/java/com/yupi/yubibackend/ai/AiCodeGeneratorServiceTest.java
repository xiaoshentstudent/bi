package com.yupi.yubibackend.ai;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;


@SpringBootTest
class AiCodeGeneratorServiceTest {

	@Resource
	private AiCodeGeneratorService aiCodeGeneratorService;

	@Test
	void generateHtmlCode() {
		String userMessage = "分析需求:\n" +
				"分析网站的用户的增长情况\n" +
				"原始数据:\n" +
				"日期，用户数\n" +
				"2023-01-01, 100\n" +
				"2023-01-02, 110\n" +
				"2023-01-03, 120\n";
		StringBuilder sb = new StringBuilder();
		sb.append("分析需求:\n" + userMessage);
		String string = aiCodeGeneratorService.generateHtmlCode(sb);
		System.out.println(string);
	}
}