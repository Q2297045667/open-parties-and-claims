/*
 * Open Parties and Claims - adds chunk claims and player parties to Minecraft
 * Copyright (C) 2022, Xaero <xaero1996@gmail.com> and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of version 3 of the GNU Lesser General Public License
 * (LGPL-3.0-only) as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received copies of the GNU Lesser General Public License
 * and the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 */

package xaero.pac.common.registry.entity;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.stream.Stream;

public class EntityRegistryForge implements IEntityRegistry {

	@Override
	public EntityType<?> getValue(ResourceLocation id) {
		return ForgeRegistries.ENTITY_TYPES.getValue(id);
	}

	@Override
	public Stream<EntityType<?>> getTagStream(TagKey<EntityType<?>> tagKey) {
		return ForgeRegistries.ENTITY_TYPES.tags().getTag(tagKey).stream();
	}

	@Override
	public ResourceLocation getKey(EntityType<?> entity) {
		return ForgeRegistries.ENTITIES.getKey(entity);
	}

	@Override
	public Iterable<EntityType<?>> getIterable(){
		return ForgeRegistries.ENTITIES.getValues();
	}

	@Override
	public Iterable<TagKey<EntityType<?>>> getTagIterable(){
		return ForgeRegistries.ENTITIES.tags().getTagNames().toList();
	}

}
