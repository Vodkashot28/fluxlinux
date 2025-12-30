#!/bin/bash
# scripts/common/setup_hw_accel_debian.sh
# Hardware Acceleration Setup for Debian (PRoot)
# Based on termux-desktop implementation by sabamdarif

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
    libglx0 \
    xdg-desktop-portal

# 2. Detect Architecture
ARCH=$(dpkg --print-architecture)
if [ "$ARCH" != "arm64" ]; then
    echo "Warning: Drivers are optimized for arm64. Your arch is $ARCH."
fi

# 3. GPU Selection Menu
echo "============================================"
echo "      Select your GPU / Acceleration Mode"
echo "============================================"
echo "1) Adreno (Turnip + Zink)"
echo "   - Best for Snapdragon devices"
echo "   - Installs custom Mesa/Turnip drivers"
echo "   - Highest performance for Adreno GPUs"
echo ""
echo "2) VirGL (Universal)"
echo "   - Works with ALL GPU types (Adreno, Mali, etc.)"
echo "   - Requires 'virgl_test_server' running in Termux"
echo "   - Good compatibility, moderate performance"
echo ""
echo "============================================"
echo "Note: Mali/Exynos users should use VirGL (option 2)"
echo "============================================"
read -r -p "Enter choice [1-2]: " GPU_CHOICE

case "$GPU_CHOICE" in
    1)
        MODE="turnip"
        ;;
    2)
        MODE="virgl"
        ;;
    *)
        echo "Invalid choice. Defaulting to VirGL."
        MODE="virgl"
        ;;
esac

echo "FluxLinux: Configuring for $MODE..."

if [ "$MODE" = "turnip" ]; then
    # Install Turnip (Mesa Turnip/Zink for Adreno)
    # Reference: https://github.com/sabamdarif/termux-desktop
    TURNIP_VERSION="25.3.2"
    DL_ARCH="aarch64"
    if [ "$ARCH" != "arm64" ]; then DL_ARCH="$ARCH"; fi

    URL="https://github.com/sabamdarif/termux-desktop/releases/download/turnip-${TURNIP_VERSION}/turnip-${TURNIP_VERSION}-${DL_ARCH}.zip"

    echo "FluxLinux: Downloading Turnip drivers v${TURNIP_VERSION}..."
    curl -L -o /tmp/turnip.zip "$URL"

    if [ -f "/tmp/turnip.zip" ]; then
        echo "FluxLinux: Installing Turnip..."
        unzip -o /tmp/turnip.zip -d /usr
        rm /tmp/turnip.zip
        echo "FluxLinux: Turnip installed successfully!"
    else
        echo "Error: Failed to download Turnip drivers."
        exit 1
    fi
fi

# 4. Create Launch Wrapper
echo "FluxLinux: Creating 'gpu-launch' wrapper..."

cat <<'EOF' > /usr/local/bin/gpu-launch
#!/bin/bash
# FluxLinux GPU Launcher
# Automatically detects and applies the correct GPU configuration

MODE="MODE_PLACEHOLDER"

# Reset environment
unset GALLIUM_DRIVER
unset MESA_LOADER_DRIVER_OVERRIDE
unset VK_ICD_FILENAMES

if [ "$MODE" = "turnip" ]; then
    # Turnip (Adreno Vulkan) + Zink
    export MESA_LOADER_DRIVER_OVERRIDE=zink
    export VK_ICD_FILENAMES=/usr/share/vulkan/icd.d/freedreno_icd.aarch64.json
    export TU_DEBUG=noconform
    export MESA_GL_VERSION_OVERRIDE=4.6
    export MESA_GLES_VERSION_OVERRIDE=3.2
    export MESA_NO_ERROR=1

elif [ "$MODE" = "virgl" ]; then
    # VirGL (Universal - works with all GPUs)
    export GALLIUM_DRIVER=virpipe
    export MESA_GL_VERSION_OVERRIDE=4.0
    export MESA_GLES_VERSION_OVERRIDE=3.1
    export MESA_NO_ERROR=1
fi

# Execute Application
exec "$@"
EOF

# Replace placeholder with actual mode
sed -i "s/MODE_PLACEHOLDER/$MODE/g" /usr/local/bin/gpu-launch
chmod +x /usr/local/bin/gpu-launch

echo ""
echo "============================================"
echo "  Hardware Acceleration Setup Complete!"
echo "============================================"
echo "Mode: $MODE"
echo ""
echo "Usage: gpu-launch <application>"
echo "Example: gpu-launch glmark2"
echo ""

if [ "$MODE" = "virgl" ]; then
    echo "IMPORTANT: VirGL requires virgl_test_server running in Termux!"
    echo "The server should start automatically when you launch GUI."
    echo ""
fi

if [ "$MODE" = "turnip" ]; then
    echo "Turnip is configured for Adreno GPUs."
    echo "If you have a different GPU, re-run this script and select VirGL."
    echo ""
fi

echo "Test your setup:"
echo "  gpu-launch glmark2"
echo "  gpu-launch glxinfo | grep 'OpenGL renderer'"
echo "============================================"
