package org.blockartistry.mod.DynSurround.lotr;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.blockartistry.mod.DynSurround.data.BiomeRegistry;

import lotr.common.LOTRDimension;
import lotr.common.world.biome.LOTRBiome;
import lotr.common.world.biome.variant.LOTRBiomeVariant;
import lotr.common.world.biome.variant.LOTRBiomeVariantList;

public class LOTRHelper {
	private final static int[] VARIANT_ARR = new int[] {
	
	// STANDARD 	FLOWERS 	FOREST 	FOREST_LIGHT 	STEPPE 	STEPPE_BARREN 	HILLS 	HILLS_FOREST 	MOUNTAIN 	CLEARING 
	   0, 			0, 			2, 	   	2,				4, 	   	4,			 	6,    	2,           	6,		 	9,	
	   
	// DENSEFOREST_OAK 	DENSEFOREST_SPRUCE 	DENSEFOREST_OAK_SPRUCE 	DEADFOREST_OAK 	DEADFOREST_SPRUCE 	DEADFOREST_OAK_SPRUCE
	   2, 			   	2, 					2,						13, 			13, 				13,	
	// SHRUBLAND_OAK 	DENSEFOREST_BIRCH
	   2, 				2,
	// SWAMP_LOWLAND 	SWAMP_UPLAND 		SAVANNAH_BAOBAB 		LAKE 			DENSEFOREST_LEBETHRON 	BOULDERS_RED 	BOULDERS_ROHAN 	
	   18, 				18,					0, 						41, 			2,						0, 				0, 				
	// JUNGLE_DENSE 	VINEYARD 			FOREST_ASPEN 			FOREST_BIRCH 
	   2, 				0,					2, 						2,	
	   
	// FOREST_BEECH 	FOREST_MAPLE 		FOREST_LARCH 	FOREST_PINE 	ORCHARD_SHIRE 	ORCHARD_APPLE_PEAR 	ORCHARD_ORANGE 	ORCHARD_LEMON 	
	   2, 				2, 					2, 				2, 				2, 				2, 					2, 				2, 				
	// ORCHARD_LIME 	ORCHARD_ALMOND 		ORCHARD_OLIVE 	ORCHARD_PLUM
	   2, 				2, 					2, 				2,
	
	// RIVER 	SCRUBLAND 		HILLS_SCRUBLAND 		WASTELAND 		ORCHARD_DATE 	DENSEFOREST_DARK_OAK 	ORCHARD_POMEGRANATE		
	   41,  	0, 				6, 						4, 				2, 				2, 						2,												
	// DUNES 	SCRUBLAND_SAND 	HILLS_SCRUBLAND_SAND 	WASTELAND_SAND
	   4, 		4,				6, 						4
			
			
	// IDs		
	// none		forest 	steppe	hills	clearing	deadForest_oak	swampLowland	lake	RIVER
	// 0 		2		4		6		9			13				18				21		41

	};
	
	public static final int getVariantGroup(final int variantid) {
		if(variantid < VARIANT_ARR.length)
			return VARIANT_ARR[variantid];
		return variantid;
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void registerLOTRBiomes() {
		
		try {
			// reflection
			Field fieldVariant = Class.forName("lotr.common.world.biome.variant.LOTRBiomeVariantList$VariantBucket").getDeclaredField("variant");
			fieldVariant.setAccessible(true);
			 
			Field fieldVariantList = LOTRBiomeVariantList.class.getDeclaredField("variantList");
			fieldVariantList.setAccessible(true);
			// scan
			for(LOTRDimension dim : LOTRDimension.values()) {
				for(LOTRBiome biome : dim.biomeList) {
					if(biome != null) {
						List<LOTRBiomeVariant> variants = new ArrayList<>();

						List bucketsLarge = (List) fieldVariantList.get(biome.getBiomeVariantsLarge());
						List bucketsSmall = (List) fieldVariantList.get(biome.getBiomeVariantsSmall());
						  
						bucketsLarge.addAll(bucketsSmall);
						  
						for(Object variantBucket : bucketsLarge) {
							  
							  LOTRBiomeVariant variant = LOTRBiomeVariant.getVariantForID(getVariantGroup(((LOTRBiomeVariant) fieldVariant.get(variantBucket)).variantID));
							  
							  if(!variants.contains(variant)) {
								 
								  variants.add(variant);
							  }
						}
	
					  // for some reason rivers can appearin the layer generator even while disabled in this biome
					  variants.add(LOTRBiomeVariant.RIVER);
					  if(biome.getEnableRiver()) {
						  variants.add(LOTRBiomeVariant.LAKE);
					  }

					  for(LOTRBiomeVariant variant : variants) {
						  BiomeRegistry.registerEntry(1000 + biome.biomeID + (variant.variantID*10000), biome);
					  }
					  
					  // Standard biom variant
					  
					  BiomeRegistry.registerEntry(1000 + biome.biomeID, biome);
					  
					}
				}
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}
