> Warning: This file is incomplete
> 
# nbt storage
```
4 bytes (signed integer) - mch version number
4 bytes (signed integer) - size of following data
[for i in size]
    4 bytes (signed integer) - version number
    [the nbt compound tag as an unnamed nbt tag]
```

# chunk storage
```
4 bytes - magic number, 0x6D636863 (ascii "mchc")
4 bytes (signed integer) - mch version number
4 bytes (signed integer) - size of the following chunk versions list
[for i in size of chunk versions list]
    4 bytes (signed integer) - chunk version number
    4 bytes (signed integer) - size of chunk data parts
    [for i in size of chunk data parts]
        1 byte - data part id
        4 bytes (signed integer) - nbt part version number
4 bytes (signed integer) - size of following chunk data part storages
[for i in size of chunk data part storages]
    1 byte - data part id
    [the data part storage, depends on the data part id]
```

# mch region files
```
4 bytes - magic number, 0x6D63672 (ascii "mchr")
4 bytes (signed integer) - mch version number
[for chunkZ in 32]
    [for chunkX in 32]
        4 bytes (signed integer) - chunk version number
        4 bytes (signed integer) - chunk last modified time
        [if chunk version number != 0]
            ChunkStorage - See the ChunkStorage section
```
