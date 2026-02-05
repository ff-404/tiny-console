package com.extclp.tiny.console.mixins;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.suggestion.Suggestion;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.dedicated.MinecraftDedicatedServer;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.config.Property;
import org.jline.reader.Candidate;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.io.IOException;
import java.io.PrintWriter;

@Mixin(targets = "net.minecraft.server.dedicated.MinecraftDedicatedServer$1")
public abstract class MixinConsoleThread {

    @Final
    @Shadow
    MinecraftDedicatedServer field_13822;

    /**
     * @reason replacement vanilla console
     * @author extclp
     */
    @Overwrite
    public void run() throws IOException {
        Terminal terminal = TerminalBuilder.terminal();
        LineReader reader = LineReaderBuilder.builder()
            .terminal(terminal)
            .completer((reader1, line, candidates) -> {
                CommandManager cm = field_13822.getCommandManager();
                CommandDispatcher<ServerCommandSource> dispatcher = cm.getDispatcher();
                ParseResults<ServerCommandSource> parse = dispatcher.parse(line.line(), field_13822.getCommandSource());
                if (!parse.getExceptions().isEmpty()) {
                    return;
                }
                dispatcher.getCompletionSuggestions(parse, line.cursor()).thenAccept(suggestions -> {
                    for (Suggestion suggestion : suggestions.getList()) {
                        candidates.add(new Candidate(suggestion.getText()));
                    }
                });
            })
            .build();

        Logger logger = (Logger) LogManager.getRootLogger();
        LoggerContext loggerContext = (LoggerContext) LogManager.getContext(false);
        LoggerConfig loggerConfig = loggerContext.getConfiguration().getLoggerConfig(logger.getName());
        Appender sysOut = loggerConfig.getAppenders().get("SysOut");
        var appender = new AbstractAppender("Console", null, sysOut.getLayout(), false, new Property[0]) {
            @Override
            public void append(LogEvent event) {
                if (reader.isReading()) {
                    reader.callWidget(LineReader.CLEAR);
                }
                PrintWriter writer = terminal.writer();

                writer.print(this.getLayout().toSerializable(event).toString());
                if (reader.isReading()) {
                    reader.callWidget(LineReader.REDRAW_LINE);
                    reader.callWidget(LineReader.REDISPLAY);
                }
                writer.flush();
            }
        };
        appender.start();

        loggerConfig.removeAppender("SysOut");
        loggerConfig.addAppender(appender, Level.INFO, null);
        loggerContext.updateLoggers();

        try {
            while (!field_13822.isStopped() && field_13822.isRunning()) {
                String line = reader.readLine("> ");
                reader.getHistory().add(line);
                field_13822.enqueueCommand(line, field_13822.getCommandSource());
            }
        } catch (UserInterruptException e) {
            field_13822.stop(false);
        }
    }
}