package org.bonkmc.limitedmaces.recipes;

import org.bonkmc.limitedmaces.LimitedMaces;
import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.*;
import org.bukkit.inventory.recipe.CraftingBookCategory;

import java.util.ArrayList;
import java.util.List;

public final class RecipeController {
    private final LimitedMaces plugin;

    private final NamespacedKey c1_3;
    private final NamespacedKey c2_3;
    private final NamespacedKey c3_3;
    private final NamespacedKey c1_2;
    private final NamespacedKey c2_2;

    public RecipeController(LimitedMaces plugin) {
        this.plugin = plugin;

        c1_3 = new NamespacedKey(plugin, "mace_c1_3x3");
        c2_3 = new NamespacedKey(plugin, "mace_c2_3x3");
        c3_3 = new NamespacedKey(plugin, "mace_c3_3x3");
        c1_2 = new NamespacedKey(plugin, "mace_c1_2x2");
        c2_2 = new NamespacedKey(plugin, "mace_c2_2x2");
    }

    public void syncWithLimit() {
        int allowed = plugin.cfg().getAllowedMaces();
        int active = plugin.registry().getActiveCount();

        if (active >= allowed) {
            removeAllMaceRecipes();
        } else {
            addOurMaceRecipes();
        }
    }

    public void removeAllMaceRecipes() {
        List<NamespacedKey> keysToRemove = new ArrayList<>();
        var it = Bukkit.recipeIterator();
        while (it.hasNext()) {
            Recipe r = it.next();
            if (r == null) continue;
            ItemStack result = r.getResult();
            if (result != null && result.getType() == Material.MACE) {
                if (r instanceof Keyed) {
                    keysToRemove.add(((Keyed) r).getKey());
                }
            }
        }

        for (NamespacedKey k : keysToRemove) {
            Bukkit.removeRecipe(k);
        }
    }

    public void addOurMaceRecipes() {
        Bukkit.removeRecipe(c1_3);
        Bukkit.removeRecipe(c2_3);
        Bukkit.removeRecipe(c3_3);
        Bukkit.removeRecipe(c1_2);
        Bukkit.removeRecipe(c2_2);

        ItemStack out = new ItemStack(Material.MACE, 1);

        addShaped3x3(c1_3, out, "H  ", "B  ", "   ");
        addShaped3x3(c2_3, out, " H ", " B ", "   ");
        addShaped3x3(c3_3, out, "  H", "  B", "   ");

        addShaped2x2(c1_2, out, "H ", "B ");
        addShaped2x2(c2_2, out, " H", " B");
    }

    private void addShaped3x3(NamespacedKey key, ItemStack out, String r1, String r2, String r3) {
        ShapedRecipe sr = new ShapedRecipe(key, out);
        sr.shape(r1, r2, r3);
        sr.setIngredient('H', Material.HEAVY_CORE);
        sr.setIngredient('B', Material.BREEZE_ROD);
        trySetCategory(sr);
        Bukkit.addRecipe(sr);
    }

    private void addShaped2x2(NamespacedKey key, ItemStack out, String r1, String r2) {
        ShapedRecipe sr = new ShapedRecipe(key, out);
        sr.shape(r1, r2);
        sr.setIngredient('H', Material.HEAVY_CORE);
        sr.setIngredient('B', Material.BREEZE_ROD);
        trySetCategory(sr);
        Bukkit.addRecipe(sr);
    }

    private void trySetCategory(ShapedRecipe sr) {
        try {
            sr.setCategory(CraftingBookCategory.EQUIPMENT);
        } catch (Throwable ignored) {}
    }
}
