package thermaldynamics.gui;

import cofh.CoFHCore;
import cofh.core.util.CoreUtils;
import cofh.lib.util.TimeTracker;
import cofh.lib.util.helpers.MathHelper;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

import java.util.LinkedList;
import java.util.List;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import thermaldynamics.block.ItemBlockDuct;
import thermaldynamics.ducts.Ducts;

public class TDCreativeTab extends CreativeTabs {

	public TDCreativeTab() {

		super("ThermalDynamics");
	}

	@Override
	@SideOnly(Side.CLIENT)
	public ItemStack getIconItemStack() {

		updateIcon();
		return Ducts.getDuct(iconIndex).itemStack;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public Item getTabIconItem() {

		return getIconItemStack().getItem();
	}

	@Override
	@SideOnly(Side.CLIENT)
	public String getTabLabel() {

		return "thermaldynamics.creativeTab";
	}

	int iconIndex = 0;
	TimeTracker iconTracker = new TimeTracker();

	private void updateIcon() {

		World world = CoFHCore.proxy.getClientWorld();

		if (CoreUtils.isClient() && iconTracker.hasDelayPassed(world, 80)) {
			int next = MathHelper.RANDOM.nextInt(Ducts.ductList.length);
			iconIndex = next >= iconIndex ? next + 1 : next;
		}
		iconTracker.markTime(world);
	}

	@Override
	@SuppressWarnings("unchecked")
	public void displayAllReleventItems(List list) {

		LinkedList<ItemStack> itemStacks = new LinkedList<ItemStack>();
		super.displayAllReleventItems(itemStacks);

		for (Ducts d : Ducts.getSortedDucts()) {
			list.add(d.itemStack.copy());
		}

		for (ItemStack item : itemStacks) {
			if (!(item.getItem() instanceof ItemBlockDuct))
				list.add(item);
		}
	}

}
