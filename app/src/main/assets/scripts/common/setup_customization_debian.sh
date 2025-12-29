#!/bin/bash
# setup_customization_debian.sh
# Applies "FluxLinux" branding and customization to Debian XFCE4 Desktop
# Works for both Chroot and Proot environments (run as root, switches to user 'flux')

CUSTOM_USER="flux"
CUSTOM_GROUP="users"
USER_HOME="/home/$CUSTOM_USER"
ASSETS_DIR="$(dirname "$0")/../../../assets"
THEME_DIR="/usr/share/themes"
ICON_DIR="/usr/share/icons"

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

echo "FluxLinux: Starting XFCE4 Customization..."

# 1. Install Dependencies
echo "FluxLinux: Installing customization tools..."
export DEBIAN_FRONTEND=noninteractive
apt update -y
apt install -y xfce4-goodies curl unzip fontconfig || handle_error "Dependency Installation"

# 2. Deploy Assets (From GitHub Release debian-v1)
ASSET_REPO="abhay-byte/fluxlinux"
ASSET_TAG="debian-v1"
BASE_URL="https://github.com/$ASSET_REPO/releases/download/$ASSET_TAG"

echo "FluxLinux: Downloading assets from $BASE_URL..."

# Helper to extract all contents
extract_all_assets() {
    local URL="$1"
    local TARGET_DIR="$2"
    local TEMP_ZIP="/tmp/$(basename "$URL")"
    
    echo " - Downloading $(basename "$URL")..."
    wget -q --show-progress "$URL" -O "$TEMP_ZIP"
    
    echo " - Extracting to $TARGET_DIR..."
    unzip -q -o "$TEMP_ZIP" -d "$TARGET_DIR"
    rm "$TEMP_ZIP"

    # Extract any nested tarballs found in the target dir
    find "$TARGET_DIR" -maxdepth 1 -name "*.tar.xz" -exec tar -xf {} -C "$TARGET_DIR" \;
    find "$TARGET_DIR" -maxdepth 1 -name "*.tar.gz" -exec tar -xzf {} -C "$TARGET_DIR" \;
    
    # Cleanup tars
    rm -f "$TARGET_DIR"/*.tar.xz "$TARGET_DIR"/*.tar.gz
}

# 3. Theme Selection Prompt
echo "------------------------------------------------"
echo "Select Theme Preference:"
echo "1) Dark (Default)"
echo "2) Light"
read -p "Enter choice [1-2]: " THEME_CHOICE
echo "------------------------------------------------"

if [ "$THEME_CHOICE" == "2" ]; then
    echo "FluxLinux: Light Mode Selected."
    SEL_THEME="Space-light"
    SEL_ICON="Papirus" # Light icons
    SEL_CURSOR="Vimix-cursors" # Dark cursor for light theme (better contrast)
    SEL_WALLPAPER="fluxlinux-light.png"
else
    echo "FluxLinux: Dark Mode Selected."
    SEL_THEME="Space-transparency"
    SEL_ICON="Papirus-Dark" # Dark icons
    SEL_CURSOR="Vimix-white-cursors" # White cursor for dark theme (better contrast)
    SEL_WALLPAPER="fluxlinux-dark.png"
fi

# Install Themes (Both)
echo "FluxLinux: Installing Themes..."
mkdir -p "$THEME_DIR"
extract_all_assets "$BASE_URL/theme.zip" "$THEME_DIR"

# Install Icons
echo "FluxLinux: Installing Icons..."
mkdir -p "$ICON_DIR"
extract_all_assets "$BASE_URL/icons.zip" "$ICON_DIR"
# Icons are assumed to have known names or we use the selected one directly.
# SEL_ICON is already set based on theme choice.

# Install Cursors (Both variants)
echo "FluxLinux: Installing Cursors..."
extract_all_assets "$BASE_URL/cursor.zip" "$ICON_DIR"

# Wallpaper Setup
WALLPAPER_DIR="$USER_HOME/Pictures/Wallpapers"
mkdir -p "$WALLPAPER_DIR"
chown -R "$CUSTOM_USER:$CUSTOM_GROUP" "$USER_HOME/Pictures" 2>/dev/null

echo "FluxLinux: Downloading Wallpaper..."
TEMP_WP_ZIP="/tmp/wallpaper.zip"
wget -q --show-progress "$BASE_URL/wallpaper.zip" -O "$TEMP_WP_ZIP"
unzip -o -j "$TEMP_WP_ZIP" -d "$WALLPAPER_DIR"
rm "$TEMP_WP_ZIP"
[ -f "$WALLPAPER_DIR/dark.png" ] && mv "$WALLPAPER_DIR/dark.png" "$WALLPAPER_DIR/fluxlinux-dark.png"
[ -f "$WALLPAPER_DIR/light.png" ] && mv "$WALLPAPER_DIR/light.png" "$WALLPAPER_DIR/fluxlinux-light.png"
chown "$CUSTOM_USER:$CUSTOM_GROUP" "$WALLPAPER_DIR"/*


# Set Font
# Install JetBrains Mono Nerd Font (From Repository Release)
FONT_DIR="/usr/local/share/fonts/NerdFonts"
if [ ! -d "$FONT_DIR" ]; then
    echo "FluxLinux: Installing JetBrains Mono Nerd Font..."
    mkdir -p "$FONT_DIR"
    extract_all_assets "$BASE_URL/font.zip" "$FONT_DIR"
    fc-cache -f
fi
# 4. Apply Settings for User 'flux'
# We use xfconf-query inside a dbus-launch session to ensure settings stick even if GUI not running.
echo "FluxLinux: Applying XFCE4 Settings..."

apply_xfce_settings() {
    # Helper to set properties
    # $1 = Channel, $2 = Property, $3 = Type, $4 = Value
    su - "$CUSTOM_USER" -c "DISPLAY=:0 dbus-launch xfconf-query -c $1 -p $2 -n -t $3 -s '$4'"
    # Note: '-n' creates if not exists
}

# 2x Scaling
apply_xfce_settings "xsettings" "/Gdk/WindowScalingFactor" "int" "2"
apply_xfce_settings "xfwm4" "/general/theme" "string" "Default"

# Apply Theme
if [ -n "$SEL_THEME" ]; then
    echo "FluxLinux: Applying Theme '$SEL_THEME'"
    apply_xfce_settings "xsettings" "/Net/ThemeName" "string" "$SEL_THEME"
    apply_xfce_settings "xfwm4" "/general/theme" "string" "$SEL_THEME"
fi

# Apply Icon Theme
if [ -n "$SEL_ICON" ]; then
    echo "FluxLinux: Applying Icon Theme '$SEL_ICON'"
    apply_xfce_settings "xsettings" "/Net/IconThemeName" "string" "$SEL_ICON"
fi

# Apply Cursor Theme
if [ -n "$SEL_CURSOR" ]; then
    echo "FluxLinux: Applying Cursor Theme '$SEL_CURSOR'"
    apply_xfce_settings "xsettings" "/Gtk/CursorThemeName" "string" "$SEL_CURSOR"
fi
apply_xfce_settings "xsettings" "/Gtk/FontName" "string" "JetBrainsMono Nerd Font 10"
apply_xfce_settings "xsettings" "/Gtk/MonospaceFontName" "string" "JetBrainsMono Nerd Font Mono 10"

# Set Wallpaper (xfce4-desktop)
MONITORS="monitor0 monitor1 monitorVNC-0 monitorbuiltin builtin monitorHDMI-A-0 monitorVirtual-0 monitorVirtual1"
WALLPAPER_PATH="$WALLPAPER_DIR/$SEL_WALLPAPER"

echo "FluxLinux: Applying Wallpaper to [$MONITORS]..."
for M in $MONITORS; do
    # Image
    su - "$CUSTOM_USER" -c "DISPLAY=:0 dbus-launch xfconf-query -c xfce4-desktop -p /backdrop/screen0/$M/workspace0/last-image -n -t string -s '$WALLPAPER_PATH'"
    # Style (Zoomed=5)
    su - "$CUSTOM_USER" -c "DISPLAY=:0 dbus-launch xfconf-query -c xfce4-desktop -p /backdrop/screen0/$M/workspace0/image-style -n -t int -s 5"
done


# 5. Configure Terminal (Direct Config File)
echo "FluxLinux: Configuring Terminal..."
TERM_CONFIG_DIR="$USER_HOME/.config/xfce4/terminal"
mkdir -p "$TERM_CONFIG_DIR"
cat <<EOF > "$TERM_CONFIG_DIR/terminalrc"
[Configuration]
FontName=JetBrainsMono Nerd Font 12
MiscAlwaysShowTabs=FALSE
MiscBell=FALSE
MiscBordersDefault=TRUE
MiscCursorBlinks=FALSE
MiscCursorShape=TERMINAL_CURSOR_SHAPE_IBEAM
MiscDefaultGeometry=80x24
MiscInheritGeometry=FALSE
MiscMenubarDefault=FALSE
MiscMouseAutohide=FALSE
MiscToolbarDefault=FALSE
MiscConfirmClose=TRUE
MiscCycleTabs=TRUE
MiscTabCloseButtons=TRUE
MiscTabCloseMiddleClick=TRUE
MiscTabPosition=TERMINAL_TAB_POSITION_TOP
MiscHighlightUrls=TRUE
MiscScrollAlternateScreen=TRUE
ScrollingLines=1000
BackgroundMode=TERMINAL_BACKGROUND_TRANSPARENT
BackgroundDarkness=0.7
EOF
chown -R "$CUSTOM_USER:$CUSTOM_GROUP" "$USER_HOME/.config"

# 6. Reload XFCE Daemons (Force restart like chroot script does)
echo "FluxLinux: Reloading Desktop..."

# Kill existing XFCE processes to force reload (matches chroot pattern)
su - "$CUSTOM_USER" -c "killall -9 xfdesktop xfwm4 xfsettingsd" 2>/dev/null
sleep 2

# Restart daemons with updated settings (run in background but wait a bit for each)
su - "$CUSTOM_USER" -c "DISPLAY=:0 nohup xfdesktop > /dev/null 2>&1 &" 2>/dev/null
sleep 0.5
su - "$CUSTOM_USER" -c "DISPLAY=:0 nohup xfwm4 --replace > /dev/null 2>&1 &" 2>/dev/null
sleep 0.5
su - "$CUSTOM_USER" -c "DISPLAY=:0 nohup xfsettingsd > /dev/null 2>&1 &" 2>/dev/null
sleep 1

echo "FluxLinux: Customization Complete!"
echo "------------------------------------------------"
read -p "Press Enter to close..."
