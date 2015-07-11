package cofh.thermaldynamics.duct.entity;

import cofh.lib.util.position.BlockPosition;
import cofh.repack.codechicken.lib.vec.Vector3;
import cofh.thermaldynamics.ThermalDynamics;
import cofh.thermaldynamics.block.TileTDBase;
import cofh.thermaldynamics.multiblock.Route;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Facing;
import net.minecraft.world.World;

public class EntityTransport extends Entity {
    public static final int DATAWATCHER_DIRECTIONS = 16;
    public static final int DATAWATCHER_PROGRESS = 17;
    public static final int DATAWATCHER_POSX = 18;
    public static final int DATAWATCHER_POSY = 19;
    public static final int DATAWATCHER_POSZ = 20;
    public static final int DATAWATCHER_STEP = 21;

    public static final int PIPE_LENGTH = 2;
    public static final int PIPE_LENGTH2 = 1;

    public byte progress;
    public byte direction;
    public byte oldDirection;
    public byte step = 1;
    public boolean reRoute = false;
    Route myPath;
    BlockPosition pos;

    @Override
    public boolean isEntityInvulnerable() {
        return true;
    }

    @Override
    public double getYOffset() {
        return super.getYOffset();
    }

    @Override
    public double getMountedYOffset() {

        Entity riddenByEntity = this.riddenByEntity;
        if(riddenByEntity == null)
            return super.getMountedYOffset();
        else {

            if (riddenByEntity == ThermalDynamics.proxy.getClientPlayerSafe()) {
                return -riddenByEntity.getYOffset();
            }

            double h = riddenByEntity.boundingBox.maxY - riddenByEntity.boundingBox.minY;
            return -riddenByEntity.getYOffset() - h / 2;
        }
    }

    public EntityTransport(World p_i1582_1_) {
        super(p_i1582_1_);
        step = 0;
        this.height = 0.1F;
        this.width = 0.1F;
        this.noClip = true;
        this.isImmuneToFire =  true;
    }


    public EntityTransport(TileTransportDuct origin, Route route, byte startDirection, byte step) {
        this(origin.world());

        this.step = step;
        pos = new BlockPosition(origin);
        myPath = route;

        progress = 0;
        this.direction = route.getNextDirection();
        this.oldDirection = startDirection;

        setPosition(0);
    }

    @Override
    public boolean shouldRiderSit() {
        return true;
    }

    public void start(EntityLivingBase passenger) {
        worldObj.spawnEntityInWorld(this);
        passenger.mountEntity(this);
    }

    @Override
    public void onUpdate() {
//        super.onUpdate();
        if (!this.worldObj.isRemote)
            if (riddenByEntity == null || riddenByEntity.isDead) {
                setDead();
                return;
            }

        if (worldObj.isRemote && this.dataWatcher.hasChanges()) {
            this.dataWatcher.func_111144_e();
            loadWatcherData();
        }

        if (pos == null)
            return;

        TileEntity tile = worldObj.getTileEntity(pos.x, pos.y, pos.z);

        if (tile == null || !(tile instanceof TileTransportDuct)) {
            if (worldObj.isRemote)
                pos = null;
            else
                dropPassenger();
            return;
        }

        TileTransportDuct homeTile = ((TileTransportDuct) tile);

        if (!worldObj.isRemote) {
            progress += step;

            if (myPath == null) {
                bouncePassenger(homeTile);
            } else if (progress >= PIPE_LENGTH) {
                progress %= PIPE_LENGTH;
                advanceTile(homeTile);
            } else if (progress >= PIPE_LENGTH2 && progress - step < PIPE_LENGTH2) {
                if (reRoute || homeTile.neighborTypes[direction] == TileTDBase.NeighborTypes.NONE) {
                    bouncePassenger(homeTile);
                }
            }

            updateWatcherData();
        } else {
            progress += step;
            if (progress >= PIPE_LENGTH) {
                BlockPosition p = pos.copy().step(direction);

                TileEntity tileEntity = worldObj.getTileEntity(p.x, p.y, p.z);
                if (!(tileEntity instanceof TileTransportDuct)) {
                    pos = null;
                    return;
                }
                TileTDBase.NeighborTypes[] neighbours = ((TileTransportDuct) tileEntity).neighborTypes;
                if (neighbours[direction ^ 1] != TileTDBase.NeighborTypes.MULTIBLOCK) {
                    pos = null;
                    return;
                }

                pos = p;
                oldDirection = direction;
                progress %= PIPE_LENGTH;

                if(neighbours[direction] != TileTDBase.NeighborTypes.MULTIBLOCK) {
                    for (byte i = 0; i < neighbours.length; i++) {
                        if(neighbours[i] == TileTDBase.NeighborTypes.MULTIBLOCK){
                            direction = i;
                        }
                    }
                }
            }
        }

        setPosition(0);

        if (riddenByEntity != null && !riddenByEntity.isDead) {
            updateRiderPosition();
        }
    }

    @Override
    public void onEntityUpdate() {
//        super.onEntityUpdate();


    }

    public void setPosition(double frame) {
        if(pos == null) return;

        Vector3 oldPos = getPos(frame - 1);
        lastTickPosX = prevPosX = oldPos.x;
        lastTickPosY = prevPosY = oldPos.y;
        lastTickPosZ = prevPosZ = oldPos.z;

        Vector3 newPos = getPos(frame);
        setPosition(newPos.x, newPos.y, newPos.z);

        motionX = newPos.x - oldPos.x;
        motionY = newPos.y - oldPos.y;
        motionZ = newPos.z - oldPos.z;
    }

    private void dropPassenger() {
        if (!worldObj.isRemote) {
            moveToSafePosition();
            riddenByEntity.mountEntity(null);
            setDead();
        }
    }

