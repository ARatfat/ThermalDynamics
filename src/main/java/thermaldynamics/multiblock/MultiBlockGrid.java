package thermaldynamics.multiblock;

import net.minecraft.world.World;
import thermaldynamics.core.TickHandler;

import java.util.HashSet;

public class MultiBlockGrid {
    public HashSet<IMultiBlock> nodeSet = new HashSet<IMultiBlock>();
    public HashSet<IMultiBlock> idleSet = new HashSet<IMultiBlock>();
    public World world;

    public MultiBlockGrid(World world) {
        this.world = world;
        TickHandler.getTickHandler(world).newGrids.add(this);
    }

    public void addIdle(IMultiBlock aMultiBlock) {
        idleSet.add(aMultiBlock);

        if (nodeSet.contains(aMultiBlock)) {
            nodeSet.remove(aMultiBlock);
        }


        balanceGrid();
    }

    public void addNode(IMultiBlock aMultiBlock) {
        nodeSet.add(aMultiBlock);
        if (idleSet.contains(aMultiBlock)) {
            idleSet.remove(aMultiBlock);
        }

        balanceGrid();
    }

    public void mergeGrids(MultiBlockGrid theGrid) {
        for (IMultiBlock aBlock : theGrid.nodeSet) {
            aBlock.setGrid(this);
        }
        nodeSet.addAll(theGrid.nodeSet);

        for (IMultiBlock aBlock : theGrid.idleSet) {
            aBlock.setGrid(this);
        }
        idleSet.addAll(theGrid.idleSet);

        theGrid.destory();
    }

    public void destory() {
        nodeSet.clear();
        idleSet.clear();

        TickHandler.getTickHandler(world).oldGrids.add(this);
    }

    public boolean canGridsMerge(MultiBlockGrid grid) {
        return true;
    }

    public void resetMultiBlocks() {
        for (IMultiBlock aBlock : nodeSet) {
            aBlock.setValidForForming();
        }
        for (IMultiBlock aBlock : idleSet) {
            aBlock.setValidForForming();
        }
    }

    /*
     * Called at the end of a world tick
     */
    public void tickGrid() {

    }

    /*
     * Called whenever a set changes so that grids that rely on set sizes can rebalance.
     */
    public void balanceGrid() {

    }

    public  void addBlock(IMultiBlock aBlock) {
        if (aBlock.isNode()) {
            addNode(aBlock);
        } else {
            addIdle(aBlock);
        }
    }
}
