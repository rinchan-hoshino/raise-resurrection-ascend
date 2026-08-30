package dev.rinchan.raiseresurrectionascend.mixin;

import com.mojang.logging.LogUtils;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderDispatcher.class)
public abstract class MissingEntityRendererGuardMixin {
    private static final Logger LOGGER = LogUtils.getLogger();

    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    private void raiseResurrectionAscend$identifyMissingRenderer(
            Entity entity,
            Frustum frustum,
            double cameraX,
            double cameraY,
            double cameraZ,
            CallbackInfoReturnable<Boolean> cir) {
        EntityRenderDispatcher dispatcher = (EntityRenderDispatcher) (Object) this;
        if (dispatcher.getRenderer(entity) == null) {
            LOGGER.error(
                    "Skipping entity with no registered client renderer: type={}, class={}, uuid={}",
                    BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()),
                    entity.getClass().getName(),
                    entity.getUUID());
            cir.setReturnValue(false);
        }
    }
}
