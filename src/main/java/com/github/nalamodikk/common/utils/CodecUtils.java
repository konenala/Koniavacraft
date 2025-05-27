package com.github.nalamodikk.common.utils;


import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public class CodecUtils {
    public static final StreamCodec<RegistryFriendlyByteBuf, BlockPos> BLOCK_POS = new StreamCodec<>() {
        @Override
        public BlockPos decode(RegistryFriendlyByteBuf buf) {
            return BlockPos.of(buf.readLong());
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, BlockPos pos) {
            buf.writeLong(pos.asLong());
        }
    };
}