    public void moveToSafePosition() {
        if (direction >= 0 && direction < 6)
            setPosition(
                    pos.x + Facing.offsetsXForSide[direction] + 0.5,
                    pos.y + Facing.offsetsYForSide[direction],
                    pos.z + Facing.offsetsZForSide[direction] + 0.5);
    }

    protected boolean canTriggerWalking() {
        return false;
    }

    public void advanceTile(TileTransportDuct homeTile) {

        if (homeTile.neighborTypes[direction] == TileTDBase.NeighborTypes.MULTIBLOCK && homeTile.connectionTypes[direction] == TileTDBase.ConnectionTypes.NORMAL) {
            TileTransportDuct newHome = (TileTransportDuct) homeTile.getConnectedSide(direction);
            if (newHome != null) {
                if (newHome.neighborTypes[direction ^ 1] == TileTDBase.NeighborTypes.MULTIBLOCK) {
                    pos = new BlockPosition(newHome);

                    if (myPath.hasNextDirection()) {
                        oldDirection = direction;
                        direction = myPath.getNextDirection();
                    } else {
                        reRoute = true;
                    }
                }
            }
        } else if (homeTile.neighborTypes[direction] == TileTDBase.NeighborTypes.OUTPUT && homeTile.connectionTypes[direction].allowTransfer) {
            dropPassenger();
        } else {
            bouncePassenger(homeTile);
        }
    }

    public void bouncePassenger(TileTransportDuct homeTile) {
        myPath = homeTile.getRoute(this, direction, step);


        if (myPath == null)
            dropPassenger();
        else {
            oldDirection = direction;
            direction = myPath.getNextDirection();
            reRoute = false;
        }
    }


    @Override
    protected void entityInit() {
        this.dataWatcher.addObject(DATAWATCHER_DIRECTIONS, (byte) 0);
        this.dataWatcher.addObject(DATAWATCHER_PROGRESS, (byte) 0);
        this.dataWatcher.addObject(DATAWATCHER_POSX, 0);
        this.dataWatcher.addObject(DATAWATCHER_POSY, 0);
        this.dataWatcher.addObject(DATAWATCHER_POSZ, 0);
        this.dataWatcher.addObject(DATAWATCHER_STEP, (byte) 1);
    }

    public void updateWatcherData() {
        byte p_75692_2_ = (byte) (direction | (oldDirection << 3));
        this.dataWatcher.updateObject(DATAWATCHER_DIRECTIONS, p_75692_2_);
        this.dataWatcher.updateObject(DATAWATCHER_PROGRESS, progress);
        this.dataWatcher.updateObject(DATAWATCHER_POSX, pos.x);
        this.dataWatcher.updateObject(DATAWATCHER_POSY, pos.y);
        this.dataWatcher.updateObject(DATAWATCHER_POSZ, pos.z);
        this.dataWatcher.updateObject(DATAWATCHER_STEP, step);

    }

    public void loadWatcherData() {
        byte b = this.dataWatcher.getWatchableObjectByte(DATAWATCHER_DIRECTIONS);
        direction = (byte) (b & 7);
        oldDirection = (byte) (b >> 3);
        progress = this.dataWatcher.getWatchableObjectByte(DATAWATCHER_PROGRESS);
        pos = new BlockPosition(
                this.dataWatcher.getWatchableObjectInt(DATAWATCHER_POSX),
                this.dataWatcher.getWatchableObjectInt(DATAWATCHER_POSY),
                this.dataWatcher.getWatchableObjectInt(DATAWATCHER_POSZ));
        step = this.dataWatcher.getWatchableObjectByte(DATAWATCHER_STEP);
    }


    @Override
    protected void readEntityFromNBT(NBTTagCompound tag) {
        if (tag.hasKey("route", 7))
            myPath = new Route(tag.getByteArray("route"));

        pos = new BlockPosition(
                tag.getInteger("posx"),
                tag.getInteger("posy"),
                tag.getInteger("posz"));

        progress = tag.getByte("progress");
        direction = tag.getByte("direction");
        oldDirection = tag.getByte("oldDirection");
        step = tag.getByte("step");
        reRoute = tag.getBoolean("reRoute");
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound tag) {
        if (myPath != null)
            tag.setByteArray("route", myPath.toByteArray());

        tag.setInteger("posx", pos.x);
        tag.setInteger("posy", pos.y);
        tag.setInteger("posz", pos.z);

        tag.setByte("progress", progress);
        tag.setByte("direction", direction);
        tag.setByte("oldDirection", oldDirection);
        tag.setByte("step", step);
        tag.setBoolean("reRoute", reRoute);
    }

    public Vector3 getPos(double framePos) {
        return getPos(progress, framePos);
    }

    public Vector3 getPos(byte progress, double framePos) {
        double v = (progress + step * framePos) / ((double) PIPE_LENGTH) - 0.5;
        int dir = v < 0 ? oldDirection : direction;

        Vector3 vec = Vector3.center.copy();
        vec.add(v * Facing.offsetsXForSide[dir],
                v * Facing.offsetsYForSide[dir],
                v * Facing.offsetsZForSide[dir]
        );
        vec.add(pos.x, pos.y, pos.z);

        return vec;
    }

    @Override
    public boolean handleWaterMovement() {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }


    @Override
    public boolean handleLavaMovement() {
        return false;
    }

    @Override
    public void moveEntity(double p_70091_1_, double p_70091_3_, double p_70091_5_) {
        setPosition(0);
    }

    @Override
    public void addVelocity(double p_70024_1_, double p_70024_3_, double p_70024_5_) {

    }

    @Override
    public boolean isPushedByWater() {
        return false;
    }

    @Override
    public boolean canBePushed() {
        return false;
    }

}
