/*
 * FastBack - Fast, incremental Minecraft backups powered by Git.
 * Copyright (C) 2022 pcal.net
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; If not, see <http://www.gnu.org/licenses/>.
 */

package net.pcal.fastback.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.ServerCommandSource;
import net.pcal.fastback.config.GitConfig;
import net.pcal.fastback.logging.UserLogger;
import net.pcal.fastback.logging.UserMessage;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.StoredConfig;

import java.nio.file.Path;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;
import static net.pcal.fastback.commands.Commands.SUCCESS;
import static net.pcal.fastback.commands.Commands.gitOp;
import static net.pcal.fastback.commands.Commands.missingArgument;
import static net.pcal.fastback.commands.Commands.subcommandPermission;
import static net.pcal.fastback.config.FastbackConfigKey.IS_FILE_REMOTE_BARE;
import static net.pcal.fastback.config.OtherConfigKey.REMOTE_PUSH_URL;
import static net.pcal.fastback.logging.UserMessage.UserMessageStyle.ERROR;
import static net.pcal.fastback.logging.UserMessage.styledLocalized;
import static net.pcal.fastback.utils.Executor.ExecutionLock.NONE;
import static net.pcal.fastback.utils.FileUtils.mkdirs;

enum CreateFileRemoteCommand implements Command {

    INSTANCE;

    private static final String COMMAND_NAME = "create-file-remote";
    private static final String ARGUMENT = "file-path";

    @Override
    public void register(final LiteralArgumentBuilder<ServerCommandSource> argb, PermissionsFactory<ServerCommandSource> pf) {
        argb.then(
                literal(COMMAND_NAME).
                        requires(subcommandPermission(COMMAND_NAME, pf)).
                        executes(cc -> missingArgument(ARGUMENT, cc)).
                        then(argument(ARGUMENT, StringArgumentType.greedyString()).
                                executes(CreateFileRemoteCommand::setFileRemote)
                        )
        );
    }

    private static int setFileRemote(final CommandContext<ServerCommandSource> cc) {
        final UserLogger ulog = UserLogger.ulog(cc);
        gitOp(NONE, ulog, repo -> {
            final String targetPath = cc.getArgument(ARGUMENT, String.class);
            final Path fupHome = Path.of(targetPath);
            if (fupHome.toFile().exists()) {
                ulog.message(styledLocalized("fastback.chat.create-file-remote-dir-exists", ERROR, fupHome.toString()));
                return;
            }
            mkdirs(fupHome);
            GitConfig conf = repo.getConfig();
            try (Git targetGit = Git.init().setBare(conf.getBoolean(IS_FILE_REMOTE_BARE)).setDirectory(fupHome.toFile()).call()) {
                final StoredConfig targetGitc = targetGit.getRepository().getConfig();
                targetGitc.setInt("pack", null, "window", 0);
                targetGitc.setInt("core", null, "bigFileThreshold", 1);
                targetGitc.save();
            }
            final String targetUrl = "file://" + fupHome.toAbsolutePath();
            repo.getConfig().updater().set(REMOTE_PUSH_URL, targetUrl).save();
            ulog.message(UserMessage.localized("fastback.chat.create-file-remote-created", targetPath, targetUrl));
        });
        return SUCCESS;
    }
}
