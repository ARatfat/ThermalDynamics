package cofh.thermaldynamics.item;

import cofh.lib.util.helpers.StringHelper;
import cofh.thermaldynamics.block.Attachment;
import cofh.thermaldynamics.block.TileTDBase;
import cofh.thermaldynamics.duct.Duct;
import cofh.thermaldynamics.duct.attachments.filter.FilterLogic;
import cofh.thermaldynamics.duct.attachments.servo.ServoBase;
import cofh.thermaldynamics.duct.attachments.servo.ServoFluid;
import cofh.thermaldynamics.duct.attachments.servo.ServoItem;
import cofh.thermaldynamics.duct.fluid.TileFluidDuct;
import cofh.thermaldynamics.duct.item.TileItemDuct;
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

public class ItemServo extends ItemAttachment {

	public ItemServo() {

		super();
		this.setUnlocalizedName("thermaldynamics.servo");
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
			icons[i] = ir.registerIcon("thermaldynamics:servo" + i);
		}
		this.itemIcon = icons[0];
	}

	@Override
	public IIcon getIconFromDamage(int i) {

		return icons[i % icons.length];
	}

	@Override
	public Attachment getAttachment(int side, ItemStack stack, TileTDBase tile) {

		int type = stack.getItemDamage() % 5;
		if (tile instanceof TileFluidDuct) {
			return new ServoFluid(tile, (byte) (side ^ 1), type);
		}
		if (tile instanceof TileItemDuct) {
			return new ServoItem(tile, (byte) (side ^ 1), type);
		}
		return null;
	}

	@Override
	public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean extraInfo) {

		super.addInformation(stack, player, list, extraInfo);

		int type = stack.getItemDamage() % 5;

		if (!StringHelper.isShiftKeyDown()) {
			list.add(StringHelper.getInfoText("item.thermaldynamics.servo.info"));

			if (StringHelper.displayShiftForDetail) {
				list.add(StringHelper.shiftForDetails());
			}
			return;
		}
		if (ServoBase.canAlterRS(type)) {
			list.add(StringHelper.localize("info.thermaldynamics.servo.redstoneInt"));
		} else {
			list.add(StringHelper.localize("info.thermaldynamics.servo.redstoneExt"));
		}
		list.add(StringHelper.YELLOW + StringHelper.localize("info.cofh.items") + StringHelper.END);

		list.add("  "
				+ StringHelper.localize("info.thermaldynamics.servo.extractRate")
				+ ": "
				+ StringHelper.WHITE
				+ ((ServoItem.tickDelays[type] % 20) == 0 ? Integer.toString(ServoItem.tickDelays[type] / 20) : Float
						.toString(ServoItem.tickDelays[type] / 20F)) + "s" + StringHelper.END);
		list.add("  " + StringHelper.localize("info.thermaldynamics.servo.maxStackSize") + ": " + StringHelper.WHITE + ServoItem.maxSize[type]
				+ StringHelper.END);
		addFiltering(list, type, Duct.Type.ITEM);

		if (ServoItem.multiStack[type]) {
			list.add("  " + StringHelper.localize("info.thermaldynamics.servo.slotMulti"));
		} else {
			list.add("  " + StringHelper.localize("info.thermaldynamics.servo.slotSingle"));
		}
		if (ServoItem.speedBoost[type] != 1) {
			list.add("  " + StringHelper.localize("info.thermaldynamics.servo.speedBoost") + ": " + StringHelper.WHITE + ServoItem.speedBoost[type] + "x "
					+ StringHelper.END);
		}
		list.add(StringHelper.YELLOW + StringHelper.localize("info.cofh.fluids") + StringHelper.END);
		list.add("  " + StringHelper.localize("info.thermaldynamics.servo.extractRate") + ": " + StringHelper.WHITE
				+ Integer.toString((int) (ServoFluid.throttle[type] * 100)) + "%" + StringHelper.END);
		addFiltering(list, type, Duct.Type.FLUID);
	}

	public static void addFiltering(List list, int type, Duct.Type duct) {

		StringBuilder b = new StringBuilder();

		b.append(StringHelper.localize("info.thermaldynamics.filter.options") + ": " + StringHelper.WHITE);
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
			if (flag) {
				s = "  " + StringHelper.WHITE + s;
			}
			flag = true;
			list.add("  " + s + StringHelper.END);
		}
	}

	/* IInitializer */
	@Override
	public boolean preInit() {

		GameRegistry.registerItem(this, "servo");

		basicServo = new ItemStack(this, 1, 0);
		hardenedServo = new ItemStack(this, 1, 1);
		reinforcedServo = new ItemStack(this, 1, 2);
		signalumServo = new ItemStack(this, 1, 3);
		resonantServo = new ItemStack(this, 1, 4);

		ServoBase.initialize();

		return true;
	}

	IIcon[] icons;

	public static EnumRarity[] rarity = { EnumRarity.common, EnumRarity.common, EnumRarity.uncommon, EnumRarity.uncommon, EnumRarity.rare };
	public static ItemStack basicServo, hardenedServo, reinforcedServo, signalumServo, resonantServo;

}
