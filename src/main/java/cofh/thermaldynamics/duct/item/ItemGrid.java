package cofh.thermaldynamics.duct.item;

import codechicken.lib.vec.BlockCoord;
import cofh.thermaldynamics.block.Attachment;
import cofh.thermaldynamics.multiblock.IMultiBlock;
import cofh.thermaldynamics.multiblock.MultiBlockGridWithRoutes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;

public class ItemGrid extends MultiBlockGridWithRoutes {

	public ItemGrid(World world) {

		super(world);
	}

	public int travelingItemsCount = 0;
	public static ArrayList<Attachment> toTick = new ArrayList<Attachment>();
	// public HashMap<BlockCoord, LinkedList<TravelingItem>> travelingItems = new HashMap<BlockCoord, LinkedList<TravelingItem>>();
	public HashMap<BlockPos, StackMap> travelingItems = new HashMap<BlockPos, StackMap>();
	public boolean shouldRepoll = true;
	public boolean repoll = false;

	@Override
	public void tickGrid() {

		super.tickGrid();

		repoll = shouldRepoll;
		if (shouldRepoll) {
			if (!travelingItems.isEmpty()) {
				travelingItems.clear();
			}
			travelingItemsCount = 0;
		}
		shouldRepoll = false;

		for (IMultiBlock m : nodeSet) {
			if (!m.tickPass(0)) {
				break;
			}
		}
		if (repoll || travelingItemsCount > 0) {
			for (IMultiBlock m : idleSet) {
				if (!m.tickPass(0)) {
					break;
				}
			}
		}
		if (!toTick.isEmpty()) {
			for (Attachment attachment : toTick) {
				attachment.tick(1);
			}
			for (Attachment attachment : toTick) {
				attachment.tick(2);
			}
			toTick.clear();
		}
		super.tickGrid();
	}

	@Override
	public boolean canAddBlock(IMultiBlock aBlock) {

		return aBlock instanceof TileItemDuct;
	}

	public void poll(TravelingItem item) {

		travelingItemsCount++;

		if (item.myPath == null) {
			return;
		}
		BlockPos dest = item.getDest();
		StackMap list = travelingItems.get(dest);
		if (list == null) {
			list = new StackMap();
			travelingItems.put(dest, list);
		}
		list.addItemEntry(item.getStackEntry(), item.stack.stackSize);
	}

	@Override
	public void onMinorGridChange() {

		super.onMinorGridChange();
		shouldRepoll = true;
	}

	@Override
	public void onMajorGridChange() {

		super.onMajorGridChange();
		shouldRepoll = true;
	}

	@Override
	public void addInfo(List<ITextComponent> info, EntityPlayer player, boolean debug) {

		super.addInfo(info, player, debug);
		addInfo(info, "items", travelingItemsCount);
	}
}
