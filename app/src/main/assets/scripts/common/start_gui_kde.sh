#!/data/data/com.termux/files/usr/bin/bash
# start_gui_kde.sh — Launch KDE Plasma on Termux:X11
#
# GPU Strategy (based on research):
#   - KWin compositor: SOFTWARE (KWIN_COMPOSE=Q) — KWin + Turnip GPU compositor is
#     documented as unreliable, causing black screens / crashes on proot Android.
#   - Individual KDE apps: GPU via Turnip + Zink if Turnip ICD is available in Termux.
#     Termux paths (/data/data/com.termux/...) are accessible from inside proot,
#     so VK_ICD_FILENAMES can point directly at the Termux Turnip ICD JSON.
#   - Fallback: LLVMpipe software rendering if Turnip not installed in Termux.
#
# Performance tweaks:
#   - Baloo file indexer disabled (massive I/O on Android storage)
#   - Akonadi disabled (mail/calendar daemon — not useful on Android)
#   - KDE Connect MDNS scanning disabled (constant network spam in proot)
#   - KWIN_EFFECTS_FORCE_ANIMATIONS=0 (no pointless animation overhead)
#
# PulseAudio: Started from Termux (same as XFCE4 approach), TCP-connected inside proot.

DISTRO=${1:-debian}

# ── Termux Turnip ICD paths ──────────────────────────────────────────────────
TERMUX_PREFIX="/data/data/com.termux/files/usr"
TURNIP_ICD_JSON="$TERMUX_PREFIX/share/vulkan/icd.d/freedreno_icd.aarch64.json"
TURNIP_LIB="$TERMUX_PREFIX/lib/libvulkan_freedreno.so"

# ── Kill stale X11 sessions ──────────────────────────────────────────────────
kill -9 $(pgrep -f "termux.x11") 2>/dev/null
sleep 1

# ── PulseAudio (XFCE4 pattern: start from Termux, connect via TCP inside proot) ─
pulseaudio --start \
    --load="module-native-protocol-tcp auth-ip-acl=127.0.0.1 auth-anonymous=1" \
    --exit-idle-time=-1 2>/dev/null || true

# ── Detect Turnip GPU availability ──────────────────────────────────────────
if [ -f "$TURNIP_ICD_JSON" ] && [ -f "$TURNIP_LIB" ]; then
    echo "[FluxLinux] Turnip GPU detected — apps will use Zink/Turnip hardware rendering"
    GPU_MODE="turnip"
    export GALLIUM_DRIVER=zink
    export MESA_LOADER_DRIVER_OVERRIDE=zink
    export VK_ICD_FILENAMES="$TURNIP_ICD_JSON"
    export TU_DEBUG=noconform          # Fix: vkGetCalibratedTimestampsEXT crash
    export ZINK_NO_TIMELINES=1         # Fix: timeline semaphore crash on Turnip
else
    echo "[FluxLinux] Turnip not found — using software rendering (LLVMpipe)"
    GPU_MODE="software"
fi

# ── Suppress Qt/KDE debug spam ──────────────────────────────────────────────
export QT_LOGGING_RULES="*.debug=false;qt.dbus.*=false;kf.*=false;kscreen.*=false"

# ── Fix XDG_RUNTIME_DIR (Qt rejects world-writable /tmp) ────────────────────
FLUX_RUNTIME_DIR="${TMPDIR:-$TERMUX_PREFIX/tmp}/runtime-flux-kde"
mkdir -p "$FLUX_RUNTIME_DIR"
chmod 700 "$FLUX_RUNTIME_DIR"
export XDG_RUNTIME_DIR="$FLUX_RUNTIME_DIR"

# ── Start Termux:X11 ─────────────────────────────────────────────────────────
termux-x11 :0 &>/dev/null &
sleep 3
am start --user 0 -n com.termux.x11/com.termux.x11.MainActivity >/dev/null 2>&1
sleep 1

# ── Launch KDE Plasma inside PRoot ──────────────────────────────────────────
proot-distro login "$DISTRO" --shared-tmp -- /bin/bash -c "
  # Fix XDG_RUNTIME_DIR inside proot
  mkdir -p /tmp/runtime-flux-kde
  chmod 700 /tmp/runtime-flux-kde

  export DISPLAY=:0
  export XDG_RUNTIME_DIR=/tmp/runtime-flux-kde

  # PulseAudio TCP (Termux runs PA, proot apps connect via loopback)
  export PULSE_SERVER=tcp:127.0.0.1
  export PULSE_LATENCY_MSEC=60

  # ── GPU: Zink + Turnip if available (Termux path accessible from proot) ───
  GPU_MODE='${GPU_MODE}'
  if [ \"\$GPU_MODE\" = 'turnip' ]; then
    export GALLIUM_DRIVER=zink
    export MESA_LOADER_DRIVER_OVERRIDE=zink
    # Termux Vulkan ICD path is accessible inside proot (/data/data/... is reachable)
    export VK_ICD_FILENAMES='${TURNIP_ICD_JSON}'
    export TU_DEBUG=noconform
    export ZINK_NO_TIMELINES=1
  fi

  # ── KWin Compositor ───────────────────────────────────────────────────────
  # RESEARCH VERDICT: KWin + Turnip GPU compositor = unreliable on proot Android.
  # Documented to cause black screens and crashes. Use software compositing:
  export KWIN_OPENGL_INTERFACE=egl
  export KWIN_COMPOSE=Q                # Software compositing — stable, always works
  export KWIN_EFFECTS_FORCE_ANIMATIONS=0

  # ── Qt settings ────────────────────────────────────────────────────────────
  export QT_QPA_PLATFORMTHEME=kde
  export QT_SCALE_FACTOR=1
  export QT_LOGGING_RULES='*.debug=false;qt.dbus.*=false;kf.*=false'

  # ── KDE performance ────────────────────────────────────────────────────────
  export BALOO_NOINDEX=1               # Disable file indexing (Android I/O is slow)
  export AKONADI_DISABLE=1             # Disable mail/calendar daemon
  export KDECONNECT_MDNS_DISABLE=1     # Disable MDNS scanning spam

  su - flux -c \"
    export DISPLAY=:0
    export XDG_RUNTIME_DIR=/tmp/runtime-flux-kde
    export PULSE_SERVER=tcp:127.0.0.1
    export PULSE_LATENCY_MSEC=60

    # GPU (Zink/Turnip for apps, not for KWin compositor)
    if [ '${GPU_MODE}' = 'turnip' ]; then
      export GALLIUM_DRIVER=zink
      export MESA_LOADER_DRIVER_OVERRIDE=zink
      export VK_ICD_FILENAMES='${TURNIP_ICD_JSON}'
      export TU_DEBUG=noconform
      export ZINK_NO_TIMELINES=1
    fi

    # KWin: software compositor (reliable on proot)
    export KWIN_OPENGL_INTERFACE=egl
    export KWIN_COMPOSE=Q
    export KWIN_EFFECTS_FORCE_ANIMATIONS=0

    # Qt
    export QT_QPA_PLATFORMTHEME=kde
    export QT_SCALE_FACTOR=1
    export QT_LOGGING_RULES='*.debug=false;qt.dbus.*=false;kf.*=false'

    # Performance: disable heavy background services
    export BALOO_NOINDEX=1
    export AKONADI_DISABLE=1
    export KDECONNECT_MDNS_DISABLE=1
    balooctl suspend 2>/dev/null || true
    balooctl disable 2>/dev/null || true

    # Launch KDE Plasma
    dbus-run-session -- startplasma-x11 2>&1
  \"
"

exit 0
