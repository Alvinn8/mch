<h1 align="center">mch</h1>

<div align="center">

[mch-cli](./mch-cli/README.md) | [mch-viewer](./mch-viewer/fabric/README.md)

A "version control system" for Minecraft worlds.

</div>

<hr>

Can be used to back up worlds in a storage efficient way and keep a history of
worlds. 

## How?
When a new commit (a backup / snapshot of the world) is made, mch will only
re-save the parts of the world that changed. Everything that didn't change
is not saved. This saves a ton of storage space and time!

## Usage Overview
> For detailed usage instructions, see the [mch-cli README](./mch-cli/README.md).

Mch is a command line interface (CLI) that works a bit like Git.

An mch repository holds the internal files mch needs to store the world. A
Minecraft world is then added to the repository. By running `mch commit` the
current snapshot of the world will be saved to the repository as a commit.

You can list commits with `mch log` and restore to previous commits using
`mch restore <commit hash>` which will restore the world to a folder called
`mch-restore`.

Since restoring an entire world can take a while when the world is big,
[mch-viewer](./mch-viewer/fabric/README.md) can be used to view commits in-game
without having to restore the entire commit.

## Sounds cool?
Check out [mch-cli](./mch-cli/README.md) for more details, or contact me on
discord `@alvinn8` if you have questions or want to chat!