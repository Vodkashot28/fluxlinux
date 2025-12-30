#!/bin/bash
# scripts/common/setup_hw_accel_debian.sh
# Based on termux-desktop implementation (sabamdarif)

# Check if running as root
if [ "$(id -u)" != "0" ]; then
    echo "This script must be run as root."
    exit 1
fi

echo "FluxLinux: Setting up Hardware Acceleration (Debian)..."

# 1. Install Dependencies
echo "FluxLinux: Installing Vulkan/Mesa dependencies..."
apt-get update
apt-get install -y \
    mesa-utils \
    libgl1-mesa-dri \
    mesa-vulkan-drivers \
    vulkan-tools \
    curl \
    unzip \
    libvulkan1 \
    libgl1 \
    libglx0

# 2. Detect Architecture
ARCH=$(dpkg --print-architecture)
if [ "$ARCH" != "arm64" ]; then
    echo "Warning: Turnip drivers are optimized for arm64 (Adreno). Your arch is $ARCH."
fi

# 3. Install Turnip (Mesa Turnip/Zink)
# Reference: https://github.com/sabamdarif/termux-desktop
TURNIP_VERSION="25.3.2"
# Note: Using aarch64 for URL mapping
DL_ARCH="aarch64"
if [ "$ARCH" != "arm64" ]; then DL_ARCH="$ARCH"; fi

URL="https://github.com/sabamdarif/termux-desktop/releases/download/turnip-${TURNIP_VERSION}/turnip-${TURNIP_VERSION}-${DL_ARCH}.zip"

echo "FluxLinux: Downloading Turnip drivers v${TURNIP_VERSION}..."
curl -L -o /tmp/turnip.zip "$URL"

if [ -f "/tmp/turnip.zip" ]; then
    echo "FluxLinux: Installing Turnip..."
    # Extract to /usr (overlays existing mesa libs or adds to local)
    # The zip usually contains /lib, /share etc.
    unzip -o /tmp/turnip.zip -d /usr
    rm /tmp/turnip.zip
    
    # 4. Create Launch Wrapper
    echo "FluxLinux: Creating 'gpu-launch' wrapper..."
    cat <<EOF > /usr/local/bin/gpu-launch
#!/bin/bash
# Wrapper to launch apps with Turnip/Zink acceleration on Adreno

# Turnip (Adreno Vulkan) + Zink (OpenGL over Vulkan)
export MESA_LOADER_DRIVER_OVERRIDE=zink
export GALLIUM_DRIVER=zink
export VK_ICD_FILENAMES=/usr/share/vulkan/icd.d/freedreno_icd.${DL_ARCH}.json 

# OpenGL Version Overrides (Improve compatibility)
export MESA_GL_VERSION_OVERRIDE=4.6
export MESA_GLES_VERSION_OVERRIDE=3.2
export MESA_GLSL_VERSION_OVERRIDE=460

# Performance hints
export TU_DEBUG=noconform

exec "\$@"
EOF
    chmod +x /usr/local/bin/gpu-launch
    
    echo "FluxLinux: GPU Drivers Installed!"
    echo "Usage: gpu-launch <application>"
    echo "Example: gpu-launch glxgears"
else
    echo "Error: Failed to download Turnip drivers."
fi
