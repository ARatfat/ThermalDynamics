package cofh.thermaldynamics.ducts.item;

import cofh.api.inventory.IInventoryConnection;
import cofh.api.transport.IItemDuct;
import cofh.core.network.PacketCoFHBase;
import cofh.core.network.PacketHandler;
import cofh.core.network.PacketTileInfo;
import cofh.lib.util.helpers.InventoryHelper;
import cofh.lib.util.helpers.ItemHelper;
import cofh.repack.codechicken.lib.vec.BlockCoord;
import cofh.thermaldynamics.block.Attachment;
import cofh.thermaldynamics.block.AttachmentRegistry;
import cofh.thermaldynamics.block.TileMultiBlock;
import cofh.thermaldynamics.core.TDProps;
import cofh.thermaldynamics.core.TickHandlerClient;
import cofh.thermaldynamics.ducts.DuctItem;
import cofh.thermaldynamics.ducts.attachments.IStuffable;
import cofh.thermaldynamics.ducts.attachments.filter.IFilterAttachment;
import cofh.thermaldynamics.ducts.attachments.filter.IFilterItems;
import cofh.thermaldynamics.ducts.attachments.servo.ServoItem;
import cofh.thermaldynamics.multiblock.IMultiBlock;
import cofh.thermaldynamics.multiblock.IMultiBlockRoute;
import cofh.thermaldynamics.multiblock.MultiBlockGrid;
import cofh.thermaldynamics.multiblock.Route;
import cofh.thermaldynamics.multiblock.RouteCache;
import com.google.common.collect.Iterables;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraftforge.common.util.ForgeDirection;
import powercrystals.minefactoryreloaded.api.IDeepStorageUnit;

public class TileItemDuct extends TileMultiBlock implements IMultiBlockRoute, IItemDuct {

    public ItemGrid internalGrid;

    public List<TravelingItem> myItems = new LinkedList<TravelingItem>();
    public List<TravelingItem> itemsToRemove = new LinkedList<TravelingItem>();
    public List<TravelingItem> itemsToAdd = new LinkedList<TravelingItem>();

    public byte pathWeightType = 0;

    // Type Helper Arrays
    static int[] _PIPE_LEN = {40, 10, 60, 1};
    static int[] _PIPE_HALF_LEN = {_PIPE_LEN[0] / 2, _PIPE_LEN[1] / 2, _PIPE_LEN[2] / 2, 1};
    static float[] _PIPE_TICK_LEN = {1F / _PIPE_LEN[0], 1F / _PIPE_LEN[1], 1F / _PIPE_LEN[2], 1F / _PIPE_LEN[3]};

    static float[][][] _SIDE_MODS = new float[4][6][3];

    static {
        for (int i = 0; i < 4; i++) {
            float j = _PIPE_TICK_LEN[i];
            _SIDE_MODS[i][0] = new float[]{0, -j, 0};
            _SIDE_MODS[i][1] = new float[]{0, j, 0};
            _SIDE_MODS[i][2] = new float[]{0, 0, -j};
            _SIDE_MODS[i][3] = new float[]{0, 0, j};
            _SIDE_MODS[i][4] = new float[]{-j, 0, 0};
            _SIDE_MODS[i][5] = new float[]{j, 0, 0};
        }
    }

    public IFilterItems[] filterCache = {IFilterItems.nullFilter, IFilterItems.nullFilter, IFilterItems.nullFilter, IFilterItems.nullFilter,
            IFilterItems.nullFilter, IFilterItems.nullFilter};
    public IInventory[] cache = new IInventory[6];
    public ISidedInventory[] cache2 = new ISidedInventory[6];
    public IDeepStorageUnit[] cache3 = new IDeepStorageUnit[6];
    public CacheType[] cacheType = {CacheType.NONE, CacheType.NONE, CacheType.NONE, CacheType.NONE, CacheType.NONE, CacheType.NONE,};

