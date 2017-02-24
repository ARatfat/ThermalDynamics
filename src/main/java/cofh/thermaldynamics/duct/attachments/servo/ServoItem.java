package cofh.thermaldynamics.duct.attachments.servo;

import codechicken.lib.util.BlockUtils;
import cofh.lib.util.helpers.ItemHelper;
import cofh.thermaldynamics.block.AttachmentRegistry;
import cofh.thermaldynamics.block.TileTDBase;
import cofh.thermaldynamics.duct.Duct;
import cofh.thermaldynamics.duct.attachments.filter.FilterLogic;
import cofh.thermaldynamics.duct.item.ItemGrid;
import cofh.thermaldynamics.duct.item.TileItemDuct;
import cofh.thermaldynamics.duct.item.TravelingItem;
import cofh.thermaldynamics.init.TDProps;
import cofh.thermaldynamics.multiblock.Route;
import cofh.thermaldynamics.multiblock.RouteCache;
import cofh.thermaldynamics.multiblock.listtypes.ListWrapper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class ServoItem extends ServoBase {

	public static int[] range = { Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE };
	public static int[] maxSize = { 4, 16, 64, 64, 64 };
	public static boolean[] multiStack = { false, false, false, true, true };

	public LinkedList<ItemStack> stuffedItems = new LinkedList<ItemStack>();

	public TileItemDuct itemDuct;

	public ServoItem(TileTDBase tile, byte side, int type) {

		super(tile, side, type);
		itemDuct = ((TileItemDuct) tile);
	}

	public ServoItem(TileTDBase tile, byte side) {

		super(tile, side);
		itemDuct = ((TileItemDuct) tile);
	}

	@Override
	public int getId() {

		return AttachmentRegistry.SERVO_ITEM;
	}

	@Override
	public void writeToNBT(NBTTagCompound tag) {

		super.writeToNBT(tag);
		if (isStuffed()) {
			NBTTagList list = new NBTTagList();
			for (ItemStack item : stuffedItems) {
				NBTTagCompound newTag = new NBTTagCompound();
				ItemHelper.writeItemStackToNBT(item, newTag);
				list.appendTag(newTag);
			}
			tag.setTag("StuffedInv", list);
		}
	}

	@Override
	public void readFromNBT(NBTTagCompound tag) {

		super.readFromNBT(tag);
		stuffedItems.clear();
		if (tag.hasKey("StuffedInv", 9)) {
			NBTTagList tlist = tag.getTagList("StuffedInv", 10);
			for (int j = 0; j < tlist.tagCount(); j++) {
				ItemStack item = ItemHelper.readItemStackFromNBT(tlist.getCompoundTagAt(j));
				if (item != null && item.getItem() != null) {
					stuffedItems.add(item);
				}
			}
		}
		stuffed = isStuffed();
	}

	@Override
	public boolean canStuff() {

		return true;
	}

	@Override
	public void stuffItem(ItemStack item) {

		for (ItemStack stuffed : stuffedItems) {
			if (ItemHelper.itemsEqualWithMetadata(item, stuffed, true)) {
				stuffed.stackSize += item.stackSize;
				if (stuffed.stackSize < 0) {
					stuffed.stackSize = Integer.MAX_VALUE;
				}
				return;
			}
		}

		stuffedItems.add(item.copy());
		onNeighborChange();
	}

	public RouteCache cache = null;
	public ListWrapper<Route> routeList = new ListWrapper<Route>();

	@Override
	public List<ItemStack> getDrops() {

		List<ItemStack> drops = super.getDrops();

		if (isStuffed()) {
			for (ItemStack stuffedItem : stuffedItems) {
				ItemStack stack = stuffedItem.copy();
				while (stack.stackSize > 0 && drops.size() <= TDProps.MAX_STUFFED_ITEMSTACKS_DROP) {
					if (stack.stackSize <= stuffedItem.getMaxStackSize()) {
						drops.add(ItemHelper.cloneStack(stack));
						break;
					} else {
						drops.add(stack.splitStack(stuffedItem.getMaxStackSize()));
					}
				}
			}
		}

		return drops;
	}

	public static int[] tickDelays = { 60, 40, 20, 10, 10 };
	public static byte[] speedBoost = { 1, 1, 1, 2, 3 };

	public int tickDelay() {

		return tickDelays[type];
	}

	@Override
	public boolean onWrenched() {

		if (isStuffed()) {
			for (ItemStack stack : stuffedItems) {
				while (stack.stackSize > 0) {
					dropItemStack(stack.splitStack(Math.min(stack.stackSize, stack.getMaxStackSize())));
				}
			}
			stuffedItems.clear();
			onNeighborChange();
			return true;
		} else {
			return super.onWrenched();
		}
	}

	@Override
	public void tick(int pass) {

		if (pass == 0) {
			if (isPowered && (isValidInput || isStuffed()) && itemDuct.world().getTotalWorldTime() % tickDelay() == 0) {
				ItemGrid.toTick.add(this);
			}
			return;
		} else if (!isPowered || itemDuct.world().getTotalWorldTime() % tickDelay() != 0) {
			return;
		}
		if (!verifyCache()) {
			return;
		}
		if (cache.outputRoutes.isEmpty()) {
			return;
		}
		if (pass == 1) {
			if (isStuffed()) {
				handleStuffedItems();
			} else if (stuffed) {
				onNeighborChange();
			}
		} else if (pass == 2 && !stuffed) {
			if (!isValidInput) {
				return;
			}
			handleItemSending();
		}
	}

	public boolean verifyCache() {

		RouteCache cache1 = itemDuct.getCache(false);
		if (!cache1.isFinishedGenerating()) {
			return false;
		}

		if (cache1 != cache || routeList.type != getSortType()) {
			cache = cache1;
			routeList.setList(cache.outputRoutes, getSortType());
		}
		return true;
	}

	public void handleItemSending() {

		if (cachedInv != null) {
			for (int slot = 0; slot < cachedInv.getSlots(); slot++) {
				ItemStack itemStack = cachedInv.getStackInSlot(slot);
				if (itemStack == null) {
					continue;
				}

				itemStack = limitOutput(itemStack.copy(), cachedInv, slot, side);

				if (itemStack == null || itemStack.stackSize == 0) {
					continue;
				}
				if (!filter.matchesFilter(itemStack)) {
					continue;
				}
				TravelingItem travelingItem = getRouteForItem(itemStack);

				if (travelingItem == null) {
					continue;
				}

				int totalSendSize = travelingItem.stack.stackSize;

				travelingItem.stack = cachedInv.extractItem(slot, travelingItem.stack.stackSize, false);

				if (travelingItem.stack == null || travelingItem.stack.stackSize <= 0) {
					continue;
				}
				if (multiStack[type]) {
					if (travelingItem.stack.stackSize < totalSendSize) {
						for (slot++; slot < cachedInv.getSlots() && travelingItem.stack.stackSize < totalSendSize; slot++) {
							itemStack = cachedInv.getStackInSlot(slot);
							if (ItemHelper.itemsEqualWithMetadata(travelingItem.stack, itemStack, true)) {
								itemStack = cachedInv.extractItem(slot, totalSendSize - travelingItem.stack.stackSize, false);
								if (itemStack != null) {
									travelingItem.stack.stackSize += itemStack.stackSize;
								}
							}
						}
					}
				}
				itemDuct.insertNewItem(travelingItem);
				return;
			}
		}
	}

	public void handleStuffedItems() {

		for (Iterator<ItemStack> iterator = stuffedItems.iterator(); iterator.hasNext(); ) {
			ItemStack stuffedItem = iterator.next();
			ItemStack send = stuffedItem.copy();
			send.stackSize = Math.min(send.stackSize, send.getMaxStackSize());
			TravelingItem travelingItem = getRouteForItem(send);

			if (travelingItem == null) {
				continue;
			}
			stuffedItem.stackSize -= travelingItem.stack.stackSize;
			if (stuffedItem.stackSize <= 0) {
				iterator.remove();
			}

			itemDuct.insertNewItem(travelingItem);
			return;
		}
	}

	public byte getSpeed() {

		return speedBoost[type];
	}

	public static TravelingItem findRouteForItem(ItemStack item, Iterable<Route> routes, TileItemDuct duct, int side, int maxRange, byte speed) {

		if (item == null || item.stackSize == 0) {
			return null;
		}

		item = item.copy();

		if (item.stackSize == 0) {
			return null;
		}

		for (Route outputRoute : routes) {
			if (outputRoute.pathDirections.size() <= maxRange) {
				TileItemDuct.RouteInfo routeInfo = outputRoute.endPoint.canRouteItem(item);
				if (routeInfo.canRoute) {
					int stackSize = item.stackSize - routeInfo.stackSize;
					if (stackSize <= 0) {
						continue;
					}
					Route itemRoute = outputRoute.copy();
					itemRoute.pathDirections.add(routeInfo.side);
					item.stackSize -= routeInfo.stackSize;
					return new TravelingItem(item, duct, itemRoute, (byte) (side ^ 1), speed);
				}
			}
		}
		return null;
	}

	public int getMaxRange() {

		return range[type];
	}

	public ItemStack limitOutput(ItemStack itemStack, IItemHandler cachedInv, int slot, byte side) {

		itemStack.stackSize = Math.min(itemStack.stackSize, filter.getLevel(FilterLogic.levelStackSize));
		return itemStack;
	}

	@Override
	public void onNeighborChange() {

		if (stuffed != !stuffedItems.isEmpty()) {
			stuffed = isStuffed();
			BlockUtils.fireBlockUpdate(tile.getWorld(), tile.getPos());
		}
		super.onNeighborChange();
	}

	@Override
	public boolean isStuffed() {

		return !stuffedItems.isEmpty();
	}

	@Override
	public boolean isValidTile(TileEntity tile) {

		return tile != null && tile.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, EnumFacing.VALUES[side ^ 1]);
	}

	@Override
	public void clearCache() {

		cachedInv = null;
	}

	@Override
	public void cacheTile(TileEntity tile) {

		cachedInv = tile.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, EnumFacing.VALUES[side ^ 1]);
	}

	public IItemHandler cachedInv;

	public ItemStack insertItem(ItemStack item) {

		if (!filter.matchesFilter(item)) {
			return item;
		}

		ItemStack sending = limitOutput(item.copy(), null, -1, (byte) 0);
		TravelingItem routeForItem = getRouteForItem(sending);
		if (routeForItem == null) {
			return item;
		}
		item.stackSize -= routeForItem.stack.stackSize;
		itemDuct.insertNewItem(routeForItem);
		return item.stackSize > 0 ? item : null;
	}

	public TravelingItem getRouteForItem(ItemStack item) {

		if (!verifyCache()) {
			return null;
		}
		return ServoItem.findRouteForItem(item, routeList, itemDuct, side, getMaxRange(), getSpeed());
	}

	public ListWrapper.SortType getSortType() {

		int level = filter.getLevel(FilterLogic.levelRouteMode);
		return ListWrapper.SortType.values()[level];
	}

	@Override
	public FilterLogic createFilterLogic() {

		return new FilterLogic(type, Duct.Type.ITEM, this);
	}

}
