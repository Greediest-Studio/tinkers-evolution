package xyz.phanta.tconevo.integration.crafttweaker;

import crafttweaker.CraftTweakerAPI;
import crafttweaker.IAction;
import crafttweaker.annotations.ZenRegister;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInterModComms;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import slimeknights.tconstruct.library.TinkerRegistry;
import slimeknights.tconstruct.library.materials.Material;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;
import xyz.phanta.tconevo.init.TconEvoPartTypes;
import xyz.phanta.tconevo.material.stats.MagicMaterialStats;

import java.util.HashSet;
import java.util.Set;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

@ZenRegister
@ZenClass("mods.tconevo.TconEvo")
@Mod.EventBusSubscriber(modid = "tconevo")
public class CrTClassTconEvo {

    // Materials that have had a placeholder magic stat added during preinit to ensure
    // the dynamic part textures (arcane focus) get generated during the texture stitch.
    // When real stats are later set (normal init loader), we replace the placeholder.
    private static final Set<String> placeholderMagic = new HashSet<>();
    private static final List<PendingMagicStat> pendingMagicStats = new ArrayList<>();

    private static class PendingMagicStat {
        final String materialId;
        final MagicMaterialStats stats;
        PendingMagicStat(String materialId, MagicMaterialStats stats) {
            this.materialId = materialId;
            this.stats = stats;
        }
    }

    @ZenMethod
    public static void setMagicMaterialStats(String materialId,
                                             int durability, float potency, float range, int harvestLevel) {
        CraftTweakerAPI.apply(new IAction() {
            @Override
            public void apply() {
                Material mat = TinkerRegistry.getMaterial(materialId);
                if (mat == null) {
                    throw new NoSuchElementException("Unknown material: " + materialId);
                }
                // If MAGIC stats not yet present, add a lightweight placeholder immediately so textures get generated.
                if (!mat.hasStats(TconEvoPartTypes.MAGIC)) {
                    TinkerRegistry.addMaterialStats(mat, new MagicMaterialStats(1, 0.01F, 0.01F, 0));
                    placeholderMagic.add(materialId);
                }
                MagicMaterialStats realStats = new MagicMaterialStats(durability, potency, range, harvestLevel);
                // Defer actual application until init to ensure all external materials & traits are registered.
                pendingMagicStats.add(new PendingMagicStat(materialId, realStats));
            }

            @Override
            public String describe() {
                return String.format("Queue MAGIC stats for %s -> {dur=%d,pot=%f,ran=%f,har=%d}%s (placeholder auto-added if absent; will apply during init)",
                        materialId, durability, potency, range, harvestLevel,
                        placeholderMagic.contains(materialId) ? " (placeholder)" : "");
            }
        });
    }

    public static void applyDeferredMagicStats() {
        if (pendingMagicStats.isEmpty()) {
            return;
        }
        CraftTweakerAPI.logInfo("[TconEvo] Applying " + pendingMagicStats.size() + " deferred MAGIC stat sets...");
        for (PendingMagicStat pending : pendingMagicStats) {
            Material mat = TinkerRegistry.getMaterial(pending.materialId);
            if (mat == null) {
                CraftTweakerAPI.logError("[TconEvo] Deferred MAGIC stats target unknown material: " + pending.materialId);
                continue;
            }
            boolean hadPlaceholder = placeholderMagic.remove(pending.materialId);
            if (mat.hasStats(TconEvoPartTypes.MAGIC)) {
                mat.addStats(pending.stats); // overrides retrieval (same key)
            } else {
                TinkerRegistry.addMaterialStats(mat, pending.stats);
            }
            CraftTweakerAPI.logInfo("[TconEvo] Applied MAGIC stats for material '" + pending.materialId + "'" + (hadPlaceholder ? " (replaced placeholder)" : ""));
        }
        pendingMagicStats.clear();
    }

    // Apply after init via IMC event (occurs after material properties set)
    @SubscribeEvent
    public static void onImcEvent(FMLInterModComms.IMCEvent event) {
        applyDeferredMagicStats();
    }

}
