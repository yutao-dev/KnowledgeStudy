package com.exercise.redisdemo01.mobile.factory;

import com.exercise.redisdemo01.mobile.model.enums.MobileBrand;
import com.exercise.redisdemo01.mobile.model.rule.MobileBrandAccountRule;

import java.util.List;
import java.util.Map;

/**
 * 手机品牌折扣规则工厂类
 * 该工厂负责创建和管理不同品牌的折扣规则，通过策略模式实现规则的灵活扩展
 * 如果要添加新品牌实现，只需要在工厂中添加对应的实现方法
 *
 * @author 王玉涛
 * @version 1.0
 * @since 2025/8/12
 */
public class MobileBrandAccountRuleFactory {

    /**
     * 手机品牌折扣规则缓存
     */
    private MobileBrandAccountRuleFactory() {}

    /**
     * 存储各品牌折扣规则的映射表
     * key: 品牌名称
     * value: 对应的折扣规则对象
     */
    private static final Map<String, MobileBrandAccountRule> MOBILE_BRAND_ACCOUNT_RULE_MAP;

    static {
        // 初始化各品牌折扣规则映射表
        MOBILE_BRAND_ACCOUNT_RULE_MAP = Map.of(
                // 小米品牌折扣规则配置
                MobileBrand.XIAOMI.getBrand(),
                new MobileBrandAccountRule(
                        // 价格区间判断条件列表
                        List.of(
                                // 价格小于等于1000元
                                price -> price <= 1000,
                                // 价格小于等于2000元
                                price -> price <= 2000,
                                // 价格大于2000元
                                price -> price > 2000
                        ),
                        // 对应的折扣率列表
                        List.of(
                                // 98折
                                0.98D,
                                // 95折
                                0.95D,
                                // 92折
                                0.92D
                        ),
                        // 对应的优惠赠品列表
                        List.of(
                                // 无赠品
                                "",
                                // 赠送有线耳机
                                "赠有线耳机",
                                // 赠送小米手环
                                "赠小米手环"
                        ),
                        MobileBrand.XIAOMI.getBrand()
                ),
                // 华为品牌折扣规则配置
                MobileBrand.HUAWEI.getBrand(),
                new MobileBrandAccountRule(
                        // 价格区间判断条件列表
                        List.of(
                                // 价格小于等于1500元
                                price -> price <= 1500,
                                // 价格小于等于3000元
                                price -> price <= 3000,
                                // 价格大于3000元
                                price -> price > 3000
                        ),
                        // 对应的折扣率列表
                        List.of(
                                // 98折
                                0.98D,
                                // 95折
                                0.95D,
                                // 92折
                                0.92D
                        ),
                        // 对应的优惠赠品列表
                        List.of(
                                // 无赠品
                                "",
                                // 赠送FreeBuds SE耳机
                                "赠FreeBuds SE",
                                // 提供24期免息分期
                                "24期免息"
                        ),
                        MobileBrand.HUAWEI.getBrand()
                ),
                // 苹果品牌折扣规则配置
                MobileBrand.APPLE.getBrand(),
                new MobileBrandAccountRule(
                        // 价格区间判断条件列表
                        List.of(
                                // 价格小于等于5000元
                                price -> price <= 5000,
                                // 价格小于等于8000元
                                price -> price <= 8000,
                                // 价格大于8000元
                                price -> price > 8000
                        ),
                        // 对应的折扣率列表
                        List.of(
                                // 无折扣
                                1.00D,
                                // 98折
                                0.98D,
                                // 95折
                                0.95D
                        ),
                        // 对应的优惠赠品列表
                        List.of(
                                // 赠送AirTag
                                "赠AirTag",
                                // 提供12期免息分期
                                "12期免息",
                                // 提供24期免息分期并赠送MagSafe
                                "24期免息 + 赠MagSafe"
                        ),
                        MobileBrand.APPLE.getBrand()
                )
        );
    }
    
    
    /**
     * 根据品牌名称获取对应的折扣规则
     *
     * @param brand 品牌名称
     * @return 对应的折扣规则对象，如果找不到则返回null
     */
    public static MobileBrandAccountRule getMobileBrandAccountRule(String brand) {
        return MOBILE_BRAND_ACCOUNT_RULE_MAP.get(brand);
    }
}
