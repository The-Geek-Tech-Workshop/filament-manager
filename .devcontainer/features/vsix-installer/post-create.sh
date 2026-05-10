#!/usr/bin/env bash
set -e

INSTALL_DIR="/usr/local/share/vsix-extensions"
EXTENSIONS_DIR="${HOME}/.vscode-server/extensions"

if [ ! -d "${INSTALL_DIR}" ]; then
  echo "vsix-installer: no download directory found, skipping."
  exit 0
fi

mapfile -t VSIX_FILES < <(find "${INSTALL_DIR}" -name "*.vsix" 2>/dev/null)

if [ "${#VSIX_FILES[@]}" -eq 0 ]; then
  echo "vsix-installer: no .vsix files found in ${INSTALL_DIR}, skipping."
  exit 0
fi

mkdir -p "${EXTENSIONS_DIR}"

for VSIX in "${VSIX_FILES[@]}"; do
  echo "vsix-installer: installing ${VSIX}..."

  TMPDIR="$(mktemp -d)"
  unzip -q "${VSIX}" -d "${TMPDIR}"

  MANIFEST="${TMPDIR}/extension.vsixmanifest"

  PUBLISHER="$(grep -oP '(?<=Publisher=")[^"]+' "${MANIFEST}" | head -1 | tr '[:upper:]' '[:lower:]')"
  NAME="$(grep -oP '(?<=Id=")[^"]+' "${MANIFEST}" | head -1 | tr '[:upper:]' '[:lower:]')"
  VERSION="$(grep -oP '(?<=Version=")[^"]+' "${MANIFEST}" | head -1)"

  TARGET="${EXTENSIONS_DIR}/${PUBLISHER}.${NAME}-${VERSION}"

  if [ -d "${TARGET}" ]; then
    echo "vsix-installer: ${PUBLISHER}.${NAME}-${VERSION} already present, skipping."
  else
    mv "${TMPDIR}/extension" "${TARGET}"
    echo "vsix-installer: installed to ${TARGET}"
  fi

  rm -rf "${TMPDIR}"
done

echo "vsix-installer: done."
