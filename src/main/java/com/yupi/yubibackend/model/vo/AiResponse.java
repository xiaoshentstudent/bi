package com.yupi.yubibackend.model.vo;

import lombok.Data;

/**
 * Bi 的返回结果
 */
@Data
public class AiResponse {

    private String genChart;

    private String genResult;

    // 新生成的图标id
    private Long chartId;
}