    @Override
    public ItemStack insertItem(ForgeDirection from, ItemStack item) {

        if (!((neighborTypes[from.ordinal()] == NeighborTypes.INPUT) || (neighborTypes[from.ordinal()] == NeighborTypes.OUTPUT && connectionTypes[from
                .ordinal()].allowTransfer))) {
            return item;
        }

        if (internalGrid == null)
            return item;

        Attachment attachment = attachments[from.ordinal()];
        if (attachment == null) {
            ItemStack itemCopy = ItemHelper.cloneStack(item);
            TravelingItem routeForItem = ServoItem.findRouteForItem(ItemHelper.cloneStack(item, Math.min(8, item.stackSize)), getCache(false).outputRoutes,
                    this, from.ordinal(), ServoItem.range[0], (byte) 1);
            if (routeForItem == null) {
                return item;
            }

            itemCopy.stackSize -= routeForItem.stack.stackSize;
            insertNewItem(routeForItem);
            return itemCopy.stackSize > 0 ? itemCopy : null;
        } else if (attachment.getId() != AttachmentRegistry.SERVO_ITEM) {
            return item;
        } else {
            return ((ServoItem) attachment).insertItem(item);
        }
    }

    public Route getRoute(IMultiBlockRoute itemDuct) {
        for (Route outputRoute : getCache().outputRoutes) {
            if (outputRoute.endPoint == itemDuct
                    || (outputRoute.endPoint.x() == itemDuct.x()
                    && outputRoute.endPoint.y() == itemDuct.y()
                    && outputRoute.endPoint.z() == itemDuct.z()))
                return outputRoute;
        }
        return null;
    }

    public static enum CacheType {
        NONE, IINV, ISIDEDINV
    }

    public static class RouteInfo {

        public RouteInfo(int stackSizeLeft, byte i) {

            canRoute = true;
            stackSize = stackSizeLeft;
            side = i;
        }

        public RouteInfo() {

        }

        public boolean canRoute = false;
        public int stackSize = -1;
        public byte side = -1;
    }

    public static final RouteInfo noRoute = new RouteInfo();

    /*
     * Should return true if theTile is significant to this multiblock
     *
     * IE: Inventory's to ItemDuct's
     */
    @Override
    public boolean isSignificantTile(TileEntity theTile, int side) {

        return theTile instanceof IInventory
                && (!(theTile instanceof IInventoryConnection) || ((IInventoryConnection) theTile)
                .canConnectInventory(ForgeDirection.VALID_DIRECTIONS[side ^ 1]) != IInventoryConnection.ConnectionType.DENY);
    }

    @Override
    public void setGrid(MultiBlockGrid newGrid) {

        super.setGrid(newGrid);
        internalGrid = (ItemGrid) newGrid;
    }

    @Override
    public MultiBlockGrid getNewGrid() {

        return new ItemGrid(worldObj);
    }

    @Override
    public boolean tickPass(int pass) {

        if (!super.tickPass(pass))
            return false;

        if (pass == 0) {
            tickItems();
        }
        return true;
    }

    @Override
    public int getWeight() {

        if (pathWeightType == DuctItem.PATHWEIGHT_DENSE)
            return 1000;
        else if (pathWeightType == DuctItem.PATHWEIGHT_VACUUM)
            return -1000;
        else
            return getDuctType().pathWeight;
    }

    @Override
    public IIcon getBaseIcon() {

        if (pathWeightType == DuctItem.PATHWEIGHT_DENSE)
            return ((DuctItem) getDuctType()).iconBaseTextureDense;
        else if (pathWeightType == DuctItem.PATHWEIGHT_VACUUM)
            return ((DuctItem) getDuctType()).iconBaseTextureVacuum;
        else
            return super.getBaseIcon();
    }

    @Override
    public boolean isOutput() {

        return isOutput;
    }

    @Override
    public boolean canStuffItem() {
        for (Attachment attachment : attachments) {
            if (attachment instanceof IStuffable)
                return true;
        }
        return false;
    }

    boolean wasVisited = false;

    @Override
    public int getMaxRange() {

        return Integer.MAX_VALUE;
    }

    @Override
    public NeighborTypes getCachedSideType(byte side) {

        return neighborTypes[side];
    }

    @Override
    public ConnectionTypes getConnectionType(byte side) {

        return connectionTypes[side];
    }

    @Override
    public IMultiBlock getCachedTile(byte side) {

        return neighborMultiBlocks[side];
    }

    @Override
    public int x() {

        return xCoord;
    }

    @Override
    public int y() {

        return yCoord;
    }

    @Override
    public int z() {

        return zCoord;
    }

    @Override
    public boolean shouldRenderInPass(int pass) {

        return pass == 0 && (!myItems.isEmpty() || !itemsToAdd.isEmpty() || centerLine > 0);
    }

    public RouteCache getCache() {

        return getCache(true);
    }

