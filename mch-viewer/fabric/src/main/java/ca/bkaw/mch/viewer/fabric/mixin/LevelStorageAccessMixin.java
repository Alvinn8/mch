package ca.bkaw.mch.viewer.fabric.mixin;

import ca.bkaw.mch.viewer.fabric.DimensionView;
import ca.bkaw.mch.viewer.fabric.MchViewerFabric;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.nio.file.Path;

@Mixin(LevelStorageSource.LevelStorageAccess.class)
public class LevelStorageAccessMixin {
    @ModifyReturnValue(method = "getDimensionPath", at = @At("RETURN"))
    public Path injected(Path original, ResourceKey<Level> levelKey) {
        MchViewerFabric mchViewer = MchViewerFabric.getInstance();
        DimensionView dimensionView = mchViewer.getDimensionView(levelKey);
        if (dimensionView == null) {
            return original;
        }

        // The game requested the path of the dimension, but the dimension
        // is an mch-viewer world.

        return dimensionView.wrapPath(original);
    }

}
