package com.yupi.yubibackend.ai;

import dev.langchain4j.service.SystemMessage;

public interface AiCodeGeneratorService {

    /**
     * 生成 HTML 代码
     *
     * @param userMessage 用户消息
     * @return 生成的代码结果
     */
    @SystemMessage(fromResource = "prompt/getVue-system-prompt.txt")
    String generateHtmlCode(StringBuilder userMessage);


}
