#!/usr/bin/env bash
set -e

INSTALL_DIR="/usr/local/share/vsix-extensions"

echo "Creating install directory: ${INSTALL_DIR}"
mkdir -p "${INSTALL_DIR}"

# Copy the post-create installer script into the container so it can be run
# later as the non-root user during the postCreateCommand lifecycle hook.
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cp "${SCRIPT_DIR}/post-create.sh" "${INSTALL_DIR}/post-create.sh"
chmod +x "${INSTALL_DIR}/post-create.sh"

# EXTENSIONS is injected by the dev container feature runtime from the
# 'extensions' option. It is a comma-separated list of .vsix URLs.
if [ -z "${EXTENSIONS}" ]; then
  echo "No extension URLs provided, nothing to download."
  exit 0
fi

IFS=',' read -ra URLS <<< "${EXTENSIONS}"
for URL in "${URLS[@]}"; do
  URL="$(echo "${URL}" | xargs)"  # trim whitespace
  FILENAME="$(basename "${URL}" | sed 's/[?#].*//')"  # strip query strings
  DEST="${INSTALL_DIR}/${FILENAME}"
  echo "Downloading ${URL} -> ${DEST}"
  curl -fSL "${URL}" -o "${DEST}"
done

echo "All extensions downloaded to ${INSTALL_DIR}"
