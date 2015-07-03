package cofh.thermaldynamics.duct.fluid;

import cofh.core.CoFHProps;
import cofh.core.network.PacketCoFHBase;
import cofh.core.network.PacketHandler;
import cofh.core.network.PacketTileInfo;
import cofh.lib.util.helpers.FluidHelper;
import cofh.lib.util.helpers.ServerHelper;
import cofh.thermaldynamics.block.TileTDBase;
import cofh.thermaldynamics.duct.attachments.filter.IFilterAttachment;
import cofh.thermaldynamics.duct.attachments.filter.IFilterFluid;
import cofh.thermaldynamics.multiblock.IMultiBlock;
import cofh.thermaldynamics.multiblock.MultiBlockGrid;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.IFluidHandler;

public class TileFluidDuct extends TileTDBase implements IFluidHandler {

	public IFluidHandler[] cache;
	public IFilterFluid[] filterCache;
	private int input;

	public TileFluidDuct() {

	}

	@Override
	public MultiBlockGrid getNewGrid() {

		return new FluidGrid(worldObj);
	}

	IFluidHandler[] importantCache = new IFluidHandler[6];

	public FluidStack mySavedFluid;
	public FluidStack myRenderFluid;
	public FluidStack fluidForGrid;
	public FluidStack myConnectionFluid;

	@Override
	public boolean isSignificantTile(TileEntity theTile, int side) {

		return FluidHelper.isFluidHandler(theTile) && !(theTile instanceof IMultiBlock);
	}

	@Override
	public boolean tickPass(int pass) {

		if (!super.tickPass(pass)) {
			return false;
		}
		if (fluidGrid == null || !cachesExist()) {
			return true;
		}
		if (pass == 0) {
			int available = fluidGrid.toDistribute;
			int sent = 0;

			for (int i = this.internalSideCounter; i < this.neighborTypes.length && sent < available; i++) {
				sent += transfer(i, available - sent, false, fluidGrid.myTank.getFluid());

				if (sent >= available) {
					this.tickInternalSideCounter(i + 1);
					break;
				}

			}
			for (int i = 0; i < this.internalSideCounter && sent < available; i++) {
				sent += transfer(i, available - sent, false, fluidGrid.myTank.getFluid());

				if (sent >= available) {
					this.tickInternalSideCounter(i + 1);
					break;
				}
			}
		}
		return true;
	}

	public int transfer(int available, boolean simulate, FluidStack base) {

		if (!cachesExist()) {
			return 0;
		}
		int sent = 0;

		for (int i = this.internalSideCounter; i < this.neighborTypes.length && sent < available; i++) {
			sent += transfer(i, available - sent, simulate, base);

			if (sent >= available) {
				this.tickInternalSideCounter(i + 1);
				break;
			}

		}
		for (int i = 0; i < this.internalSideCounter && sent < available; i++) {
			sent += transfer(i, available - sent, simulate, base);

			if (sent >= available) {
				this.tickInternalSideCounter(i + 1);
				break;
			}
		}
		return sent;
	}

	public int transfer(int bSide, int available, boolean simulate, FluidStack fluid) {

		if (neighborTypes[bSide] != NeighborTypes.OUTPUT || connectionTypes[bSide] == ConnectionTypes.BLOCKED) {
			return 0;
		}
		if (cache[bSide] == null || fluid == null) {
			return 0;
		}
		if (!filterCache[bSide].allowFluid(fluid)) {
			return 0;
		}

		FluidStack tempFluid = fluid.copy();
		tempFluid.amount = available;
		int amountSent = cache[bSide].fill(ForgeDirection.VALID_DIRECTIONS[bSide ^ 1], tempFluid, false);

		if (amountSent > 0) {
			if (simulate) {
				return amountSent;
			} else {
				return cache[bSide].fill(ForgeDirection.VALID_DIRECTIONS[bSide ^ 1], fluidGrid.myTank.drain(amountSent, true), true);
			}
		} else {
			return 0;
		}
	}

	@Override
	public int getLightValue() {

		if (getDuctType().opaque) {
			return 0;
		}
		if (ServerHelper.isClientWorld(world())) {
			return FluidHelper.getFluidLuminosity(myRenderFluid);
		}
		if (fluidGrid != null) {
			return FluidHelper.getFluidLuminosity(fluidGrid.getFluid());
		}
		return super.getLightValue();
	}

	public void updateFluid() {

		if (!getDuctType().opaque) {
			sendRenderPacket();
		}
	}

	@Override
	public boolean shouldRenderInPass(int pass) {

		return !getDuctType().opaque && myRenderFluid != null && super.shouldRenderInPass(pass);
	}

	@Override
	public boolean isConnectable(TileEntity theTile, int side) {

		return theTile instanceof TileFluidDuct;
	}

	public FluidStack getConnectionFluid() {

		if (ServerHelper.isClientWorld(worldObj)) {
			return myRenderFluid;
		}
		return fluidGrid == null ? myConnectionFluid : fluidGrid.getFluid();
	}

	public FluidGrid fluidGrid;

	@Override
	public void setGrid(MultiBlockGrid newGrid) {

		super.setGrid(newGrid);
		fluidGrid = (FluidGrid) newGrid;
	}

	@Override
	public void handleInfoPacket(PacketCoFHBase payload, boolean isServer, EntityPlayer thePlayer) {

		if (ServerHelper.isClientWorld(worldObj)) {
			byte b = payload.getByte();
			handleTileInfoPacketType(payload, b);
		}
	}

