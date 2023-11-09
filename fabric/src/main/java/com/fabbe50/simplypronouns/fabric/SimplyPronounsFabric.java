package com.fabbe50.simplypronouns.fabric;

import com.fabbe50.simplypronouns.SimplyPronouns;
import net.fabricmc.api.ClientModInitializer;

public class SimplyPronounsFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        SimplyPronouns.init();
    }
}