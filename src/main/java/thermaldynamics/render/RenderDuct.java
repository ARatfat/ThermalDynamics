package thermaldynamics.render;

import cofh.core.block.BlockCoFHBase;
import cofh.core.render.IconRegistry;
import cofh.core.render.RenderUtils;
import cofh.lib.render.RenderHelper;
import cofh.repack.codechicken.lib.render.CCModel;
import cofh.repack.codechicken.lib.render.CCRenderState;
import cofh.repack.codechicken.lib.vec.Scale;
import cofh.repack.codechicken.lib.vec.Translation;
import cofh.repack.codechicken.lib.vec.Vector3;
import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler;
import cpw.mods.fml.client.registry.RenderingRegistry;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.client.IItemRenderer;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import org.lwjgl.opengl.GL11;
import thermaldynamics.block.Attachment;
import thermaldynamics.block.BlockDuct;
import thermaldynamics.block.BlockDuct.DuctTypes;
import thermaldynamics.block.BlockDuct.RenderTypes;
import thermaldynamics.block.TileMultiBlock;
import thermaldynamics.block.TileMultiBlock.ConnectionTypes;
import thermaldynamics.core.TDProps;
import thermaldynamics.ducts.item.TileItemDuct;
import thermaldynamics.ducts.item.TravelingItem;

import java.util.List;

public class RenderDuct extends TileEntitySpecialRenderer implements ISimpleBlockRenderingHandler, IItemRenderer {

    public static final RenderDuct instance = new RenderDuct();

    public static final int ITEMS_TO_RENDER_PER_DUCT = 16;


    static RenderItem travelingItemRender;
    static EntityItem travelingEntityItem = new EntityItem(null);
    static float travelingItemSpin = 0.25F;

    static final float ITEM_RENDER_SCALE = 0.6F;

    static final int[] INV_CONNECTIONS = {BlockDuct.ConnectionTypes.DUCT.ordinal(), BlockDuct.ConnectionTypes.DUCT.ordinal(), 0, 0, 0, 0};
    static int[] connections = new int[6];

    static IIcon[] textureDuct = new IIcon[RenderTypes.values().length];
    static IIcon[] textureConnection = new IIcon[BlockDuct.ConnectionTypes.values().length];
    static IIcon textureCenterLine;

    static IIcon textureSolidRedstone;
    static IIcon textureFluidRedstone;
    static IIcon textureFluidGlowstone;

    static CCModel[][] modelFluid = new CCModel[6][7];
    static CCModel[][] modelConnection = new CCModel[3][6];
    static CCModel modelCenter;

    static CCModel[] modelLine = new CCModel[6];
    static CCModel modelLineCenter;

    static {
        TDProps.renderDuctId = RenderingRegistry.getNextAvailableRenderId();
        RenderingRegistry.registerBlockHandler(instance);

        travelingItemRender = new RenderItem() {

            @Override
            public boolean shouldBob() {
                return false;
            }

            ;

            @Override
            public boolean shouldSpreadItems() {
                return false;
            }

            ;
        };

        travelingItemRender.setRenderManager(RenderManager.instance);
        travelingEntityItem.hoverStart = 0;

        generateFluidModels();
        generateModels();
    }


