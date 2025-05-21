package com.github.nalamodikk.core;

import com.github.NalaParticleLib;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.FriendlyByteBuf;
import com.mojang.brigadier.StringReader;

import java.util.UUID;

public class ControllableParticleEffect implements ParticleOptions {
    public final UUID controlId;

    public ControllableParticleEffect(UUID controlId) {
        this.controlId = controlId;
    }

    @Override
    public ParticleType<?> getType() {
        return ParticleTypesInit.CONTROLLABLE.get();
    }

    @Override
    public void writeToNetwork(FriendlyByteBuf buf) {
        buf.writeUUID(controlId);
    }

    @Override
    public String writeToString() {
        return controlId.toString();
    }


    public static final Codec<ControllableParticleEffect> CODEC = Codec.STRING.xmap(
            s -> new ControllableParticleEffect(UUID.fromString(s)),
            effect -> effect.controlId.toString()
    );

    public static final Deserializer<ControllableParticleEffect> DESERIALIZER = new Deserializer<>() {
        @Override
        public ControllableParticleEffect fromCommand(ParticleType<ControllableParticleEffect> pParticleType, StringReader pReader) throws CommandSyntaxException {
            return new ControllableParticleEffect(UUID.fromString(pReader.readUnquotedString()));
        }

        @Override
        public ControllableParticleEffect fromNetwork(ParticleType<ControllableParticleEffect> type, FriendlyByteBuf buf) {
            UUID uuid = buf.readUUID();
            NalaParticleLib.LOGGER.info("📥 封包解析成功，UUID = {}", uuid);
            return new ControllableParticleEffect(uuid);
        }
    };
}
