package thermaldynamics.block;

import cofh.api.core.IInitializer;
import cofh.core.render.IconRegistry;
import cofh.core.render.hitbox.ICustomHitBox;
import cofh.core.render.hitbox.RenderHitbox;
import cofh.repack.codechicken.lib.raytracer.RayTracer;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.common.MinecraftForge;
import thermaldynamics.ThermalDynamics;
import thermaldynamics.core.TDProps;
import thermaldynamics.ducts.energy.TileEnergyDuct;
import thermaldynamics.ducts.item.TileItemDuct;
import thermaldynamics.multiblock.IMultiBlock;
import thermaldynamics.multiblock.MultiBlockFormer;
import thermaldynamics.render.TextureOverlay;
import thermaldynamics.render.TextureTransparent;

import java.util.ArrayList;
import java.util.List;

public class BlockDuct extends BlockMultiBlock implements IInitializer {

    public BlockDuct() {

        super(Material.iron);
        setHardness(25.0F);
        setResistance(120.0F);
        setStepSound(soundTypeMetal);
        setBlockName("thermalducts.duct");
        setCreativeTab(ThermalDynamics.tab);
    }

    @Override
    public void getSubBlocks(Item item, CreativeTabs tab, List list) {

        for (int i = 0; i < NAMES.length; i++) {
            list.add(new ItemStack(item, 1, i));
        }
    }

    @Override
    public TileEntity createNewTileEntity(World var1, int var2) {

        switch (var2) {
            case 1:
                return new TileEnergyDuct();
        }
        return new TileItemDuct();
    }

    @Override
    public int damageDropped(int i) {

        return i;
    }

