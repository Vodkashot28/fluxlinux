#!/bin/sh
# HyperOS-Compatible X11 Launcher with Debug Logging
# FluxLinux - Debian 13 Chroot GUI Starter

echo "========================================"
echo "FluxLinux: Starting Debian 13 GUI"
echo "HyperOS Compatibility Mode"
echo "========================================"

TARGET_TERMUX_PREFIX="/data/data/com.termux/files/usr"
BB="/data/adb/magisk/busybox"

# Debug: Check SELinux
SELINUX_STATUS=$(getenforce)
echo "[INFO] SELinux Status: $SELINUX_STATUS"

if [ "$SELINUX_STATUS" = "Enforcing" ]; then
    echo "[FIX] Setting SELinux to Permissive for X11..."
    setenforce 0
    if [ "$(getenforce)" = "Permissive" ]; then
        echo "[OK] SELinux set to Permissive"
        echo "[NOTE] Will reset to Enforcing on reboot"
    else
        echo "[WARN] SELinux change failed - continuing anyway"
    fi
fi

# 1. Kill old X11 processes
echo "[1/7] Cleaning up old X11 processes..."
killall -9 termux-x11 Xwayland >/dev/null 2>&1
pkill -f com.termux.x11 >/dev/null 2>&1
sleep 1

# 2. Clean sockets and locks
echo "[2/7] Cleaning X11 sockets..."
rm -rf $TARGET_TERMUX_PREFIX/tmp/.X11-unix
rm -rf $TARGET_TERMUX_PREFIX/tmp/.X0-lock
rm -rf $TARGET_TERMUX_PREFIX/tmp/.X1-lock

# Create X11 socket directory with correct permissions
mkdir -p $TARGET_TERMUX_PREFIX/tmp/.X11-unix
chmod 1777 $TARGET_TERMUX_PREFIX/tmp/.X11-unix

# 3. Fix SELinux contexts (HyperOS fix)
if command -v chcon >/dev/null 2>&1; then
    echo "[3/7] Fixing SELinux contexts..."
    chcon -R u:object_r:tmpfs:s0 $TARGET_TERMUX_PREFIX/tmp 2>/dev/null || true
    chcon u:object_r:tmpfs:s0 $TARGET_TERMUX_PREFIX/tmp/.X11-unix 2>/dev/null || true
fi

# 4. Start Termux X11 app
echo "[4/7] Starting Termux X11 app..."
am start --user 0 -n com.termux.x11/com.termux.x11.MainActivity >/dev/null 2>&1

# 5. Mount /tmp to chroot
echo "[5/7] Mounting /tmp to chroot..."
$BB mount --bind $TARGET_TERMUX_PREFIX/tmp /data/local/tmp/chrootDebian13/tmp 2>/dev/null
chmod -R 1777 $TARGET_TERMUX_PREFIX/tmp

# 6. Start X server with debug logging
echo "[6/7] Starting X server on :0..."
export XDG_RUNTIME_DIR="$TARGET_TERMUX_PREFIX/tmp"
export TMPDIR="$XDG_RUNTIME_DIR"
export LD_LIBRARY_PATH=$TARGET_TERMUX_PREFIX/lib
export TERMUX_X11_DEBUG=1

$TARGET_TERMUX_PREFIX/bin/termux-x11 :0 -ac > /tmp/termux-x11.log 2>&1 &
X11_PID=$!
echo "[INFO] X11 PID: $X11_PID"

sleep 3

# Verify X11 socket creation
if [ -S "$TARGET_TERMUX_PREFIX/tmp/.X11-unix/X0" ]; then
    echo "[OK] X11 socket created successfully"
    echo "[INFO] Socket details:"
    ls -laZ $TARGET_TERMUX_PREFIX/tmp/.X11-unix/ 2>/dev/null | head -3 || ls -la $TARGET_TERMUX_PREFIX/tmp/.X11-unix/ | head -3
else
    echo "[ERROR] X11 socket NOT created!"
    echo ""
    echo "[DEBUG] X11 process status:"
    ps aux | grep termux-x11 | grep -v grep
    echo ""
    echo "[DEBUG] /tmp contents:"
    ls -la $TARGET_TERMUX_PREFIX/tmp/ | head -10
    echo ""
    echo "[DEBUG] Recent SELinux denials:"
    dmesg | grep "avc.*denied.*termux" | tail -3
    echo ""
    echo "Check detailed logs: cat /tmp/termux-x11.log"
    echo ""
fi

# Verify X11 process is running
if ps -p $X11_PID >/dev/null 2>&1; then
    echo "[OK] X11 server running (PID: $X11_PID)"
else
    echo "[ERROR] X11 server process died!"
    echo "Check logs: cat /tmp/termux-x11.log"
fi

# 7. Verify services
echo "[7/7] Checking services..."
pgrep -f pulseaudio >/dev/null && echo "[OK] PulseAudio running" || echo "[!] PulseAudio not running - audio may fail"

if pgrep -f virgl_test_server >/dev/null; then
    echo "[OK] VirGL server running"
    ls -la $TARGET_TERMUX_PREFIX/tmp/.virgl_test 2>/dev/null || echo "[WARN] VirGL socket not visible"
else
    echo "[!] VirGL not running - GPU acceleration will NOT work"
    echo "[!] Please restart the GUI from FluxLinux app"
fi

echo ""
echo "Entering chroot..."
sh /data/local/tmp/start_debian13.sh
