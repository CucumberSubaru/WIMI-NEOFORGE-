package com.backpackinv.mixin;

import com.backpackinv.client.BackpackInvConfig;
import com.backpackinv.client.InventoryAnimation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.client.resources.model.BakedModel;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {

    @Shadow @Final private Minecraft minecraft;

    @Shadow protected abstract float calculateMapTilt(float pitch);

    @Inject(method = "renderHandsWithItems", at = @At("HEAD"), cancellable = true)
    private void backpackinv$renderBackpack(float partialTicks, PoseStack poseStack, MultiBufferSource.BufferSource buffer,
                                            LocalPlayer player, int light, CallbackInfo ci) {
        if (!InventoryAnimation.isActive()) {
            return;
        }

        BackpackInvConfig cfg = InventoryAnimation.config();
        float bag = InventoryAnimation.bagProgress(partialTicks);
        float lid = InventoryAnimation.lidProgress(partialTicks);
        float pitch = Mth.lerp(partialTicks, player.xRotO, player.getXRot());
        float tilt = cfg.useVanillaTilt ? this.calculateMapTilt(pitch) : cfg.fixedTilt;

        poseStack.pushPose();

        poseStack.translate(0.0F, 0.04F + (1.0F - bag) * -1.2F + tilt * -0.5F, -0.72F);
        poseStack.mulPose(Axis.XP.rotationDegrees(tilt * -85.0F));

        // наклон при доставании
        float raiseT = InventoryAnimation.raiseTilt(partialTicks);
        float lowerT = InventoryAnimation.lowerFactor(partialTicks);

        // доставание и убирание: одинаковый набор движений (смещение, наклон, поворот)
        poseStack.translate(cfg.raiseShiftX * raiseT + cfg.lowerShiftX * lowerT, 0.0F, 0.0F);
        poseStack.mulPose(Axis.XP.rotationDegrees(cfg.raiseTiltAngle * raiseT + cfg.lowerTiltAngle * lowerT));
        poseStack.mulPose(Axis.ZP.rotationDegrees(cfg.raiseRollAngle * raiseT + cfg.lowerRollAngle * lowerT));
        
        if (cfg.showHands && !player.isInvisible()) {
            this.backpackinv$renderHand(poseStack, buffer, light, player, HumanoidArm.LEFT, cfg, 0.0F);
            this.backpackinv$renderHand(poseStack, buffer, light, player, HumanoidArm.RIGHT, cfg, lid);
        }

        // рюкзак между руками
        poseStack.pushPose();
        poseStack.translate(cfg.offsetX, cfg.offsetY, cfg.offsetZ);
        poseStack.mulPose(Axis.XP.rotationDegrees(cfg.rotX));
        poseStack.mulPose(Axis.YP.rotationDegrees(cfg.rotY));
        poseStack.mulPose(Axis.ZP.rotationDegrees(cfg.rotZ));
        poseStack.scale(cfg.scale, cfg.scale, cfg.scale);
        poseStack.translate(0.0F, 0.21875F, 0.0F); // центрируем модель по высоте

        // корпус
        this.backpackinv$renderPart(poseStack, buffer, light, InventoryAnimation.BODY_MODEL);

        // крышка поворачивается вокруг задней верхней кромки корпуса
        poseStack.pushPose();
        // точка шарнира задана в координатах модели (до сдвига центровки): y = (5.5 - 8) / 16, z = (6 - 8) / 16
        poseStack.translate(0.0F, -0.15625F, -0.125F);
        poseStack.mulPose(Axis.XP.rotationDegrees(-cfg.lidOpenAngle * lid));
        poseStack.translate(0.0F, 0.15625F, 0.125F);
        this.backpackinv$renderPart(poseStack, buffer, light, InventoryAnimation.LID_MODEL);
        poseStack.popPose();

        poseStack.popPose();
        poseStack.popPose();
        buffer.endBatch();
        ci.cancel();
    }

    @Unique
    private void backpackinv$renderPart(PoseStack poseStack, MultiBufferSource.BufferSource buffer, int light, ModelResourceLocation modelId) {
        BakedModel model = this.minecraft.getModelManager().getModel(modelId);
        this.minecraft.getItemRenderer().render(
                InventoryAnimation.propStack(),
                ItemDisplayContext.FIXED,
                false,
                poseStack,
                buffer,
                light,
                OverlayTexture.NO_OVERLAY,
                model
        );
    }

    @Unique
    private void backpackinv$renderHand(PoseStack poseStack, MultiBufferSource buffer, int light, LocalPlayer player,
                                        HumanoidArm side, BackpackInvConfig cfg, float lid) {
        float f = side == HumanoidArm.RIGHT ? 1.0F : -1.0F;

        poseStack.pushPose();

        // положение руки в плоскости рюкзака (x вправо, y вверх, z к камере)
        poseStack.translate(f * -cfg.handInward, cfg.handUp, cfg.handForward);
        poseStack.mulPose(Axis.XP.rotationDegrees(cfg.handExtraRotX));
        poseStack.mulPose(Axis.YP.rotationDegrees(f * cfg.handExtraRotY));
        poseStack.mulPose(Axis.ZP.rotationDegrees(f * cfg.handExtraRotZ));

        // правая рука тянется к крышке и откидывает её
        if (side == HumanoidArm.RIGHT && lid != 0.0F) {
            poseStack.translate(0.0F, cfg.openHandUp * lid, cfg.openHandBack * lid);
            poseStack.mulPose(Axis.XP.rotationDegrees(cfg.openHandRotX * lid));
        }

        // дальше та же поза, что у ванильной руки с картой
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(92.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(45.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(f * -41.0F));
        poseStack.translate(f * 0.3F, -1.1F, 0.45F);

        PlayerRenderer renderer = (PlayerRenderer) this.minecraft.getEntityRenderDispatcher().getRenderer(player);
        if (side == HumanoidArm.RIGHT) {
            renderer.renderRightHand(poseStack, buffer, light, player);
        } else {
            renderer.renderLeftHand(poseStack, buffer, light, player);
        }

        poseStack.popPose();
    }
}
