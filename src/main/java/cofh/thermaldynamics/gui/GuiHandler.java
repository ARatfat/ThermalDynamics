package cofh.thermaldynamics.gui;

import cofh.core.block.TileCoFHBaseOld;
import cofh.thermaldynamics.block.Attachment;
import cofh.thermaldynamics.block.TileTDBase;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.IGuiHandler;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class GuiHandler implements IGuiHandler {

	public static final int TILE_ID = 0;
	public static final int TILE_ATTACHMENT_ID = 1;
	public static final int TILE_ATTACHMENT_ID_END = 6;
	public static final int TILE_CONFIG = 7;

	@Override
	public Object getServerGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        BlockPos pos = new BlockPos(x,y,z);
		TileEntity tile = world.getTileEntity(pos);
		if (id >= TILE_ATTACHMENT_ID && id <= TILE_ATTACHMENT_ID_END) {
			if (tile instanceof TileTDBase) {
				Attachment attachment = ((TileTDBase) tile).attachments[id - TILE_ATTACHMENT_ID];
				if (attachment != null) {
					return attachment.getGuiServer(player.inventory);
				}
			}
		}
		switch (id) {
		case TILE_ID:
			tile = world.getTileEntity(pos);
			if (tile instanceof TileCoFHBaseOld) {
				return ((TileCoFHBaseOld) tile).getGuiServer(player.inventory);
			}

		case TILE_CONFIG:
			tile = world.getTileEntity(pos);
			if (tile instanceof TileTDBase) {
				return ((TileTDBase) tile).getConfigGuiServer(player.inventory);
			} else {
				return null;
			}
		default:
			return null;
		}
	}

	@Override
	public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        BlockPos pos = new BlockPos(x,y,z);
		TileEntity tile = world.getTileEntity(pos);
		if (id >= TILE_ATTACHMENT_ID && id <= TILE_ATTACHMENT_ID_END) {
			if (tile instanceof TileTDBase) {
				Attachment attachment = ((TileTDBase) tile).attachments[id - TILE_ATTACHMENT_ID];
				if (attachment != null) {
					return attachment.getGuiClient(player.inventory);
				}
			}
		}
		switch (id) {
		case TILE_ID:
			tile = world.getTileEntity(pos);
			if (tile instanceof TileCoFHBaseOld) {
				return ((TileCoFHBaseOld) tile).getGuiClient(player.inventory);
			} else {
				return null;
			}

		case TILE_CONFIG:
			tile = world.getTileEntity(pos);
			if (tile instanceof TileTDBase) {
				return ((TileTDBase) tile).getConfigGuiClient(player.inventory);
			} else {
				return null;
			}

		default:
			return null;
		}
	}

}