    @Override
    public boolean canCreatureSpawn(EnumCreatureType type, IBlockAccess world, int x, int y, int z) {

        return false;
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void onBlockHighlight(DrawBlockHighlightEvent event) {

        if (event.target.typeOfHit == MovingObjectType.BLOCK
                && event.player.worldObj.getBlock(event.target.blockX, event.target.blockY, event.target.blockZ).getUnlocalizedName()
                .equals(getUnlocalizedName())) {
            RayTracer.retraceBlock(event.player.worldObj, event.player, event.target.blockX, event.target.blockY, event.target.blockZ);

            ICustomHitBox theTile = ((ICustomHitBox) event.player.worldObj.getTileEntity(event.target.blockX, event.target.blockY, event.target.blockZ));
            if (theTile.shouldRenderCustomHitBox(event.target.subHit, event.player)) {
                event.setCanceled(true);
                RenderHitbox.drawSelectionBox(event.player, event.target, event.partialTicks, theTile.getCustomHitBox(event.target.subHit, event.player));
            }
        }
    }

    @Override
    public int getLightValue(IBlockAccess world, int x, int y, int z) {

        return 0;
    }


    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister ir) {

//        IconRegistry.addIcon("DuctEnergy00", "thermaldynamics:duct/energy/DuctEnergy00", ir);
        IconRegistry.addIcon("CenterLine", "thermaldynamics:duct/item/CenterLine", ir);

        IconRegistry.addIcon("Fluid_Glowstone_Still", TextureTransparent.registerTransparentIcon(ir, "thermalfoundation:fluid/Fluid_Glowstone_Still", (byte) 128));
        IconRegistry.addIcon("Fluid_Redstone_Still", TextureTransparent.registerTransparentIcon(ir, "thermalfoundation:fluid/Fluid_Ender_Still", (byte) 192));

        IconRegistry.addIcon("DuctStructure", "thermaldynamics:duct/structure", ir);

        IconRegistry.addIcon("DuctEnergy00", TextureOverlay.generateTexture(ir, false, 0, 1, 0));

        IconRegistry.addIcon("DuctEnergy10", "thermaldynamics:duct/energy/DuctEnergy10", ir);
        IconRegistry.addIcon("DuctEnergy20", "thermaldynamics:duct/energy/DuctEnergy20", ir);

        IconRegistry.addIcon("DuctFluid00", "thermaldynamics:duct/fluid/DuctFluid00", ir);
        IconRegistry.addIcon("DuctFluid10", "thermaldynamics:duct/fluid/DuctFluid10", ir);

        IconRegistry.addIcon("DuctItem00", "thermaldynamics:duct/item/DuctItem00", ir);
        IconRegistry.addIcon("DuctItem10", "thermaldynamics:duct/item/DuctItem10", ir);
        IconRegistry.addIcon("DuctItem20", "thermaldynamics:duct/item/DuctItem20", ir);
        IconRegistry.addIcon("DuctItem30", "thermaldynamics:duct/item/DuctItem30", ir);

        IconRegistry.addIcon("DuctItem01", "thermaldynamics:duct/item/DuctItem01", ir);
        IconRegistry.addIcon("DuctItem02", "thermaldynamics:duct/item/DuctItem02", ir);
        IconRegistry.addIcon("DuctItem03", "thermaldynamics:duct/item/DuctItem03", ir);

        IconRegistry.addIcon("DuctItem11", "thermaldynamics:duct/item/DuctItem11", ir);
        IconRegistry.addIcon("DuctItem12", "thermaldynamics:duct/item/DuctItem12", ir);
        IconRegistry.addIcon("DuctItem13", "thermaldynamics:duct/item/DuctItem13", ir);

        IconRegistry.addIcon("DuctItem21", "thermaldynamics:duct/item/DuctItem21", ir);
        IconRegistry.addIcon("DuctItem22", "thermaldynamics:duct/item/DuctItem22", ir);
        IconRegistry.addIcon("DuctItem23", "thermaldynamics:duct/item/DuctItem23", ir);

        IconRegistry.addIcon("DuctItem31", "thermaldynamics:duct/item/DuctItem31", ir);
        IconRegistry.addIcon("DuctItem32", "thermaldynamics:duct/item/DuctItem32", ir);
        IconRegistry.addIcon("DuctItem33", "thermaldynamics:duct/item/DuctItem33", ir);

//        IconRegistry.addIcon("Connection" + ConnectionTypes.ENERGY_BASIC.ordinal(), "thermaldynamics:duct/energy/ConnectionEnergy00", ir);
        IconRegistry.addIcon("Connection" + ConnectionTypes.ENERGY_BASIC.ordinal(), "thermaldynamics:duct/item/Servo", ir);
        IconRegistry.addIcon("Connection" + ConnectionTypes.ENERGY_HARDENED.ordinal(), "thermaldynamics:duct/energy/ConnectionEnergy10", ir);
        IconRegistry.addIcon("Connection" + ConnectionTypes.ENERGY_REINFORCED.ordinal(), "thermaldynamics:duct/energy/ConnectionEnergy20", ir);

        IconRegistry.addIcon("Connection" + ConnectionTypes.FLUID_NORMAL.ordinal(), "thermaldynamics:duct/fluid/ConnectionFluid00", ir);
        IconRegistry.addIcon("Connection" + ConnectionTypes.FLUID_INPUT_ON.ordinal(), "thermaldynamics:duct/fluid/ConnectionFluid01", ir);

        IconRegistry.addIcon("Connection" + ConnectionTypes.ITEM_NORMAL.ordinal(), "thermaldynamics:duct/item/ConnectionItem00", ir);
        IconRegistry.addIcon("Connection" + ConnectionTypes.ITEM_INPUT_ON.ordinal(), "thermaldynamics:duct/item/ConnectionItem01", ir);
        IconRegistry.addIcon("Connection" + ConnectionTypes.ITEM_STUFFED_ON.ordinal(), "thermaldynamics:duct/item/ConnectionItem02", ir);
    }

    @Override
    public int getRenderType() {

        return TDProps.renderDuctId;
    }

    @Override
    public boolean canRenderInPass(int pass) {

        renderPass = pass;
        return pass < 2;
    }

    @Override
    public int getRenderBlockPass() {

        return 1;
    }

