package com.github.nalamodikk.common.register;

import com.github.nalamodikk.common.API.IComponentBehavior;
import com.github.nalamodikk.common.API.machine.behavior.ManaProducerBehavior;

import java.util.HashMap;
import java.util.Map;

/**
 * 所有行為的註冊表，用來把字串 ID 對應到 Java 類別實體
 */
public class ComponentBehaviorRegistry {

    // 儲存所有行為的對應表（行為 ID → Java 實體）
    private static final Map<String, IComponentBehavior> BEHAVIORS = new HashMap<>();

    /**
     * 註冊行為的方法
     * @param id      行為的字串名稱（如 "mana_producer"）
     * @param behavior 對應的 Java 實體
     */
    public static void register(String id, IComponentBehavior behavior) {
        BEHAVIORS.put(id, behavior);
    }

    /**
     * 根據行為 ID 取得對應的 Java 實體
     * @param id 行為 ID
     * @return 對應的行為實體，如果找不到會丟例外
     */
    public static IComponentBehavior get(String id) {
        IComponentBehavior behavior = BEHAVIORS.get(id);
        if (behavior == null) {
            throw new IllegalArgumentException("找不到行為: " + id);
        }
        return behavior;
    }

    /**
     * 模組初始化時，註冊所有支援的行為
     */
    public static void registerAll() {
        register("mana_producer", new ManaProducerBehavior());
        // register("speed_boost", new SpeedBoostBehavior()); // 你可以加更多
    }
}
