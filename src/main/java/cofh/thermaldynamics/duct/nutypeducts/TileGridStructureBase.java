package cofh.thermaldynamics.duct.nutypeducts;

import cofh.thermaldynamics.duct.DuctUnitStructural;
import cofh.thermaldynamics.multiblock.MultiBlockGrid;
import cofh.thermaldynamics.util.TickHandler;
import com.google.common.collect.ImmutableMap;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

import javax.annotation.Nullable;
import javax.annotation.OverridingMethodsMustInvokeSuper;
import java.util.Map;

public abstract class TileGridStructureBase extends TileGrid {


	private Map<DuctToken, DuctUnit> ducts;

	@SuppressWarnings("unchecked")
	@Nullable
	@Override
	@OverridingMethodsMustInvokeSuper
	public <T extends DuctUnit<T, G, C>, G extends MultiBlockGrid<T>, C> T getDuct(DuctToken<T, G, C> token) {
		DuctUnit ductUnit = ducts.get(token);
		if (ductUnit == null && token == DuctToken.STRUCTURAL) {
			ImmutableMap.Builder<DuctToken, DuctUnit> builder = ImmutableMap.builder();
			builder.putAll(ducts);
			DuctUnitStructural structural;
			builder.put(DuctToken.STRUCTURAL, structural = new DuctUnitStructural(this, ducts.get(getPrimaryDuctToken())));
			TickHandler.addMultiBlockToCalculate(structural);
			ducts = builder.build();
			return (T)structural;
		}
		return (T) ductUnit;
	}

	protected abstract DuctToken getPrimaryDuctToken();

	public DuctUnit getPrimaryDuctUnit(){
		return getDuct(getPrimaryDuctToken());
	}

	@Override
	public Iterable<DuctUnit> getDuctUnits() {
		return ducts.values();
	}


	@Override
	public boolean isPowered() {
		DuctUnitStructural ductUnit = (DuctUnitStructural) ducts.get(DuctToken.STRUCTURAL);
		if (ductUnit != null && ductUnit.grid != null && ductUnit.grid.rs != null) {
			if (ductUnit.grid.rs.redstoneLevel > 0) {
				return true;
			}
		}

		return super.isPowered();
	}

	@Override
	public TextureAtlasSprite getBaseIcon() {
		return getPrimaryDuctUnit().getBaseIcon();
	}
}
