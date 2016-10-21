package cofh.thermaldynamics.duct.attachments.cover;

import codechicken.lib.render.CCRenderState;
import cofh.core.network.PacketCoFHBase;
import cofh.lib.render.RenderHelper;
import codechicken.lib.vec.Cuboid6;
import codechicken.lib.vec.Rotation;
import codechicken.lib.vec.Vector3;
import cofh.thermaldynamics.block.Attachment;
import cofh.thermaldynamics.block.AttachmentRegistry;
import cofh.thermaldynamics.block.TileTDBase;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.LinkedList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import org.lwjgl.opengl.GL11;

public class Cover extends Attachment {

	private static Cuboid6 bound = new Cuboid6(0, 0, 0, 1, 0.0625, 1);

	public static Cuboid6[] bounds = { bound, bound.copy().apply(Rotation.sideRotations[1].at(Vector3.center)),
			bound.copy().apply(Rotation.sideRotations[2].at(Vector3.center)), bound.copy().apply(Rotation.sideRotations[3].at(Vector3.center)),
			bound.copy().apply(Rotation.sideRotations[4].at(Vector3.center)), bound.copy().apply(Rotation.sideRotations[5].at(Vector3.center)) };

	public Block block;
	public int meta;

	public Cover(TileTDBase tile, byte side, Block block, int meta) {

		super(tile, side);
		this.block = block;
		this.meta = meta;
	}

	public Cover(TileTDBase tile, byte side) {

		super(tile, side);
	}

	@Override
	public String getName() {

		return "item.thermalfoundation.cover.name";
	}

	@Override
	public int getId() {

		return AttachmentRegistry.FACADE;
	}

	@Override
	public Cuboid6 getCuboid() {

		return bounds[side].copy();
	}

	@Override
	public boolean onWrenched() {

		tile.removeFacade(this);

		for (ItemStack stack : getDrops()) {
			dropItemStack(stack);
		}
		return true;
	}

	@Override
	public TileTDBase.NeighborTypes getNeighborType() {

		return TileTDBase.NeighborTypes.NONE;
	}

	@Override
	public boolean isNode() {

		return false;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public boolean render(int pass, CCRenderState renderBlocks) {

		//if (!block.canRenderInPass(pass)) {//TODO
		//	return false;
		//}

		//Attachment attachment = tile.attachments[side];
		//CoverHoleRender.ITransformer[] hollowMask = null;
		//if (attachment != null) {
		//	hollowMask = attachment.getHollowMask();
		//}
		//if (hollowMask == null) {
		//	hollowMask = tile.getHollowMask(side);
		//}

		//return CoverRenderer.renderCover(renderBlocks, tile.xCoord, tile.yCoord, tile.zCoord, side, block, meta, getCuboid(), false, false, hollowMask,
		//		tile.covers);
        return false;
	}

	@Override
	public boolean makesSideSolid() {

		return true;
	}

	@Override
	public ItemStack getPickBlock() {

		return CoverHelper.getCoverStack(block, meta);
	}

	@Override
	public List<ItemStack> getDrops() {

		LinkedList<ItemStack> itemStacks = new LinkedList<ItemStack>();
		itemStacks.add(getPickBlock());
		return itemStacks;
	}

	@Override
	public boolean addToTile() {

		return tile.addFacade(this);
	}

	@Override
	public void addDescriptionToPacket(PacketCoFHBase packet) {

		packet.addShort(Block.getIdFromBlock(block));
		packet.addByte(meta);
	}

	@Override
	public void getDescriptionFromPacket(PacketCoFHBase packet) {

		block = Block.getBlockById(packet.getShort());
		meta = packet.getByte();
	}

	@Override
	public void writeToNBT(NBTTagCompound tag) {

		super.writeToNBT(tag);
		tag.setString("block", ForgeRegistries.BLOCKS.getKey(block).toString());
		tag.setByte("meta", (byte) meta);
	}

	@Override
	public boolean canAddToTile(TileTDBase tileMultiBlock) {

		return tileMultiBlock.covers[side] == null;
	}

	@Override
	public void readFromNBT(NBTTagCompound tag) {

		super.readFromNBT(tag);
		block = Block.getBlockFromName(tag.getString("block"));
		if (block == null) {
			block = Blocks.AIR;
		}
		meta = tag.getByte("meta");
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void drawSelectionExtra(EntityPlayer player, RayTraceResult target, float partialTicks) {

		super.drawSelectionExtra(player, target, partialTicks);

		/*RenderHelper.setBlockTextureSheet();
		net.minecraft.client.renderer.RenderHelper.enableStandardItemLighting();
        GlStateManager.enableAlpha();
		GlStateManager.enableColorMaterial();
		GlStateManager.enableBlend();
		GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GlStateManager.enableTexture2D();

		GlStateManager.depthMask(false);
		double d0 = player.lastTickPosX + (player.posX - player.lastTickPosX) * partialTicks;
		double d1 = player.lastTickPosY + (player.posY - player.lastTickPosY) * partialTicks;
		double d2 = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * partialTicks;
		GlStateManager.color(1, 1, 1, 0.5F);
		GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1F);
		GlStateManager.enableBlend();
		GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
		GlStateManager.pushMatrix();
		{

			GlStateManager.translate(-d0, -d1, -d2);
			GlStateManager.translate(tile.xCoord + 0.5, tile.yCoord + 0.5, tile.zCoord + 0.5);
			GlStateManager.scale(1 + RenderHelper.RENDER_OFFSET, 1 + RenderHelper.RENDER_OFFSET, 1 + RenderHelper.RENDER_OFFSET);
			GlStateManager.translate(-tile.xCoord - 0.5, -tile.yCoord - 0.5, -tile.zCoord - 0.5);

			Tessellator tess = Tessellator.instance;
			tess.startDrawingQuads();
			Tessellator.instance.setNormal(0, 1, 0);
			tess.setColorRGBA_F(1, 1, 1, 0.5F);
			tess.setBrightness(tile.getBlockType().getMixedBrightnessForBlock(tile.world(), tile.xCoord, tile.yCoord, tile.zCoord));
			RenderBlocks renderBlocks = CoverRenderer.renderBlocks;
			renderBlocks.blockAccess = player.getEntityWorld();
			for (int i = 0; i < 2; i++) {
				if (block.canRenderInPass(i)) {
					CoverRenderer.renderCover(renderBlocks, tile.xCoord, tile.yCoord, tile.zCoord, side, block, meta, getCuboid(), false, true, null);
				}
			}
			tess.draw();
		}
		GlStateManager.popMatrix();
		net.minecraft.client.renderer.RenderHelper.disableStandardItemLighting();
		GlStateManager.depthMask(true);
		GlStateManager.disableAlpha();
		GlStateManager.disableColorMaterial();
		GlStateManager.disableLighting();
		GlStateManager.disableBlend();*/
	}

}
