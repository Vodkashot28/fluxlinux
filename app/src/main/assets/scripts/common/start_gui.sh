#!/data/data/com.termux/files/usr/bin/bash
# start_gui.sh
# Launch XFCE4 Desktop Environment in PRoot Distro
# Based on LinuxDroidMaster reference implementation

# WORKAROUND for X11 lock file permissions:
# Use a unique XDG_RUNTIME_DIR to avoid conflicts with stale lock files
export XDG_RUNTIME_DIR="$TMPDIR/fluxlinux-$$"
mkdir -p "$XDG_RUNTIME_DIR"

# Kill any existing X11 processes
pkill -9 -f "termux.x11" 2>/dev/null
pkill -9 -f "com.termux.x11" 2>/dev/null

# Try to clean lock files (may fail without root, but that's OK now)
rm -rf "$TMPDIR/.X0-lock" "$TMPDIR/.X11-unix/X0" 2>/dev/null

# Wait for processes to terminate
sleep 1

# Enable PulseAudio over Network
pulseaudio --start --load="module-native-protocol-tcp auth-ip-acl=127.0.0.1 auth-anonymous=1" --exit-idle-time=-1

# Start termux-x11 with our custom runtime dir
termux-x11 :0 >/dev/null &

# Wait for termux-x11 to start
sleep 3

# Launch Termux X11 main activity
am start --user 0 -n com.termux.x11/com.termux.x11.MainActivity > /dev/null 2>&1
sleep 1

# Login in PRoot Environment
# Usage: ./start_gui.sh <distro_alias>
DISTRO=${1:-debian}

if [ "$DISTRO" == "termux" ]; then
    echo "Launching GUI for Termux Native..."
    export PULSE_SERVER=127.0.0.1
    env DISPLAY=:0 startxfce4
else
    echo "Launching GUI for $DISTRO (PRoot)..."
    proot-distro login $DISTRO --shared-tmp -- /bin/bash -c 'export PULSE_SERVER=127.0.0.1 && export XDG_RUNTIME_DIR=${TMPDIR} && su - flux -c "env DISPLAY=:0 startxfce4"'
fi

exit 0
