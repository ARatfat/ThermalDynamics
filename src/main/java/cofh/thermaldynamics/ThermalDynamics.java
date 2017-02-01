package cofh.thermaldynamics;

import cofh.CoFHCore;
import cofh.api.core.IInitializer;
import cofh.core.init.CoreProps;
import cofh.core.util.ConfigHandler;
import cofh.lib.util.helpers.MathHelper;
import cofh.thermaldynamics.proxy.Proxy;
import cofh.thermaldynamics.core.TDProps;
import cofh.thermaldynamics.core.TickHandler;
import cofh.thermaldynamics.debughelper.CommandThermalDebug;
import cofh.thermaldynamics.debughelper.DebugHelper;
import cofh.thermaldynamics.debughelper.PacketDebug;
import cofh.thermaldynamics.duct.BlockDuct;
import cofh.thermaldynamics.duct.TDDucts;
import cofh.thermaldynamics.duct.entity.TileTransportDuctCrossover;
import cofh.thermaldynamics.gui.GuiHandler;
import cofh.thermaldynamics.gui.TDCreativeTab;
import cofh.thermaldynamics.gui.TDCreativeTabCovers;
import cofh.thermaldynamics.item.*;
import cofh.thermaldynamics.util.crafting.RecipeCover;
import cofh.thermaldynamics.util.crafting.TDCrafting;
import cofh.thermalfoundation.ThermalFoundation;
import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.CustomProperty;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.*;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.oredict.RecipeSorter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.LinkedList;

@Mod(modid = ThermalDynamics.MOD_ID, name = ThermalDynamics.MOD_NAME, version = ThermalDynamics.VERSION, dependencies = ThermalDynamics.DEPENDENCIES, guiFactory = ThermalDynamics.MOD_GUI_FACTORY, customProperties = @CustomProperty(k = "cofhversion", v = "true"))
public class ThermalDynamics {

	public static final String MOD_ID = "ThermalDynamics";
	public static final String MOD_NAME = "Thermal Dynamics";

	public static final String VERSION = "1.2.0";
	public static final String VERSION_MAX = "1.3.0";
	public static final String VERSION_GROUP = "required-after:" + MOD_ID + "@[" + VERSION + "," + /*VERSION_MAX +*/ ");";

	public static final String DEPENDENCIES = CoFHCore.VERSION_GROUP + ThermalFoundation.VERSION_GROUP;
	public static final String MOD_GUI_FACTORY = "cofh.thermaldynamics.gui.GuiConfigTDFactory";

	@Instance(MOD_ID)
	public static ThermalDynamics instance;

	@SidedProxy(clientSide = "cofh.thermaldynamics.proxy.ProxyClient", serverSide = "cofh.thermaldynamics.proxy.Proxy")
	public static Proxy proxy;

	public static final Logger log = LogManager.getLogger(MOD_ID);
	public static final ConfigHandler config = new ConfigHandler(VERSION);
	public static final ConfigHandler configClient = new ConfigHandler(VERSION);
	public static final GuiHandler guiHandler = new GuiHandler();

	public static CreativeTabs tabCommon;
	public static CreativeTabs tabCovers;


    public static BlockDuct[] blockDuct;
    public static ItemServo itemServo;
    public static ItemFilter itemFilter;
    public static ItemCover itemCover;
    public static ItemRetriever itemRetriever;
    public static ItemRelay itemRelay;

	/* INIT SEQUENCE */
	public ThermalDynamics() {

		super();
	}

	@EventHandler
	public void preInit(FMLPreInitializationEvent event) {

		//UpdateManager.registerUpdater(new UpdateManager(this, RELEASE_URL, CoFHProps.DOWNLOAD_URL));
		config.setConfiguration(new Configuration(new File(CoreProps.configDir, "/cofh/thermaldynamics/common.cfg"), true));
		configClient.setConfiguration(new Configuration(new File(CoreProps.configDir, "cofh/thermaldynamics/client.cfg"), true));

		tabCommon = new TDCreativeTab();

		RecipeSorter.register("thermaldynamics:cover", RecipeCover.class, RecipeSorter.Category.UNKNOWN, "after:forge:shapedore");

		configOptions();

		TDDucts.addDucts();

		int numBlocks = (int) Math.ceil(TDDucts.ductList.size() / 16.0);
		blockDuct = new BlockDuct[numBlocks];
		for (int i = 0; i < numBlocks; i++) {
			blockDuct[i] = addBlock(new BlockDuct(i));
		}
		itemServo = addItem(new ItemServo());
		itemFilter = addItem(new ItemFilter());
		itemCover = addItem(new ItemCover());
		itemRetriever = addItem(new ItemRetriever());
		itemRelay = addItem(new ItemRelay());

		for (IInitializer initializer : initializerList) {
			initializer.preInit();
		}

        proxy.preInit();
	}

