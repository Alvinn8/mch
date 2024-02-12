package ca.bkaw.mch.viewer.fabric.mixin;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.nio.file.Path;

@Mixin(LevelStorageSource.LevelStorageAccess.class)
public class LevelStorageAccessMixin {
    @Inject(method = "getDimensionPath", at = @At("RETURN"))
    public void injected(ResourceKey<Level> resourceKey, CallbackInfoReturnable<Path> cir) {
        Path path = cir.getReturnValue();
        System.out.println("getDimensionPath() = " + path);
        Thread.dumpStack();
    }
}
