package cofh.thermaldynamics.ducts;

import cofh.thermaldynamics.block.TileMultiBlock;
import cofh.thermaldynamics.multiblock.MultiBlockGrid;

import net.minecraft.tileentity.TileEntity;

public class TileStructuralDuct extends TileMultiBlock {

	@Override
	public void tickMultiBlock() {

		onNeighborBlockChange();
	}

	@Override
	public boolean isConnectable(TileEntity theTile, int side) {

		return theTile != null && theTile.getClass() == this.getClass() && theTile.getBlockType() == this.getBlockType()
				&& theTile.getBlockMetadata() == this.getBlockMetadata();
	}

	@Override
	public void formGrid() {

		// No Grids needed
	}

	@Override
	public void cacheImportant(TileEntity tile, int side) {

	}

	@Override
	public void clearCache(int side) {

	}

	@Override
	public MultiBlockGrid getNewGrid() {

		return null;
	}

	@Override
	public boolean isStructureTile(TileEntity tile, int side) {

		return false;
	}

}