    public static void initialize() {

        textureDuct[RenderTypes.ENERGY_BASIC.ordinal()] = IconRegistry.getIcon("DuctEnergy00");
        textureDuct[RenderTypes.ENERGY_HARDENED.ordinal()] = IconRegistry.getIcon("DuctEnergy10");
        textureDuct[RenderTypes.ENERGY_REINFORCED.ordinal()] = IconRegistry.getIcon("DuctEnergy20");

        textureDuct[RenderTypes.FLUID_TRANS.ordinal()] = IconRegistry.getIcon("DuctFluid00");
        textureDuct[RenderTypes.FLUID_OPAQUE.ordinal()] = IconRegistry.getIcon("DuctFluid10");

        textureDuct[RenderTypes.ITEM_TRANS.ordinal()] = IconRegistry.getIcon("DuctItem00");
        textureDuct[RenderTypes.ITEM_OPAQUE.ordinal()] = IconRegistry.getIcon("DuctItem10");
        textureDuct[RenderTypes.ITEM_FAST_TRANS.ordinal()] = IconRegistry.getIcon("DuctItem20");
        textureDuct[RenderTypes.ITEM_FAST_OPAQUE.ordinal()] = IconRegistry.getIcon("DuctItem30");

        textureDuct[RenderTypes.ITEM_TRANS_SHORT.ordinal()] = IconRegistry.getIcon("DuctItem01");
        textureDuct[RenderTypes.ITEM_TRANS_LONG.ordinal()] = IconRegistry.getIcon("DuctItem02");
        textureDuct[RenderTypes.ITEM_TRANS_ROUNDROBIN.ordinal()] = IconRegistry.getIcon("DuctItem03");

        textureDuct[RenderTypes.ITEM_OPAQUE_SHORT.ordinal()] = IconRegistry.getIcon("DuctItem11");
        textureDuct[RenderTypes.ITEM_OPAQUE_LONG.ordinal()] = IconRegistry.getIcon("DuctItem12");
        textureDuct[RenderTypes.ITEM_OPAQUE_ROUNDROBIN.ordinal()] = IconRegistry.getIcon("DuctItem13");

        textureDuct[RenderTypes.ITEM_FAST_TRANS_SHORT.ordinal()] = IconRegistry.getIcon("DuctItem21");
        textureDuct[RenderTypes.ITEM_FAST_TRANS_LONG.ordinal()] = IconRegistry.getIcon("DuctItem22");
        textureDuct[RenderTypes.ITEM_FAST_TRANS_ROUNDROBIN.ordinal()] = IconRegistry.getIcon("DuctItem23");

        textureDuct[RenderTypes.ITEM_FAST_OPAQUE_SHORT.ordinal()] = IconRegistry.getIcon("DuctItem31");
        textureDuct[RenderTypes.ITEM_FAST_OPAQUE_LONG.ordinal()] = IconRegistry.getIcon("DuctItem32");
        textureDuct[RenderTypes.ITEM_FAST_OPAQUE_ROUNDROBIN.ordinal()] = IconRegistry.getIcon("DuctItem33");
        textureDuct[RenderTypes.STRUCTURE.ordinal()] = IconRegistry.getIcon("DuctStructure");

        textureConnection[BlockDuct.ConnectionTypes.ENERGY_BASIC.ordinal()] = IconRegistry.getIcon("Connection2");
        textureConnection[BlockDuct.ConnectionTypes.ENERGY_BASIC_BLOCKED.ordinal()] = IconRegistry.getIcon("Connection2");

        textureConnection[BlockDuct.ConnectionTypes.ENERGY_HARDENED.ordinal()] = IconRegistry.getIcon("Connection4");
        textureConnection[BlockDuct.ConnectionTypes.ENERGY_HARDENED_BLOCKED.ordinal()] = IconRegistry.getIcon("Connection4");

        textureConnection[BlockDuct.ConnectionTypes.ENERGY_REINFORCED.ordinal()] = IconRegistry.getIcon("Connection6");
        textureConnection[BlockDuct.ConnectionTypes.ENERGY_REINFORCED_BLOCKED.ordinal()] = IconRegistry.getIcon("Connection6");

        textureConnection[BlockDuct.ConnectionTypes.FLUID_NORMAL.ordinal()] = IconRegistry.getIcon("Connection8");
        textureConnection[BlockDuct.ConnectionTypes.FLUID_BLOCKED.ordinal()] = IconRegistry.getIcon("Connection8");
        textureConnection[BlockDuct.ConnectionTypes.FLUID_INPUT_ON.ordinal()] = IconRegistry.getIcon("Connection10");
        textureConnection[BlockDuct.ConnectionTypes.FLUID_INPUT_OFF.ordinal()] = IconRegistry.getIcon("Connection10");

        textureConnection[BlockDuct.ConnectionTypes.ITEM_NORMAL.ordinal()] = IconRegistry.getIcon("Connection12");
        textureConnection[BlockDuct.ConnectionTypes.ITEM_BLOCKED.ordinal()] = IconRegistry.getIcon("Connection12");
        textureConnection[BlockDuct.ConnectionTypes.ITEM_INPUT_ON.ordinal()] = IconRegistry.getIcon("Connection14");
        textureConnection[BlockDuct.ConnectionTypes.ITEM_INPUT_OFF.ordinal()] = IconRegistry.getIcon("Connection14");
        textureConnection[BlockDuct.ConnectionTypes.ITEM_STUFFED_ON.ordinal()] = IconRegistry.getIcon("Connection16");
        textureConnection[BlockDuct.ConnectionTypes.ITEM_STUFFED_OFF.ordinal()] = IconRegistry.getIcon("Connection16");

        textureSolidRedstone = IconRegistry.getIcon("StorageRedstone");
        textureFluidRedstone = IconRegistry.getIcon("Fluid_Redstone_Still");
        textureFluidGlowstone = IconRegistry.getIcon("Fluid_Glowstone_Still");


        textureCenterLine = IconRegistry.getIcon("CenterLine");
    }

