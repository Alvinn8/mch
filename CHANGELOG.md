# mch version numbers
Version numbers are utilized in mch to ensure proper reading of data, even when
there are format changes in different versions of mch. Each time a change is made
to the binary format used by mch, the version number is increased. When mch reads
data, if it encounters a version number that indicates the data was stored in an
older format, specific actions are taken to parse the data in the older format,
ensuring backward compatibility. This section describes the changes that were
made with each increment of the mch version number.
## 9
Current version number.
The version number was incremented multiple times during development to
invalidate old testing runs.
## 1
Old prototype of mch. The idea of how mch would work was very different and was
not implemented, so there are no mch repositories with this version number, so
no backwards compatibility with mch version number 1 exists since it does not
exist in practice.