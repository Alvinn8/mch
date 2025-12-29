# mch-cli
The mch CLI is the main way to interact with an mch repository.

## Installing
See the [README](../README.md) for installation instructions.

## Creating a repository

To get started, create an mch repository by running `mch init` in the terminal
in a folder where you want to keep the repository. This can be any folder, since
unlike Git repositories the actual content of the folder you run `mch init` in does
not matter.

One idea is to create repositories inside a folder on your computer where you
store mch repositories, for example `~/Documents/mch-repos/my-repo1`

Another idea is to create the mch repository inside the world folder of the world
you want to track. You can do so using the following commands:

```
mch init
mch world add local .
```

then use `mch commit` to commit a snapshot of the world to the repository.

## Adding a world on the same computer (`mch world add local`)

To add a world that is located on the same computer as the mch repository, add
the world using `mch world add local <path>` where <path> is the path to the
world folder. The path can be relative, for example `.` or `./world`, mch will
find the absolute path and store that.

## Adding a world from a remote server via FTP or SFTP

If your world is hosted on a remote server, you can add the world by specifying
FTP or SFTP (SSH) credentials.

First, we need to add an FTP or SFTP profile to the repository. This profile can
be used to connect to the server, and we then add worlds by specifying the profile.

Run `mch ftp add <profile name>` or `mch sftp add <profile name>` to add a profile.
Choose a profile name, for example `server1`. You will be prompted to enter the host,
port, username and password. Currently, SFTP authentication with SSH keys is not
implemented, but this is a planned feature!

> [!WARNING]  
> FTP and SFTP passwords will be saved in **plain text** in the repository
> configuration. Make sure only authorized people have access to the computer
> where the mch repository is located.

After adding the profile, add the world using
`mch world add ftp <profile name> <remote path>` or
`mch world add sftp <profile name> <remote path>`
where `<profile name` is the profile created in the previous step.
The `<remote path>` argument is the path on the remote server where the world
can be located.

If the server is a Linux server, an example of a remote path could be `~/my_smp/world`.

If the server is hosted on a server host or using a control panel like Pterodactyl,
the FTP or SFTP connection usually leads to the server's directory upon connecting.
In that case a remote path of `./world` may be appropriate to refer to the server's
world directory (called `world` by default).

## Committing
To save the world and create a snapshot ("commit") of the world, run `mch commit`.
This will save the world to the repository.

A commit message can be specified with the `-m` flag (`--message`), for example
`mch commit -m "World before rebuild of spawn"`.

Unlike Git, it is common for mch commits to not have any message. If the world
is being backed up automatically with `mch commit` every day, the date of the
commit is often sufficient.

In rare cases, the `--no-cache` flag may be used. If the repository
configuration was changed in a way that changes what is saved in the repository,
for example the `InhabitedTime` limit was changed (See "Is mch lossless?"), it
might be worth running `mch commit --no-cache` to make sure the entire world is
processed. Still, only the parts of the world that change will be re-saved. The
`--no-cache` flag simply disables skipping region files whose last modification
date hasn't changed since last commit, and similar optimizations.

## Listing commits
To list commits in the repository, run `mch log`. It will show the 10 last commits.

Currently, it's not possible to view more commits. This is a planned feature though!

## Restoring to a commit
If you want to restore the world to how it looked at a specific commit, you can use
the `mch restore <commit>` command with the commit hash as `<commit>`.

This will create a folder in the folder where you created your repository called
`mch-restore`.

Restoring large worlds may take a while since many files need to be written to
the filesystem.

If you instead want to view the world in-game, which is faster and allows you to
change the viewed commits, you can use [mch-viewer](../mch-viewer/fabric/README.md).

## Deleting commits?
> Bad news, currently deleting commits is not implemented! So your repository will
just grow in size the more commits you create.
> 
> While an mch repository grows much slower than saving the world fully every time,
you may still want to prune it. This is a planned feature!

Due to how mch works, deleting commits may result in very little space saved,
or it could lead to a lot of space saved! Mch stores parts of the world as
"objects" in an object storage, and references these objects. This means many
commits will reference the same objects. So even if a commit is deleted, the
objects it references cannot be deleted since they may be referenced by other
commits.

Therefore, deleting multiple commits where the server had little activity will
likely save less space than deleting one commit from when the server was very active.

Some chunks will also be references by MANY commits, and getting rid of them
would require deleting basically all commits. For example chunks far out that
players rarely go to.

In summary, while deleting commits might seem like a good way to reduce the size of
a repository, it may have less of an impact from what you imagine.

A more efficient approach at reducing the size of the repository may be changing
the threshold for which chunks are saved in the repository by reducing the
`InhabitedTime` limit. (See "Is mch lossless?")

## Is mch lossless?
Yesn't.

> Note! Currently, it's not possible to change this behavior, and mch will skip
all chunks with an `InhabitedTime` of `0`. It is planned that this should be
configurable!

By default, mch is 100% lossless. This means the entire world will be saved in
the mch repository. (Actually not, see note above)

However, it is possible to save a lot of space if certain chunks are skipped.
Conveniently Minecraft saves a value in chunks called `InhabitedTime` which
counts how long players have been in or near a chunk. Chunks with an
`InhabitedTime` of `0` have therefore never been touched by players, and it may
be worth skipping these chunks.

For example, if players explore many thousands of blocks to find a woodland
mansion, many of the chunks explored on the way are unimportant and have an
`InhabitedTime` of `0` and can be skipped.

If chunks are pre-generated to improve performance, pre-generated chunks that
have never been explored by players will also have an `InhabitedTime`of `0`.