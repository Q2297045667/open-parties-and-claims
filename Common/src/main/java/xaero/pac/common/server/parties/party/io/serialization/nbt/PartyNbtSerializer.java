/*
 * Open Parties and Claims - adds chunk claims and player parties to Minecraft
 * Copyright (C) 2022-2023, Xaero <xaero1996@gmail.com> and contributors
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

package xaero.pac.common.server.parties.party.io.serialization.nbt;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import xaero.pac.common.parties.party.ally.PartyAlly;
import xaero.pac.common.parties.party.member.PartyInvite;
import xaero.pac.common.parties.party.member.PartyMember;
import xaero.pac.common.server.io.serialization.SimpleSerializer;
import xaero.pac.common.server.parties.party.PartyManager;
import xaero.pac.common.server.parties.party.ServerParty;
import xaero.pac.common.server.parties.party.io.serialization.nbt.member.PartyInviteNbtSerializer;
import xaero.pac.common.server.parties.party.io.serialization.nbt.member.PartyMemberNbtSerializer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class PartyNbtSerializer implements SimpleSerializer<CompoundTag, String, ServerParty, PartyManager>{
	
	private final PartyMemberNbtSerializer partyMemberNbtSerializer;
	private final PartyInviteNbtSerializer partyInviteNbtSerializer;

	public PartyNbtSerializer(PartyMemberNbtSerializer partyMemberNbtSerializer,
			PartyInviteNbtSerializer partyInviteNbtSerializer) {
		super();
		this.partyMemberNbtSerializer = partyMemberNbtSerializer;
		this.partyInviteNbtSerializer = partyInviteNbtSerializer;
	}

	@Override
	public CompoundTag serialize(ServerParty party) {
		CompoundTag result = new CompoundTag();
		result.put("owner", partyMemberNbtSerializer.serialize(party.getOwner()));
		result.putLong("confirmedActivity", party.getConfirmedActivity());
		ListTag membersTag = new ListTag();
		ListTag invitesTag = new ListTag();
		ListTag alliesTag = new ListTag();
		
		party.getInvitedPlayersStream().forEach(p -> invitesTag.add(partyInviteNbtSerializer.serialize(p)));
		party.getAllyPartiesStream().forEach(a -> alliesTag.add(NbtUtils.createUUID(a.getPartyId())));
		party.getMemberInfoStream().filter(mi -> mi != party.getOwner()).forEach(mi -> membersTag.add(partyMemberNbtSerializer.serialize((PartyMember) mi)));

		result.put("invites", invitesTag);
		result.put("allies", alliesTag);
		result.put("members", membersTag);
		return result;
	}

	@Override
	public ServerParty deserialize(String id, PartyManager manager, CompoundTag serializedData) {
		PartyMember owner = partyMemberNbtSerializer.deserialize(serializedData.getCompound("owner"), true);
		long confirmedActivity = serializedData.getLong("confirmedActivity");
		
		ListTag membersTag = serializedData.getList("members", Tag.TAG_COMPOUND);
		ListTag invitesTag = serializedData.getList("invites", Tag.TAG_COMPOUND);
		ListTag alliesTag = serializedData.getList("allies", Tag.TAG_INT_ARRAY);

		Map<UUID, PartyMember> members = new HashMap<>(32);
		Map<UUID, PartyInvite> invites = new HashMap<>(32);
		Map<UUID, PartyAlly> allies = new HashMap<>();
		membersTag.forEach(t -> {
			PartyMember member = partyMemberNbtSerializer.deserialize((CompoundTag) t, false);
			members.put(member.getUUID(), member);
		});
		invitesTag.forEach(t -> {
			PartyInvite invite = partyInviteNbtSerializer.deserialize((CompoundTag) t);
			invites.put(invite.getUUID(), invite);
		});
		alliesTag.forEach(t -> {
			UUID ally = NbtUtils.loadUUID(t);
			allies.put(ally, new PartyAlly(ally));
		});
		ServerParty result = ServerParty.Builder.begin().setManagedBy(manager).setOwner(owner).setId(UUID.fromString(id)).setMemberInfo(members).setInvitedPlayers(invites).setAllyParties(allies).build();
		result.setConfirmedActivity(confirmedActivity);
		return result;
	}
	
	public static final class Builder {

		private Builder() {
		}

		private Builder setDefault() {
			return this;
		}

		public PartyNbtSerializer build() {
			return new PartyNbtSerializer(new PartyMemberNbtSerializer(), new PartyInviteNbtSerializer());
		}

		public static Builder begin() {
			return new Builder().setDefault();
		}

	}

}
