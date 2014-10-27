package thermaldynamics.ducts.facades;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.common.util.ForgeDirection;
import thermaldynamics.block.Attachment;
import thermaldynamics.block.TileMultiBlock;

import static thermaldynamics.ducts.facades.FacadeBlockAccess.Result.*;

public class FacadeBlockAccess implements IBlockAccess {

    IBlockAccess world;

    public static FacadeBlockAccess instance = new FacadeBlockAccess();

    public static boolean enclosingBedrock = false;

    public static FacadeBlockAccess setEnclosingBedrock(boolean enclosingBedrock) {
        FacadeBlockAccess.enclosingBedrock = enclosingBedrock;
        return instance;
    }

    public static FacadeBlockAccess getInstance(IBlockAccess world, int blockX, int blockY, int blockZ, int side, Block block, int meta) {
        instance.world = world;
        instance.blockX = blockX;
        instance.blockY = blockY;
        instance.blockZ = blockZ;
        instance.side = side;
        instance.block = block;
        instance.meta = meta;
        return instance;
    }

    public int blockX, blockY, blockZ;
    public int side;

    public Block block;
    public int meta;

    public enum Result {
        ORIGINAL, AIR, BEDROCK, BASE
    }

    public Result getAction(int x, int y, int z) {
        if (x == blockX && y == blockY && z == blockZ)
            return BASE;

        if (enclosingBedrock && ((side == 0 && y >= blockY) ||
                (side == 1 && y <= blockY) ||
                (side == 2 && z >= blockZ) ||
                (side == 3 && z <= blockZ) ||
                (side == 4 && x >= blockX) ||
                (side == 5 && x <= blockX)))
            return BEDROCK;

        if (world.getBlock(x, y, z) == block && world.getBlockMetadata(x, y, z) == meta)
            return BEDROCK;


        TileEntity tile = world.getTileEntity(x, y, z);
        if (tile instanceof TileMultiBlock) {
            Attachment attachment = ((TileMultiBlock) tile).attachments[side];
            if (attachment != null && attachment.getID() == 0) {
                Facade f = ((Facade) attachment);
                if (f.block == block && f.meta == meta)
                    return BASE;
                else
                    return BEDROCK;
            }
            return AIR;
        }


        return ORIGINAL;
    }

    @Override
    public Block getBlock(int x, int y, int z) {
        Result action = getAction(x, y, z);
        return action == ORIGINAL ? world.getBlock(x, y, z) : action == AIR ? Blocks.air : action == BEDROCK ? Blocks.bedrock : block;
    }

    @Override
    public TileEntity getTileEntity(int x, int y, int z) {
        return getAction(x, y, z) == ORIGINAL ? world.getTileEntity(x, y, z) : null;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getLightBrightnessForSkyBlocks(int x, int y, int z, int t) {
        return world.getLightBrightnessForSkyBlocks(x, y, z, t);
    }

    @Override
    public int getBlockMetadata(int x, int y, int z) {
        Result action = getAction(x, y, z);
        return action == ORIGINAL ? world.getBlockMetadata(x, y, z) : action == AIR || action == BEDROCK ? 0 : meta;
    }

    @Override
    public int isBlockProvidingPowerTo(int x, int y, int z, int s) {
        return world.isBlockProvidingPowerTo(x, y, z, s);
    }

    @Override
    public boolean isAirBlock(int x, int y, int z) {
        Result action = getAction(x, y, z);
        return action == AIR || (action == ORIGINAL && world.isAirBlock(x, y, z));
    }

    @Override
    @SideOnly(Side.CLIENT)
    public BiomeGenBase getBiomeGenForCoords(int x, int z) {
        return world.getBiomeGenForCoords(x, z);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getHeight() {
        return world.getHeight();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean extendedLevelsInChunkCache() {
        return world.extendedLevelsInChunkCache();
    }

    @Override
    public boolean isSideSolid(int x, int y, int z, ForgeDirection side, boolean _default) {
        Result action = getAction(x, y, z);
        return action == BEDROCK || ((action == BASE || action == ORIGINAL) && world.isSideSolid(x, y, z, side, _default));
    }
}
