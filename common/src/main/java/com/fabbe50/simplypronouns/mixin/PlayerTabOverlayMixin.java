package com.fabbe50.simplypronouns.mixin;

import com.fabbe50.simplypronouns.SimplyPronouns;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.scores.PlayerTeam;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerTabOverlay.class)
public abstract class PlayerTabOverlayMixin {
    @Shadow protected abstract Component decorateName(PlayerInfo arg, MutableComponent arg2);

    @Inject(at = @At("HEAD"), method = "getNameForDisplay", cancellable = true)
    public void injectGetNameForDisplay(PlayerInfo playerInfo, CallbackInfoReturnable<Component> cir) {
        Component nameComponent = playerInfo.getTabListDisplayName() != null ? this.decorateName(playerInfo, playerInfo.getTabListDisplayName().copy()) : this.decorateName(playerInfo, PlayerTeam.formatNameForTeam(playerInfo.getTeam(), Component.literal(playerInfo.getProfile().getName())));
        String pronoun = SimplyPronouns.getCachedShortFormPronoun(playerInfo.getProfile().getId().toString(), true);
        cir.setReturnValue(nameComponent.copy().append(Component.literal(" " + pronoun).withStyle(ChatFormatting.GRAY)));
        cir.cancel();
    }
}
