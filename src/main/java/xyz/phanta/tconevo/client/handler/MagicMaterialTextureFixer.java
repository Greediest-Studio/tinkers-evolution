package xyz.phanta.tconevo.client.handler;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import slimeknights.tconstruct.library.TinkerRegistry;
import slimeknights.tconstruct.library.materials.Material;
import xyz.phanta.tconevo.init.TconEvoItems;
import xyz.phanta.tconevo.init.TconEvoPartTypes;
import xyz.phanta.tconevo.TconEvoMod;

// Client-side helper: detects materials that gained MAGIC stats after the initial
// texture stitch and forces a resource reload once so their arcane focus part
// sprites get generated.
public class MagicMaterialTextureFixer {

    private boolean performedReload = false;

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.world == null) { // wait until a world is loaded
            return;
        }
        if (performedReload) {
            return;
        }
        // Heuristic: find any material that has MAGIC stats but whose focus part model
        // still resolves to the base (untinted) texture. If found, trigger one reload.
        try {
            String baseTexPath = "tconevo:items/part/arcane_focus"; // defined in part_arcane_focus.tmat.json
            TextureMap texMap = mc.getTextureMapBlocks();
            boolean needsReload = false;
            for (Material mat : TinkerRegistry.getAllMaterials()) {
                if (mat.hasStats(TconEvoPartTypes.MAGIC)) {
                    String expectedSprite = baseTexPath + "_" + mat.identifier; // tinted sprite name produced by render info
                    if (texMap.getTextureExtry(expectedSprite) == null) {
                        // Missing tinted sprite; schedule reload
                        needsReload = true;
                        break;
                    }
                }
            }
            if (needsReload) {
                TconEvoMod.LOGGER.info("Detected missing MAGIC part sprites; performing one-time resource reload to generate them.");
                performedReload = true; // set first to avoid recursion
                mc.refreshResources();
            } else {
                performedReload = true; // no need; mark done
            }
        } catch (Exception e) {
            TconEvoMod.LOGGER.warn("MagicMaterialTextureFixer encountered an exception; skipping reload.", e);
            performedReload = true;
        }
    }
}
