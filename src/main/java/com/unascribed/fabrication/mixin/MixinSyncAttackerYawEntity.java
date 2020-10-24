package com.unascribed.fabrication.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.unascribed.fabrication.support.EligibleIf;
import com.unascribed.fabrication.support.MixinConfigPlugin.RuntimeChecks;

import io.netty.buffer.Unpooled;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.CustomPayloadS2CPacket;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

@Mixin(LivingEntity.class)
@EligibleIf(configEnabled="*.sync_attacker_yaw")
public abstract class MixinSyncAttackerYawEntity extends Entity {

	public MixinSyncAttackerYawEntity(EntityType<?> type, World world) {
		super(type, world);
	}

	private static final Identifier FABRICATION$ATTACKER_YAW = new Identifier("fabrication", "attacker_yaw");
		
	private float fabrication$lastAttackerYaw;
	
	// actually attackerYaw. has the wrong name in this version of yarn
	@Shadow
	private float knockbackVelocity;
	
	@Inject(at=@At("HEAD"), method="damage(Lnet/minecraft/entity/damage/DamageSource;F)Z")
	public void damageHead(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
		if (!RuntimeChecks.check("*.sync_attacker_yaw")) return;
		fabrication$lastAttackerYaw = knockbackVelocity;
	}
	
	@Inject(at=@At("RETURN"), method="damage(Lnet/minecraft/entity/damage/DamageSource;F)Z")
	public void damageReturn(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
		if (!RuntimeChecks.check("*.sync_attacker_yaw")) return;
		if (knockbackVelocity != fabrication$lastAttackerYaw && !world.isClient) {
			PacketByteBuf data = new PacketByteBuf(Unpooled.buffer(4));
			data.writeInt(getEntityId());
			data.writeFloat(knockbackVelocity);
			((ServerWorld)world).getChunkManager().sendToNearbyPlayers(this, new CustomPayloadS2CPacket(FABRICATION$ATTACKER_YAW, data));
		}
	}
	
}