#!/bin/bash
# ============================================================
# FluxLinux — Native Termux XFCE4 Desktop Setup
# Location: termux/setup/setup_xfce4_termux.sh
# Runs on: HOST Termux (native, no container)
# Root required: no
# ============================================================

CALLBACK_NAME="xfce4_desktop"

handle_error() {
    echo ""
    echo "❌ FluxLinux Error: Script failed at step: $1"
    echo "---------------------------------------------------"
    echo "Please check the error message above for details."
    echo "---------------------------------------------------"
    read -p "Press Enter to acknowledge error and exit..."
    exit 1
}

echo ""
echo "══════════════════════════════════════════════"
echo "  FluxLinux — Native XFCE4 Desktop Setup"
echo "══════════════════════════════════════════════"
echo ""

# ── Step 1: Repositories ──────────────────────────────────
echo "FluxLinux: Enabling required repositories..."
pkg install x11-repo -y || handle_error "x11-repo install"
pkg install tur-repo -y || handle_error "tur-repo install"

# ── Step 2: Update ───────────────────────────────────────
echo "FluxLinux: Updating package lists..."
pkg update -y || handle_error "pkg update"

# ── Step 3: Core XFCE4 packages ──────────────────────────
echo ""
echo "===== Installing XFCE4 Desktop Environment ====="
pkg install -y \
    xfce4 \
    xfce4-goodies \
    xfwm4 \
    xfce4-terminal \
    mousepad \
    ristretto \
    xfce4-taskmanager \
    thunar \
    tumbler \
    || handle_error "XFCE4 packages"

# ── Step 4: Display server ───────────────────────────────
echo ""
echo "===== Installing Termux:X11 Display Server ====="
pkg install -y termux-x11-nightly || handle_error "Termux:X11 install"

# ── Step 5: Session dependencies ─────────────────────────
echo ""
echo "===== Installing Session Dependencies ====="
pkg install -y \
    dbus \
    at-spi2-core \
    || handle_error "Session dependencies"

# ── Step 6: Audio ────────────────────────────────────────
echo ""
echo "===== Installing PulseAudio ====="
pkg install -y pulseaudio || handle_error "PulseAudio install"

# ── Step 7: Extra apps ───────────────────────────────────
echo ""
echo "===== Installing Extra Applications ====="
pkg install -y \
    firefox \
    geany \
    vlc \
    || true   # Best-effort — not all may be available

# ── Step 8: Font support ─────────────────────────────────
echo ""
echo "===== Installing Fonts ====="
pkg install -y \
    fontconfig \
    noto-fonts \
    noto-fonts-extra \
    || handle_error "Font install"

# Rebuild font cache
fc-cache -fv 2>/dev/null || true

# ── Verification ─────────────────────────────────────────
verify_installation() {
    echo ""
    echo "🔎 FluxLinux: Verifying XFCE4 Installation..."
    echo "------------------------------------------------"
    MISSING=0

    if command -v xfce4-session >/dev/null 2>&1; then
        echo " [✅] XFCE4 Session"
    else
        echo " [❌] XFCE4 Session Missing"
        MISSING=1
    fi

    if command -v xfce4-terminal >/dev/null 2>&1; then
        echo " [✅] XFCE4 Terminal"
    else
        echo " [❌] XFCE4 Terminal Missing"
        MISSING=1
    fi

    if command -v termux-x11 >/dev/null 2>&1; then
        echo " [✅] Termux:X11"
    else
        echo " [❌] Termux:X11 Missing"
        MISSING=1
    fi

    if command -v pulseaudio >/dev/null 2>&1; then
        echo " [✅] PulseAudio"
    else
        echo " [⚠️] PulseAudio not found (audio may not work)"
    fi

    if command -v dbus-launch >/dev/null 2>&1; then
        echo " [✅] D-Bus"
    else
        echo " [❌] D-Bus Missing"
        MISSING=1
    fi

    echo "------------------------------------------------"
    if [ $MISSING -eq 1 ]; then
        echo "⚠️  Some components failed. Re-run or check errors above."
    else
        echo "🎉 XFCE4 Desktop installed successfully!"
        echo "   Run 'start_xfce4_termux.sh' to launch."
    fi
}

verify_installation

# ── Callback to app ──────────────────────────────────────
am start -a android.intent.action.VIEW \
  -d "fluxlinux://callback?result=success&name=${CALLBACK_NAME}" \
  --flags 0x10000000 2>/dev/null || true

echo ""
echo "Note: Install Hardware Acceleration next for GPU support."
echo "      Then use 'start_xfce4_termux.sh' to launch."
read -p "Press Enter to close..."
