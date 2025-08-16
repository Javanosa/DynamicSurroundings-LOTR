/*
 * This file is part of Dynamic Surroundings, licensed under the MIT License (MIT).
 *
 * Copyright (c) OreCruncher
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.blockartistry.mod.DynSurround.client;

import org.blockartistry.mod.DynSurround.Module;
import org.blockartistry.mod.DynSurround.client.EnvironStateHandler.EnvironState;
import org.blockartistry.mod.DynSurround.data.FakeBiome;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gnu.trove.map.hash.TObjectIntHashMap;
import lotr.common.LOTRDimension;
import lotr.common.world.LOTRWorldChunkManager;
import lotr.common.world.biome.LOTRBiome;
import lotr.common.world.biome.variant.LOTRBiomeVariant;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.biome.WorldChunkManager;

@SideOnly(Side.CLIENT)
public final class BiomeSurveyHandler implements IClientEffectHandler {

	private static final int BIOME_SURVEY_RANGE = 6;

	private static int area;
	private static final TObjectIntHashMap<BiomeDataMutable> weights = new TObjectIntHashMap<BiomeDataMutable>();

	private static BiomeGenBase lastPlayerBiome = null;
	private static int lastDimension = 0;
	private static int lastPlayerX = 0;
	private static int lastPlayerY = 0;
	private static int lastPlayerZ = 0;

	public static int getArea() {
		return area;
	}

	public static TObjectIntHashMap<BiomeDataMutable> getBiomes() {
		return weights;
	}
	
	// unpack
	public static final BiomeGenBase getBiome(final int key) {
		final int dimIndex = key >> 8 & 255;
		
		final BiomeGenBase[] biomes = dimIndex > 0 ? LOTRDimension.values()[dimIndex].biomeList : BiomeGenBase.getBiomeGenArray();
		return biomes[key & 255];
	}
	
	public static final int createKey(final BiomeGenBase biomeIn, final LOTRBiomeVariant variantIn) {
		return biomeIn.biomeID |
		  ((Module.lotr && biomeIn instanceof LOTRBiome) 
				  ? ((LOTRBiome) biomeIn).biomeDimension.ordinal() >> 8 | variantIn.variantID >> 16
				  : 0
		  );
	}
	
	// pack
	public static final LOTRBiomeVariant getVariant(final int key) {
		return LOTRBiomeVariant.getVariantForID(key >> 16);
	}
	
	// pack #2
	public static final class BiomeData {
		  public final BiomeGenBase biome;
		  public final LOTRBiomeVariant variant;
		  private final int hash;
		  
		  public BiomeData(final BiomeGenBase biomeIn, final LOTRBiomeVariant variantIn) {
			  biome = biomeIn;
			  variant = variantIn;
			  hash = createKey(biomeIn, variantIn);
		  }
		  
		  @Override
		  public final int hashCode() {
			  return hash;
		  }
	}
	
	public static final class BiomeDataMutable {
		  public BiomeGenBase biome;
		  public LOTRBiomeVariant variant;
		  private int hash;
		  
		  public final BiomeDataMutable set(final BiomeGenBase biomeIn, final LOTRBiomeVariant variantIn) {
			  biome = biomeIn;
			  variant = variantIn;
			  hash = createKey(biomeIn, variantIn);
			  return this;
		  }
		  
		  @Override
		  public final int hashCode() {
			  return hash;
		  }
	}
	
	public static BiomeDataMutable cacheInstance = new BiomeDataMutable();

	private static void doSurvey(final EntityPlayer player, final int range) {
		area = 0;
		weights.clear();

		if (EnvironState.getPlayerBiome() instanceof FakeBiome) {
			area = 1;
			if(weights.put(cacheInstance.set(EnvironState.getPlayerBiome(), null), 1) == 1) {
				// key didnt existed yet
				cacheInstance = new BiomeDataMutable();
			}
		} 
		
		else {
			WorldChunkManager chunkManager = player.worldObj.getWorldChunkManager();
			
			boolean lookupVariants = Module.lotr && chunkManager instanceof LOTRWorldChunkManager; 
			
			final int x = MathHelper.floor_double(player.posX);
			final int z = MathHelper.floor_double(player.posZ);

			for (int dX = -range; dX <= range; dX++)
				for (int dZ = -range; dZ <= range; dZ++) {
					area++;
					final BiomeGenBase biome = player.worldObj.getBiomeGenForCoords(x + dX, z + dZ);
					LOTRBiomeVariant variant = null;
					if(lookupVariants) {
						variant = ((LOTRWorldChunkManager) chunkManager).getBiomeVariantAt(x + dX, z + dZ);
					}
					
					if(weights.adjustOrPutValue(cacheInstance.set(biome, variant), 1, 1) == 1) {
						// key didnt existed yet
						cacheInstance = new BiomeDataMutable();
					}
				}
		}
	}

	@Override
	public void process(World world, EntityPlayer player) {
		final int playerX = MathHelper.floor_double(player.posX);
		final int playerY = MathHelper.floor_double(player.posY);
		final int playerZ = MathHelper.floor_double(player.posZ);

		if (lastDimension != EnvironState.getDimensionId() || playerX != lastPlayerX || playerY != lastPlayerY
				|| playerZ != lastPlayerZ || lastPlayerBiome != EnvironState.getPlayerBiome()) {
			lastPlayerBiome = EnvironState.getPlayerBiome();
			lastDimension = EnvironState.getDimensionId();
			lastPlayerX = playerX;
			lastPlayerY = playerY;
			lastPlayerZ = playerZ;
			doSurvey(player, BIOME_SURVEY_RANGE);
		}
	}

	@Override
	public boolean hasEvents() {
		return false;
	}

}
