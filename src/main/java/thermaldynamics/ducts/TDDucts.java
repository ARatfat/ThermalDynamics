package thermaldynamics.ducts;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import thermaldynamics.ThermalDynamics;
import thermaldynamics.ducts.Duct.Type;

public class TDDucts {

	private TDDucts() {

	}

	public static ArrayList<Duct> ductList = new ArrayList<Duct>();
	public static ArrayList<Duct> ductListSorted = null;

	static Duct addDuct(int id, boolean opaque, int pathWeight, int type, String name, Type ductType, DuctFactory factory, String baseTexture,
                        String connectionTexture, String fluidTexture, int fluidTransparency, String frameTexture, String frameFluidTexture, int frameFluidTransparency, boolean pathModifiable) {

		Duct newDuct = new Duct(id, opaque, pathWeight, type, name, ductType, factory, baseTexture, connectionTexture, fluidTexture, fluidTransparency,
				frameTexture, frameFluidTexture, frameFluidTransparency, pathModifiable);

        while (id < ductList.size()) ductList.add(null);
        Duct oldDuct = ductList.set(id, newDuct);
        if (oldDuct != null)
            ThermalDynamics.log.info("Replacing " + oldDuct.unlocalizedName + " with " + newDuct.unlocalizedName);
        return newDuct;
	}

	public static Duct getDuct(int id) {

		if (isValid(id)) {
			return ductList.get(id);
		} else {
			return structure;
		}
	}

	public static List<Duct> getSortedDucts() {

		if (ductListSorted == null) {
			ductListSorted = new ArrayList<Duct>();
			for (Duct duct : ductList) {
				if (duct != null) {
					ductListSorted.add(duct);
				}
			}
			Collections.sort(ductListSorted, new Comparator<Duct>() {

				@Override
				public int compare(Duct o1, Duct o2) {

					int i = o1.ductType.compareTo(o2.ductType);
					if (i == 0) {
						i = o1.compareTo(o2);
					}
					return i;
				}
			});
		}
		return ductListSorted;
	}

	public static boolean isValid(int id) {

		return id < ductList.size() && ductList.get(id) != null;
	}

	public static Duct getType(int id) {

		return ductList.get(id) != null ? ductList.get(id) : structure;
	}

	public static boolean addDucts() {

		addEnergyDucts();
		addFluidDucts();
		addItemDucts();
		addSupportDucts();

		return true;
	}

	static void addEnergyDucts() {

		energyBasic = addDuct(OFFSET_ENERGY + 0, false, 1, 0, "energyBasic", Type.ENERGY, DuctFactory.energy, "lead", "lead", Duct.REDSTONE_BLOCK, 255, null,
				null, 255, false);

		energyHardened = addDuct(OFFSET_ENERGY + 1, false, 1, 1, "energyHardened", Type.ENERGY, DuctFactory.energy, "invar", "invar", Duct.REDSTONE_BLOCK, 255,
				null, null, 0, false);

		energyReinforced = addDuct(OFFSET_ENERGY + 2, false, 1, 2, "energyReinforced", Type.ENERGY, DuctFactory.energy, "electrum", "electrum",
				"thermalfoundation:fluid/Fluid_Redstone_Still", 192, null, null, 0, false);
		energyReinforcedEmpty = addDuct(OFFSET_ENERGY + 3, false, 1, -1, "energyReinforcedEmpty", Type.STRUCTURAL, DuctFactory.structural, "electrum",
				"electrum", null, 0, null, null, 0, false);

		energyResonant = addDuct(OFFSET_ENERGY + 3, false, 1, 3, "energyResonant", Type.ENERGY, DuctFactory.energy, "enderium", "enderium",
				"thermalfoundation:fluid/Fluid_Redstone_Still", 192, null, null, 0, false);
		energyResonantEmpty = addDuct(OFFSET_ENERGY + 5, false, 1, -1, "energyResonant", Type.STRUCTURAL, DuctFactory.energy, "enderium", "enderium", null, 0,
				null, null, 0, false);

		energySuperCond = addDuct(OFFSET_ENERGY + 4, false, 1, 4, "energySuperconductor", Type.ENERGY, DuctFactory.energy_super, "electrum", "electrum",
				"thermalfoundation:fluid/Fluid_Redstone_Still", 192, "electrum", "thermalfoundation:fluid/Fluid_Cryotheum_Still", 128, false);
		energySuperCondEmpty = addDuct(OFFSET_ENERGY + 7, false, 1, -1, "energySuperconductorEmpty", Type.STRUCTURAL, DuctFactory.structural, "electrum",
				"electrum", "thermalfoundation:fluid/Fluid_Redstone_Still", 192, "electrum", null, 0, false);
	}

