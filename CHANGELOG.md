# mch version numbers
Version numbers are used in mch to ensure proper reading of data, even when
there are format changes in different versions of mch. Each time a change is made
to the binary format used by mch, the version number is increased. When mch reads
data, if it encounters a version number that indicates the data was stored in an
older format, specific actions are taken to parse the data in the older format,
ensuring backward compatibility. This section describes the changes 
made with each increment of the mch version number.

## 14
- The directory that stores mch repository information was renamed from `mch`
  to `.mch`.
- To support the new world format introduced in Minecraft 26, the world object now
  stores the world layout enum and miscellaneous files after the magic number.

## 13
Added SFTP profiles to mch configuration.

## 2–12
The version number was incremented multiple times during development to
invalidate old testing runs.
## 1
Old prototype of mch. The idea of how mch would work was very different and was
not implemented. There are no mch repositories with this version number, so
no backwards compatibility with mch version number 1 exists since it does not
exist in practice.