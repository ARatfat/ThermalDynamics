package cofh.thermaldynamics.duct;

import codechicken.lib.render.CCRenderState;
import codechicken.lib.vec.Cuboid6;
import codechicken.lib.vec.Vector3;
import cofh.core.network.PacketCoFHBase;
import cofh.core.network.PacketTileInfo;
import cofh.core.util.CoreUtils;
import cofh.thermaldynamics.duct.attachments.cover.CoverHoleRender;
import cofh.thermaldynamics.duct.nutypeducts.TileGrid;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;
import java.util.Random;

public abstract class Attachment {

	public final TileGrid tile;
	public final byte side;

	public Attachment(TileGrid tile, byte side) {

		this.tile = tile;
		this.side = side;
	}

	public abstract String getName();

	public abstract int getId();

	public void writeToNBT(NBTTagCompound tag) {

	}

	public void readFromNBT(NBTTagCompound tag) {

	}

	public void addDescriptionToPacket(PacketCoFHBase packet) {

	}

	public void getDescriptionFromPacket(PacketCoFHBase packet) {

	}

	public abstract Cuboid6 getCuboid();

	public boolean onWrenched() {

		tile.removeAttachment(this);
		for (ItemStack stack : getDrops()) {
			dropItemStack(stack);
		}
		return true;
	}

	public abstract NeighborType getNeighborType();

	public abstract boolean isNode();

	public boolean doesTick() {

		return false;
	}

	public void tick(int pass) {

	}

	public void dropItemStack(ItemStack item) {

		Cuboid6 c = getCuboid();
		Random rand = tile.getWorld().rand;
		Vector3 dif = c.max.copy().subtract(c.max);
		Vector3 vec = Vector3.fromBlockPos(tile.getPos()).add(c.min).add(rand.nextFloat(), rand.nextFloat(), rand.nextFloat()).multiply(dif);
		CoreUtils.dropItemStackIntoWorldWithVelocity(item, tile.getWorld(), vec.vec3());
	}

	@SideOnly (Side.CLIENT)
	public abstract boolean render(IBlockAccess world, BlockRenderLayer layer, CCRenderState ccRenderState);

	public void addCollisionBoxesToList(AxisAlignedBB entityBox, List<AxisAlignedBB> list, Entity entity) {

		Cuboid6 cuboid6 = getCuboid().add(tile.getPos());
		if (cuboid6.intersects(new Cuboid6(entityBox))) {
			list.add(cuboid6.aabb());
		}
	}

	public boolean makesSideSolid() {

		return false;
	}

	public void onNeighborChange() {

	}

	public abstract ItemStack getPickBlock();

	public boolean canAddToTile(TileGrid tileMultiBlock) {

		return tileMultiBlock.getAttachment(side) == null;
	}

	public abstract List<ItemStack> getDrops();

	@SideOnly (Side.CLIENT)
	public Object getGuiClient(InventoryPlayer inventory) {

		return null;
	}

	public Object getGuiServer(InventoryPlayer inventory) {

		return null;
	}

	public boolean isUseable(EntityPlayer player) {

		return tile.isUsable(player);
	}

	public void receiveGuiNetworkData(int i, int j) {

	}

	public PacketTileInfo getNewPacket() {

		PacketTileInfo packet = PacketTileInfo.newPacket(tile);
		packet.addByte(1 + side);
		return packet;
	}

	@SuppressWarnings ("rawtypes")
	public void sendGuiNetworkData(Container container, List<IContainerListener> player, boolean newGuy) {

	}

	public int getInvSlotCount() {

		return 0;
	}

	public boolean openGui(EntityPlayer player) {

		return false;
	}

	public void handleInfoPacket(PacketCoFHBase payload, boolean isServer, EntityPlayer thePlayer) {

	}

	public BlockDuct.ConnectionType getRenderConnectionType() {

		return TileDuctBase.getDefaultConnectionType(getNeighborType(), cofh.thermaldynamics.duct.ConnectionType.NORMAL);
	}

	public boolean allowPipeConnection() {

		return false;
	}

	public boolean addToTile() {

		return canAddToTile(tile) && tile.addAttachment(this);
	}

	public void drawSelectionExtra(EntityPlayer player, RayTraceResult target, float partialTicks) {

	}

	public void postNeighbourChange() {

	}

	public int getRSOutput() {

		return 0;
	}

	public boolean shouldRSConnect() {

		return false;
	}

	public boolean respondsToSignallum() {

		return false;
	}

	public void checkSignal() {

	}

	public void addInfo(List<ITextComponent> info, EntityPlayer player, boolean debug) {

	}

	@SideOnly (Side.CLIENT)
	public CoverHoleRender.ITransformer[] getHollowMask() {

		return null;
	}

}