    private static void generateFluidModels() {

        for (int i = 1; i < 7; i++) {
            double d1 = 0.47 - 0.025 * i;
            double d2 = 0.53 + 0.025 * i;
            double d3 = 0.32 + 0.06 * i;
            double c1 = 0.32;
            double c2 = 0.68;
            double[][] boxes = new double[][]{{d1, 0, d1, d2, c1, d2}, {d1, d3, d1, d2, 1, d2}, {c1, c1, 0, c2, d3, c1}, {c1, c1, c2, c2, d3, 1},
                    {0, c1, c1, c1, d3, c2}, {c2, c1, c1, 1, d3, c2}, {c1, c1, c1, c2, d3, c2}};

            for (int s = 0; s < 7; s++) {
                modelFluid[i - 1][s] = CCModel.quadModel(24).generateBlock(0, boxes[s][0], boxes[s][1], boxes[s][2], boxes[s][3], boxes[s][4], boxes[s][5])
                        .computeNormals();
            }
        }
    }


    public static void generateModels() {

        modelCenter = CCModel.quadModel(48).generateBox(0, -3, -3, -3, 6, 6, 6, 0, 0, 32, 32, 16);
        modelConnection[0][1] = CCModel.quadModel(48).generateBox(0, -3, 3, -3, 6, 5, 6, 0, 16, 32, 32, 16);
        modelConnection[1][1] = CCModel.quadModel(24).generateBox(0, -4, 4, -4, 8, 4, 8, 0, 0, 32, 32, 16).computeNormals();
        modelConnection[2][1] = CCModel.quadModel(24).generateBox(0, -4, 4, -4, 8, 4, 8, 0, 16, 32, 32, 16).computeNormals();

        double h = 0.4;
        modelLineCenter = CCModel.quadModel(24).generateBlock(0, h, h, h, 1 - h, 1 - h, 1 - h).computeNormals();
        modelLine[1] = CCModel.quadModel(16).generateBlock(0, h, 1 - h, h, 1 - h, 1.0, 1 - h, 3).computeNormals();
        CCModel.generateSidedModels(modelLine, 1, Vector3.center);


        CCModel.generateBackface(modelCenter, 0, modelCenter, 24, 24);
        CCModel.generateBackface(modelConnection[0][1], 0, modelConnection[0][1], 24, 24);

        modelCenter.computeNormals();//.computeLighting(LightModel.standardLightModel);
        modelConnection[0][1].computeNormals();

        for (int i = 0; i < modelConnection.length; i++) {
            CCModel.generateSidedModels(modelConnection[i], 1, Vector3.zero);
        }
        Scale[] mirrors = new Scale[]{new Scale(1, -1, 1), new Scale(1, 1, -1), new Scale(-1, 1, 1)};
        for (int i = 0; i < modelConnection.length; i++) {
            CCModel[] sideModels = modelConnection[i];
            for (int s = 2; s < 6; s += 2) {
                sideModels[s] = sideModels[0].sidedCopy(0, s, Vector3.zero);
            }
            for (int s = 1; s < 6; s += 2) {
                sideModels[s] = sideModels[s - 1].backfacedCopy().apply(mirrors[s / 2]);
            }
        }
    }

