package io.akarin.server.mixin.core;

import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import co.aikar.timings.Timing;
import co.aikar.timings.TimingsManager;
import io.akarin.api.Akari;
import io.akarin.server.core.AkarinGlobalConfig;

@Mixin(targets = "co.aikar.timings.TimingHandler", remap = false)
public class MixinTimingHandler {
    @Shadow @Final String name;
    @Shadow private boolean enabled;
    @Shadow private volatile long start;
    @Shadow private volatile int timingDepth;
    
    @Overwrite
    public void stopTimingIfSync() {
        if (Bukkit.isPrimaryThread()) {
            stopTiming(true); // Avoid twice thread check
        }
    }
    
    @Overwrite
    public void stopTiming() {
        // Akarin start - avoid twice thread check
        stopTiming(false);
    }
    
    @Shadow void addDiff(long diff) {}
    
    
    
    public Timing startTiming() {
        if (enabled && ++timingDepth == 1) {
            start = System.nanoTime();
            parent = TimingsManager.CURRENT;
            TimingsManager.CURRENT = this;
        }
        return this;
    }
    
    public void stopTiming(boolean sync) {
        if (enabled && --timingDepth == 0 && start != 0) {
            if (Akari.silentTiming) { // It must be off-main thread now
                start = 0;
                return;
            } else {
                if (!sync && !Bukkit.isPrimaryThread()) {
                    if (AkarinGlobalConfig.silentAsyncTimings) {
                        Bukkit.getLogger().log(Level.SEVERE, "stopTiming called async for " + name);
                        new Throwable().printStackTrace();
                    }
                    start = 0;
                    return;
                }
            }
            addDiff(System.nanoTime() - start);
            start = 0;
        }
    }
}