    @Override
    public void onBlockClicked(World p_149699_1_, int p_149699_2_, int p_149699_3_, int p_149699_4_, EntityPlayer p_149699_5_) {

        p_149699_5_.addChatMessage(new ChatComponentText("Forming Grid..."));
        MultiBlockFormer theFormer = new MultiBlockFormer();
        IMultiBlock theTile = (IMultiBlock) p_149699_1_.getTileEntity(p_149699_2_, p_149699_3_, p_149699_4_);
        theFormer.formGrid(theTile);
        p_149699_5_.addChatMessage(new ChatComponentText("Ducts Found: " + theTile.getGrid().idleSet.size()));
    }

    public static final String[] NAMES = {"testDuct", "energyDuct"};

    public static ItemStack blockDuct;

    public static enum DuctTypes {
        ENERGY_BASIC, ENERGY_HARDENED, ENERGY_REINFORCED, FLUID_TRANS, FLUID_OPAQUE, ITEM_TRANS, ITEM_OPAQUE, ITEM_FAST_TRANS, ITEM_FAST_OPAQUE
    }

    public static enum RenderTypes {
        ENERGY_BASIC, ENERGY_HARDENED, ENERGY_REINFORCED, FLUID_TRANS, FLUID_OPAQUE, ITEM_TRANS, ITEM_OPAQUE, ITEM_FAST_TRANS, ITEM_FAST_OPAQUE, ITEM_TRANS_SHORT, ITEM_TRANS_LONG, ITEM_TRANS_ROUNDROBIN, ITEM_OPAQUE_SHORT, ITEM_OPAQUE_LONG, ITEM_OPAQUE_ROUNDROBIN, ITEM_FAST_TRANS_SHORT, ITEM_FAST_TRANS_LONG, ITEM_FAST_TRANS_ROUNDROBIN, ITEM_FAST_OPAQUE_SHORT, ITEM_FAST_OPAQUE_LONG, ITEM_FAST_OPAQUE_ROUNDROBIN, STRUCTURE;
    }

    public static enum ConnectionTypes {
        NONE(false), DUCT, ENERGY_BASIC, ENERGY_BASIC_BLOCKED(false), ENERGY_HARDENED, ENERGY_HARDENED_BLOCKED(false), ENERGY_REINFORCED, ENERGY_REINFORCED_BLOCKED(
                false), FLUID_NORMAL, FLUID_BLOCKED(false), FLUID_INPUT_ON, FLUID_INPUT_OFF, ITEM_NORMAL, ITEM_ONEWAY, ITEM_BLOCKED(false), ITEM_INPUT_ON, ITEM_INPUT_OFF, ITEM_STUFFED_ON, ITEM_STUFFED_OFF, STRUCTURE;

        private final boolean renderDuct;

        private ConnectionTypes() {

            renderDuct = true;
        }

        private ConnectionTypes(boolean renderDuct) {

            this.renderDuct = renderDuct;
        }

        public boolean renderDuct() {

            return renderDuct;
        }
    }

    @Override
    public ArrayList<ItemStack> dismantleBlock(EntityPlayer player, NBTTagCompound nbt, World world, int x, int y, int z, boolean returnDrops, boolean simulate) {

        return null;
    }

    /* IInitializer */
    @Override
    public boolean preInit() {

        GameRegistry.registerBlock(this, ItemBlockDuct.class, "TestDuct");

        blockDuct = new ItemStack(this, 1, 0);

        // ItemHelper.registerWithHandlers("testDuct", blockDuct);

        return true;
    }

    @Override
    public boolean initialize() {

        MinecraftForge.EVENT_BUS.register(ThermalDynamics.blockDuct);
        GameRegistry.registerTileEntity(TileEnergyDuct.class, "thermalducts.ducts.energy.TileEnergyDuct");
        GameRegistry.registerTileEntity(TileItemDuct.class, "thermalducts.ducts.energy.TileItemDuct");
        return true;
    }

    @Override
    public boolean postInit() {

        return true;
    }

}
