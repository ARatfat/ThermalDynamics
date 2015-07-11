package cofh.thermaldynamics.duct.item;

import cofh.api.energy.IEnergyHandler;
import cofh.api.energy.IEnergyProvider;
import cofh.thermaldynamics.block.SubTileMultiBlock;
import cofh.thermaldynamics.duct.BlockDuct;
import cofh.thermaldynamics.duct.energy.subgrid.SubTileEnergy;

import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;

public abstract class TileItemDuctPowered extends TileItemDuct implements IEnergyHandler {

	SubTileEnergy energy;

	public TileItemDuctPowered() {

		super();
	}

	public void setSubEnergy(SubTileEnergy energy) {

		this.energy = energy;
		this.subTiles = new SubTileMultiBlock[] { energy };
	}

	@Override
	public BlockDuct.ConnectionTypes getConnectionType(int side) {

		if (attachments[side] != null) {
			return attachments[side].getRenderConnectionType();
		}

		if (neighborTypes[side] == NeighborTypes.STRUCTURE) {
			return connectionTypes[side] != ConnectionTypes.BLOCKED ? BlockDuct.ConnectionTypes.STRUCTURE : BlockDuct.ConnectionTypes.NONE;
		} else {
			return super.getConnectionType(side);
		}
	}

	@Override
	public boolean isStructureTile(TileEntity theTile, int side) {

		return theTile instanceof IEnergyProvider && ((IEnergyProvider) theTile).canConnectEnergy(ForgeDirection.getOrientation(side ^ 1));
	}

	@Override
	public int receiveEnergy(ForgeDirection from, int maxReceive, boolean simulate) {

		return energy.energyGrid != null && canConnectEnergy(from) ? energy.energyGrid.myStorage.receiveEnergy(maxReceive, simulate) : 0;
	}

	@Override
	public int extractEnergy(ForgeDirection from, int maxExtract, boolean simulate) {

		return 0;
	}

	@Override
	public int getEnergyStored(ForgeDirection from) {

		return energy.energyGrid != null ? energy.energyGrid.myStorage.getEnergyStored() : 0;
	}

	@Override
	public int getMaxEnergyStored(ForgeDirection from) {

		return energy.energyGrid != null ? energy.energyGrid.myStorage.getMaxEnergyStored() : 0;
	}

	@Override
	public boolean canConnectEnergy(ForgeDirection from) {

		return connectionTypes[from.ordinal()] != ConnectionTypes.BLOCKED;
		// && (neighborTypes[from.ordinal()] == NeighborTypes.OUTPUT || neighborTypes[from.ordinal()] == NeighborTypes.STRUCTURE);
	}

}
