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

package xaero.pac.common.event;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TagsUpdatedEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.PlayerTickEvent;
import net.minecraftforge.event.TickEvent.ServerTickEvent;
import net.minecraftforge.event.entity.*;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.event.entity.player.*;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerRespawnEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.ExplosionEvent;
import net.minecraftforge.eventbus.api.Event.Result;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.server.permission.events.PermissionGatherEvent;
import org.apache.commons.lang3.tuple.Pair;
import xaero.pac.OpenPartiesAndClaims;
import xaero.pac.common.claims.player.IPlayerChunkClaim;
import xaero.pac.common.claims.player.IPlayerClaimPosList;
import xaero.pac.common.claims.player.IPlayerDimensionClaims;
import xaero.pac.common.event.api.OPACServerAddonRegisterEvent;
import xaero.pac.common.parties.party.IPartyPlayerInfo;
import xaero.pac.common.parties.party.ally.IPartyAlly;
import xaero.pac.common.parties.party.member.IPartyMember;
import xaero.pac.common.server.IServerData;
import xaero.pac.common.server.claims.IServerClaimsManager;
import xaero.pac.common.server.claims.IServerDimensionClaimsManager;
import xaero.pac.common.server.claims.IServerRegionClaims;
import xaero.pac.common.server.claims.player.IServerPlayerClaimInfo;
import xaero.pac.common.server.core.ServerCore;
import xaero.pac.common.server.data.ServerDataReloadListenerForge;
import xaero.pac.common.server.parties.party.IServerParty;
import xaero.pac.common.server.player.permission.impl.ForgePermissionsSystem;

public class CommonEventsForge extends CommonEvents {

