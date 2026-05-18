package snownee.cuisine.items;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.lwjgl.input.Keyboard;

import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import snownee.cuisine.Cuisine;
import snownee.cuisine.CuisineRegistry;
import snownee.cuisine.api.CompositeFood;
import snownee.cuisine.api.CulinaryCapabilities;
import snownee.cuisine.api.CulinaryHub;
import snownee.cuisine.api.FoodContainer;
import snownee.cuisine.api.Form;
import snownee.cuisine.api.Ingredient;
import snownee.cuisine.api.Material;
import snownee.cuisine.api.Seasoning;
import snownee.cuisine.internal.CuisinePersistenceCenter;
import snownee.cuisine.util.I18nUtil;
import snownee.kiwi.item.ItemMod;
import snownee.kiwi.util.NBTHelper;

public class ItemRecipePage extends ItemMod
{
    private static final String TAG_RECIPE = "Recipe";
    private static final String TAG_INGREDIENTS = "Ingredients";
    private static final String TAG_SEASONINGS = "Seasonings";
    private static final String TAG_CUSTOM_NAME = "customName";

    public ItemRecipePage(String name)
    {
        super(name);
        setMaxStackSize(1);
        setCreativeTab(Cuisine.CREATIVE_TAB);
    }

    private boolean hasRecipe(ItemStack stack)
    {
        NBTTagCompound tag = stack.getTagCompound();
        return tag != null && tag.hasKey(TAG_RECIPE, Constants.NBT.TAG_COMPOUND);
    }

    private void recordRecipe(ItemStack stack, CompositeFood food)
    {
        NBTTagCompound recipeTag = new NBTTagCompound();

        NBTTagList ingredientList = new NBTTagList();
        for (Ingredient ingredient : food.getIngredients())
        {
            ingredientList.appendTag(CuisinePersistenceCenter.serialize(ingredient));
        }
        recipeTag.setTag(TAG_INGREDIENTS, ingredientList);

        NBTTagList seasoningList = new NBTTagList();
        for (Seasoning seasoning : food.getSeasonings())
        {
            seasoningList.appendTag(CuisinePersistenceCenter.serialize(seasoning));
        }
        recipeTag.setTag(TAG_SEASONINGS, seasoningList);

        NBTHelper helper = NBTHelper.of(stack);
        helper.setTag(TAG_RECIPE, recipeTag);
    }

    public static boolean tryFillIngredients(EntityPlayer player, ItemStack recipePage)
    {
        NBTTagCompound tag = recipePage.getTagCompound();
        if (tag == null || !tag.hasKey(TAG_RECIPE, Constants.NBT.TAG_COMPOUND))
        {
            return false;
        }

        NBTTagCompound recipeTag = tag.getCompoundTag(TAG_RECIPE);
        NBTTagList ingredientList = recipeTag.getTagList(TAG_INGREDIENTS, Constants.NBT.TAG_COMPOUND);
        if (ingredientList.tagCount() == 0)
        {
            return false;
        }

        InventoryPlayer inv = player.inventory;
        boolean addedAny = false;

        for (int i = 0; i < ingredientList.tagCount(); i++)
        {
            NBTTagCompound ingData = ingredientList.getCompoundTagAt(i);
            Ingredient ingredient = CuisinePersistenceCenter.deserializeIngredient(ingData);
            if (ingredient == null)
            {
                continue;
            }

            Material material = ingredient.getMaterial();
            Form form = ingredient.getForm();

            ItemStack foundStack = findMatchingItem(inv, material);
            if (foundStack.isEmpty())
            {
                continue;
            }

            ItemStack ingredientItem = ItemIngredient.make(material, form);
            if (ingredientItem.isEmpty())
            {
                continue;
            }

            foundStack.shrink(1);
            if (!player.addItemStackToInventory(ingredientItem))
            {
                player.dropItem(ingredientItem, false);
            }
            addedAny = true;
        }

        return addedAny;
    }

