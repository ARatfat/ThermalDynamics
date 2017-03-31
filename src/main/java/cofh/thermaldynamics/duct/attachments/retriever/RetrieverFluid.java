package cofh.thermaldynamics.duct.attachments.retriever;

import codechicken.lib.render.CCRenderState;
import codechicken.lib.vec.Translation;
import codechicken.lib.vec.Vector3;
import codechicken.lib.vec.uv.IconTransformation;
import cofh.thermaldynamics.duct.Attachment;
import cofh.thermaldynamics.duct.AttachmentRegistry;
import cofh.thermaldynamics.duct.BlockDuct;
import cofh.thermaldynamics.duct.attachments.servo.ServoFluid;
import cofh.thermaldynamics.duct.fluid.DuctUnitFluid;
import cofh.thermaldynamics.duct.nutypeducts.TileGrid;
import cofh.thermaldynamics.init.TDItems;
import cofh.thermaldynamics.init.TDTextures;
import cofh.thermaldynamics.render.RenderDuct;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;

import java.util.Iterator;

public class RetrieverFluid extends ServoFluid {

	public RetrieverFluid(TileGrid tile, byte side) {

		super(tile, side);
	}

	public RetrieverFluid(TileGrid tile, byte side, int type) {

		super(tile, side, type);
	}

	@Override
	public int getId() {

		return AttachmentRegistry.RETRIEVER_FLUID;
	}

	@Override
	public void tick(int pass) {

		if (pass != 1 || fluidDuct.fluidGrid == null || !isPowered || !isValidInput) {
			return;
		}
		int maxInput = (int) Math.ceil(fluidDuct.fluidGrid.myTank.fluidThroughput * throttle[type]);

		if (fluidDuct.fluidGrid.myTank.getFluid() != null) {
			if (!fluidPassesFiltering(fluidDuct.fluidGrid.myTank.getFluid())) {
				return;
			}
		}
		for (Iterator<?> iterator = fluidDuct.fluidGrid.nodeSet.iterator(); iterator.hasNext() && maxInput > 0; ) {
			DuctUnitFluid fluidDuct = (DuctUnitFluid) iterator.next();

			for (int k = 0; k < 6 && maxInput > 0; k++) {
				int i = (k + fluidDuct.internalSideCounter) % 6;

				DuctUnitFluid.Cache cache = fluidDuct.tileCaches[i];

				if (cache == null || (!fluidDuct.isOutput(side) && !fluidDuct.isInput(side))) {
					continue;
				}

				Attachment attachment = fluidDuct.parent.getAttachment(side);
				if (attachment != null && attachment.getId() == this.getId()) {
					continue;
				}

				IFluidHandler ductHandler = fluidDuct.getFluidCapability(EnumFacing.VALUES[i]);

				if(ductHandler == null) continue;

				IFluidHandler handler = cache.getHandler(side ^ 1);

				if(handler == null) continue;

				int input = ductHandler.fill(handler.drain(maxInput, false), false);

				if (input == 0) {
					continue;
				}
				FluidStack fluid = handler.drain(input, false);

				if (fluid != null && fluid.amount > 0 && fluidPassesFiltering(fluid) && handler.getTankProperties()[0].canDrainFluidType(fluid)) {

					fluid = handler.drain(input, true);

					maxInput -= ductHandler.fill(fluid, true);

					if (this.fluidDuct.fluidGrid.toDistribute > 0 && this.fluidDuct.fluidGrid.myTank.getFluid() != null) {
						this.fluidDuct.transfer(side, Math.min(this.fluidDuct.fluidGrid.myTank.getFluid().amount, this.fluidDuct.fluidGrid.toDistribute), false, this.fluidDuct.fluidGrid.myTank.getFluid(), true);
					}
				}
			}
		}
	}

	@Override
	public ItemStack getPickBlock() {

		return new ItemStack(TDItems.itemRetriever, 1, type);
	}

	@Override
	public String getName() {

		return "item.thermaldynamics.retriever." + type + ".name";
	}

	@Override
	public boolean render(IBlockAccess world, BlockRenderLayer layer, CCRenderState ccRenderState) {

		if (layer != BlockRenderLayer.SOLID) {
			return false;
		}

		Translation trans = Vector3.fromTileCenter(tile).translation();
		RenderDuct.modelConnection[isPowered ? 1 : 2][side].render(ccRenderState, trans, new IconTransformation(TDTextures.RETRIEVER_BASE[stuffed ? 1 : 0][type]));
		return true;
	}

	/* IPortableData */
	@Override
	public void writePortableData(EntityPlayer player, NBTTagCompound tag) {

		super.writePortableData(player, tag);
		tag.setString("DisplayType", "item.thermaldynamics.retriever.0.name");
	}

}
