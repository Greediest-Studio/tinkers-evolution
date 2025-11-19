package xyz.phanta.tconevo.integration.crafttweaker;

import crafttweaker.CraftTweakerAPI;
import crafttweaker.IAction;
import crafttweaker.annotations.ZenRegister;
import slimeknights.tconstruct.library.TinkerRegistry;
import slimeknights.tconstruct.library.materials.Material;
import slimeknights.tconstruct.library.traits.ITrait;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;
import xyz.phanta.tconevo.init.TconEvoPartTypes;
import xyz.phanta.tconevo.material.stats.MagicMaterialStats;

import javax.annotation.Nullable;
import java.util.NoSuchElementException;

@ZenRegister
@ZenClass("mods.tconevo.TconEvo")
public class CrTClassTconEvo {

    @ZenMethod
    public static void setMagicMaterialStats(String materialId,
                                             int durability, float potency, float range, int harvestLevel,
                                             @Nullable String[] traitIds) {
        CraftTweakerAPI.apply(new IAction() {
            @Override
            public void apply() {
                Material mat = TinkerRegistry.getMaterial(materialId);
                if (mat == null) {
                    throw new NoSuchElementException("Unknown material: " + materialId);
                }
                if (mat.hasStats(TconEvoPartTypes.MAGIC)) {
                    // probably should fire a MaterialEvent.StatRegisterEvent here, but oh well
                    mat.addStats(new MagicMaterialStats(durability, potency, range, harvestLevel));
                } else {
                    TinkerRegistry.addMaterialStats(
                            mat, new MagicMaterialStats(durability, potency, range, harvestLevel));
                }
                
                // Add traits to the magic part type
                if (traitIds != null && traitIds.length > 0) {
                    for (String traitId : traitIds) {
                        ITrait trait = TinkerRegistry.getTrait(traitId);
                        if (trait != null) {
                            if (!mat.hasTrait(traitId, TconEvoPartTypes.MAGIC)) {
                                mat.addTrait(trait, TconEvoPartTypes.MAGIC);
                            }
                        } else {
                            CraftTweakerAPI.logWarning("Unknown trait: " + traitId + " for material: " + materialId);
                        }
                    }
                }
            }

            @Override
            public String describe() {
                StringBuilder desc = new StringBuilder(String.format(
                        "Setting magic material stats for %s to {dur=%d,pot=%f,ran=%f,har=%d}",
                        materialId, durability, potency, range, harvestLevel));
                if (traitIds != null && traitIds.length > 0) {
                    desc.append(" with traits: ");
                    desc.append(String.join(", ", traitIds));
                }
                return desc.toString();
            }
        });
    }

}
