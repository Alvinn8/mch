package ca.bkaw.mch.viewer.fabric;

import ca.bkaw.mch.Sha1;
import ca.bkaw.mch.object.ObjectStorageTypes;
import ca.bkaw.mch.object.Reference20;
import ca.bkaw.mch.object.commit.Commit;
import ca.bkaw.mch.repository.MchRepository;
import ca.bkaw.mch.repository.TrackedWorld;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;

import java.io.IOException;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class HistoryCommand {
    public static final SimpleCommandExceptionType NOT_VIEWING_HISTORY = new SimpleCommandExceptionType(net.minecraft.network.chat.Component.literal(
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
                        ctx.getSource().sendFailure(Component.text(e.toString()));
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
                                ctx.getSource().sendFailure(Component.text(e.toString()));
                                e.printStackTrace();
                                return 0;
                            }
                        })
                )
                .then(
                    Commands.literal("commit")
                        .then(
                            Commands.argument("hash", StringArgumentType.word())
                                .executes(ctx -> {
                                    String hash = StringArgumentType.getString(ctx, "hash");
                                    Sha1 sha1 = Sha1.fromString(hash);
                                    try {
                                        return commit(ctx, sha1);
                                    } catch (Throwable e) {
                                        ctx.getSource().sendFailure(Component.text(e.toString()));
                                        e.printStackTrace();
                                        return 0;
                                    }
                                })
                        )
                )
        );
    }

    public static HistoryView getHistoryView(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        MchViewerFabric mchViewer = MchViewerFabric.getInstance();
        ResourceKey<Level> levelKey = ctx.getSource().getLevel().dimension();
        DimensionView historyView = mchViewer.getDimensionView(levelKey);
        if (historyView == null) {
            throw NOT_VIEWING_HISTORY.create();
        }
        return historyView.getParent();
    }

    public static int test(CommandContext<CommandSourceStack> ctx) throws IOException {
        MchViewerFabric mchViewer = MchViewerFabric.getInstance();

        MinecraftServer server = ctx.getSource().getServer();
        MchRepository repository = new MchRepository(Path.of("/Users/Alvin/Documents/mch/metacraft/bygg/mch"));
        repository.readConfiguration();
        TrackedWorld trackedWorld = repository.getConfiguration().getTrackedWorld(Sha1.fromString("dde930208d9c6dd54e13ef13ad5be79a476b27bf"));

        Reference20<Commit> headCommitRef = repository.getHeadCommit();
        if (headCommitRef == null) {
            throw new IllegalArgumentException("Repository is empty");
        }

        Commit commit = headCommitRef.resolve(repository);
        CommitInfo commitInfo = new CommitInfo(commit, headCommitRef.getSha1());

        HistoryView view = mchViewer.view(server, repository, trackedWorld, commitInfo);

        // TODO send player to view

        return 1;
    }

    public static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    public static final DateFormat DETAILED_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss zzz");

    private static int log(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException, IOException {
        HistoryView historyView = getHistoryView(ctx);
        CachedCommits cachedCommits = historyView.getCachedCommits();

        CommitInfo current = historyView.getCommit();
        CommitInfo[] commits = new CommitInfo[11];
        commits[5] = current;

        // Fill [0-4]
        CommitInfo next = current;
        for (int i = 4; i >= 0; i--) {
            next = cachedCommits.nextCommit(next);
            if (next == null) {
                break;
            }
            commits[i] = next;
        }

        // Fill [6-10]
        CommitInfo prev = current;
        for (int i = 6; i <= 10; i++) {
            prev = cachedCommits.previousCommit(prev);
            if (prev == null) {
                break;
            }
            commits[i] = prev;
        }

        TextComponent.Builder builder = Component.text();

        builder.append(Component.text("====[ ", NamedTextColor.YELLOW));
        builder.append(Component.text("commits", NamedTextColor.GOLD));
        builder.append(Component.text(" ]=====", NamedTextColor.YELLOW));
        builder.append(Component.newline());

        for (CommitInfo commitInfo : commits) {
            if (commitInfo == null) {
                continue;
            }
            Commit commit = commitInfo.commit();
            Sha1 hash = commitInfo.hash();
            boolean isCurrentCommit = commitInfo.equals(current);
            TextComponent.Builder row = Component.text();
            row.clickEvent(ClickEvent.runCommand("/history commit " + hash.asHex()));
            if (isCurrentCommit) {
                row.append(
                    Component.text()
                        .content("-> ")
                        .hoverEvent(HoverEvent.showText(Component.text("You are currently viewing this commit")))
                );
            }
            row.append(Component
                .text()
                .content(hash.asHex().substring(0, 7))
                .color(NamedTextColor.GRAY)
                .hoverEvent(HoverEvent.showText(Component.text(hash.asHex())))
            );
            row.append(Component.space());
            Date date = new Date(commit.getTime());
            row.append(Component.text()
                .content(DATE_FORMAT.format(date))
                .hoverEvent(Component.text(DETAILED_DATE_FORMAT.format(date)))
            );
            String message = commit.getMessage();
            if (!message.isBlank()) {
                if (message.length() > 13) {
                    row.append(Component.text()
                        .content(' ' + message.substring(0, 10) + "...")
                        .hoverEvent(HoverEvent.showText(Component.text(message)))
                    );
                } else {
                    row.append(Component.text(message));
                }
            }
            row.append(Component.newline());
            builder.append(row);
        }

        builder.append(Component.text("====", NamedTextColor.YELLOW));
        if (prev != null && cachedCommits.hasPrevious(prev)) {
            builder.append(Component.text("[ < ]", NamedTextColor.GOLD));
        } else {
            builder.append(Component.text("[ < ]", NamedTextColor.GRAY));
        }
        builder.append(Component.text("==", NamedTextColor.YELLOW));
        if (next != null && cachedCommits.hasNext(next)) {
            builder.append(Component.text("[ > ]", NamedTextColor.GOLD));
        } else {
            builder.append(Component.text("[ > ]", NamedTextColor.GRAY));
        }
        builder.append(Component.text("=====", NamedTextColor.YELLOW));
        builder.append(Component.newline());

        ctx.getSource().sendMessage(builder.build());

        return 1;
    }

    private static int commit(CommandContext<CommandSourceStack> ctx, Sha1 sha1) throws CommandSyntaxException, IOException {
        HistoryView historyView = getHistoryView(ctx);
        MchRepository repository = historyView.getRepository();

        Reference20<Commit> ref = new Reference20<>(ObjectStorageTypes.COMMIT, sha1);
        Commit commit = ref.resolve(repository);

        historyView.setCommit(new CommitInfo(commit, ref.getSha1()));

        log(ctx);

        return 1;
    }

}
