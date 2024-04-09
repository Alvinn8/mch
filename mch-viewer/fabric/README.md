# mch-viewer (Fabric)
The mch-viewer mod for Fabric allows you to view commits of an mch repository in-game.

## Configuration
Add the mod jar to the mods folder, start the server, then stop it again to
generate the config file located in `config/mch-viewer/config.toml`. The default
configuration is empty. Add a repository to the configuration to get started.

```toml
# Example configuration

default_repo="my_smp"

[repos.my_smp]
path="/root/mch-repos/my_smp"
world="d6cd92545d49add9a5157a6bc7647a59ea4b1c38"
spawn={ x = 0, y = 100, z = -2000 }

[repos.my_creative_server]
path="/root/mch-repos/creative_server1"
```

### `default_repo` (optional)
Sets the default repository that is viewed when running `/history`. If only one
repository is configured then that repository will be the default. Other
repositories can be viewed by running `/history view <repo>`.

### `[repos.<repo>]`
Configuration for a repo with key `<repo>`, for example `[repos.my_smp]`. The
key may be any string, though spaces are discouraged since it will be used in
Minecraft commands. 

### `[repos.<repo>]` > `path` (required)
The path on the filesystem to the mch repository that should be viewed.

Can either end with `/mch` or not, it will be added automatically.

### `[repos.<repo>]` > `world` (optional*)
The name or id of the world to view in the repository. If the repository only
has one world (most repositories only have one world), then this option is not
needed.

<span>*</span> Only optional if the world only has one world.

### `[repos.<repo>]` > `spawn` (optional)
Change the position where players spawn when they start viewing the world. If
not specified the world spawn position will be used. You can for example use
this if you want player's viewing the archive to spawn next to a particular
build that isn't at spawn.

If a spawn override is specified, the x, y, and z coordinates must be specified
using an object: `spawn={ x = 0, y = 100, z = -2000 }`. A spawn angle may
optionally be specified by setting
`yaw`: `spawn={ x = 0, y = 100, z = -2000, yaw = 90 }`.