    private static ItemStack findMatchingItem(InventoryPlayer inv, Material material)
    {
        for (int i = 0; i < inv.getSizeInventory(); i++)
        {
            ItemStack stack = inv.getStackInSlot(i);
            if (stack.isEmpty())
            {
                continue;
            }

            Ingredient ing = CulinaryHub.API_INSTANCE.findIngredient(stack);
            if (ing != null && ing.getMaterial() == material)
            {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    @Nonnull
    public String getItemStackDisplayName(ItemStack stack)
    {
        String customName = NBTHelper.of(stack).getString(TAG_CUSTOM_NAME, "");
        if (!customName.isEmpty())
        {
            return customName;
        }
        if (hasRecipe(stack))
        {
            return TextFormatting.ITALIC + super.getItemStackDisplayName(stack);
        }
        return super.getItemStackDisplayName(stack);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn)
    {
        if (!hasRecipe(stack))
        {
            tooltip.add(TextFormatting.GRAY + I18nUtil.translate("tip.recipe_page.empty"));
            return;
        }

        NBTTagCompound tag = stack.getTagCompound();
        NBTTagCompound recipeTag = tag.getCompoundTag(TAG_RECIPE);

        NBTTagList ingredientList = recipeTag.getTagList(TAG_INGREDIENTS, Constants.NBT.TAG_COMPOUND);
        NBTTagList seasoningList = recipeTag.getTagList(TAG_SEASONINGS, Constants.NBT.TAG_COMPOUND);

        if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT))
        {
            if (ingredientList.tagCount() > 0)
            {
                tooltip.add(TextFormatting.YELLOW + I18nUtil.translate("tip.ingredients"));
                for (int i = 0; i < ingredientList.tagCount(); i++)
                {
                    Ingredient ingredient = CuisinePersistenceCenter.deserializeIngredient(ingredientList.getCompoundTagAt(i));
                    if (ingredient != null)
                    {
                        tooltip.add(TextFormatting.WHITE + "  " + ingredient.getTranslation());
                    }
                }
            }

            if (seasoningList.tagCount() > 0)
            {
                if (ingredientList.tagCount() > 0)
                {
                    tooltip.add("");
                }
                tooltip.add(TextFormatting.YELLOW + I18nUtil.translate("tip.seasonings"));
                for (int i = 0; i < seasoningList.tagCount(); i++)
                {
                    Seasoning seasoning = CuisinePersistenceCenter.deserializeSeasoning(seasoningList.getCompoundTagAt(i));
                    if (seasoning != null)
                    {
                        tooltip.add(TextFormatting.WHITE + "  " + I18n.format(seasoning.getSpice().getTranslationKey()) + " * " + seasoning.getSize());
                    }
                }
            }
        }
        else
        {
            int ingredientCount = ingredientList.tagCount();
            tooltip.add(TextFormatting.GRAY + I18nUtil.translate("tip.recipe_page.ingredients_count", ingredientCount));

            for (int i = 0; i < Math.min(ingredientCount, 3); i++)
            {
                Ingredient ingredient = CuisinePersistenceCenter.deserializeIngredient(ingredientList.getCompoundTagAt(i));
                if (ingredient != null)
                {
                    tooltip.add(TextFormatting.WHITE + "  " + ingredient.getTranslation());
                }
            }
            if (ingredientCount > 3)
            {
                tooltip.add(TextFormatting.GRAY + "  ...");
            }

            tooltip.add(TextFormatting.WHITE + TextFormatting.ITALIC.toString() + I18nUtil.translate("tip.shift_ingredients"));
        }

        tooltip.add(TextFormatting.DARK_GRAY + I18nUtil.translate("tip.recipe_page.rename"));
    }

    @Override
    public boolean hasEffect(ItemStack stack)
    {
        return hasRecipe(stack);
    }

    @Override
    public net.minecraft.item.EnumAction getItemUseAction(ItemStack stack)
    {
        return hasRecipe(stack) ? net.minecraft.item.EnumAction.BLOCK : super.getItemUseAction(stack);
    }

    @Override
    public int getMaxItemUseDuration(ItemStack stack)
    {
        return hasRecipe(stack) ? 16 : 0;
    }

    @Override
    @Nonnull
    public ItemStack onItemUseFinish(@Nonnull ItemStack stack, @Nonnull World worldIn, @Nonnull net.minecraft.entity.EntityLivingBase entityLiving)
    {
        if (!worldIn.isRemote && entityLiving instanceof EntityPlayer && hasRecipe(stack))
        {
            tryFillIngredients((EntityPlayer) entityLiving, stack);
        }
        return stack;
    }

    @Override
    @Nonnull
    public ActionResult<ItemStack> onItemRightClick(@Nonnull World world, @Nonnull EntityPlayer player, @Nonnull EnumHand hand)
    {
        ItemStack stack = player.getHeldItem(hand);

        if (hasRecipe(stack))
        {
            if (player.isSneaking())
            {
                if (!world.isRemote)
                {
                    NBTHelper.of(stack).remove(TAG_RECIPE);
                    NBTHelper.of(stack).remove(TAG_CUSTOM_NAME);
                }
                return new ActionResult<>(EnumActionResult.SUCCESS, stack);
            }

            player.setActiveHand(hand);
            return new ActionResult<>(EnumActionResult.SUCCESS, stack);
        }

        if (world.isRemote)
        {
            return new ActionResult<>(EnumActionResult.SUCCESS, stack);
        }

        ItemStack otherHand = player.getHeldItem(hand == EnumHand.MAIN_HAND ? EnumHand.OFF_HAND : EnumHand.MAIN_HAND);
        FoodContainer container = otherHand.getCapability(CulinaryCapabilities.FOOD_CONTAINER, null);
        CompositeFood food;
        if (container != null && (food = container.get()) != null)
        {
            recordRecipe(stack, food);
            return new ActionResult<>(EnumActionResult.SUCCESS, stack);
        }

        return new ActionResult<>(EnumActionResult.PASS, stack);
    }

    @Override
    public boolean isEnchantable(ItemStack stack)
    {
        return false;
    }
}