    public boolean renderFrame(boolean invRender, int renderType, int[] connection, double x, double y, double z) {

        x += 0.5;
        y += 0.5;
        z += 0.5;

        Translation trans = RenderUtils.getRenderVector(x, y, z).translation();

        for (int s = 0; s < 6; s++) {

            if (BlockDuct.ConnectionTypes.values()[connection[s]].renderDuct()) {
                RenderUtils.ScaledIconTransformation icon;

                if (BlockDuct.ConnectionTypes.values()[connection[s]] == BlockDuct.ConnectionTypes.STRUCTURE) {
                    icon = RenderUtils.getIconTransformation(textureDuct[RenderTypes.STRUCTURE.ordinal()]);
                    modelConnection[0][s].render(0, 4, trans, icon);
                    modelConnection[0][s].render(8, 24, trans, icon);
                    modelConnection[0][s].render(24, 28, trans, icon);
                    modelConnection[0][s].render(32, 48, trans, icon);
                } else {
                    icon = RenderUtils.getIconTransformation(textureDuct[renderType]);
                    if (!invRender) {
                        modelConnection[0][s].render(0, 4, trans, icon);
                        modelConnection[0][s].render(8, 24, trans, icon);
                        modelConnection[0][s].render(24, 28, trans, icon);
                        modelConnection[0][s].render(32, 48, trans, icon);
                    } else {
                        modelConnection[0][s].render(trans, icon);
                    }

                    if (connection[s] != BlockDuct.ConnectionTypes.DUCT.ordinal()
                            && connection[s] != BlockDuct.ConnectionTypes.STRUCTURE.ordinal()
                            ) {
                        if (connection[s] % 2 == 0) {
                            modelConnection[1][s].render(trans, RenderUtils.getIconTransformation(textureConnection[connection[s]]));
                        } else {
                            modelConnection[2][s].render(trans, RenderUtils.getIconTransformation(textureConnection[connection[s]]));
                        }
                    }
                }
            } else {
                modelCenter.render(s * 4, s * 4 + 4, trans, RenderUtils.getIconTransformation(textureDuct[renderType]));
                modelCenter.render(24 + s * 4, 28 + s * 4, trans, RenderUtils.getIconTransformation(textureDuct[renderType]));
            }
        }
        return true;
    }

    public boolean renderWorldExtra(TileMultiBlock tile, int renderType, int[] connection, double x, double y, double z) {
        Tessellator.instance.setColorOpaque_F(1, 1, 1);
        IIcon texture = null;

        if (renderType == RenderTypes.ENERGY_BASIC.ordinal() || renderType == RenderTypes.ENERGY_HARDENED.ordinal()) {
            texture = textureSolidRedstone;
        } else if (renderType == RenderTypes.ENERGY_REINFORCED.ordinal()) {
            texture = textureFluidRedstone;
        } else if (renderType == RenderTypes.ITEM_FAST_TRANS.ordinal() || renderType == RenderTypes.ITEM_FAST_TRANS_SHORT.ordinal()
                || renderType == RenderTypes.ITEM_FAST_TRANS_LONG.ordinal() || renderType == RenderTypes.ITEM_FAST_TRANS_ROUNDROBIN.ordinal()) {
            texture = textureFluidGlowstone;
        } else {
            return false;
        }

        CCModel[] models = modelFluid[5];

        for (int s = 0; s < 6; s++) {
            if (BlockDuct.ConnectionTypes.values()[connection[s]].renderDuct() && connection[s] != BlockDuct.ConnectionTypes.STRUCTURE.ordinal()) {
                models[s].render(x, y, z, RenderUtils.getIconTransformation(texture));
            }
        }
        models[6].render(x, y, z, RenderUtils.getIconTransformation(texture));

        return true;

        // if (texture != null) {
        // for (int s = 0; s < 6; s++) {
        // if (BlockDuct.ConnectionTypes.values()[connection[s]].renderDuct()) {
        // modelFluid[5][s].render(x, y, z, RenderUtils.getIconTransformation(texture));
        // }
        // }
        // modelFluid[5][6].render(x, y, z, RenderUtils.getIconTransformation(texture));
        // return true;
        // }
        // return false;
    }

