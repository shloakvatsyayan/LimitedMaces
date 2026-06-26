package org.bonkmc.limitedmaces.recipes;

import org.bonkmc.limitedmaces.LimitedMaces;
import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.recipe.CraftingBookCategory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class RecipeController {
    private final LimitedMaces plugin;

    private final NamespacedKey leftThreeByThreeKey;
    private final NamespacedKey centerThreeByThreeKey;
    private final NamespacedKey rightThreeByThreeKey;
    private final NamespacedKey leftTwoByTwoKey;
    private final NamespacedKey rightTwoByTwoKey;
    private boolean hasLoggedCategoryFailure;

    public RecipeController(LimitedMaces plugin) {
        this.plugin = plugin;

        leftThreeByThreeKey = new NamespacedKey(plugin, "mace_c1_3x3");
        centerThreeByThreeKey = new NamespacedKey(plugin, "mace_c2_3x3");
        rightThreeByThreeKey = new NamespacedKey(plugin, "mace_c3_3x3");
        leftTwoByTwoKey = new NamespacedKey(plugin, "mace_c1_2x2");
        rightTwoByTwoKey = new NamespacedKey(plugin, "mace_c2_2x2");
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
        Iterator<Recipe> recipeIterator = Bukkit.recipeIterator();
        while (recipeIterator.hasNext()) {
            Recipe recipe = recipeIterator.next();
            if (recipe == null) continue;
            ItemStack recipeResult = recipe.getResult();
            if (recipeResult != null && recipeResult.getType() == Material.MACE) {
                if (recipe instanceof Keyed keyedRecipe) {
                    keysToRemove.add(keyedRecipe.getKey());
                }
            }
        }

        for (NamespacedKey recipeKey : keysToRemove) {
            Bukkit.removeRecipe(recipeKey);
        }
    }

    public void addOurMaceRecipes() {
        Bukkit.removeRecipe(leftThreeByThreeKey);
        Bukkit.removeRecipe(centerThreeByThreeKey);
        Bukkit.removeRecipe(rightThreeByThreeKey);
        Bukkit.removeRecipe(leftTwoByTwoKey);
        Bukkit.removeRecipe(rightTwoByTwoKey);

        ItemStack maceOutput = new ItemStack(Material.MACE, 1);

        addShaped3x3(leftThreeByThreeKey, maceOutput, "H  ", "B  ", "   ");
        addShaped3x3(centerThreeByThreeKey, maceOutput, " H ", " B ", "   ");
        addShaped3x3(rightThreeByThreeKey, maceOutput, "  H", "  B", "   ");

        addShaped2x2(leftTwoByTwoKey, maceOutput, "H ", "B ");
        addShaped2x2(rightTwoByTwoKey, maceOutput, " H", " B");
    }

    private void addShaped3x3(NamespacedKey key, ItemStack output, String firstRow, String secondRow, String thirdRow) {
        ShapedRecipe recipe = new ShapedRecipe(key, output);
        recipe.shape(firstRow, secondRow, thirdRow);
        recipe.setIngredient('H', Material.HEAVY_CORE);
        recipe.setIngredient('B', Material.BREEZE_ROD);
        trySetCategory(recipe);
        Bukkit.addRecipe(recipe);
    }

    private void addShaped2x2(NamespacedKey key, ItemStack output, String firstRow, String secondRow) {
        ShapedRecipe recipe = new ShapedRecipe(key, output);
        recipe.shape(firstRow, secondRow);
        recipe.setIngredient('H', Material.HEAVY_CORE);
        recipe.setIngredient('B', Material.BREEZE_ROD);
        trySetCategory(recipe);
        Bukkit.addRecipe(recipe);
    }

    private void trySetCategory(ShapedRecipe recipe) {
        try {
            recipe.setCategory(CraftingBookCategory.EQUIPMENT);
        } catch (RuntimeException | LinkageError exception) {
            if (!hasLoggedCategoryFailure) {
                plugin.getLogger().warning("Failed to set mace recipe crafting book category: " + exception.getMessage());
                hasLoggedCategoryFailure = true;
            }
        }
    }
}