	static void addFluidDucts() {

		fluidBasic = addDuct(OFFSET_FLUID + 0, false, 1, 0, "fluidBasic", Type.FLUID, DuctFactory.fluid_fragile, "copper", "copper", null, 0, null, null, 0, false);
		fluidBasicOpaque = addDuct(OFFSET_FLUID + 1, true, 1, 0, "fluidBasic", Type.FLUID, DuctFactory.fluid_fragile, "copper", "copper", null, 0, null, null,
				0, false);

		fluidHardened = addDuct(OFFSET_FLUID + 2, false, 1, 1, "fluidHardened", Type.FLUID, DuctFactory.fluid, "invar", "invar", null, 0, null, null, 0, false);
		fluidHardenedOpaque = addDuct(OFFSET_FLUID + 3, true, 1, 1, "fluidHardened", Type.FLUID, DuctFactory.fluid, "invar", "invar", null, 0, null, null, 0, false);

		// fluidEnergy = addDuct(OFFSET_FLUID + 4, false, 1, 1, "fluidFlux", Type.FLUID, DuctFactory.fluid_flux, "invar", "invar", null, 0, Duct.SIDE_DUCTS,
		// "thermalfoundation:fluid/Fluid_Redstone_Still", 192);
		// fluidEnergyEmpty = addDuct(OFFSET_FLUID + 5, false, 1, 1, "fluidFluxEmpty", Type.STRUCTURAL, DuctFactory.structural, "invar", "invar", null, 0,
		// Duct.SIDE_DUCTS, null, 0);
		//
		// fluidEnergyOpaque = addDuct(OFFSET_FLUID + 6, true, 1, 1, "fluidFlux", Type.FLUID, DuctFactory.fluid_flux, "invar", "invar", null, 0,
		// Duct.SIDE_DUCTS,
		// "thermalfoundation:fluid/Fluid_Redstone_Still", 192);
		// fluidEnergyOpaqueEmpty = addDuct(OFFSET_FLUID + 7, true, 1, 1, "fluidFluxEmpty", Type.STRUCTURAL, DuctFactory.structural, "invar", "invar", null, 0,
		// Duct.SIDE_DUCTS, null, 0);
	}

