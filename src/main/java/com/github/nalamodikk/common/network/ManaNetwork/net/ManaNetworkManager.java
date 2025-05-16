package com.github.nalamodikk.common.network.ManaNetwork.net;

import com.github.nalamodikk.common.MagicalIndustryMod;
import com.github.nalamodikk.common.block.entity.Conduit.ManaConduitBlockEntity;
import net.minecraft.world.level.Level;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ManaNetworkManager {
    private static final Map<Level, ManaNetworkManager> INSTANCES = new HashMap<>();
    private final Level level;
    private final Set<ManaConduitBlockEntity> conduits = new HashSet<>();
    private final Queue<ManaConduitBlockEntity> updateQueue = new LinkedList<>();
    private static final ExecutorService THREAD_POOL = Executors.newFixedThreadPool(2); // ✅ 非同步計算魔力分配

    private ManaNetworkManager(Level level) {
        this.level = level;
    }

    public static ManaNetworkManager getInstance(Level level) {
        return INSTANCES.computeIfAbsent(level, ManaNetworkManager::new);
    }

    public void addConduit(ManaConduitBlockEntity conduit) {
        conduits.add(conduit);
        MagicalIndustryMod.LOGGER.debug("[ManaNetwork] 新增導管: {}，總數: {}", conduit.getBlockPos(), conduits.size());
    }

    public void removeConduit(ManaConduitBlockEntity conduit) {
        conduits.remove(conduit);
        MagicalIndustryMod.LOGGER.debug("[ManaNetwork] 移除導管: {}，總數: {}", conduit.getBlockPos(), conduits.size());
    }

    public void queueManaUpdate(ManaConduitBlockEntity conduit) {
        synchronized (updateQueue) {
            if (!updateQueue.contains(conduit)) {
                updateQueue.add(conduit);
            }
        }
    }


    public void processManaUpdate(ManaConduitBlockEntity conduit) {
        THREAD_POOL.submit(() -> {
            synchronized (conduits) { // ✅ 確保不會跟 `tick()` 同時操作
                distributeMana(conduit);
            }
        });
    }



    private void distributeMana(ManaConduitBlockEntity sourceConduit) {
        int totalMana = conduits.stream().mapToInt(ManaConduitBlockEntity::getMana).sum();
        List<ManaConduitBlockEntity> receivers = new ArrayList<>();

        for (ManaConduitBlockEntity conduit : conduits) {
            if (conduit.getManaStorage().getNeeded() > 0) {
                receivers.add(conduit);
            }
        }

        if (receivers.isEmpty()) return;

        int perReceiver = Math.max(1, totalMana / receivers.size());
        MagicalIndustryMod.LOGGER.debug("[ManaNetwork] ⚡ 重新分配 Mana: 總量={}，每個接收={}，來源={}",
                totalMana, perReceiver, sourceConduit.getBlockPos());

        for (ManaConduitBlockEntity conduit : receivers) {
            int currentMana = conduit.getMana();
            int targetMana = Math.min(conduit.getManaStorage().getMaxMana(), currentMana + perReceiver);
            conduit.setMana(targetMana);
        }

        // 🔥 讓所有導管開始傳輸 Mana
        for (ManaConduitBlockEntity conduit : conduits) {
            conduit.transferManaToNeighbors();
        }
    }



}
