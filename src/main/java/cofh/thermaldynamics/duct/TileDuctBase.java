package cofh.thermaldynamics.duct;

import codechicken.lib.raytracer.IndexedCuboid6;
import codechicken.lib.raytracer.RayTracer;
import codechicken.lib.util.BlockUtils;
import codechicken.lib.vec.Cuboid6;
import cofh.api.core.IPortableData;
import cofh.api.tileentity.ITileInfo;
import cofh.core.block.TileCore;
import cofh.core.network.ITileInfoPacketHandler;
import cofh.core.network.ITilePacketHandler;
import cofh.core.network.PacketCoFHBase;
import cofh.core.render.hitbox.CustomHitBox;
import cofh.core.render.hitbox.ICustomHitBox;
import cofh.lib.util.helpers.BlockHelper;
import cofh.lib.util.helpers.ServerHelper;
import cofh.lib.util.helpers.WrenchHelper;
import cofh.thermaldynamics.duct.attachments.cover.Cover;
import cofh.thermaldynamics.duct.attachments.relay.Relay;
import cofh.thermaldynamics.duct.nutypeducts.TileGrid;
import cofh.thermaldynamics.multiblock.IGridTile;
import cofh.thermaldynamics.multiblock.MultiBlockGrid;
import cofh.thermaldynamics.util.TickHandler;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public abstract class TileDuctBase extends TileCore implements IGridTile, ITilePacketHandler, ICustomHitBox, ITileInfoPacketHandler, IPortableData, ITileInfo {

	static {
		GameRegistry.registerTileEntity(TileDuctBase.class, "thermaldynamics.duct");
	}



	public int facadeMask;

	public boolean isValid = true;
	public boolean isNode = false;
	public MultiBlockGrid myGrid;
	public IGridTile neighborMultiBlocks[] = new IGridTile[EnumFacing.VALUES.length];
	public NeighborType neighborTypes[] = { NeighborType.NONE, NeighborType.NONE, NeighborType.NONE, NeighborType.NONE, NeighborType.NONE, NeighborType.NONE };
	public ConnectionType connectionTypes[] = { cofh.thermaldynamics.duct.ConnectionType.NORMAL, cofh.thermaldynamics.duct.ConnectionType.NORMAL, cofh.thermaldynamics.duct.ConnectionType.NORMAL, cofh.thermaldynamics.duct.ConnectionType.NORMAL, cofh.thermaldynamics.duct.ConnectionType.NORMAL, cofh.thermaldynamics.duct.ConnectionType.NORMAL, cofh.thermaldynamics.duct.ConnectionType.BLOCKED };
	public byte internalSideCounter = 0;

	public Attachment attachments[] = new Attachment[] { null, null, null, null, null, null };
	public Cover[] covers = new Cover[6];

	LinkedList<Attachment> tickingAttachments = new LinkedList<>();

	public long lastUpdateTime = -1;
	public int hashCode = 0;

	public LinkedList<WeakReference<Chunk>> neighbourChunks = new LinkedList<>();

	@Override
	public void onChunkUnload() {

		if (ServerHelper.isServerWorld(worldObj)) {
			for (SubTileGridTile subTile : subTiles) {
				subTile.onChunkUnload();
			}

			if (myGrid != null) {
				tileUnloading();
				myGrid.removeBlock(this);
			}
		}

		super.invalidate();
	}

	public void tileUnloading() {

	}

	@Override
	public void invalidate() {

		super.invalidate();

		if (ServerHelper.isServerWorld(worldObj)) {
			for (SubTileGridTile subTile : subTiles) {
				subTile.invalidate();
			}
			if (myGrid != null) {
				myGrid.removeBlock(this);
			}
		}
	}

	public boolean isStructureTile(TileEntity tile, int side) {

		return false;
	}

	public boolean removeAttachment(Attachment attachment) {

		if (attachment == null) {
			return false;
		}
		attachments[attachment.side] = null;
		tickingAttachments.remove(attachment);
		connectionTypes[attachment.side] = cofh.thermaldynamics.duct.ConnectionType.NORMAL;
		worldObj.notifyNeighborsOfStateChange(getPos(), getBlockType());
		onNeighborBlockChange();
		if (myGrid != null) {
			myGrid.destroyAndRecreate();
		}
		for (SubTileGridTile subTile : subTiles) {
			subTile.destroyAndRecreate();
		}
		BlockUtils.fireBlockUpdate(world(), getPos());
		return true;
	}

	public boolean addAttachment(Attachment attachment) {

		if (attachments[attachment.side] != null || !attachment.canAddToTile(this)) {
			return false;
		}
		if (ServerHelper.isClientWorld(worldObj)) {
			return true;
		}

		attachments[attachment.side] = attachment;
		if (attachment.doesTick()) {
			tickingAttachments.add(attachment);
		}
		connectionTypes[attachment.side] = cofh.thermaldynamics.duct.ConnectionType.BLOCKED;
		worldObj.notifyNeighborsOfStateChange(getPos(), getBlockType());
		onNeighborBlockChange();
		if (myGrid != null) {
			myGrid.destroyAndRecreate();
		}
		for (SubTileGridTile subTile : subTiles) {
			subTile.destroyAndRecreate();
		}
		return true;
	}

	@Override
	public void blockPlaced() {

		if (ServerHelper.isServerWorld(worldObj)) {
			TickHandler.addMultiBlockToCalculate(this);
		}
	}

	@Override
	public void onNeighborBlockChange() {

		if (ServerHelper.isClientWorld(worldObj) && lastUpdateTime == worldObj.getTotalWorldTime()) {
			return;
		}

		if (isInvalid()) {
			return;
		}

		boolean wasNode = isNode;
		isNode = false;
		boolean wasInput = isInput;
		isInput = false;
		boolean wasOutput = isOutput;
		isOutput = false;

		for (byte i = 0; i < EnumFacing.VALUES.length; i++) {
			handleSideUpdate(i);
		}

		if (myGrid != null) {
			if (wasNode != isNode) {
				myGrid.addBlock(this);
			} else if ((isOutput != wasOutput || isInput != wasInput)) {
				myGrid.onMajorGridChange();
			}
		}

		for (SubTileGridTile subTile : subTiles) {
			subTile.onNeighbourChange();
		}

		for (Attachment tickingAttachment : tickingAttachments) {
			tickingAttachment.postNeighbourChange();
		}

		if (ServerHelper.isServerWorld(worldObj)) {
			rebuildChunkCache();
		}

		BlockUtils.fireBlockUpdate(getWorld(), getPos());
	}

	public void handleSideUpdate(int i) {

		if (cachesExist()) {
			clearCache(i);
		}

		handleAttachmentUpdate(i);
		handleTileSideUpdate(i);
	}

	public void handleAttachmentUpdate(int i) {

		TileEntity theTile;
		neighborTypes[i] = null;
		if (attachments[i] != null) {
			attachments[i].onNeighborChange();
			neighborMultiBlocks[i] = null;

			neighborTypes[i] = attachments[i].getNeighborType();
			if (neighborTypes[i] == NeighborType.MULTIBLOCK) {
				theTile = getAdjTileEntitySafe(i);
				if (isConnectable(theTile, i) && isUnblocked(theTile, i)) {
					neighborMultiBlocks[i] = (IGridTile) theTile;
				} else {
					neighborTypes[i] = NeighborType.NONE;
				}
			} else if (neighborTypes[i] == NeighborType.OUTPUT) {
				theTile = getAdjTileEntitySafe(i);
				if (isSignificantTile(theTile, i)) {
					if (!cachesExist()) {
						createCaches();
					}
					cacheImportant(theTile, i);
				}
				isOutput = true;
			} else if (neighborTypes[i] == NeighborType.INPUT) {
				theTile = getAdjTileEntitySafe(i);
				if (theTile != null) {
					if (!cachesExist()) {
						createCaches();
					}
					cacheInputTile(theTile, i);
				}
				isInput = true;
			} else {
				neighborMultiBlocks[i] = null;
			}

			connectionTypes[i] = cofh.thermaldynamics.duct.ConnectionType.NORMAL;
			isNode = attachments[i].isNode();
		}
	}

	public void handleTileSideUpdate(int i) {

		TileEntity theTile;
		if (neighborTypes[i] == null) {
			theTile = getAdjTileEntitySafe(i);
			if (theTile == null) {
				neighborMultiBlocks[i] = null;
				neighborTypes[i] = NeighborType.NONE;
				if (connectionTypes[i] != cofh.thermaldynamics.duct.ConnectionType.FORCED) {
					connectionTypes[i] = cofh.thermaldynamics.duct.ConnectionType.NORMAL;
				}
			} else if (isConnectable(theTile, i) && isUnblocked(theTile, i)) {
				neighborMultiBlocks[i] = (IGridTile) theTile;
				neighborTypes[i] = NeighborType.MULTIBLOCK;
			} else if (connectionTypes[i].allowTransfer && isSignificantTile(theTile, i)) {
				neighborMultiBlocks[i] = null;
				neighborTypes[i] = NeighborType.OUTPUT;
				if (!cachesExist()) {
					createCaches();
				}
				cacheImportant(theTile, i);
				isNode = true;
				isOutput = true;
			} else if (connectionTypes[i].allowTransfer && isStructureTile(theTile, i)) {
				neighborMultiBlocks[i] = null;
				neighborTypes[i] = NeighborType.STRUCTURE;
				if (!cachesExist()) {
					createCaches();
				}
				cacheStructural(theTile, i);
				isNode = true;
			} else {
				neighborMultiBlocks[i] = null;
				neighborTypes[i] = NeighborType.NONE;
			}
		}
	}

	public void cacheInputTile(TileEntity theTile, int side) {

	}

	public TileEntity getAdjTileEntitySafe(int i) {

		return (BlockHelper.getAdjacentTileEntity(this, i));
	}

	public boolean checkForChunkUnload() {

		if (neighbourChunks.isEmpty()) {
			return false;
		}
		for (WeakReference<Chunk> neighbourChunk : neighbourChunks) {
			Object chunk = neighbourChunk.get();
			if (chunk != null && !((Chunk) chunk).isChunkLoaded) {
				neighbourChunks.clear();
				onNeighborBlockChange();
				return true;
			}
		}
		return false;
	}

	public void rebuildChunkCache() {

		if (!neighbourChunks.isEmpty()) {
			neighbourChunks.clear();
		}
		if (!isNode) {
			return;
		}
		Chunk base = worldObj.getChunkFromBlockCoords(getPos());

		for (byte i = 0; i < 6; i++) {
			if (neighborTypes[i] == NeighborType.INPUT || neighborTypes[i] == NeighborType.OUTPUT) {
				EnumFacing facing = EnumFacing.VALUES[i];
				Chunk chunk = worldObj.getChunkFromBlockCoords(getPos().offset(facing));
				if (chunk != base) {
					neighbourChunks.add(new WeakReference<>(chunk));
				}
			}
		}
	}

	public void cacheStructural(TileEntity theTile, int i) {

	}

	@Override
	public void onNeighborTileChange(BlockPos pos) {

		if (ServerHelper.isClientWorld(worldObj) && lastUpdateTime == worldObj.getTotalWorldTime()) {
			return;
		}
		if (isInvalid()) {
			return;
		}
		int i = BlockHelper.determineAdjacentSide(this, pos);

		boolean wasNode = isNode;
		boolean wasInput = isInput;
		boolean wasOutput = isOutput;

		handleSideUpdate(i);

		for (SubTileGridTile subTile : subTiles) {
			subTile.onNeighbourChange();
		}

		checkIsNode();
		if (wasNode != isNode && myGrid != null) {
			myGrid.addBlock(this);
		} else if (myGrid != null && (isOutput != wasOutput || isInput != wasInput)) {
			myGrid.onMajorGridChange();
		}

		for (Attachment tickingAttachment : tickingAttachments) {
			tickingAttachment.postNeighbourChange();
		}

		if (ServerHelper.isServerWorld(worldObj)) {
			rebuildChunkCache();
		}
	}

	public void checkIsNode() {

		isNode = false;
		for (byte i = 0; i < EnumFacing.VALUES.length; i++) {
			if (neighborTypes[i] == NeighborType.OUTPUT || neighborTypes[i] == NeighborType.STRUCTURE || (attachments[i] != null && attachments[i].isNode())) {
				isNode = true;
			}
			if (neighborTypes[i] == NeighborType.OUTPUT) {
				isOutput = true;
			}

			if (neighborTypes[i] == NeighborType.INPUT) {
				isInput = true;
			}
		}
	}

	public void tickInternalSideCounter(int start) {

		for (int a = start; a < neighborTypes.length; a++) {
			if (neighborTypes[a] == NeighborType.OUTPUT && connectionTypes[a] == cofh.thermaldynamics.duct.ConnectionType.NORMAL) {
				internalSideCounter = (byte) a;
				return;
			}
		}
		for (int a = 0; a < start; a++) {
			if (neighborTypes[a] == NeighborType.OUTPUT && connectionTypes[a] == cofh.thermaldynamics.duct.ConnectionType.NORMAL) {
				internalSideCounter = (byte) a;
				return;
			}
		}
	}

	/*
	 * Should return true if theTile is an instance of this multiblock. This must also be an instance of IGridTile
	 */
	public boolean isConnectable(TileEntity theTile, int side) {

		return theTile instanceof TileDuctBase;
	}

	public boolean isUnblocked(TileEntity tile, int side) {

		return !isBlockedSide(side) && !((TileDuctBase) tile).isBlockedSide(side ^ 1);
	}

	/*
	 * Should return true if theTile is significant to this multiblock IE: Inventory's to ItemDuct's
	 */
	public boolean isSignificantTile(TileEntity theTile, int side) {

		return false;
	}

	@Override
	public String getTileName() {

		return "tile.thermaldynamics.multiblock.name";
	}

	@Override
	public int getType() {

		return 0;
	}

	public void formGrid() {

		if (myGrid == null && ServerHelper.isServerWorld(worldObj)) {
			// DebugHelper.startTimer();
			new MultiBlockFormer().formGrid(this);
			// DEBUG CODE
			// DebugHelper.stopTimer("Grid");
			// DebugHelper.info("Grid Formed: " + (myGrid != null ? myGrid.nodeSet.size() + myGrid.idleSet.size() : "Failed"));
		}
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {

		super.readFromNBT(nbt);
		for (byte i = 0; i < 6; i++) {
			if (nbt.hasKey("attachment" + i, 10)) {
				NBTTagCompound tag = nbt.getCompoundTag("attachment" + i);
				int id = tag.getShort("id");
				attachments[i] = AttachmentRegistry.createAttachment(this, i, id);
				if (attachments[i] != null) {
					attachments[i].readFromNBT(tag);
					if (attachments[i].doesTick()) {
						tickingAttachments.add(attachments[i]);
					}
				}
			} else {
				attachments[i] = null;
			}

			if (nbt.hasKey("facade" + i, 10)) {
				NBTTagCompound tag = nbt.getCompoundTag("facade" + i);
				covers[i] = new Cover(this, i);
				covers[i].readFromNBT(tag);
			} else {
				covers[i] = null;
			}

			connectionTypes[i] = cofh.thermaldynamics.duct.ConnectionType.values()[nbt.getByte("conTypes" + i)];
		}

		recalcFacadeMask();

		for (int i = 0; i < this.subTiles.length; i++) {
			this.subTiles[i].readFromNBT(nbt.getCompoundTag("subTile" + i));
		}


		TickHandler.addMultiBlockToCalculate(this);
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt) {

		super.writeToNBT(nbt);
		for (int i = 0; i < 6; i++) {
			if (attachments[i] != null) {
				NBTTagCompound tag = new NBTTagCompound();
				tag.setShort("id", (short) attachments[i].getId());
				attachments[i].writeToNBT(tag);
				nbt.setTag("attachment" + i, tag);
			}

			if (covers[i] != null) {
				NBTTagCompound tag = new NBTTagCompound();
				covers[i].writeToNBT(tag);
				nbt.setTag("facade" + i, tag);
			}

			nbt.setByte("conTypes" + i, (byte) connectionTypes[i].ordinal());
		}

		for (int i = 0; i < this.subTiles.length; i++) {
			SubTileGridTile a = this.subTiles[i];
			NBTTagCompound tag = new NBTTagCompound();
			a.writeToNBT(tag);
			nbt.setTag("subTile" + i, tag);
		}
		return nbt;
	}


	Duct duct = null;

	public Duct getDuctType() {

		if (duct == null) {
			duct = TDDucts.getDuct(((BlockDuct) getBlockType()).offset + getBlockMetadata());
		}
		return duct;
	}




	public void handleInfoPacket(PacketCoFHBase payload, boolean isServer, EntityPlayer thePlayer) {

	}

	public abstract boolean cachesExist();

	public abstract void createCaches();

	public abstract void cacheImportant(TileEntity tile, int side);

	public abstract void clearCache(int side);

	/* ITilePacketHandler */
	@Override
	public void handleTilePacket(PacketCoFHBase payload, boolean isServer) {

		if (!isServer) {
			for (byte i = 0; i < neighborTypes.length; i++) {
				neighborTypes[i] = NeighborType.values()[payload.getByte()];
				connectionTypes[i] = cofh.thermaldynamics.duct.ConnectionType.values()[payload.getByte()];
			}

			isNode = payload.getBool();

			int attachmentMask = payload.getByte();
			for (byte i = 0; i < 6; i++) {
				if ((attachmentMask & (1 << i)) != 0) {
					attachments[i] = AttachmentRegistry.createAttachment(this, i, payload.getByte());
					attachments[i].getDescriptionFromPacket(payload);
				} else {
					attachments[i] = null;
				}
			}

			facadeMask = payload.getByte();
			for (byte i = 0; i < 6; i++) {
				if ((facadeMask & (1 << i)) != 0) {
					covers[i] = new Cover(this, i);
					covers[i].getDescriptionFromPacket(payload);
				} else {
					covers[i] = null;
				}
			}
			recalcFacadeMask();

			hashCode = payload.getInt();

			BlockUtils.fireBlockUpdate(getWorld(), getPos());

			lastUpdateTime = worldObj.getTotalWorldTime();
		}
	}

	public boolean isOutput = false;
	public boolean isInput = false;

	public BlockDuct.ConnectionType getRenderConnectionType(int side) {

		if (attachments[side] != null) {
			return attachments[side].getRenderConnectionType();
		} else {
			return TileGrid.getDefaultConnectionType(neighborTypes[side], connectionTypes[side]);
		}
	}

	public void randomDisplayTick() {

	}

	public boolean isSubNode() {

		return false;
	}

	public TextureAtlasSprite getBaseIcon() {

		return getDuctType().iconBaseTexture;
	}

	public ItemStack getDrop() {

		return new ItemStack(getBlockType(), 1, getBlockMetadata());
	}

	public void onPlacedBy(EntityLivingBase living, ItemStack stack) {

	}

	public void dropAdditional(ArrayList<ItemStack> ret) {

	}

	@Override
	@SideOnly (Side.CLIENT)
	public AxisAlignedBB getRenderBoundingBox() {

		return new AxisAlignedBB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1);
	}

	@Override
	public String getDataType() {

		return "tile.thermaldynamics.duct";
	}

	/* IGridTile */
	@Override
	public World world() {

		return getWorld();
	}

	@Override
	public int x() {

		return getPos().getX();
	}

	@Override
	public int y() {

		return getPos().getY();
	}

	@Override
	public int z() {

		return getPos().getZ();
	}


	@Override
	public boolean isBlockedSide(int side) {

		return connectionTypes[side] == cofh.thermaldynamics.duct.ConnectionType.BLOCKED || (attachments[side] != null && !attachments[side].allowPipeConnection());
	}

	@Override
	public boolean isSideConnected(byte side) {

		if (side >= neighborMultiBlocks.length) {
			return false;
		}
		IGridTile tileEntity = neighborMultiBlocks[side];
		return tileEntity != null && !isBlockedSide(side) && !tileEntity.isBlockedSide(side ^ 1);
	}

	@Override
	public void singleTick() {

		if (isInvalid()) {
			return;
		}

		onNeighborBlockChange();
		formGrid();

		for (SubTileGridTile subTile : subTiles) {
			subTile.onNeighbourChange();
			subTile.formGrid();
		}
	}

	@Override
	public boolean tickPass(int pass) {

		if (checkForChunkUnload()) {
			return false;
		}

		if (!tickingAttachments.isEmpty()) {
			for (Attachment attachment : tickingAttachments) {
				attachment.tick(pass);
			}
		}
		return true;
	}

	@Override
	public boolean isNode() {

		return isNode;
	}

	@Override
	public boolean existsYet() {

		return worldObj != null && worldObj.isBlockLoaded(getPos()) && worldObj.getBlockState(getPos()).getBlock() instanceof BlockDuct;
	}

	@Override
	public boolean isOutdated() {

		return isInvalid();
	}


	@Override
	public void addRelays() {

		for (Attachment attachment : attachments) {
			if (attachment != null) {
				if (attachment.getId() == AttachmentRegistry.RELAY) {
					Relay signaller = (Relay) attachment;
					if (signaller.isInput()) {
						myGrid.addSignalInput(signaller);
					} else {
						myGrid.addSignalOutput(attachment);
					}
				} else if (attachment.respondsToSignallum()) {
					myGrid.addSignalOutput(attachment);
				}
			}
		}
	}

	/* ICustomHitBox */
	@Override
	public boolean shouldRenderCustomHitBox(int subHit, EntityPlayer thePlayer) {

		return subHit == 13 || (subHit > 5 && subHit < 13 && !WrenchHelper.isHoldingUsableWrench(thePlayer, RayTracer.retrace(thePlayer)));
	}

	@Override
	public CustomHitBox getCustomHitBox(int subHit, EntityPlayer thePlayer) {

		double v1 = getDuctType().isLargeTube() ? 0.075 : .3;
		double v = (1 - v1 * 2);

		CustomHitBox hb = new CustomHitBox(v, v, v, pos.getX() + v1, pos.getY() + v1, pos.getZ() + v1);

		for (int i = 0; i < neighborTypes.length; i++) {
			if (neighborTypes[i] == NeighborType.MULTIBLOCK) {
				hb.drawSide(i, true);
				hb.setSideLength(i, v1);
			} else if (neighborTypes[i] != NeighborType.NONE) {
				hb.drawSide(i, true);
				hb.setSideLength(i, .04);
			}
		}
		return hb;
	}

	/* IPortableData */
	@Override
	public void readPortableData(EntityPlayer player, NBTTagCompound tag) {

		if (!tag.hasKey("AttachmentType", 8)) {
			return;
		}
		RayTraceResult rayTrace = RayTracer.retraceBlock(worldObj, player, getPos());
		if (rayTrace == null) {
			return;
		}
		int subHit = rayTrace.subHit;
		if (subHit <= 13 || subHit >= 20) {
			return;
		}
		if (!(attachments[subHit - 14] instanceof IPortableData)) {
			return;
		}
		IPortableData iPortableData = (IPortableData) attachments[subHit - 14];

		if (tag.getString("AttachmentType").equals(iPortableData.getDataType())) {
			iPortableData.readPortableData(player, tag);
		}
	}

	@Override
	public void writePortableData(EntityPlayer player, NBTTagCompound tag) {

		RayTraceResult rayTrace = RayTracer.retraceBlock(worldObj, player, getPos());
		if (rayTrace == null) {
			return;
		}

		int subHit = rayTrace.subHit;
		if (subHit <= 13 || subHit >= 20) {
			return;
		}
		if (!(attachments[subHit - 14] instanceof IPortableData)) {
			return;
		}
		IPortableData iPortableData = (IPortableData) attachments[subHit - 14];
		iPortableData.writePortableData(player, tag);
		if (!tag.hasNoTags()) {
			tag.setString("AttachmentType", iPortableData.getDataType());
		}
	}

	@Override
	public void getTileInfo(List<ITextComponent> info, EnumFacing side, EntityPlayer player, boolean debug) {

		MultiBlockGrid grid = getGrid();
		if (grid != null) {
			info.add(new TextComponentTranslation("info.thermaldynamics.info.duct"));
			grid.addInfo(info, player, debug);

			if (subTiles.length != 0) {
				for (SubTileGridTile subTile : subTiles) {
					if (subTile.grid != null) {
						subTile.grid.addInfo(info, player, debug);
					}
				}
			}
		}

		Attachment attachment = getAttachmentSelected(player);
		if (attachment != null) {

			info.add(new TextComponentTranslation("info.thermaldynamics.info.attachment"));
			int v = info.size();
			attachment.addInfo(info, player, debug);
			if (info.size() == v) {
				info.remove(v - 1);
			}
		}
	}

	public Attachment getAttachmentSelected(EntityPlayer player) {

		RayTraceResult rayTrace = RayTracer.retraceBlock(worldObj, player, getPos());
		if (rayTrace == null) {
			return null;
		}

		int subHit = rayTrace.subHit;
		if (subHit > 13 && subHit < 20) {
			return attachments[subHit - 14];
		}

		if (subHit >= 20 && subHit < 26) {
			return covers[subHit - 20];
		}

		return null;
	}

	public Object getConfigGuiServer(InventoryPlayer inventory) {

		return null;
	}

	public Object getConfigGuiClient(InventoryPlayer inventory) {

		return null;
	}
}

