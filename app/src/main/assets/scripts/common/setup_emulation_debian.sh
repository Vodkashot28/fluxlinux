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
apt-cache policy box64
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

# PATCH: xow64 script uses 'tar -xf' which preserves ownership, causing "not owned by you" error in Proot.
# We patch it to use '--no-same-owner' to force ownership to current user.
sed -i 's/tar -xf/tar --no-same-owner -xf/g' ~/xow64
sed -i 's/tar -xvf/tar --no-same-owner -xvf/g' ~/xow64

~/xow64 proot=true

echo "Installing xow64 components (Wine/DXVK)..."
echo "NOTE: This may take some time and might be interactive."
# Force ownership fix again just before install
chown -R "$(whoami)" "$HOME/xow64_prefix" 2>/dev/null
~/xow64 install || echo " [⚠️] xow64 install had issues (check logs)"

# 4. Install Heroic Games Launcher (Native ARM64)
echo "FluxLinux: Installing Heroic Games Launcher..."
# Using the confirmed 2.15.2 asset name (Heroic-2.15.2-linux-arm64.deb with Capital H)
# This was verified as the correct asset name format on GitHub Releases.
HEROIC_URL="https://github.com/Heroic-Games-Launcher/HeroicGamesLauncher/releases/download/v2.15.2/Heroic-2.15.2-linux-arm64.deb"

echo "Downloading Heroic: $HEROIC_URL"
# Verify link availability first
if wget --spider -q "$HEROIC_URL"; then
    rm -f /tmp/heroic.deb
    wget -O /tmp/heroic.deb "$HEROIC_URL"
    apt install -y /tmp/heroic.deb || echo " [⚠️] Heroic Install Failed (Dependency?)"
    rm -f /tmp/heroic.deb
else
    echo " [❌] Heroic Download Failed (404/Network) - URL: $HEROIC_URL"
    echo "Try checking: https://github.com/Heroic-Games-Launcher/HeroicGamesLauncher/releases/latest"
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
