package cofh.thermaldynamics.item;

import cofh.lib.util.helpers.StringHelper;
import cofh.thermaldynamics.block.Attachment;
import cofh.thermaldynamics.block.TileTDBase;
import cofh.thermaldynamics.duct.Duct;
import cofh.thermaldynamics.duct.attachments.filter.FilterLogic;
import cofh.thermaldynamics.duct.attachments.retriever.RetrieverFluid;
import cofh.thermaldynamics.duct.attachments.retriever.RetrieverItem;
import cofh.thermaldynamics.duct.attachments.servo.ServoBase;
import cofh.thermaldynamics.duct.attachments.servo.ServoFluid;
import cofh.thermaldynamics.duct.attachments.servo.ServoItem;
import cofh.thermaldynamics.duct.fluid.TileFluidDuct;
import cofh.thermaldynamics.duct.item.TileItemDuct;
import net.minecraft.client.Minecraft;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fml.common.registry.GameRegistry;

import java.util.List;

public class ItemRetriever extends ItemAttachment {

    public static EnumRarity[] rarity = { EnumRarity.COMMON, EnumRarity.COMMON, EnumRarity.UNCOMMON, EnumRarity.UNCOMMON, EnumRarity.RARE };
    public static ItemStack basicRetriever, hardenedRetriever, reinforcedRetriever, signalumRetriever, resonantRetriever;

    public ItemRetriever() {

        super();
        this.setUnlocalizedName("thermaldynamics.retriever");
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

	/*@Override
    @SideOnly(Side.CLIENT)
	public void registerIcons(IIconRegister ir) {

		icons = new IIcon[5];
		for (int i = 0; i < 5; i++) {
			icons[i] = ir.registerIcon("thermaldynamics:retriever" + i);
		}
		this.itemIcon = icons[0];
	}

	@Override
	public IIcon getIconFromDamage(int i) {

		return icons[i % icons.length];
	}*/

    @Override
    public Attachment getAttachment(EnumFacing side, ItemStack stack, TileTDBase tile) {

        int type = stack.getItemDamage() % 5;
        if (tile instanceof TileFluidDuct) {
            return new RetrieverFluid(tile, (byte) (side.ordinal() ^ 1), type);
        }
        if (tile instanceof TileItemDuct) {
            return new RetrieverItem(tile, (byte) (side.ordinal() ^ 1), type);
        }
        return null;
    }

    @Override
    public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean extraInfo) {

        super.addInformation(stack, player, list, extraInfo);

        int type = stack.getItemDamage() % 5;

        if (!StringHelper.isShiftKeyDown()) {
            list.add(StringHelper.getInfoText("item.thermaldynamics.retriever.info"));

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

        list.add("  " + StringHelper.localize("info.thermaldynamics.servo.extractRate") + ": " + StringHelper.WHITE + ((ServoItem.tickDelays[type] % 20) == 0 ? Integer.toString(ServoItem.tickDelays[type] / 20) : Float.toString(ServoItem.tickDelays[type] / 20F)) + "s" + StringHelper.END);
        list.add("  " + StringHelper.localize("info.thermaldynamics.servo.maxStackSize") + ": " + StringHelper.WHITE + ServoItem.maxSize[type] + StringHelper.END);
        addFiltering(list, type, Duct.Type.ITEM);

        if (ServoItem.multiStack[type]) {
            list.add("  " + StringHelper.localize("info.thermaldynamics.servo.slotMulti"));
        } else {
            list.add("  " + StringHelper.localize("info.thermaldynamics.servo.slotSingle"));
        }
        if (ServoItem.speedBoost[type] != 1) {
            list.add("  " + StringHelper.localize("info.thermaldynamics.servo.speedBoost") + ": " + StringHelper.WHITE + ServoItem.speedBoost[type] + "x " + StringHelper.END);
        }
        list.add(StringHelper.YELLOW + StringHelper.localize("info.cofh.fluids") + StringHelper.END);
        list.add("  " + StringHelper.localize("info.thermaldynamics.servo.extractRate") + ": " + StringHelper.WHITE + Integer.toString((int) (ServoFluid.throttle[type] * 100)) + "%" + StringHelper.END);
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
        for (String s : Minecraft.getMinecraft().fontRendererObj.listFormattedStringToWidth(b.toString(), 140)) {
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

        GameRegistry.registerItem(this, "retriever");

        basicRetriever = new ItemStack(this, 1, 0);
        hardenedRetriever = new ItemStack(this, 1, 1);
        reinforcedRetriever = new ItemStack(this, 1, 2);
        signalumRetriever = new ItemStack(this, 1, 3);
        resonantRetriever = new ItemStack(this, 1, 4);

        return true;
    }

}
