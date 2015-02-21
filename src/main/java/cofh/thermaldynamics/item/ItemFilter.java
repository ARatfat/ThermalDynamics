package cofh.thermaldynamics.item;

import cofh.lib.util.helpers.StringHelper;
import cofh.thermaldynamics.block.Attachment;
import cofh.thermaldynamics.block.TileMultiBlock;
import cofh.thermaldynamics.ducts.Duct;
import cofh.thermaldynamics.ducts.attachments.filter.FilterFluid;
import cofh.thermaldynamics.ducts.attachments.filter.FilterItem;
import cofh.thermaldynamics.ducts.attachments.filter.FilterLogic;
import cofh.thermaldynamics.ducts.fluid.TileFluidDuct;
import cofh.thermaldynamics.ducts.item.TileItemDuct;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;

public class ItemFilter extends ItemAttachment {

	public ItemFilter() {

		super();
		this.setUnlocalizedName("thermaldynamics.filter");
	}

	@Override
	public String getUnlocalizedName(ItemStack item) {

		return super.getUnlocalizedName(item) + "." + item.getItemDamage();
	}

	@Override
	public void getSubItems(Item item, CreativeTabs tab, List list) {

		for (int i = 0; i < 5; i++) {
			list.add(new ItemStack(item, 1, i));
		}
	}

	@Override
	public EnumRarity getRarity(ItemStack stack) {

		return rarity[stack.getItemDamage() % 5];
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void registerIcons(IIconRegister ir) {

		icons = new IIcon[5];
		for (int i = 0; i < 5; i++) {
			icons[i] = ir.registerIcon("thermaldynamics:filter" + i);
		}
		this.itemIcon = icons[0];
	}

	@Override
	public IIcon getIconFromDamage(int i) {

		return icons[i % icons.length];
	}

	@Override
	public Attachment getAttachment(int side, ItemStack stack, TileMultiBlock tile) {

		int type = stack.getItemDamage() % 5;
		if (tile instanceof TileFluidDuct) {
			return new FilterFluid(tile, (byte) (side ^ 1), type);
		}
		if (tile instanceof TileItemDuct) {
			return new FilterItem(tile, (byte) (side ^ 1), type);
		}
		return null;
	}

	@Override
	public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean extraInfo) {

		super.addInformation(stack, player, list, extraInfo);

		int type = stack.getItemDamage() % 5;

		if (!StringHelper.isShiftKeyDown()) {
			list.add(StringHelper.getInfoText("item.thermaldynamics.filter.info"));

			if (StringHelper.displayShiftForDetail) {
				list.add(StringHelper.shiftForDetails());
			}
			return;
		}
		list.add(StringHelper.YELLOW + StringHelper.localize("info.cofh.items") + StringHelper.END);
		addFiltering(list, type, Duct.Type.ITEM);
		list.add(StringHelper.YELLOW + StringHelper.localize("info.cofh.fluids") + StringHelper.END);
		addFiltering(list, type, Duct.Type.FLUID);
	}

	public static void addFiltering(List list, int type, Duct.Type duct) {

		StringBuilder b = new StringBuilder();

		b.append("Filter Options: " + StringHelper.WHITE);
		boolean flag = false;
		for (int i = 0; i < FilterLogic.flagTypes.length; i++) {
			if (FilterLogic.canAlterFlag(duct, type, i)) {
				if (flag) {
					b.append(", ");
				} else {
					flag = true;
				}
				b.append(StringHelper.localize("info.thermaldynamics.filter." + FilterLogic.flagTypes[i]));
			}
		}
		flag = false;
		for (String s : (List<String>) Minecraft.getMinecraft().fontRenderer.listFormattedStringToWidth(b.toString(), 140)) {
			if (flag)
				s = "  " + StringHelper.WHITE + s;
			flag = true;
			list.add("  " + s + StringHelper.END);
		}
	}

	/* IInitializer */
	@Override
	public boolean preInit() {

		GameRegistry.registerItem(this, "filter");

		basicFilter = new ItemStack(this, 1, 0);
		hardenedFilter = new ItemStack(this, 1, 1);
		reinforcedFilter = new ItemStack(this, 1, 2);
		signalumFilter = new ItemStack(this, 1, 3);
		resonantFilter = new ItemStack(this, 1, 4);

		return true;
	}

	IIcon[] icons;

	public static EnumRarity[] rarity = { EnumRarity.common, EnumRarity.common, EnumRarity.uncommon, EnumRarity.uncommon, EnumRarity.rare };
	public static ItemStack basicFilter, hardenedFilter, reinforcedFilter, signalumFilter, resonantFilter;

}