	@EventHandler
	public void initialize(FMLInitializationEvent event) {

		NetworkRegistry.INSTANCE.registerGuiHandler(instance, guiHandler);
		MinecraftForge.EVENT_BUS.register(proxy);

		for (IInitializer initializer : initializerList) {
			initializer.initialize();
		}

		MinecraftForge.EVENT_BUS.register(TickHandler.instance);

        proxy.init();

		PacketDebug.initialize();
		DebugHelper.initialize();
	}

	@EventHandler
	public void postInit(FMLPostInitializationEvent event) {

		for (IInitializer initializer : initializerList) {
			initializer.postInit();
		}
		TDCrafting.loadRecipes();

        proxy.postInit();
	}

	@EventHandler
	public void loadComplete(FMLLoadCompleteEvent event) {

		config.cleanUp(false, true);
		configClient.cleanUp(false, true);

		log.info("Thermal Dynamics: Load Complete.");
	}

	@EventHandler
	public void serverStarting(FMLServerStartingEvent event) {

		if (DebugHelper.debug) {
			event.registerServerCommand(new CommandThermalDebug());
		}
	}

	/* LOADING FUNCTIONS */
	void configOptions() {

		/* Duct */
		String category = "Duct.Transport";
		String comment = "Must be between 0 and 120 ticks.";
		TileTransportDuctCrossover.CHARGE_TIME = (byte) MathHelper.clamp(
				ThermalDynamics.config.get(category, "CrossoverChargeTime", TileTransportDuctCrossover.CHARGE_TIME, comment), 0,
				TileTransportDuctCrossover.CHARGE_TIME);

		/* Models */
		comment = "This value affects the size of the inner duct model, such as fluids. Lower it if you experience texture z-fighting.";
		TDProps.smallInnerModelScaling = MathHelper.clamp((float) ThermalDynamics.configClient.get("Render", "InnerModelScaling", 0.99, comment), 0.50F, 0.99F);

		comment = "This value affects the size of the inner duct model, such as fluids, on the large (octagonal) ducts. Lower it if you experience texture z-fighting.";
		TDProps.largeInnerModelScaling = MathHelper.clamp((float) ThermalDynamics.configClient.get("Render", "LargeInnerModelScaling", 0.99, comment), 0.50F,
				0.99F);

		/* Interface */
		ItemCover.enableCreativeTab = ThermalDynamics.configClient.get("Interface.CreativeTab", "Covers.Enable", ItemCover.enableCreativeTab);

		if (ItemCover.enableCreativeTab) {
			tabCovers = new TDCreativeTabCovers();
		}
		ItemCover.showInNEI = ThermalDynamics.configClient.get("Plugins.NEI", "Covers.Show", ItemCover.showInNEI, "Set to TRUE to show Covers in NEI.");
	}

	LinkedList<IInitializer> initializerList = new LinkedList<IInitializer>();

	public <T extends Block & IInitializer> T addBlock(T a) {

		initializerList.add(a);
		return a;
	}

	public <T extends Item & IInitializer> T addItem(T a) {

		initializerList.add(a);
		return a;
	}

	/* BaseMod */
	public String getModId() {

		return MOD_ID;
	}

	public String getModName() {

		return MOD_NAME;
	}

	public String getModVersion() {

		return VERSION;
	}

	@EventHandler
	public void checkMappings(FMLMissingMappingsEvent event) {

		for (FMLMissingMappingsEvent.MissingMapping map : event.get()) {
			if ((MOD_ID + ":TestDuct").equals(map.name)) {
				if (map.type == GameRegistry.Type.BLOCK) {
					map.remap(blockDuct[0]);
				} else {
					map.remap(Item.getItemFromBlock(blockDuct[0]));
				}
			}
		}
	}

}