	@Override
	public boolean cachesExist() {

		return cache != null;
	}

	@Override
	public void createCaches() {

		cache = new IFluidHandler[6];
		filterCache = new IFilterFluid[] { IFilterFluid.nullFilter, IFilterFluid.nullFilter, IFilterFluid.nullFilter, IFilterFluid.nullFilter,
				IFilterFluid.nullFilter, IFilterFluid.nullFilter };
	}

	@Override
	public void cacheImportant(TileEntity tile, int side) {

		if (attachments[side] instanceof IFilterAttachment) {
			filterCache[side] = ((IFilterAttachment) attachments[side]).getFluidFilter();
		}
		cache[side] = (IFluidHandler) tile;
	}

	@Override
	public void clearCache(int side) {

		filterCache[side] = IFilterFluid.nullFilter;
		cache[side] = null;
	}

	public void handleTileInfoPacketType(PacketCoFHBase payload, byte b) {

		if (b == TileFluidPackets.UPDATE_RENDER) {
			myRenderFluid = payload.getFluidStack();
			worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
			worldObj.func_147451_t(xCoord, yCoord, zCoord);
		}
	}

	@Override
	public void tileUnloading() {

		if (mySavedFluid != null && fluidGrid != null) {
			fluidGrid.myTank.drain(mySavedFluid.amount, true);
		}
	}

	public int getRenderFluidLevel() {

		return myRenderFluid == null ? 0 : myRenderFluid.amount;
	}

	@Override
	public PacketCoFHBase getPacket() {

		PacketCoFHBase packet = super.getPacket();
		if (fluidGrid != null) {
			packet.addFluidStack(fluidGrid.getRenderFluid());
		} else {
			packet.addFluidStack(myConnectionFluid);
		}
		return packet;
	}

	@Override
	public void handleTilePacket(PacketCoFHBase payload, boolean isServer) {

		super.handleTilePacket(payload, isServer);
		myRenderFluid = payload.getFluidStack();

		if (worldObj != null) {
			worldObj.func_147451_t(xCoord, yCoord, zCoord);
		}
	}

	public void sendRenderPacket() {

		if (fluidGrid == null) {
			return;
		}
		if (!getDuctType().opaque) {
			worldObj.func_147451_t(xCoord, yCoord, zCoord);

			PacketTileInfo myPayload = PacketTileInfo.newPacket(this);
			myPayload.addByte(0);
			myPayload.addByte(TileFluidPackets.UPDATE_RENDER);
			myPayload.addFluidStack(fluidGrid.getRenderFluid());
			PacketHandler.sendToAllAround(myPayload, this);
		}
	}

	public class TileFluidPackets {

		public static final byte GUI_BUTTON = 0;
		public static final byte SET_FILTER = 1;
		public static final byte FILTERS = 2;
		public static final byte UPDATE_RENDER = 3;
		public static final byte TEMPERATURE = 4;
	}

	@Override
	public int fill(ForgeDirection from, FluidStack resource, boolean doFill) {

		if (isOpen(from) && matchesFilter(from, resource)) {
			return fluidGrid.myTank.fill(resource, doFill);
		}
		return 0;
	}

	public boolean matchesFilter(ForgeDirection from, FluidStack resource) {

		return filterCache == null || from == ForgeDirection.UNKNOWN || filterCache[from.ordinal()].allowFluid(resource);
	}

	@Override
	public FluidStack drain(ForgeDirection from, FluidStack resource, boolean doDrain) {

		if (isOpen(from)) {
			return fluidGrid.myTank.drain(resource, doDrain);
		}
		return null;
	}

	@Override
	public FluidStack drain(ForgeDirection from, int maxDrain, boolean doDrain) {

		if (isOpen(from)) {
			return fluidGrid.myTank.drain(maxDrain, doDrain);
		}
		return null;
	}

	@Override
	public boolean canFill(ForgeDirection from, Fluid fluid) {

		return isOpen(from);
	}

	@Override
	public boolean canDrain(ForgeDirection from, Fluid fluid) {

		return isOpen(from);
	}

	public boolean isOpen(ForgeDirection from) {

		return fluidGrid != null
				&& (from == ForgeDirection.UNKNOWN || ((neighborTypes[from.ordinal()] == NeighborTypes.OUTPUT || neighborTypes[from.ordinal()] == NeighborTypes.INPUT) && connectionTypes[from
						.ordinal()] != ConnectionTypes.BLOCKED));
	}

	@Override
	public FluidTankInfo[] getTankInfo(ForgeDirection from) {

		return fluidGrid != null ? new FluidTankInfo[] { fluidGrid.myTank.getInfo() } : CoFHProps.EMPTY_TANK_INFO;
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt) {

		super.writeToNBT(nbt);

		if (fluidGrid != null && fluidGrid.hasValidFluid()) {
			mySavedFluid = fluidGrid.getNodeShare(this);
			mySavedFluid.writeToNBT(nbt);

			nbt.setTag("ConnFluid", new NBTTagCompound());
			myConnectionFluid = fluidGrid.getFluid().copy();
			myConnectionFluid.writeToNBT(nbt.getCompoundTag("ConnFluid"));
		} else {
			mySavedFluid = null;
			myConnectionFluid = null;
		}
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {

		super.readFromNBT(nbt);

		fluidForGrid = FluidStack.loadFluidStackFromNBT(nbt);
		if (nbt.hasKey("ConnFluid")) {
			myConnectionFluid = FluidStack.loadFluidStackFromNBT(nbt.getCompoundTag("ConnFluid"));
		}
	}

}
