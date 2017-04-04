package cofh.thermaldynamics.duct.tiles;

import cofh.thermaldynamics.duct.Duct;
import cofh.thermaldynamics.duct.TDDucts;
import cofh.thermaldynamics.duct.energy.DuctUnitEnergySuper;
import cofh.thermaldynamics.duct.nutypeducts.DuctToken;
import cofh.thermaldynamics.duct.nutypeducts.DuctUnit;
import cofh.thermaldynamics.duct.nutypeducts.TileGridSingle;

public class TileEnergySuperDuct extends TileGridSingle {

	public TileEnergySuperDuct() {
		super(DuctToken.ENERGY, TDDucts.energySuperCond);
	}

	@Override
	public DuctUnit createDuctUnit(DuctToken token, Duct ductType) {
		return new DuctUnitEnergySuper(this, ductType, 1000, 1000);
	}


}