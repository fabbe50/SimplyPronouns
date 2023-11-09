package com.fabbe50.simplypronouns.forge;

import dev.architectury.platform.forge.EventBuses;
import com.fabbe50.simplypronouns.SimplyPronouns;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.NetworkConstants;

@Mod(SimplyPronouns.MOD_ID)
public class SimplyPronounsForge {
    public SimplyPronounsForge() {
        ModLoadingContext.get().registerExtensionPoint(IExtensionPoint.DisplayTest.class, () -> new IExtensionPoint.DisplayTest(() -> NetworkConstants.IGNORESERVERONLY, (a, b) -> true));
		// Submit our event bus to let architectury register our content on the right time
        EventBuses.registerModEventBus(SimplyPronouns.MOD_ID, FMLJavaModLoadingContext.get().getModEventBus());
        DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> SimplyPronounsForgeImpl::init);
    }
}