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
    echo "Warning: Drivers are optimized for arm64. Your arch is $ARCH."
fi

# 3. GPU Selection Menu
echo "============================================"
echo "      Select your GPU / Acceleration Mode"
echo "============================================"
echo "1) Adreno (Turnip + Zink)"
echo "   - Best for Snapdragon devices."
echo "   - Installs custom Mesa/Turnip drivers inside Debian."
echo ""
echo "2) Mali / Other (Zink + Host Wrapper)"
echo "   - For Mali/Exynos/Mediatek devices."
echo "   - REQUIRES 'vulkan-wrapper-android' installed in locally."
echo "   - Uses Zink (OpenGL over Vulkan)."
echo ""
echo "3) Generic (VirGL)"
echo "   - Universal compatibility."
echo "   - Requires 'virgl_test_server' running on Host."
echo "   - Good for desktop, slower for games."
echo "============================================"
read -r -p "Enter choice [1-3]: " GPU_CHOICE

case "$GPU_CHOICE" in
    1)
        MODE="adreno"
        ;;
    2)
        MODE="mali"
        ;;
    3)
        MODE="virgl"
        ;;
    *)
        echo "Invalid choice. Defaulting to VirGL."
        MODE="virgl"
        ;;
esac

echo "FluxLinux: Configuring for $MODE..."

if [ "$MODE" = "adreno" ]; then
    # Install Turnip (Mesa Turnip/Zink)
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
    else
        echo "Error: Failed to download Turnip drivers."
        exit 1
    fi
fi

if [ "$MODE" = "mali" ]; then
    # Install Leegao Vulkan Wrapper (Copy from Host)
    # We use the Pipetto package installed by setup_termux.sh
    
    TERMUX_PREFIX="/data/data/com.termux/files/usr"
    HOST_LIB_PATH="$TERMUX_PREFIX/lib/libvulkan_wrapper.so"
    
    # Fallback search if not in standard path
    if [ ! -f "$HOST_LIB_PATH" ]; then
        HOST_LIB_PATH=$(find "$TERMUX_PREFIX/lib" -name "libvulkan_wrapper.so" 2>/dev/null | head -n 1)
    fi

    TARGET_LIB="/usr/lib/libvulkan_wrapper.so"
    ICD_DIR="/etc/vulkan/icd.d"
    ICD_FILE="$ICD_DIR/wrapper_icd.json"

    if [ -n "$HOST_LIB_PATH" ] && [ -f "$HOST_LIB_PATH" ]; then
        echo "FluxLinux: Found Host Wrapper at $HOST_LIB_PATH"
        echo "FluxLinux: Copying to $TARGET_LIB..."
        cp "$HOST_LIB_PATH" "$TARGET_LIB"
        chmod +x "$TARGET_LIB"

        echo "FluxLinux: Configuring Wrapper ICD..."
        mkdir -p "$ICD_DIR"
        cat <<EOF > "$ICD_FILE"
{
    "file_format_version": "1.0.0",
    "ICD": {
        "library_path": "$TARGET_LIB",
        "api_version": "1.1.0"
    }
}
EOF
        echo "FluxLinux: Wrapper Configured Successfully!"
    else
        echo "Error: Host Vulkan Wrapper not found in Termux."
        echo "checked: $TERMUX_PREFIX/lib/libvulkan_wrapper.so"
        echo ""
        echo "Please run the 'Setup Termux' script from the app menu first!"
        echo "This installs the required 'vulkan-wrapper-android' package."
        exit 1
    fi
fi

# 4. Create Launch Wrapper
echo "FluxLinux: Creating 'gpu-launch' wrapper..."

cat <<EOF > /usr/local/bin/gpu-launch
#!/bin/bash
# FluxLinux GPU Launcher

MODE="$MODE"
ARCH="$ARCH"
DL_ARCH="aarch64" # Default for wrapper naming
if [ "\$ARCH" != "arm64" ]; then DL_ARCH="\$ARCH"; fi

# Reset vars
unset GALLIUM_DRIVER
unset MESA_LOADER_DRIVER_OVERRIDE
unset VK_ICD_FILENAMES

if [ "\$MODE" = "adreno" ]; then
    # Turnip (Adreno Vulkan) + Zink
    export MESA_LOADER_DRIVER_OVERRIDE=zink
    export GALLIUM_DRIVER=zink
    export VK_ICD_FILENAMES=/usr/share/vulkan/icd.d/freedreno_icd.\${DL_ARCH}.json
    export TU_DEBUG=noconform
    # OpenGL Overrides
    export MESA_GL_VERSION_OVERRIDE=4.6
    export MESA_GLES_VERSION_OVERRIDE=3.2
    export MESA_GLSL_VERSION_OVERRIDE=460

elif [ "\$MODE" = "mali" ]; then
    # Zink over Self-Contained Wrapper (Mali)
    # Uses locally installed Leegao wrapper in /usr/lib
    
    WRAPPER_JSON="/etc/vulkan/icd.d/wrapper_icd.json"
    
    if [ -f "\$WRAPPER_JSON" ]; then
        export VK_ICD_FILENAMES="\$WRAPPER_JSON"
        export MESA_LOADER_DRIVER_OVERRIDE=zink
        export GALLIUM_DRIVER=zink
        export MESA_GL_VERSION_OVERRIDE=4.6
        export MESA_GLES_VERSION_OVERRIDE=3.2
        # Mali specific optimizations
        export MESA_VK_WSI_PRESENT_MODE=mailbox
        export MESA_VK_WSI_DEBUG=blit 
    else
        echo "Error: Wrapper configuration not found at \$WRAPPER_JSON"
        echo "Please re-run hardware acceleration setup."
    fi

elif [ "\$MODE" = "virgl" ]; then
    # VirGL (Client)
    export GALLIUM_DRIVER=virpipe
    export MESA_GL_VERSION_OVERRIDE=4.0
    export MESA_GLES_VERSION_OVERRIDE=3.1
    export MESA_GLSL_VERSION_OVERRIDE=400
fi

# Execute Application
exec "\$@"
EOF

chmod +x /usr/local/bin/gpu-launch

echo "FluxLinux: GPU Drivers Configured for $MODE!"
echo "Usage: gpu-launch <application>"
if [ "$MODE" = "virgl" ]; then
    echo "Note: Ensure 'virgl_test_server' is running in Termux."
fi

