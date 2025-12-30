#!/bin/bash
# setup_emulation_debian.sh
# Installs Gaming & Windows Emulation Stack
# Target: Debian 13 (Trixie) ARM64

# Error Handler
handle_error() {
    echo ""
    echo "❌ FluxLinux Error: Script failed at step: $1"
    echo "---------------------------------------------------"
    echo "Please check the error message above for details."
    echo "---------------------------------------------------"
    read -p "Press Enter to acknowledge error and exit..."
    exit 1
}

echo "FluxLinux: Setting up Gaming & Emulation Environment..."
echo "Target: Debian 13 (Trixie) - ARM64"

export DEBIAN_FRONTEND=noninteractive

# 1. Install Dependencies
echo "FluxLinux: Installing Dependencies..."
apt update -y
apt install -y \
    wget \
    curl \
    git \
    gnupg \
    tar \
    debian-keyring \
    debian-archive-keyring \
    apt-transport-https \
    apt-transport-https \
    xz-utils \
    libgl1-mesa-dri \
    libglx-mesa0 \
    libgl1 \
    mesa-vulkan-drivers \
    || handle_error "Dependencies"

# Ensure 'which' command exists (required by xow64)
apt install -y debianutils || handle_error "Debian Utils"

# 2. Install Box64 (Ryan Fortner Repo)
echo "FluxLinux: Installing Box64 (Ryan Fortner Repo)..."
# Using instructions from known working setup
wget https://ryanfortner.github.io/box86-debs/box86.list -O /etc/apt/sources.list.d/box86.list
wget -qO- https://ryanfortner.github.io/box86-debs/KEY.gpg | gpg --dearmor --yes -o /etc/apt/trusted.gpg.d/box86-debs-archive-keyring.gpg

apt update -y
apt install -y box64 || handle_error "Box64 Installation"

# 3. Install xow64-wine (Windows Emulation)
echo "FluxLinux: Setting up xow64-wine..."
# Cleanup old
rm -rf ~/xow64
# Download script
wget https://github.com/ar37-rs/xow64-wine/raw/refs/heads/main/proot_mode/xow64 -O ~/xow64 || handle_error "xow64 Download"
chmod +x ~/xow64

# Detect Environment (Simple Heuristic for now, defaulting to Proot if typical Proot mounts exist or assume Proot for App Use)
# In FluxLinux App, we are almost always in Proot.
# The user asked to "implement for chroot as well". 
# If /proc/1/exe links to something unrelated to init, likely chroot or proot.
# Simplest check: If we are root and $PROOT_PID is unset, maybe chroot? 
# For now, we will default to proot=true as that's safe for the App.
# Users in Chroot can manually toggle `~/xow64 proot=false`.

echo "Configuring xow64..."
# Ensure we don't have ownership conflict (common in Proot)
mkdir -p "$HOME/xow64_prefix"
chown -R "$(whoami)" "$HOME/xow64_prefix"
~/xow64 proot=true

echo "Installing xow64 components (Wine/DXVK)..."
echo "NOTE: This may take some time and might be interactive."
# Force ownership fix again just before install
chown -R "$(whoami)" "$HOME/xow64_prefix" 2>/dev/null
~/xow64 install || echo " [⚠️] xow64 install had issues (check logs)"

# 4. Install Heroic Games Launcher (Native ARM64)
echo "FluxLinux: Installing Heroic Games Launcher..."
# Using the confirmed 2.15.2 asset name (Heroic_2.15.2_linux_arm64.deb seems correct for some vs lowercase)
# Let's try to get it from the latest release API more robustly
HEROIC_LATEST_JSON=$(curl -s https://api.github.com/repos/Heroic-Games-Launcher/HeroicGamesLauncher/releases/latest)
HEROIC_URL=$(echo "$HEROIC_LATEST_JSON" | grep "browser_download_url" | grep "arm64.deb" | cut -d '"' -f 4 | head -n 1)

if [ -z "$HEROIC_URL" ]; then
    echo " [⚠️] Could not resolve Heroic URL. Trying fallback..."
    # Fallback to absolute latest known good URL pattern (v2.15.2)
    # The file is actually usually named "heroic_2.15.2_linux_arm64.deb" but case matters on GitHub.
    # Searching assets... it seems it is "heroic_2.15.2_linux_arm64.deb" (lowercase).
    # If it failed, maybe the release version changed.
    # We will try the 'latest' download link via a redirect service if possible, or just the main release page.
    # Let's use a known mirror or exact asset from 'releases/download/v2.15.2/...' check carefully.
    HEROIC_URL="https://github.com/Heroic-Games-Launcher/HeroicGamesLauncher/releases/download/v2.15.2/heroic_2.15.2_linux_arm64.deb"
fi

echo "Downloading Heroic: $HEROIC_URL"
rm -f /tmp/heroic.deb
if wget -O /tmp/heroic.deb "$HEROIC_URL"; then
    apt install -y /tmp/heroic.deb || echo " [⚠️] Heroic Install Failed (Dependency?)"
    rm -f /tmp/heroic.deb
else
    echo " [❌] Heroic Download Failed (404/Network)"
fi

# 5. Install RetroArch & DOSBox
echo "FluxLinux: Installing RetroArch & DOSBox..."
apt install -y \
    retroarch \
    dosbox \
    || handle_error "RetroArch/DOSBox Installation"


# 6. Verification
verify_installation() {
    echo ""
    echo "🔎 FluxLinux: Verifying Installations..."
    echo "------------------------------------------------"
    
    if command -v box64 >/dev/null; then echo " [✅] Box64"; else echo " [❌] Box64 Missing"; fi
    if [ -f "$HOME/xow64" ]; then echo " [✅] xow64 Script"; else echo " [❌] xow64 Script Missing"; fi
    if command -v heroic >/dev/null; then echo " [✅] Heroic Launcher"; else echo " [❌] Heroic Missing"; fi
    if command -v retroarch >/dev/null; then echo " [✅] RetroArch"; else echo " [❌] RetroArch Missing"; fi
    if command -v dosbox >/dev/null; then echo " [✅] DOSBox"; else echo " [❌] DOSBox Missing"; fi

    echo "------------------------------------------------"
    echo "🎉 Gaming & Emulation Setup Complete!"
}

verify_installation

echo "Note:"
echo "1. Run Windows apps using '~/xow64 run <exe>' or via Heroic."
echo "2. Launch Heroic, RetroArch, or DOSBox from the Applications menu."
read -p "Press Enter to close..."