    public RouteCache getCache(boolean urgent) {

        return urgent ? internalGrid.getRoutesFromOutput(this) : internalGrid.getRoutesFromOutputNonUrgent(this);
    }

    // @Override
    // public boolean openGui(EntityPlayer player) {
    // if (!isOutput())
    // return false;
    //
    // if (ServerHelper.isClientWorld(worldObj) || !isOutput())
    // return true;
    //
    // LinkedList<Route> routes = internalGrid.getRoutesFromOutput(this).outputRoutes;
    //
    // if (routes.size() <= 1)
    // return true;
    //
    //
    // for (Route route : routes) {
    // if (route.pathDirections.size() < 1)
    // continue;
    //
    // byte input;
    // for (input = 0; input < 6 && neighborTypes[input ^ 1] != NeighborTypes.OUTPUT; ) input++;
    // byte output;
    // for (output = 0; output < 6 && ((TileItemDuct) route.endPoint).neighborTypes[output] != NeighborTypes.OUTPUT; )
    // output++;
    //
    // Route itemRoute = route.copy();
    // itemRoute.pathDirections.add(output);
    // final TravelingItem travelingItem = new TravelingItem(new ItemStack(Blocks.glowstone), x(), y(), z(), itemRoute, input);
    // travelingItem.goingToStuff = true;
    // insertItem(travelingItem);
    //
    // break;
    // }
    // // player.addChatComponentMessage(new ChatComponentText("Routes: " + routes.size()));
    //
    // return true;
    // }

    public void pulseLineDo(int dir) {

        if (!getDuctType().opaque) {
            PacketTileInfo myPayload = PacketTileInfo.newPacket(this);
            myPayload.addByte(0);
            myPayload.addByte(TileInfoPackets.PULSE_LINE);
            myPayload.addByte(dir);

            PacketHandler.sendToAllAround(myPayload, this);
        }
    }

    public void pulseLine(byte dir) {

        pulseLineDo(1 << dir);
    }

    public void pulseLine(byte dir1, byte dir2) {

        pulseLineDo((1 << dir1) | (1 << dir2));
    }

    public void pulseLine() {

        pulseLineDo(63);
    }

    public int getPipeLength() {

        return _PIPE_LEN[getDuctType().type];
    }

    public int getPipeHalfLength() {

        return _PIPE_HALF_LEN[getDuctType().type];
    }

    public float[][] getSideCoordsModifier() {

        return _SIDE_MODS[getDuctType().type];
    }

    public void stuffItem(TravelingItem travelingItem) {

        Attachment attachment = attachments[travelingItem.direction];
        if (attachment instanceof IStuffable) {
            ((IStuffable) attachment).stuffItem(travelingItem.stack);
        }
    }

    public boolean acceptingItems() {

        return true;
    }

    public void insertNewItem(TravelingItem travelingItem) {

        internalGrid.poll(travelingItem);
        insertItem(travelingItem);
    }

    public void insertItem(TravelingItem travelingItem) {

        itemsToAdd.add(travelingItem);
    }

    public IInventory getCachedTileEntity(byte direction) {

        return cache[direction];
    }

    public boolean hasChanged = false;

