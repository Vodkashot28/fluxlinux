#!/bin/bash
# setup_video_editing_debian.sh
# Installs Video Editing & Processing Stack
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

echo "FluxLinux: Setting up Video Editing & Media Environment..."
echo "Target: Debian 13 (Trixie) - ARM64"

# 1. System Dependencies & FFmpeg
echo "FluxLinux: Installing Core Media Tools (FFmpeg)..."
export DEBIAN_FRONTEND=noninteractive
apt update -y

# Enable contrib/non-free for codecs if not already enabled (redundant check but safe)
sed -i 's/main$/main contrib non-free non-free-firmware/g' /etc/apt/sources.list
sed -i 's/main contrib$/main contrib non-free non-free-firmware/g' /etc/apt/sources.list
apt update -y

# Core: FFmpeg, Media Info, Codecs
apt install -y \
    ffmpeg \
    mediainfo \
    gstreamer1.0-plugins-base gstreamer1.0-plugins-good gstreamer1.0-plugins-bad gstreamer1.0-plugins-ugly \
    libavcodec-extra \
    || handle_error "Core Media Tools Installation"

# 2. Video Editors
echo "FluxLinux: Installing Video Editors..."

# Kdenlive: Advanced Non-Linear Editor (KDE)
# Shotcut: Cross-platform, frequent updates
# OpenShot: User-friendly Qt based
# Flowblade: Fast, precise, Python-based (GTK)
# Pitivi: Gnome native, integrates well

echo " - Installing Kdenlive, Shotcut, OpenShot, Flowblade, Pitivi..."
apt install -y \
    kdenlive \
    shotcut \
    openshot-qt \
    flowblade \
    pitivi \
    || handle_error "Video Editors Installation"

# 3. Audio Tools
echo "FluxLinux: Installing Audio Tools..."
# Audacity: The standard for audio editing
apt install -y audacity || handle_error "Audio Tools Installation"

# 4. Media Players
echo "FluxLinux: Installing Media Players..."
# VLC: The classic
# MPV: Lightweight, powerful, hardware accel friendly
# SMPlayer: GUI for MPV/MPlayer
apt install -y \
    vlc \
    mpv \
    smplayer \
    || handle_error "Media Players Installation"

# 5. Optional: Blender (3D & Video Editing)
# Often heavy, but useful. Included in many video workflows.
# Checking availability (Blender on ARM64 Trixie works well via apt)
echo "FluxLinux: Installing Blender (3D/VFX)..."
apt install -y blender || echo " [⚠️] Blender install failed (optional)"

# 6. Verification
verify_installation() {
    echo ""
    echo "🔎 FluxLinux: Verifying Installations..."
    echo "------------------------------------------------"
    
    # Core
    if command -v ffmpeg >/dev/null; then echo " [✅] FFmpeg"; else echo " [❌] FFmpeg Missing"; fi
    
    # Editors
    if command -v kdenlive >/dev/null; then echo " [✅] Kdenlive"; else echo " [❌] Kdenlive Missing"; fi
    if command -v shotcut >/dev/null; then echo " [✅] Shotcut"; else echo " [❌] Shotcut Missing"; fi
    if command -v openshot-qt >/dev/null; then echo " [✅] OpenShot"; else echo " [❌] OpenShot Missing"; fi
    if command -v flowblade >/dev/null; then echo " [✅] Flowblade"; else echo " [❌] Flowblade Missing"; fi
    if command -v pitivi >/dev/null; then echo " [✅] Pitivi"; else echo " [❌] Pitivi Missing"; fi
    
    # Players
    if command -v vlc >/dev/null; then echo " [✅] VLC"; else echo " [❌] VLC Missing"; fi
    if command -v mpv >/dev/null; then echo " [✅] MPV"; else echo " [❌] MPV Missing"; fi
    if command -v smplayer >/dev/null; then echo " [✅] SMPlayer"; else echo " [❌] SMPlayer Missing"; fi
    
    # Audio
    if command -v audacity >/dev/null; then echo " [✅] Audacity"; else echo " [❌] Audacity Missing"; fi

    echo "------------------------------------------------"
    echo "🎉 Video Editing Setup Complete!"
}

verify_installation

echo "Note: For best performance, enable Hardware Acceleration (VirGL) in FluxLinux settings if available."
read -p "Press Enter to close..."
