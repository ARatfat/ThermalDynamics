package thermaldynamics.core;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import thermaldynamics.ducts.item.TileItemDuct;

import java.util.HashSet;
import java.util.Iterator;

public class TickHandlerClient {
    public static TickHandlerClient INSTANCE = new TickHandlerClient();
    public static HashSet<TileItemDuct> tickBlocks = new HashSet<TileItemDuct>();
    public static HashSet<TileItemDuct> tickBlocksToAdd = new HashSet<TileItemDuct>();
    public static HashSet<TileItemDuct> tickBlocksToRemove = new HashSet<TileItemDuct>();

    boolean needsMenu = false;


    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public void tick(TickEvent.ClientTickEvent evt) {
        Minecraft mc = Minecraft.getMinecraft();


        if (evt.phase == TickEvent.Phase.END) {

            if (!tickBlocksToAdd.isEmpty()) {
                tickBlocks.addAll(tickBlocksToAdd);
                tickBlocksToAdd.clear();
            }
            if (!mc.isGamePaused() && !tickBlocks.isEmpty()) {
                for (Iterator<TileItemDuct> iterator = tickBlocks.iterator(); iterator.hasNext(); ) {
                    TileItemDuct aCond = iterator.next();
                    if (aCond.isInvalid())
                        iterator.remove();
                    else
                        aCond.tickItemsClient();
                }
                tickBlocks.removeAll(tickBlocksToRemove);
                tickBlocksToRemove.clear();
            }

            GuiScreen gui = Minecraft.getMinecraft().currentScreen;
            if (gui == null || !gui.doesGuiPauseGame()) {
                gameTicks++;
            }

            if (mc.currentScreen instanceof GuiMainMenu) {
                if (needsMenu) {
                    onMainMenu();
                    needsMenu = false;
                }
            } else if (mc.inGameHasFocus) {
                needsMenu = true;
            }
        }
    }


    public void onMainMenu() {
        synchronized (TickHandler.handlers) {
            TickHandler.handlers.clear();
        }
    }


    public static int gameTicks = 0;


}
