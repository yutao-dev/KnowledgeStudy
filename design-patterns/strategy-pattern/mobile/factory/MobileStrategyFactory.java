package com.exercise.redisdemo01.mobile.factory;

import com.exercise.redisdemo01.mobile.model.strategy.MobileBrandStrategy;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 手机品牌策略工厂类
 * <p>
 * 该工厂类根据传入的手机品牌名称，返回对应的手机品牌策略实现类。
 * 支持的品牌包括：小米、华为、苹果。
 * </p>
 *
 * @author 王玉涛
 * @version 1.0
 * @since 2025/8/12
 */
@Component
public class MobileStrategyFactory {

    /**
     * 存储手机品牌与对应策略实现类映射关系的静态Map
     * key: 手机品牌名称
     * value: 对应的手机品牌策略实现类
     */
    private static final Map<String, MobileBrandStrategy> MOBILE_BRAND_STRATEGY_MAP;

    // 静态初始化块，初始化策略映射Map
    static {
        MOBILE_BRAND_STRATEGY_MAP = new HashMap<>();
    }

    /**
     * 构造函数，通过Spring注入的所有手机品牌策略实现类列表来初始化策略映射Map
     *
     * @param mobileBrandStrategies Spring容器中所有实现了MobileBrandStrategy接口的Bean列表
     */
    public MobileStrategyFactory(List<MobileBrandStrategy> mobileBrandStrategies) {
        // 遍历所有手机品牌策略实现类
        for (MobileBrandStrategy mobileBrandStrategy : mobileBrandStrategies) {
            // 获取策略实现类对应的手机品牌
            String brand = mobileBrandStrategy.getBrand();
            // 将手机品牌与对应的策略实现类存入映射Map中
            MOBILE_BRAND_STRATEGY_MAP.put(brand, mobileBrandStrategy);
        }
    }

    /**
     * 根据手机品牌名称获取对应的策略实现类
     * <p>
     * 通过比较传入的品牌名称与已注册的品牌名称，返回相应的策略对象。
     * 如果没有匹配的品牌，则返回 null。
     * </p>
     *
     * @param mobileBrand 手机品牌名称
     * @return 对应的手机品牌策略实现类，如果未找到则返回 null
     */
    public MobileBrandStrategy getMobileStrategy(String mobileBrand) {
        // 从映射Map中获取对应品牌的策略实现类
        return MOBILE_BRAND_STRATEGY_MAP.get(mobileBrand);
    }
}
