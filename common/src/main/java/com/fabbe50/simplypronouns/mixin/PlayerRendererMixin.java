package com.fabbe50.simplypronouns.mixin;

import com.fabbe50.simplypronouns.SimplyPronouns;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerRenderer.class)
public class PlayerRendererMixin extends LivingEntityRenderer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    public PlayerRendererMixin(EntityRendererProvider.Context context, PlayerModel<AbstractClientPlayer> entityModel, float f) {
        super(context, entityModel, f);
    }

    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/LivingEntityRenderer;renderNameTag(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/network/chat/Component;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"), method = "renderNameTag(Lnet/minecraft/client/player/AbstractClientPlayer;Lnet/minecraft/network/chat/Component;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", cancellable = true)
    private void injectRenderNameTag(AbstractClientPlayer abstractClientPlayer, Component component, PoseStack poseStack, MultiBufferSource multiBufferSource, int i, CallbackInfo ci) {
        double d = this.entityRenderDispatcher.distanceToSqr(abstractClientPlayer);
        if (d < 100.0) {
            String pronoun = SimplyPronouns.getCachedShortFormPronoun(abstractClientPlayer.getStringUUID(), true);
            if (!pronoun.isEmpty()) {
                super.renderNameTag(abstractClientPlayer, Component.literal(pronoun).withStyle(ChatFormatting.GRAY), poseStack, multiBufferSource, i);
                poseStack.translate(0, 9.0f * 1.15f * 0.025f, 0);
            }
        }
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(AbstractClientPlayer entity) {
        return entity.getSkinTextureLocation();
    }
}
