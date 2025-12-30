# Quick Fix for Existing Chroot Installation

## Problem
VirGL server is running but chroot can't connect due to missing XDG_RUNTIME_DIR

## Solution
Add XDG_RUNTIME_DIR=/tmp to the chroot launcher

## Steps to Fix Existing Installation

### 1. Edit the launcher script as root
```bash
su
nano /data/local/tmp/start_debian13.sh
```

### 2. Find the line (around line 256):
```bash
export DISPLAY=:0 && export PULSE_SERVER=127.0.0.1 && dbus-launch --exit-with-session startxfce4
```

### 3. Change it to:
```bash
export DISPLAY=:0 && export PULSE_SERVER=127.0.0.1 && export XDG_RUNTIME_DIR=/tmp && dbus-launch --exit-with-session startxfce4
```

### 4. Save (Ctrl+O, Enter, Ctrl+X)

### 5. Restart chroot GUI
```bash
sh /data/local/tmp/start_debian13_gui.sh
```

### 6. Test in chroot
```bash
echo $XDG_RUNTIME_DIR  # Should show: /tmp
gpu-launch glmark2     # Should work without "lost connection" error
```

## Verification
- VirGL server running in Termux: `ps aux | grep virgl`
- XDG_RUNTIME_DIR set in chroot: `echo $XDG_RUNTIME_DIR`
- GPU acceleration working: `gpu-launch glxinfo | grep "OpenGL renderer"` should show "virgl"