    public void tickItems() {

        if (itemsToAdd.size() > 0) {
            myItems.addAll(itemsToAdd);
            itemsToAdd.clear();
            hasChanged = true;
        }
        if (myItems.size() > 0) {
            for (TravelingItem item : myItems) {
                item.tickForward(this);
                if (internalGrid.repoll)
                    internalGrid.poll(item);
            }
            if (itemsToRemove.size() > 0) {
                myItems.removeAll(itemsToRemove);
                itemsToRemove.clear();
                hasChanged = true;
            }
        }

        if (hasChanged) {
            internalGrid.shouldRepoll = true;
            sendTravelingItemsPacket();
            hasChanged = false;
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {

        super.readFromNBT(nbt);

        itemsToAdd.clear();
        myItems.clear();
        NBTTagList list = nbt.getTagList("TravellingItems", 10);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound compound = list.getCompoundTagAt(i);
            myItems.add(new TravelingItem(compound));
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {

        super.writeToNBT(nbt);

        NBTTagList items = new NBTTagList();
        for (TravelingItem travelingItem : Iterables.concat(itemsToAdd, myItems)) {
            NBTTagCompound tag = new NBTTagCompound();
            travelingItem.toNBT(tag);
            items.appendTag(tag);
        }

        nbt.setTag("TravellingItems", items);
    }

    public void sendTravelingItemsPacket() {

        if (!getDuctType().opaque) {
            PacketTileInfo myPayload = PacketTileInfo.newPacket(this);
            myPayload.addByte(0);
            myPayload.addByte(TileInfoPackets.TRAVELING_ITEMS);

            int loopStop = myItems.size();
            loopStop = Math.min(loopStop, TDProps.MAX_ITEMS_TRANSMITTED);
            myPayload.addByte(loopStop);
            for (int i = 0; i < loopStop; i++) {
                myItems.get(i).writePacket(myPayload);
            }

            PacketHandler.sendToAllAround(myPayload, this);
        }
    }

    @Override
    public void handleInfoPacket(PacketCoFHBase payload, boolean isServer, EntityPlayer thePlayer) {

        int b = payload.getByte();
        handlePacketType(payload, b);
    }

    public void handlePacketType(PacketCoFHBase payload, int b) {

        if (b == TileInfoPackets.PULSE_LINE) {
            int c = payload.getByte();
            for (int i = 0; i < 6; i++) {
                if ((c & (1 << i)) != 0) {
                    centerLineSub[i] = maxCenterLine;
                }
            }

            centerLine = maxCenterLine;
            if (!TickHandlerClient.tickBlocks.contains(this) && !TickHandlerClient.tickBlocksToAdd.contains(this)) {
                TickHandlerClient.tickBlocksToAdd.add(this);
            }
        } else if (b == TileInfoPackets.TRAVELING_ITEMS) {
            myItems.clear();
            byte n = payload.getByte();
            if (n > 0) {
                for (byte i = 0; i < n; i++) {
                    myItems.add(TravelingItem.fromPacket(payload, this));
                }

                if (!TickHandlerClient.tickBlocks.contains(this) && !TickHandlerClient.tickBlocksToAdd.contains(this)) {
                    TickHandlerClient.tickBlocksToAdd.add(this);
                }
            }
        }
    }

    @Override
    public void cacheImportant(TileEntity tile, int side) {

        cache[side] = (IInventory) tile;
        if (tile instanceof IDeepStorageUnit)
            cache3[side] = (IDeepStorageUnit) tile;
        if (tile instanceof ISidedInventory) {
            cache2[side] = ((ISidedInventory) tile);
            cacheType[side] = CacheType.ISIDEDINV;
        } else {
            cacheType[side] = CacheType.IINV;
        }
        if (attachments[side] instanceof IFilterAttachment)
            filterCache[side] = ((IFilterAttachment) attachments[side]).getItemFilter();
    }

    @Override
    public void clearCache(int side) {

        filterCache[side] = IFilterItems.nullFilter;
        cache[side] = null;
        cache2[side] = null;
        cache3[side] = null;
        cacheType[side] = CacheType.NONE;
    }

    public void removeItem(TravelingItem travelingItem) {

        itemsToRemove.add(travelingItem);
    }

    public class TileInfoPackets {

        public static final byte GUI_BUTTON = 0;
        public static final byte STUFFED_UPDATE = 1;
        public static final byte TRAVELING_ITEMS = 2;
        public static final byte STUFFED_ITEMS = 3;
        public static final byte REQUEST_STUFFED_ITEMS = 4;
        public static final byte PULSE_LINE = 5;
        public static final byte ENDER_POWER = 6;
    }

    @Override
    public PacketCoFHBase getPacket() {
        PacketCoFHBase packet = super.getPacket();
        packet.addByte(pathWeightType);
        return packet;
    }

    @Override
    public void handleTilePacket(PacketCoFHBase payload, boolean isServer) {

        super.handleTilePacket(payload, isServer);
        if (!isServer) pathWeightType = payload.getByte();
    }

    @Override
    public void onPlacedBy(EntityLivingBase living, ItemStack stack) {
        super.onPlacedBy(living, stack);
        if (stack.hasTagCompound()) {
            byte b = stack.getTagCompound().getByte(DuctItem.PATHWEIGHT_NBT);
            if (b == DuctItem.PATHWEIGHT_DENSE || b == DuctItem.PATHWEIGHT_VACUUM)
                pathWeightType = b;
        }
    }

    @Override
    public ItemStack getDrop() {
        ItemStack drop = super.getDrop();
        if (drop.stackTagCompound == null) drop.stackTagCompound = new NBTTagCompound();
        drop.stackTagCompound.setByte(DuctItem.PATHWEIGHT_NBT, pathWeightType);
        return drop;
    }

    public void tickItemsClient() {

        if (centerLine > 0) {
            centerLine--;
            for (int i = 0; i < 6; i++) {
                if (centerLineSub[i] > 0)
                    centerLineSub[i]--;
            }
        }

        if (itemsToAdd.size() > 0) {
            myItems.addAll(itemsToAdd);
            itemsToAdd.clear();
        }
        if (myItems.size() > 0) {
            for (int i = 0; i < myItems.size(); i++) {
                myItems.get(i).tickClientForward(this);
            }
            if (itemsToRemove.size() > 0) {
                myItems.removeAll(itemsToRemove);
                itemsToRemove.clear();
            }
        } else if (centerLine == 0) {
            TickHandlerClient.tickBlocksToRemove.add(this);
        }
    }

    @Override
    public boolean isConnectable(TileEntity theTile, int side) {

        return theTile instanceof TileItemDuct;
    }

    public static final int maxCenterLine = 10;
    public int centerLine = 0;
    public int[] centerLineSub = new int[6];

    // public int getIncoming(ItemStack anItem, int side) {
    // int stackSize = 0;
    // HashSet<TravelingItem> travelingItems = internalGrid.travelingItems.get(new BlockCoord(this).offset(side));
    // if (travelingItems != null && !travelingItems.isEmpty()) {
    // for (TravelingItem travelingItem : travelingItems) {
    // if (ItemHelper.itemsEqualWithMetadata(anItem, travelingItem.stack, true)) {
    // stackSize += travelingItem.stack.stackSize;
    // }
    // }
    // }
    //
    // return stackSize;
    // }

    @Override
    public RouteInfo canRouteItem(ItemStack anItem) {

        if (internalGrid == null)
            return noRoute;
        int stackSizeLeft;
        ItemStack curItem;

        for (byte i = internalSideCounter; i < ForgeDirection.VALID_DIRECTIONS.length; i++) {
            if (neighborTypes[i] == NeighborTypes.OUTPUT && connectionTypes[i].allowTransfer && itemPassesFiltering(i, anItem)) {
                curItem = anItem.copy();
                curItem.stackSize = Math.min(getMoveStackSize(i), curItem.stackSize);

                if (curItem.stackSize > 0) {

                    stackSizeLeft = simTransferI(i, curItem.copy());
                    stackSizeLeft = (anItem.stackSize - curItem.stackSize) + stackSizeLeft;
                    if (stackSizeLeft < anItem.stackSize) {
                        tickInternalSideCounter(i + 1);
                        return new RouteInfo(stackSizeLeft, i);
                    }
                }
            }
        }
        for (byte i = 0; i < internalSideCounter; i++) {
            if (neighborTypes[i] == NeighborTypes.OUTPUT && connectionTypes[i].allowTransfer && itemPassesFiltering(i, anItem)) {
                curItem = anItem.copy();
                curItem.stackSize = Math.min(getMoveStackSize(i), curItem.stackSize);
                if (curItem.stackSize > 0) {
                    stackSizeLeft = simTransferI(i, curItem.copy());
                    stackSizeLeft = (anItem.stackSize - curItem.stackSize) + stackSizeLeft;
                    if (stackSizeLeft < anItem.stackSize) {
                        tickInternalSideCounter(i + 1);
                        return new RouteInfo(stackSizeLeft, i);
                    }
                }
            }
        }
        return noRoute;
    }

    public int simTransferI(int side, ItemStack insertingItem) {

        ItemStack itemStack = simTransfer(side, insertingItem);
        return itemStack == null ? 0 : itemStack.stackSize;
    }

    public ItemStack simTransfer(int side, ItemStack insertingItem) {

        if (insertingItem == null)
            return null;
        if (internalGrid == null)
            return insertingItem;

        boolean routeItems = filterCache[side].shouldIncRouteItems();

        if (cache3[side] != null) { // IDeepStorage
            ItemStack cacheStack = cache3[side].getStoredItemType();
            if (cacheStack != null && !ItemHelper.itemsEqualWithMetadata(cacheStack, insertingItem))
                return insertingItem;

            int s = cacheStack != null ? cacheStack.stackSize : 0;
            int m = cache3[side].getMaxStoredCount();
            if (s >= m)
                return insertingItem;

            if (routeItems) {
                LinkedList<TravelingItem> travelingItems = internalGrid.travelingItems.get(new BlockCoord(this).offset(side));
                if (travelingItems != null && !travelingItems.isEmpty()) {
                    for (Iterator<TravelingItem> iterator = travelingItems.iterator(); s < m && iterator.hasNext(); ) {
                        TravelingItem travelingItem = iterator.next();
                        boolean equalsItem = ItemHelper.itemsEqualWithMetadata(insertingItem, travelingItem.stack);
                        if (cacheStack == null && !equalsItem)
                            return insertingItem;
                        if (equalsItem)
                            s += travelingItem.stack.stackSize;
                    }
                    if (s >= m)
                        return insertingItem;
                }
            }

            insertingItem.stackSize -= (m - s);
            if (insertingItem.stackSize <= 0)
                return null;

            return InventoryHelper.simulateInsertItemStackIntoInventory(cache[side], insertingItem, side ^ 1);
        } else {
            if (!routeItems)
                return InventoryHelper.simulateInsertItemStackIntoInventory(cache[side], insertingItem, side ^ 1);

            LinkedList<TravelingItem> travelingItems = internalGrid.travelingItems.get(new BlockCoord(this).offset(side));
            if (travelingItems == null || travelingItems.isEmpty())
                return InventoryHelper.simulateInsertItemStackIntoInventory(cache[side], insertingItem, side ^ 1);

            if (travelingItems.size() == 1) {
                if (ItemHelper.itemsEqualWithMetadata(insertingItem, travelingItems.iterator().next().stack)) {
                    insertingItem.stackSize += travelingItems.iterator().next().stack.stackSize;
                    return InventoryHelper.simulateInsertItemStackIntoInventory(cache[side], insertingItem, side ^ 1);
                }
            } else {
                int s = 0;
                for (TravelingItem travelingItem : travelingItems) {
                    if (!ItemHelper.itemsEqualWithMetadata(insertingItem, travelingItem.stack)) {
                        s = -1;
                        break;
                    } else {
                        s += travelingItem.stack.stackSize;
                    }
                }

                if (s >= 0) {
                    insertingItem.stackSize += s;
                    return InventoryHelper.simulateInsertItemStackIntoInventory(cache[side], insertingItem, side ^ 1);
                }
            }

            // Super hacky - must optimize at some point
            SimulatedInv simulatedInv = cacheType[side] == CacheType.ISIDEDINV ? SimulatedInv.wrapInvSided(cache2[side]) : SimulatedInv.wrapInv(cache[side]);

            for (TravelingItem travelingItem : travelingItems) {
                if (travelingItem.myPath != null
                        && InventoryHelper.insertItemStackIntoInventory(simulatedInv, travelingItem.stack.copy(), travelingItem.myPath.getLastSide() ^ 1) != null
                        && ItemHelper.itemsEqualWithMetadata(insertingItem, travelingItem.stack))
                    return insertingItem;
            }

            insertingItem = InventoryHelper.simulateInsertItemStackIntoInventory(simulatedInv, insertingItem, side ^ 1);
            simulatedInv.clear();
            return insertingItem;
        }
    }

    @Override
    public byte getStuffedSide() {

        for (byte i = 0; i < 6; i++) {
            if (attachments[i] instanceof IStuffable) {
                if (((IStuffable) attachments[i]).canStuff())
                    return i;

            }
        }

        for (byte i = 0; i < 6; i++) {
            if (attachments[i] instanceof IStuffable) {
                return i;
            }
        }

        throw new RuntimeException("IStuffable disapeared during calculation");
    }

    @Override
    public boolean acceptingStuff() {

        for (byte i = 0; i < 6; i++) {
            if (attachments[i] instanceof IStuffable)
                return ((IStuffable) attachments[i]).canStuff();
        }
        return false;
    }

    private boolean stuffed() {

        return false;
    }

    private boolean itemPassesFiltering(byte i, ItemStack anItem) {

        return filterCache[i].matchesFilter(anItem);
    }

    public int getMoveStackSize(byte side) {

        return 64;
    }

    public int insertIntoInventory(ItemStack stack, int direction) {

        if (cache[direction] == null)
            return stack.stackSize;
        if (!filterCache[direction].matchesFilter(stack))
            return stack.stackSize;

        return insertIntoInventory_do(stack, direction);
    }

    public int insertIntoInventory_do(ItemStack stack, int direction) {

        stack = InventoryHelper.insertItemStackIntoInventory(cache[direction], stack, direction ^ 1);
        return stack == null ? 0 : stack.stackSize;
    }
}
