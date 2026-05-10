#!/usr/bin/env bash
set -e

PLATFORMS_VERSION="${PLATFORMSVERSION:-"34"}"
BUILD_TOOLS_VERSION="${BUILDTOOLSVERSION:-"34.0.0"}"
INSTALL_NDK="${INSTALLNDK:-"false"}"
NDK_VERSION="${NDKVERSION:-"26.1.10909125"}"
INSTALL_CMAKE="${INSTALLCMAKE:-"false"}"
CMAKE_VERSION="${CMAKEVERSION:-"3.22.1"}"

mkdir -p "$ANDROID_SDK_ROOT"
cd "$ANDROID_SDK_ROOT"

# Checks if packages are installed and installs them if not
check_packages() {
  if ! dpkg -s "$@" >/dev/null 2>&1; then
    apt-get update -y
    apt-get -y install --no-install-recommends "$@"
  fi
}

# Install Android SDK
install() {
  local cmdlinetoolsVersion="10406996"
  local cmdlinetoolsUrl="https://dl.google.com/android/repository/commandlinetools-linux-${cmdlinetoolsVersion}_latest.zip"
  local cmdlinetoolsZipFile="commandlinetools.zip"

  curl -L -o "$cmdlinetoolsZipFile" "$cmdlinetoolsUrl"
  unzip -q -d "${ANDROID_SDK_ROOT}"/cmdline-tools "$cmdlinetoolsZipFile"
  mv "${ANDROID_SDK_ROOT}"/cmdline-tools/cmdline-tools "${ANDROID_SDK_ROOT}"/cmdline-tools/latest
  rm -f "$cmdlinetoolsZipFile"

  touch repositories.cfg

  yes | sdkmanager --update

  # Install platform-tools, build-tools, platforms
  yes | sdkmanager "platform-tools" "build-tools;${BUILD_TOOLS_VERSION}" "platforms;android-${PLATFORMS_VERSION}"

  # Install NDK
  if [[ "${INSTALL_NDK}" = "true" ]]; then
    yes | sdkmanager --install "ndk;${NDK_VERSION}"
  fi

  # Install CMake
  if [[ "${INSTALL_CMAKE}" = "true" ]]; then
    yes | sdkmanager --install "cmake;${CMAKE_VERSION}"
  fi
}

# Install dependencies
check_packages curl ca-certificates unzip

install

echo "Done!"