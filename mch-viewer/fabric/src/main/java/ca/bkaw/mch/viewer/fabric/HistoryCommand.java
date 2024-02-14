package ca.bkaw.mch.viewer.fabric;

import ca.bkaw.mch.Sha1;
import ca.bkaw.mch.repository.MchRepository;
import ca.bkaw.mch.repository.TrackedWorld;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;

import java.io.IOException;
import java.nio.file.Path;

public class HistoryCommand {
    public static final SimpleCommandExceptionType NOT_VIEWING_HISTORY = new SimpleCommandExceptionType(Component.literal(
        "You are not viewing the history right now."
    ));

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("history")
                .requires(ctx -> ctx.hasPermission(2))
                .executes(ctx -> {
                    try {
                        return test(ctx);
                    } catch (Throwable e) {
                        ctx.getSource().sendFailure(Component.literal(e.toString()));
                        e.printStackTrace();
                        return 0;
                    }
                })
                .then(
                    Commands.literal("log")
                        .executes(ctx -> {
                            try {
                                return log(ctx);
                            } catch (Throwable e) {
                                ctx.getSource().sendFailure(Component.literal(e.toString()));
                                e.printStackTrace();
                                return 0;
                            }
                        })
                )
        );
    }

    public static HistoryView getHistoryView(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        MchViewerFabric mchViewer = MchViewerFabric.getInstance();
        ResourceKey<Level> levelKey = ctx.getSource().getLevel().dimension();
        HistoryView historyView = mchViewer.getHistoryView(levelKey);
        if (historyView == null) {
            throw NOT_VIEWING_HISTORY.create();
        }
        return historyView;
    }

    public static int test(CommandContext<CommandSourceStack> ctx) throws IOException {
        MchViewerFabric mchViewer = MchViewerFabric.getInstance();

        MinecraftServer server = ctx.getSource().getServer();
        MchRepository repository = new MchRepository(Path.of("/root/test/mch"));
        repository.readConfiguration();
        TrackedWorld trackedWorld = repository.getConfiguration().getTrackedWorld(Sha1.fromString("d6cd92545d49add9a5157a6bc7647a59ea4b1c38"));

        HistoryView view = mchViewer.view(server, repository, trackedWorld);

        ctx.getSource().sendSuccess(() -> Component.literal(
            "Created dimension with key " + view.getWorldHandle().getRegistryKey()
        ), false);

        return 1;
    }

    private static int log(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        HistoryView historyView = getHistoryView(ctx);

        // TODO

        return 1;
    }

}
