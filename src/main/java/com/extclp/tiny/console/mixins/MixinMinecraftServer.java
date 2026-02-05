package com.extclp.tiny.console.mixins;

import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Optional;

@Mixin(MinecraftServer.class)
public class MixinMinecraftServer {

    @Shadow
    @Final
    private static Logger LOGGER;

    /**
     * @author extclp
     * @reason reimplement
     */
    @Overwrite
    public void sendMessage(Text message) {
        var builder = new StringBuilder();
        message.visit((style, text) -> {
            if (style.isBold()) {
                builder.append("\u001B[1m");
            }
            if (style.isItalic()) {
                builder.append("\u001B[3m");
            }
            if (style.isUnderlined()) {
                builder.append("\u001B[4m");
            }
            if (style.isObfuscated()) {
                builder.append("\u001B[5m");
            }
            if (style.isStrikethrough()) {
                builder.append("\u001B[9m");
            }
            TextColor color = style.getColor();
            if (color != null) {
                int rgb = color.getRgb();
                builder.append("\u001B[38;2;%d;%d;%dm".formatted(((rgb >> 16) & 0xff), ((rgb >> 8) & 0xff), (rgb & 0xff)));
            }
            builder.append(text);
            if (!style.isEmpty()) {
                builder.append("\u001B[0m");
            }
            return Optional.empty();
        }, Style.EMPTY);

        LOGGER.info(builder.toString());
    }
}
