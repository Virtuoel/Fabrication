package com.unascribed.fabrication.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.unascribed.fabrication.support.EligibleIf;
import com.unascribed.fabrication.support.MixinConfigPlugin.RuntimeChecks;

import com.google.common.collect.Lists;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

@Mixin(PlayerEntity.class)
@EligibleIf(configEnabled="*.repelling_void")
public abstract class MixinRepellingVoid extends LivingEntity {

	protected MixinRepellingVoid(EntityType<? extends LivingEntity> entityType, World world) {
		super(entityType, world);
	}

	private Vec3d fabrication$lastGroundPos;
	private final List<Vec3d> fabrication$voidFallTrail = Lists.newArrayList();
	
	@Inject(at=@At("TAIL"), method="tick()V")
	public void tick(CallbackInfo ci) {
		if (!RuntimeChecks.check("*.repelling_void")) return;
		if (onGround) {
			fabrication$lastGroundPos = getPos();
			fabrication$voidFallTrail.clear();
		} else if (fabrication$voidFallTrail.size() < 20) {
			fabrication$voidFallTrail.add(getPos());
		}
	}
	
	
	@Override
	protected void destroy() {
		Vec3d pos = fabrication$lastGroundPos;
		if (RuntimeChecks.check("*.repelling_void") && pos != null) {
			teleport(pos.x, pos.y, pos.z);
			fallDistance = 0;
			world.playSound(null, pos.x, pos.y, pos.z, SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 1, 0.5f);
			world.playSound(null, pos.x, pos.y, pos.z, SoundEvents.BLOCK_SHROOMLIGHT_PLACE, SoundCategory.PLAYERS, 1, 0.5f);
			world.playSound(null, pos.x, pos.y, pos.z, SoundEvents.BLOCK_SHROOMLIGHT_PLACE, SoundCategory.PLAYERS, 1, 0.75f);
			world.playSound(null, pos.x, pos.y, pos.z, SoundEvents.ENTITY_WITHER_SHOOT, SoundCategory.PLAYERS, 0.2f, 0.5f);
			Object self = this;
			if (!world.isClient && self instanceof ServerPlayerEntity) {
				Box box = getBoundingBox();
				((ServerWorld)world).spawnParticles((ServerPlayerEntity)self, ParticleTypes.PORTAL, true, pos.x, pos.y+(box.getYLength()/2), pos.z, 32, box.getXLength()/2, box.getYLength()/2, box.getZLength()/2, 0.2);
				((ServerWorld)world).spawnParticles(ParticleTypes.PORTAL, pos.x, pos.y+(box.getYLength()/2), pos.z, 32, box.getXLength()/2, box.getYLength()/2, box.getZLength()/2, 0.2);
				for (Vec3d vec : fabrication$voidFallTrail) {
					((ServerWorld)world).spawnParticles((ServerPlayerEntity)self, ParticleTypes.WITCH, true, vec.x, vec.y, vec.z, 0, 1, 0, 1, 0.2);
					((ServerWorld)world).spawnParticles(ParticleTypes.WITCH, vec.x, vec.y, vec.z, 0, 1, 0, 1, 0.2);
				}
			}
			damage(DamageSource.OUT_OF_WORLD, 12);
		} else {
			super.destroy();
		}
	}
	
	@Inject(at = @At("TAIL"), method = "writeCustomDataToTag(Lnet/minecraft/nbt/CompoundTag;)V")
	public void writeCustomDataToTag(CompoundTag tag, CallbackInfo ci) {
		if (fabrication$lastGroundPos != null) {
			Vec3d pos = fabrication$lastGroundPos;
			tag.putDouble("fabrication:LastGroundPosX", pos.x);
			tag.putDouble("fabrication:LastGroundPosY", pos.y);
			tag.putDouble("fabrication:LastGroundPosZ", pos.z);
		}
	}
	
	@Inject(at = @At("TAIL"), method = "readCustomDataFromTag(Lnet/minecraft/nbt/CompoundTag;)V")
	public void readCustomDataFromTag(CompoundTag tag, CallbackInfo ci) {
		if (tag.contains("fabrication:LastGroundPosX")) {
			fabrication$lastGroundPos = new Vec3d(
					tag.getDouble("fabrication:LastGroundPosX"),
					tag.getDouble("fabrication:LastGroundPosY"),
					tag.getDouble("fabrication:LastGroundPosZ")
			);
		}
	}
	
}