    public void renderFluid(FluidStack stack, int[] connection, int level, double x, double y, double z) {

        if (stack == null || stack.amount <= 0 || level <= 0 || stack.fluidID <= 0) {
            return;
        }
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        CCRenderState.startDrawing();
        Fluid fluid = stack.getFluid();

        RenderUtils.setFluidRenderColor(stack);
        RenderHelper.bindTexture(RenderHelper.MC_BLOCK_SHEET);
        IIcon fluidTex = RenderHelper.getFluidTexture(stack);

        if (fluid.isGaseous(stack)) {
            CCRenderState.setColour(RenderUtils.getFluidRenderColor(stack) << 8 | 32 + 36 * level);
            level = 6;
        }
        CCModel[] models = modelFluid[level - 1];

        for (int s = 0; s < 6; s++) {
            if (BlockDuct.ConnectionTypes.values()[connection[s]].renderDuct()
                    && connection[s] != BlockDuct.ConnectionTypes.STRUCTURE.ordinal()
                    ) {
                models[s].render(x, y, z, RenderUtils.getIconTransformation(fluidTex));
            }
        }
        models[6].render(x, y, z, RenderUtils.getIconTransformation(fluidTex));
        CCRenderState.draw();
    }

    public void getDuctConnections(TileMultiBlock tile) {
        for (int i = 0; i < 6; i++) {
            if (tile.connectionTypes[i] == ConnectionTypes.BLOCKED) {
                connections[i] = BlockDuct.ConnectionTypes.NONE.ordinal();
            } else {
                connections[i] = tile.getConnectionType(i).ordinal();
            }
        }
    }

    /* ISimpleBlockRenderingHandler */
    @Override
    public void renderInventoryBlock(Block block, int metadata, int modelID, RenderBlocks renderer) {

        // RenderUtils.preRender();

        CCRenderState.startDrawing();
        renderFrame(true, metadata, INV_CONNECTIONS, -0.5, -0.5, -0.5);
        CCRenderState.draw();

        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        CCRenderState.startDrawing();
        renderWorldExtra(null, metadata, INV_CONNECTIONS, -0.5, -0.5 - RenderHelper.RENDER_OFFSET, -0.5);
        CCRenderState.draw();

        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_BLEND);

