#!/usr/bin/env bash
set -euo pipefail

# mch installer (Unix)
#
# Installs the mch CLI jar into a versioned directory and installs a small
# launcher script on your PATH.
#
# Default (user-local) locations:
#   jar:  ~/.local/share/mch/versions/<version>/mch-cli-all.jar
#   link: ~/.local/share/mch/current -> versions/<version>
#   bin:  ~/.local/bin/mch

REPO="Alvinn8/mch"   # GitHub owner/repo

PREFIX="${PREFIX:-$HOME/.local}"
BIN_DIR="${BIN_DIR:-$PREFIX/bin}"
DATA_DIR="${DATA_DIR:-$PREFIX/share/mch}"
VERSION="${VERSION:-latest}"
TAG=""

usage() {
  cat <<'EOF'
Usage: install.sh [--version <version|latest>] [--prefix <dir>] [--bin-dir <dir>] [--data-dir <dir>]

Environment variables (same as flags):
  VERSION, PREFIX, BIN_DIR, DATA_DIR

Examples:
  curl -fsSL https://raw.githubusercontent.com/Alvinn8/mch/main/scripts/install.sh | bash
  curl -fsSL https://raw.githubusercontent.com/Alvinn8/mch/main/scripts/install.sh | VERSION=v0.1.0 bash
  curl -fsSL .../install.sh | bash -s -- --prefix /usr/local
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --version) VERSION="$2"; shift 2;;
    --prefix) PREFIX="$2"; BIN_DIR="$PREFIX/bin"; DATA_DIR="$PREFIX/share/mch"; shift 2;;
    --bin-dir) BIN_DIR="$2"; shift 2;;
    --data-dir) DATA_DIR="$2"; shift 2;;
    -h|--help) usage; exit 0;;
    *) echo "Unknown arg: $1"; usage; exit 2;;
  esac
done

require_cmd() {
  command -v "$1" >/dev/null 2>&1 || {
    echo "Missing required command: $1" >&2
    exit 1
  }
}

require_cmd java
if command -v curl >/dev/null 2>&1; then
  DL="curl -fsSL"
elif command -v wget >/dev/null 2>&1; then
  DL="wget -qO-"
else
  echo "Need curl or wget to download releases" >&2
  exit 1
fi

mkdir -p "$BIN_DIR" "$DATA_DIR/versions"

if [[ "$VERSION" == "latest" ]]; then
  # Avoid GitHub API auth; follow redirect to latest release tag page.
  # Example final URL: https://github.com/<owner>/<repo>/releases/tag/v0.1.0
  latest_url=$(curl -fsSI "https://github.com/${REPO}/releases/latest" | tr -d '\r' | awk -F': ' 'tolower($1)=="location"{print $2}' | tail -n1)
  if [[ -z "${latest_url:-}" ]]; then
    echo "Failed to resolve latest version from GitHub" >&2
    exit 1
  fi
  TAG="${latest_url##*/}"
  echo "Latest version is $TAG"
else
  TAG="v$VERSION"
fi

# Strip leading 'v' from tag for asset naming (e.g. v0.1 -> 0.1)
if [[ "$TAG" == [vV]* ]]; then
  VERSION="${TAG:1}"
else
  VERSION="$TAG"
fi
ASSET="mch-cli-all-${VERSION}.jar"
URL="https://github.com/${REPO}/releases/download/${TAG}/${ASSET}"

tmpdir=$(mktemp -d)
cleanup() { rm -rf "$tmpdir"; }
trap cleanup EXIT

jar_tmp="$tmpdir/$ASSET"

# Download to file (support curl or wget)
if [[ "$DL" == curl* ]]; then
  curl -fsSL "$URL" -o "$jar_tmp"
else
  wget -qO "$jar_tmp" "$URL"
fi

# Basic sanity check: it's a zip/jar and has a manifest
if ! unzip -tq "$jar_tmp" >/dev/null 2>&1; then
  echo "Downloaded file doesn't look like a valid jar: $URL" >&2
  exit 1
fi

install_dir="$DATA_DIR/versions/$VERSION"
mkdir -p "$install_dir"
install -m 0644 "$jar_tmp" "$install_dir/mch-cli-all.jar"

# Update current symlink (atomic-ish)
ln -sfn "$install_dir" "$DATA_DIR/current"

# Install launcher
launcher="$BIN_DIR/mch"
cat >"$launcher" <<'EOF'
#!/usr/bin/env sh
set -eu

# mch launcher
#
# Primary layout (prefix install):
#   <prefix>/bin/mch
#   <prefix>/share/mch/current/mch-cli-all.jar
#
# This script also supports being symlinked into a global bin dir (e.g.
# /usr/local/bin/mch). In that case, the prefix-relative jar path won't exist,
# so we fall back to the default user-local install under ~/.local.

BIN_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
PREFIX=$(CDPATH= cd -- "$BIN_DIR/.." && pwd)

JAR="$PREFIX/share/mch/current/mch-cli-all.jar"
if [ ! -f "$JAR" ]; then
  # Fallback: global symlink to user-local install
  JAR="$HOME/.local/share/mch/current/mch-cli-all.jar"
fi

if [ ! -f "$JAR" ]; then
  echo "mch is installed but no jar was found." >&2
  echo "Expected one of:" >&2
  echo "  $PREFIX/share/mch/current/mch-cli-all.jar" >&2
  echo "  $HOME/.local/share/mch/current/mch-cli-all.jar" >&2
  echo "Install (user-local) with:" >&2
  echo "  curl -fsSL https://raw.githubusercontent.com/Alvinn8/mch/main/scripts/install.sh | bash" >&2
  exit 1
fi

if ! command -v java >/dev/null 2>&1; then
  echo "java is required to run mch (install a JRE/JDK first)." >&2
  exit 1
fi

exec java -jar "$JAR" "$@"
EOF
chmod +x "$launcher"

cat <<EOF
Installed mch $VERSION
  Jar: $DATA_DIR/current/mch-cli-all.jar
  Bin: $launcher

Make sure '$BIN_DIR' is on your PATH.
For bash/zsh, you can add this line to ~/.bashrc or ~/.zshrc:
  export PATH="$BIN_DIR:\$PATH"

Optional: If you prefer a convenience symlink instead of modifying your PATH, run:
  sudo ln -sf "$launcher" /usr/local/bin/mch

EOF
