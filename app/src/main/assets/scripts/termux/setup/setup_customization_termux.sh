#!/bin/bash
# ============================================================
# FluxLinux — Native Termux XFCE4 Customization
# Location: termux/setup/setup_customization_termux.sh
# Runs on: HOST Termux (native, no container)
# Root required: no
#
# Applies the FluxLinux dark theme to native Termux XFCE4:
# - GTK dark theme (Adwaita-dark fallback)
# - Papirus-Dark icon theme
# - Noto Sans font at 2x DPI for Android screens
# - Wallpaper
# - Disables compositor for performance
# - 2x window scaling for HiDPI
# ============================================================

CALLBACK_NAME="customization"

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
echo "  FluxLinux — XFCE4 Customization (Native)"
echo "══════════════════════════════════════════════"
echo ""

# ── Directories ───────────────────────────────────────────
mkdir -p "$HOME/.themes"
mkdir -p "$HOME/.icons"
mkdir -p "$HOME/.fonts"
mkdir -p "$HOME/.fluxlinux/wallpapers"
mkdir -p "$HOME/.config/xfce4/xfconf/xfce-perchannel-xml"

# ── Step 1: Icon theme ────────────────────────────────────
echo "===== Installing Papirus Icon Theme ====="
pkg install -y papirus-icon-theme 2>/dev/null || {
    echo " [⚠️] papirus-icon-theme not in repo — downloading..."
    PAPIRUS_URL="https://github.com/PapirusDevelopmentTeam/papirus-icon-theme/archive/refs/heads/master.tar.gz"
    wget -q --show-progress "$PAPIRUS_URL" -O /tmp/papirus.tar.gz || {
        echo " [⚠️] Papirus download failed — using Adwaita icons as fallback"
    }
    if [ -f /tmp/papirus.tar.gz ]; then
        tar -xzf /tmp/papirus.tar.gz -C "$HOME/.icons/" 2>/dev/null && \
        mv "$HOME/.icons/papirus-icon-theme-master/Papirus-Dark" "$HOME/.icons/Papirus-Dark" 2>/dev/null && \
        mv "$HOME/.icons/papirus-icon-theme-master/Papirus" "$HOME/.icons/Papirus" 2>/dev/null || true
        rm -rf "$HOME/.icons/papirus-icon-theme-master" /tmp/papirus.tar.gz 2>/dev/null || true
    fi
}
echo " [✅] Icon theme ready"

# ── Step 2: Wallpaper ─────────────────────────────────────
echo ""
echo "===== Setting Up Wallpaper ====="
# Download a FluxLinux-style dark wallpaper
WALLPAPER_PATH="$HOME/.fluxlinux/wallpapers/flux_dark.jpg"
if [ ! -f "$WALLPAPER_PATH" ]; then
    # Use a public domain dark abstract wallpaper
    wget -q --show-progress \
        "https://raw.githubusercontent.com/sabamdarif/termux-desktop/main/wallpaper/wall-0.jpg" \
        -O "$WALLPAPER_PATH" 2>/dev/null || {
        # Fallback: create a solid dark wallpaper via convert (ImageMagick)
        if command -v convert >/dev/null 2>&1; then
            convert -size 1920x1080 xc:#0d1117 "$WALLPAPER_PATH" 2>/dev/null || true
        fi
        echo " [⚠️] Using fallback wallpaper"
    }
fi
echo " [✅] Wallpaper ready: $WALLPAPER_PATH"

# ── Step 3: XFCE4 xfconf settings ────────────────────────
echo ""
echo "===== Applying XFCE4 Theme Settings ====="

# GTK Theme (Adwaita-dark is built into GTK — always available)
xfconf-query -c xsettings -p /Net/ThemeName -s "Adwaita-dark" 2>/dev/null || \
    mkdir -p "$HOME/.config/gtk-3.0" && \
    echo '[Settings]
gtk-theme-name=Adwaita-dark
gtk-icon-theme-name=Papirus-Dark
gtk-font-name=Noto Sans 10
gtk-application-prefer-dark-theme=1' > "$HOME/.config/gtk-3.0/settings.ini"
echo " [✅] GTK dark theme applied"

# Icon theme
xfconf-query -c xsettings -p /Net/IconThemeName -s "Papirus-Dark" 2>/dev/null || true
echo " [✅] Icon theme applied"

# Font
xfconf-query -c xsettings -p /Gtk/FontName -s "Noto Sans 10" 2>/dev/null || true
xfconf-query -c xfwm4 -p /general/title_font -s "Noto Sans Bold 10" 2>/dev/null || true
echo " [✅] Fonts applied"

# Wallpaper (applied to default monitor/workspace)
xfconf-query -c xfce4-desktop \
    -p /backdrop/screen0/monitorVirtual-1/workspace0/last-image \
    -s "$WALLPAPER_PATH" 2>/dev/null || true
# Also try monitor0 naming
xfconf-query -c xfce4-desktop \
    -p /backdrop/screen0/monitor0/workspace0/last-image \
    -s "$WALLPAPER_PATH" 2>/dev/null || true
echo " [✅] Wallpaper applied"

# ── Step 4: Performance settings ─────────────────────────
echo ""
echo "===== Applying Performance Settings ====="

# Disable compositor (mobile GPU cannot handle it well)
xfconf-query -c xfwm4 -p /general/use_compositing -s false 2>/dev/null || true
echo " [✅] Compositor disabled (performance)"

# HiDPI scaling (2x for Android phone screens)
xfconf-query -c xsettings -p /Gdk/WindowScalingFactor -s 2 2>/dev/null || true
echo " [✅] Window scaling set to 2x (HiDPI)"

# ── Step 5: Panel layout ──────────────────────────────────
echo ""
echo "===== Configuring XFCE4 Panel ====="
# Set a minimal panel config with dark appearance
PANEL_CFG="$HOME/.config/xfce4/xfconf/xfce-perchannel-xml/xfce4-panel.xml"
if [ ! -f "$PANEL_CFG" ]; then
    cat > "$PANEL_CFG" << 'PANELEOF'
<?xml version="1.0" encoding="UTF-8"?>
<channel name="xfce4-panel" version="1.0">
  <property name="configver" type="sint" value="2"/>
  <property name="panels" type="array">
    <value type="int" value="1"/>
    <property name="panel-1">
      <property name="position" type="string" value="p=8;x=0;y=0"/>
      <property name="length" type="uint" value="100"/>
      <property name="position-locked" type="bool" value="true"/>
      <property name="size" type="uint" value="36"/>
      <property name="plugin-ids" type="array">
        <value type="int" value="1"/>
        <value type="int" value="2"/>
        <value type="int" value="3"/>
        <value type="int" value="4"/>
        <value type="int" value="5"/>
      </property>
    </property>
  </property>
</channel>
PANELEOF
    echo " [✅] Panel layout configured"
fi

# ── Callback to app ──────────────────────────────────────
am start -a android.intent.action.VIEW \
  -d "fluxlinux://callback?result=success&name=${CALLBACK_NAME}" \
  --flags 0x10000000 2>/dev/null || true

echo ""
echo "🎨 XFCE4 customization applied!"
echo "   Theme: Adwaita-dark | Icons: Papirus-Dark | Scale: 2x"
echo "   Compositor disabled for mobile GPU performance."
echo "   Restart XFCE4 for changes to fully take effect."
read -p "Press Enter to close..."