        CCRenderState.useNormals = false;
    }

    @Override
    public boolean renderWorldBlock(IBlockAccess world, int x, int y, int z, Block block, int modelId, RenderBlocks renderer) {

        TileEntity tile = world.getTileEntity(x, y, z);
        if (!(tile instanceof TileMultiBlock)) {
            return false;
        }
        TileMultiBlock theTile = (TileMultiBlock) tile;

        RenderUtils.preWorldRender(world, x, y, z);
        getDuctConnections(theTile);

        boolean flag = false;


        for (Attachment attachment : theTile.attachments) {
            if (attachment != null) {
                flag = attachment.render(BlockCoFHBase.renderPass, renderer) || flag;
            }
        }

        if (BlockCoFHBase.renderPass == 0) {
            renderFrame(false, RenderTypes.ENERGY_BASIC.ordinal(), connections, x, y, z);
            flag = true;
        } else {
            flag = renderWorldExtra(theTile, RenderTypes.ITEM_OPAQUE.ordinal(), connections, x, y, z) || flag;
        }


        return flag;
    }

    @Override
    public boolean shouldRender3DInInventory(int modelId) {

        return true;
    }

    @Override
    public int getRenderId() {

        return TDProps.renderDuctId;
    }

    /* IItemRenderer */
    @Override
    public boolean handleRenderType(ItemStack item, ItemRenderType type) {

        return true;
    }

    @Override
    public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {

        return true;
    }

    @Override
    public void renderItem(ItemRenderType type, ItemStack item, Object... data) {

        // int metadata = 2;
        // boolean renderExtra = false;
        //
        // GL11.glPushMatrix();
        // double offset = -0.5;
        // if (type == ItemRenderType.EQUIPPED || type == ItemRenderType.EQUIPPED_FIRST_PERSON) {
        // offset = 0;
        // } else if (type == ItemRenderType.ENTITY) {
        // GL11.glScaled(0.5, 0.5, 0.5);
        // }
        // RenderHelper.setBlockTextureSheet();
        // RenderUtils.preRender();
        //
        // CCRenderState.startDrawing();
        // instance.renderFrame(true, metadata, INV_CONNECTIONS, offset, offset, offset);
        // CCRenderState.draw();
        //
        // GL11.glDisable(GL11.GL_LIGHTING);
        // GL11.glEnable(GL11.GL_BLEND);
        // GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        //
        // if (renderExtra) {
        // CCRenderState.startDrawing();
        // renderWorldExtra(null, metadata, INV_CONNECTIONS, offset, offset - RenderHelper.RENDER_OFFSET, offset);
        // CCRenderState.draw();
        // }
        //
        // GL11.glEnable(GL11.GL_LIGHTING);
        // GL11.glDisable(GL11.GL_BLEND);
        //
        // CCRenderState.useNormals = false;
        // RenderHelper.setItemTextureSheet();
        //
        // GL11.glPopMatrix();
    }

    public IIcon getFrameTexture(TileMultiBlock duct, byte side) {

        return textureDuct[DuctTypes.ENERGY_REINFORCED.ordinal()];
    }

    public void renderTravelingItems(List<TravelingItem> items, World world, double x, double y, double z, float frame) {

        if (items == null || items.size() <= 0) {
            return;
        }


        GL11.glPushMatrix();

        travelingItemSpin += .001;
        travelingItemSpin %= 180;
        travelingEntityItem.hoverStart = travelingItemSpin;

        TravelingItem renderItem;

        for (int i = 0; i < items.size() && i < ITEMS_TO_RENDER_PER_DUCT; i++) {
            renderItem = items.get(i);
            if (renderItem == null || renderItem.stack == null) {
                continue;
            }
            GL11.glPushMatrix();

            GL11.glTranslated(x + renderItem.x, y + renderItem.y, z + renderItem.z);
            GL11.glScalef(ITEM_RENDER_SCALE, ITEM_RENDER_SCALE, ITEM_RENDER_SCALE);

            travelingEntityItem.setEntityItemStack(renderItem.stack);
            travelingItemRender.doRender(travelingEntityItem, 0, -0.1F, 0, 0, 0);
            GL11.glPopMatrix();
        }
        GL11.glPopMatrix();
    }

    @Override
    public void func_147496_a(World p_147496_1_) {
        super.func_147496_a(p_147496_1_);
    }

    int pass = 0;

    @Override
    public void renderTileEntityAt(TileEntity tile, double x, double y, double z, float frame) {

        TileItemDuct duct = (TileItemDuct) tile;

        RenderUtils.preWorldRender(tile.getWorldObj(), tile.xCoord, tile.yCoord, tile.zCoord);
        CCRenderState.useNormals = true;
        renderTravelingItems(duct.myItems, tile.getWorldObj(), x, y, z, frame);
        CCRenderState.useNormals = false;
        CCRenderState.reset();

        if (duct.centerLine > 0) {
            GL11.glPushMatrix();

            Translation trans = (new Vector3(x, y, z)).translation();

            GL11.glEnable(GL11.GL_BLEND);
            GL11.glEnable(GL11.GL_CULL_FACE);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
            RenderHelper.bindTexture(RenderHelper.MC_BLOCK_SHEET);
            RenderUtils.preWorldRender(tile.getWorldObj(), tile.xCoord, tile.yCoord, tile.zCoord);
            CCRenderState.setColour(-1);
            CCRenderState.setBrightness(15728880);
            CCRenderState.alphaOverride = (int) (((duct.centerLine - frame) * 255.0) / (TileItemDuct.maxCenterLine)) & 0xFF;
            getDuctConnections(duct);
            CCRenderState.startDrawing();
            for (int s = 0; s < 6; s++) {
                if (BlockDuct.ConnectionTypes.values()[connections[s]].renderDuct() && (duct.centerLineMask & (1 << s)) != 0) {
                    modelLine[s].render(trans, RenderUtils.getIconTransformation(textureCenterLine));
                } else {
                    modelLineCenter.render(s * 4, s * 4 + 4, trans, RenderUtils.getIconTransformation(textureCenterLine));
                }
            }
            CCRenderState.draw();
            CCRenderState.alphaOverride = -1;
            CCRenderState.reset();
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glDisable(GL11.GL_BLEND);

            CCRenderState.useNormals = false;
            GL11.glPopMatrix();

        }
        //CCRenderState.setBrightness();

    }
}