	static void addItemDucts() {

		itemBasic = addDuct(OFFSET_ITEM + 0, false, 1, 0, "itemBasic", Type.ITEM, DuctFactory.item, "tin", "tin", null, 0, null, null, 0, false);

		itemBasicOpaque = addDuct(OFFSET_ITEM + 1, true, 1, 0, "itemBasic", Type.ITEM, DuctFactory.item, "tin", "tin", null, 0, null, null, 0, true);

		itemFast = addDuct(OFFSET_ITEM + 2, false, 1, 1, "itemFast", Type.ITEM, DuctFactory.item, "tin", "tin",
				"thermalfoundation:fluid/Fluid_Glowstone_Still", 128, null, null, 0, true);

		itemFastOpaque = addDuct(OFFSET_ITEM + 3, true, 1, 1, "itemFast", Type.ITEM, DuctFactory.item, "tin_1", "tin", null, 0, null, null, 0, true);

		itemEnder = addDuct(OFFSET_ITEM + 4, false, 0, 2, "itemEnder", Type.ITEM, DuctFactory.item_ender, "enderium", "enderium", null, 48, null, null, 0, true);

		itemEnderOpaque = addDuct(OFFSET_ITEM + 5, true, 0, 2, "itemEnder", Type.ITEM, DuctFactory.item_ender, "enderium", "enderium", null, 48, null, null, 0, true);

		// itemEnergy = addDuct(OFFSET_ITEM + 24, false, 1, 3, "itemFlux", Type.ITEM, DuctFactory.item_flux, "tin", "tin",
		// "thermalfoundation:fluid/Fluid_Redstone_Still", 48, null, null, 0);
		//
		// itemEnergyDense = addDuct(OFFSET_ITEM + 25, false, 1000, 3, "itemFlux", Type.ITEM, DuctFactory.item_flux, "tin", "tin",
		// "thermalfoundation:fluid/Fluid_Redstone_Still", 48, null, null, 0);
		//
		// itemEnergyVacuum = addDuct(OFFSET_ITEM + 26, false, -1000, 3, "itemFlux", Type.ITEM, DuctFactory.item_flux, "tin", "tin",
		// "thermalfoundation:fluid/Fluid_Redstone_Still", 48, null, null, 0);
		//
		// itemEnergyOpaque = addDuct(OFFSET_ITEM + 28, true, 1, 3, "itemFlux", Type.ITEM, DuctFactory.item_flux, "tin_2", "tin", null, 0, null, null, 0);
	}

	static void addSupportDucts() {

		structure = addDuct(OFFSET_STRUCTURE + 0, true, 1, -1, "structure", Type.STRUCTURAL, DuctFactory.structural, "support", null, null, 0, null, null, 0, false);
	}

	public static int OFFSET_ENERGY = 0 * 16;
	public static int OFFSET_FLUID = 1 * 16;
	public static int OFFSET_ITEM = 2 * 16;
	public static int OFFSET_STRUCTURE = 4 * 16;

	/* ENERGY */
	public static Duct energyBasic;

	public static Duct energyHardened;

	public static Duct energyReinforced;
	public static Duct energyReinforcedEmpty;

	public static Duct energyResonant;
	public static Duct energyResonantEmpty;

	public static Duct energySuperCond;
	public static Duct energySuperCondEmpty;

	/* FLUID */
	public static Duct fluidBasic;
	public static Duct fluidBasicOpaque;

	public static Duct fluidHardened;
	public static Duct fluidHardenedOpaque;

	// public static Duct fluidEnergy;
	// public static Duct fluidEnergyEmpty;
	//
	// public static Duct fluidEnergyOpaque;
	// public static Duct fluidEnergyOpaqueEmpty;

	/* ITEM */
	public static Duct itemBasic;
	public static Duct itemBasicDense;
	public static Duct itemBasicVacuum;

	public static Duct itemBasicOpaque;
	public static Duct itemBasicOpaqueDense;
	public static Duct itemBasicOpaqueVacuum;

	public static Duct itemFast;
	public static Duct itemFastDense;
	public static Duct itemFastVacuum;

	public static Duct itemFastOpaque;
	public static Duct itemFastOpaqueDense;
	public static Duct itemFastOpaqueVacuum;

	public static Duct itemEnder;
	public static Duct itemEnderDense;
	public static Duct itemEnderVacuum;

	public static Duct itemEnderOpaque;
	public static Duct itemEnderOpaqueDense;
	public static Duct itemEnderOpaqueVacuum;

	// public static Duct itemEnergy;
	// public static Duct itemEnergyDense;
	// public static Duct itemEnergyVacuum;
	//
	// public static Duct itemEnergyOpaque;
	// public static Duct itemEnergyOpaqueDense;
	// public static Duct itemEnergyOpaqueVacuum;

	/* STRUCTURE */
	public static Duct structure;

	/* HELPERS - NOT REAL */
	public static Duct structureInvis = new Duct(-1, false, 1, -1, "structure", Type.STRUCTURAL, DuctFactory.structural, "support", null, null, 0, null, null,
			0, false);

	public static Duct placeholder = new Duct(-1, false, 1, -1, "structure", Type.STRUCTURAL, DuctFactory.structural, "support", null, null, 0, null, null, 0, false);

}
