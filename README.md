# mch
A "version control system" for Minecraft worlds.

## Can be used to
- Back up worlds in a storage efficient way.
- Keep a history of worlds.
- Version control system for worlds.

## How?
When a new commit (a backup / snapshot of the world) is made, mch will only save
the parts of the world that changed again. Everything that didn't change is not
saved again.

## Usage
This tool is a command line interface (CLI).

To set up an mch repository inside a world folder that tracks the world, use the following commands.

```
mch init
mch world add .
```

then use `mch commit` to commit a snapshot of the world to the repository.

## ---

Can be used to keep a history of a minecraft world. <ins>m</ins>ine<ins>c</ins>raft <ins>h</ins>istory = mch