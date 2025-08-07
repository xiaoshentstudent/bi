package com.yupi.yubibackend.model.dto.chart;

import com.yupi.yubibackend.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@EqualsAndHashCode(callSuper = true)
@Data
public class ChartQueryRequest extends PageRequest implements Serializable {

    private Long id;

    /**
     * 图表名称
     */
    private String name;

    /**
     * 分析目标
     */
    private String goal;

    private int current;

    /**
     * 图表类型
     */
    private String chartType;

    /**
     * 用户 id
     */
    private Long userId;

    private static final long serialVersionUID = 1L;
}