	public CommonEventsForge(OpenPartiesAndClaims modMain) {
		super(modMain);
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void onEntityPlaceBlock(BlockEvent.EntityPlaceEvent event) {
		if(super.onEntityPlaceBlock(event.getWorld(), event.getPos(), event.getEntity(), event.getPlacedBlock(), event.getBlockSnapshot().getReplacedBlock()))
			event.setCanceled(true);
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void onEntityMultiPlaceBlock(BlockEvent.EntityMultiPlaceEvent event) {
		if(super.onEntityMultiPlaceBlock(event.getWorld(), event.getReplacedBlockSnapshots().stream().map(s -> Pair.of(s.getPos(), s.getCurrentBlock())), event.getEntity()))
			event.setCanceled(true);
	}

	@SubscribeEvent
	public void onServerAboutToStart(ServerAboutToStartEvent event) throws Throwable {
		super.onServerAboutToStart(event.getServer());
	}

	@SubscribeEvent
	public void onServerStarting(ServerStartingEvent event) {
		super.onServerStarting(event.getServer());
	}
	
	@SubscribeEvent
	public void onPlayerRespawn(PlayerRespawnEvent event) {
		super.onPlayerRespawn(event.getPlayer());
	}
	
	@SubscribeEvent
	public void onPlayerChangedDimension(PlayerChangedDimensionEvent event) {
		super.onPlayerChangedDimension(event.getPlayer());
	}
	
	@SubscribeEvent
	public void onPlayerLogIn(PlayerLoggedInEvent event) {
		super.onPlayerLogIn(event.getPlayer());
	}

	@SubscribeEvent
	public void onPlayerClone(PlayerEvent.Clone event) {
		super.onPlayerClone(event.getOriginal(), event.getPlayer());
	}
	
	@SubscribeEvent
	public void onPlayerLogOut(PlayerLoggedOutEvent event) {
		super.onPlayerLogOut(event.getPlayer());
	}
	
	@SubscribeEvent
	public void onServerTick(ServerTickEvent event) throws Throwable {
		//TODO probably need to stop using this event that doesn't provide the server instance
		if(lastServerStarted == null || !lastServerStarted.isSameThread())
			throw new RuntimeException("The last recorded server does not have the expected value!");
		super.onServerTick(lastServerStarted, event.phase == Phase.START);
	}
	
	@SubscribeEvent
	public void onPlayerTick(PlayerTickEvent event) throws Throwable {
		super.onPlayerTick(event.phase == Phase.START, event.side == LogicalSide.SERVER, event.player);
	}
	
	@SubscribeEvent
	public void onServerStopped(ServerStoppedEvent event) {
		super.onServerStopped(event.getServer());
	}
	
	@SubscribeEvent
	public void onRegisterCommands(RegisterCommandsEvent event) {
		super.onRegisterCommands(event.getDispatcher(), event.getEnvironment());
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
		if(super.onLeftClickBlock(event.getWorld(), event.getPos(), event.getPlayer()))
			event.setCanceled(true);
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void onDestroyBlock(BlockEvent.BreakEvent event) {
		if(super.onDestroyBlock(event.getWorld(), event.getPos(), event.getPlayer()))
			event.setCanceled(true);
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
		if(super.onRightClickBlock(event.getWorld(), event.getPos(), event.getPlayer(), event.getHand(), event.getHitVec()))
			event.setCanceled(true);
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void onItemRightClick(PlayerInteractEvent.RightClickItem event) {
		if(super.onItemRightClick(event.getWorld(), event.getPos(), event.getPlayer(), event.getHand(), event.getItemStack()))
			event.setCanceled(true);
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void onItemUseTick(LivingEntityUseItemEvent.Tick event) {
		if(super.onItemUseTick(event.getEntityLiving(), event.getItem()))
			event.setCanceled(true);
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void onItemUseTick(LivingEntityUseItemEvent.Stop event) {
		if(super.onItemUseStop(event.getEntityLiving(), event.getItem()))
			event.setCanceled(true);
	}

	@SubscribeEvent
	public void onMobGrief(EntityMobGriefingEvent event) {
		if(event.getEntity() == null)
			return;
		MinecraftServer server = event.getEntity().getServer();
		if(server == null)
			return;
		if(ServerCore.isMobGriefingForItems(server.getTickCount()))//this means that the mob griefing rule is being checked for item pickup
			return;
		if(super.onMobGrief(event.getEntity()))
			event.setResult(Result.DENY);
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void onLivingHurt(LivingAttackEvent event) {
		if(super.onLivingHurt(event.getSource(), event.getEntity()))
			event.setCanceled(true);
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void onEntityAttack(AttackEntityEvent event) {
		if(super.onEntityAttack(event.getPlayer(), event.getTarget()))
			event.setCanceled(true);
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
		if(super.onEntityInteract(event.getEntity(), event.getTarget(), event.getHand()))
			event.setCanceled(true);
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void onInteractEntitySpecific(PlayerInteractEvent.EntityInteractSpecific event) {
		if(super.onInteractEntitySpecific(event.getEntity(), event.getTarget(), event.getHand()))
			event.setCanceled(true);
	}
	
	@SubscribeEvent
	public void onExplosionDetonate(ExplosionEvent.Detonate event) {
		super.onExplosionDetonate(event.getWorld(), event.getExplosion(), event.getAffectedEntities(), event.getAffectedBlocks());
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void onChorusFruit(EntityTeleportEvent.ChorusFruit event){
		if(super.onChorusFruit(event.getEntity(), event.getTarget()))
			event.setCanceled(true);
	}

	@SubscribeEvent
	public void onEntityJoinWorld(EntityJoinWorldEvent event){
		if(super.onEntityJoinWorld(event.getEntity(), event.getWorld(), event.loadedFromDisk()))
			event.setCanceled(true);
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void onEntityEnteringSection(EntityEvent.EnteringSection event){
		super.onEntityEnteringSection(event.getEntity(), event.getOldPos(), event.getNewPos(), event.didChunkChange());
	}

	@SubscribeEvent
	public void onPermissionsChanged(PermissionsChangedEvent event){
		if(event.getPlayer() instanceof ServerPlayer serverPlayer)
			super.onPermissionsChanged(serverPlayer);
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void onCropTrample(BlockEvent.FarmlandTrampleEvent event) {
		if(super.onCropTrample(event.getEntity(), event.getPos()))
			event.setCanceled(true);
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void onBucketUse(FillBucketEvent event){
		if(super.onBucketUse(event.getEntity(), event.getWorld(), event.getTarget(), event.getEmptyBucket()))
			event.setCanceled(true);
	}

	@SubscribeEvent
	public void onTagsUpdate(TagsUpdatedEvent event) {
		super.onTagsUpdate();
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void onItemPickup(EntityItemPickupEvent event){
		if(super.onItemPickup(event.getPlayer(), event.getItem()))
			event.setCanceled(true);
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void onMobCheckSpawn(LivingSpawnEvent.CheckSpawn event){
		if(super.onMobSpawn(event.getEntity(), event.getX(), event.getY(), event.getZ(), event.getSpawnReason()))
			event.setResult(Result.DENY);
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void onProjectileImpact(ProjectileImpactEvent event){
		if(super.onProjectileImpact(event.getRayTraceResult(), event.getProjectile()))
			event.setCanceled(true);
	}

	@SubscribeEvent
	public void onAddReloadListenerEvent(AddReloadListenerEvent event){
		event.addListener(new ServerDataReloadListenerForge());
	}

	@SubscribeEvent
	public void onAddonRegister(OPACServerAddonRegisterEvent event){
		super.onAddonRegister(event.getServer(), event.getPermissionSystemManager(), event.getPartySystemManagerAPI(), event.getClaimsManagerTrackerAPI());

		event.getPermissionSystemManager().register("permission_api", new ForgePermissionsSystem());
	}

	@SubscribeEvent
	protected void onForgePermissionGather(PermissionGatherEvent.Nodes event) {
		ForgePermissionsSystem.registerNodes(event);
	}

	@Override
	protected void fireAddonRegisterEvent(IServerData<IServerClaimsManager<IPlayerChunkClaim, IServerPlayerClaimInfo<IPlayerDimensionClaims<IPlayerClaimPosList>>, IServerDimensionClaimsManager<IServerRegionClaims>>, IServerParty<IPartyMember, IPartyPlayerInfo, IPartyAlly>> serverData) {
		MinecraftForge.EVENT_BUS.post(new OPACServerAddonRegisterEvent(serverData.getServer(), serverData.getPlayerPermissionSystemManager(), serverData.getPlayerPartySystemManager(), serverData.getServerClaimsManager().getTracker()));
	}

}
