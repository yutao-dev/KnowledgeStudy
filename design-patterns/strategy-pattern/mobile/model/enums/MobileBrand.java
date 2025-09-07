package com.exercise.redisdemo01.mobile.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 手机品牌枚举类，用于定义系统支持的手机品牌及其描述信息
 *
 * @author 王玉涛
 * @version 1.0
 * @since 2025/8/12
 */
@Getter
@AllArgsConstructor
public enum MobileBrand {
    /**
     * 小米品牌
     */
    XIAOMI("小米", "小米手机"),
    
    /**
     * 华为品牌
     */
    HUAWEI("华为", "华为手机"),
    
    /**
     * 苹果品牌
     */
    APPLE("苹果", "苹果手机");
    
    /**
     * 品牌名称
     */
    private final String brand;
    
    /**
     * 品牌描述信息
     */
    private final String description;
}
