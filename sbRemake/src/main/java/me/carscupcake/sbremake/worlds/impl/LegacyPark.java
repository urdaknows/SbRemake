package me.carscupcake.sbremake.worlds.impl;

import me.carscupcake.sbremake.blocks.Log;
import me.carscupcake.sbremake.util.Pair;
import me.carscupcake.sbremake.util.TaskScheduler;
import me.carscupcake.sbremake.worlds.SkyblockWorld;
import me.carscupcake.sbremake.worlds.region.Region;
import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.coordinate.Pos;

import java.util.HashMap;

public class LegacyPark extends SkyblockWorld.WorldProvider {
    public final HashMap<BlockVec, Log.LogInfo> brokenLogs = new HashMap<>();
    private final TaskScheduler foragingReset = new TaskScheduler() {
        @Override
        public void run() {
            brokenLogs.forEach((block, log) -> log.regen(container, block));
            brokenLogs.clear();
        }
    };

    @Override
    public SkyblockWorld type() {
        return SkyblockWorld.LegacyPark;
    }

    @Override
    public Pos spawn() {
        return new Pos(-276.5, 82.5, -13.5, 90, 0);
    }

    @Override
    public Region[] regions() {
        return new Region[0];
    }

    @Override
    protected void register() {
        foragingReset.repeatTask(20 * 30);
    }

    @Override
    protected void unregister() {
        foragingReset.cancel();
    }

    @Override
    public boolean useCustomMining() {
        return false;
    }
    @Override
    public Pair<Pos, Pos> getChunksToLoad() {
        return new  Pair<>(new Pos(0, 0, 0), new Pos(0, 0, 0));
    }
}
