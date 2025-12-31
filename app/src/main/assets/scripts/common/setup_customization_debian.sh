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
if [ -n "$FLUX_THEME" ]; then
    echo "FluxLinux: Auto-applying Theme: $FLUX_THEME"
    if [ "$FLUX_THEME" == "light" ]; then
        THEME_CHOICE="2"
    else
        THEME_CHOICE="1"
    fi
else
    echo "------------------------------------------------"
    echo "Select Theme Preference:"
    echo "1) Dark (Default)"
    echo "2) Light"
    read -p "Enter choice [1-2]: " THEME_CHOICE
    echo "------------------------------------------------"
fi

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
    su -s /bin/bash - "$CUSTOM_USER" -c "DISPLAY=:0 dbus-launch xfconf-query -c $1 -p $2 -n -t $3 -s '$4'"
    # Note: '-n' creates if not exists
}

# 2x Scaling
apply_xfce_settings "xsettings" "/Gdk/WindowScalingFactor" "int" "2"

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

# Apply Fonts
apply_xfce_settings "xsettings" "/Gtk/FontName" "string" "JetBrainsMono Nerd Font 10"
apply_xfce_settings "xsettings" "/Gtk/MonospaceFontName" "string" "JetBrainsMono Nerd Font Mono 10"
apply_xfce_settings "xfwm4" "/general/title_font" "string" "JetBrainsMono Nerd Font Bold 10"

# Set Wallpaper (xfce4-desktop)
MONITORS="monitor0 monitor1 monitorVNC-0 monitorbuiltin builtin monitorHDMI-A-0 monitorVirtual-0 monitorVirtual1"
WALLPAPER_PATH="$WALLPAPER_DIR/$SEL_WALLPAPER"

echo "FluxLinux: Applying Wallpaper to [$MONITORS]..."
for M in $MONITORS; do
    # Image
    su -s /bin/bash - "$CUSTOM_USER" -c "DISPLAY=:0 dbus-launch xfconf-query -c xfce4-desktop -p /backdrop/screen0/$M/workspace0/last-image -n -t string -s '$WALLPAPER_PATH'"
    # Style (Zoomed=5)
    su -s /bin/bash - "$CUSTOM_USER" -c "DISPLAY=:0 dbus-launch xfconf-query -c xfce4-desktop -p /backdrop/screen0/$M/workspace0/image-style -n -t int -s 5"
done


# 5. Configure XFCE4 Panel
echo "FluxLinux: Configuring Panel..."
PANEL_CONFIG_DIR="$USER_HOME/.config/xfce4/xfconf/xfce-perchannel-xml"
mkdir -p "$PANEL_CONFIG_DIR"

cat <<'EOF' > "$PANEL_CONFIG_DIR/xfce4-panel.xml"
<?xml version="1.1" encoding="UTF-8"?>

<channel name="xfce4-panel" version="1.0">
  <property name="configver" type="int" value="2"/>
  <property name="panels" type="array">
    <value type="int" value="1"/>
    <value type="int" value="2"/>
    <property name="dark-mode" type="bool" value="true"/>
    <property name="panel-1" type="empty">
      <property name="position" type="string" value="p=6;x=0;y=0"/>
      <property name="length" type="double" value="100"/>
      <property name="position-locked" type="bool" value="true"/>
      <property name="icon-size" type="uint" value="16"/>
      <property name="size" type="uint" value="25"/>
      <property name="plugin-ids" type="array">
        <value type="int" value="1"/>
        <value type="int" value="2"/>
        <value type="int" value="3"/>
        <value type="int" value="33"/>
        <value type="int" value="20"/>
        <value type="int" value="21"/>
        <value type="int" value="32"/>
        <value type="int" value="23"/>
        <value type="int" value="31"/>
        <value type="int" value="24"/>
        <value type="int" value="34"/>
        <value type="int" value="5"/>
        <value type="int" value="6"/>
        <value type="int" value="7"/>
        <value type="int" value="8"/>
        <value type="int" value="9"/>
        <value type="int" value="10"/>
      </property>
    </property>
    <property name="panel-2" type="empty">
      <property name="autohide-behavior" type="uint" value="1"/>
      <property name="position" type="string" value="p=10;x=0;y=0"/>
      <property name="length" type="uint" value="1"/>
      <property name="position-locked" type="bool" value="true"/>
      <property name="size" type="uint" value="48"/>
      <property name="plugin-ids" type="array">
        <value type="int" value="11"/>
        <value type="int" value="12"/>
        <value type="int" value="13"/>
        <value type="int" value="14"/>
        <value type="int" value="15"/>
        <value type="int" value="16"/>
        <value type="int" value="17"/>
        <value type="int" value="18"/>
      </property>
    </property>
  </property>
  <property name="plugins" type="empty">
    <property name="plugin-1" type="string" value="applicationsmenu">
      <property name="button-title" type="string" value="Menu"/>
      <property name="button-icon" type="string" value="open-menu"/>
      <property name="small" type="bool" value="true"/>
      <property name="show-tooltips" type="bool" value="false"/>
      <property name="show-generic-names" type="bool" value="false"/>
      <property name="custom-menu" type="bool" value="false"/>
      <property name="show-menu-icons" type="bool" value="false"/>
      <property name="show-button-title" type="bool" value="false"/>
    </property>
    <property name="plugin-2" type="string" value="tasklist">
      <property name="grouping" type="uint" value="1"/>
      <property name="flat-buttons" type="bool" value="false"/>
      <property name="show-only-minimized" type="bool" value="false"/>
      <property name="include-all-workspaces" type="bool" value="false"/>
      <property name="show-wireframes" type="bool" value="false"/>
      <property name="show-labels" type="bool" value="false"/>
    </property>
    <property name="plugin-3" type="string" value="separator">
      <property name="expand" type="bool" value="true"/>
      <property name="style" type="uint" value="0"/>
    </property>
    <property name="plugin-5" type="string" value="separator">
      <property name="style" type="uint" value="2"/>
    </property>
    <property name="plugin-6" type="string" value="systray">
      <property name="square-icons" type="bool" value="true"/>
    </property>
    <property name="plugin-7" type="string" value="separator">
      <property name="style" type="uint" value="0"/>
    </property>
    <property name="plugin-8" type="string" value="clock">
      <property name="digital-layout" type="uint" value="1"/>
      <property name="mode" type="uint" value="4"/>
      <property name="show-seconds" type="bool" value="true"/>
      <property name="show-inactive" type="bool" value="true"/>
      <property name="show-meridiem" type="bool" value="false"/>
      <property name="timezone" type="string" value="Asia/Kolkata"/>
    </property>
    <property name="plugin-9" type="string" value="separator">
      <property name="style" type="uint" value="0"/>
    </property>
    <property name="plugin-10" type="string" value="actions"/>
    <property name="plugin-11" type="string" value="showdesktop"/>
    <property name="plugin-12" type="string" value="separator"/>
    <property name="plugin-13" type="string" value="launcher">
      <property name="items" type="array">
        <value type="string" value="17669070471.desktop"/>
      </property>
    </property>
    <property name="plugin-14" type="string" value="launcher">
      <property name="items" type="array">
        <value type="string" value="17669070472.desktop"/>
      </property>
    </property>
    <property name="plugin-15" type="string" value="launcher">
      <property name="items" type="array">
        <value type="string" value="17669070473.desktop"/>
      </property>
    </property>
    <property name="plugin-16" type="string" value="launcher">
      <property name="items" type="array">
        <value type="string" value="17669070474.desktop"/>
      </property>
    </property>
    <property name="plugin-17" type="string" value="separator"/>
    <property name="plugin-18" type="string" value="directorymenu">
      <property name="base-directory" type="string" value="/home/flux"/>
    </property>
    <property name="plugin-20" type="string" value="cpufreq"/>
    <property name="plugin-21" type="string" value="cpugraph">
      <property name="update-interval" type="int" value="2"/>
      <property name="time-scale" type="int" value="0"/>
      <property name="size" type="int" value="16"/>
      <property name="mode" type="int" value="0"/>
      <property name="color-mode" type="int" value="0"/>
      <property name="frame" type="int" value="1"/>
      <property name="border" type="int" value="1"/>
      <property name="bars" type="int" value="1"/>
      <property name="per-core" type="int" value="0"/>
      <property name="tracked-core" type="int" value="0"/>
      <property name="in-terminal" type="int" value="1"/>
      <property name="startup-notification" type="int" value="0"/>
      <property name="load-threshold" type="int" value="0"/>
      <property name="smt-stats" type="int" value="1"/>
      <property name="smt-issues" type="int" value="1"/>
      <property name="per-core-spacing" type="int" value="1"/>
      <property name="command" type="string" value=""/>
      <property name="background" type="array">
        <value type="double" value="1"/>
        <value type="double" value="1"/>
        <value type="double" value="1"/>
        <value type="double" value="0"/>
      </property>
      <property name="foreground-1" type="array">
        <value type="double" value="0"/>
        <value type="double" value="1"/>
        <value type="double" value="0"/>
        <value type="double" value="1"/>
      </property>
      <property name="foreground-2" type="array">
        <value type="double" value="1"/>
        <value type="double" value="0"/>
        <value type="double" value="0"/>
        <value type="double" value="1"/>
      </property>
      <property name="foreground-3" type="array">
        <value type="double" value="0"/>
        <value type="double" value="0"/>
        <value type="double" value="1"/>
        <value type="double" value="1"/>
      </property>
      <property name="smt-issues-color" type="array">
        <value type="double" value="0.90000000000000002"/>
        <value type="double" value="0"/>
        <value type="double" value="0"/>
        <value type="double" value="1"/>
      </property>
      <property name="foreground-system" type="array">
        <value type="double" value="0.90000000000000002"/>
        <value type="double" value="0.10000000000000001"/>
        <value type="double" value="0.10000000000000001"/>
        <value type="double" value="1"/>
      </property>
      <property name="foreground-user" type="array">
        <value type="double" value="0.10000000000000001"/>
        <value type="double" value="0.40000000000000002"/>
        <value type="double" value="0.90000000000000002"/>
        <value type="double" value="1"/>
      </property>
      <property name="foreground-nice" type="array">
        <value type="double" value="0.90000000000000002"/>
        <value type="double" value="0.80000000000000004"/>
        <value type="double" value="0.20000000000000001"/>
        <value type="double" value="1"/>
      </property>
      <property name="foreground-iowait" type="array">
        <value type="double" value="0.20000000000000001"/>
        <value type="double" value="0.90000000000000002"/>
        <value type="double" value="0.40000000000000002"/>
        <value type="double" value="1"/>
      </property>
    </property>
    <property name="plugin-23" type="string" value="fsguard">
      <property name="display-meter" type="bool" value="false"/>
      <property name="show-size" type="bool" value="true"/>
    </property>
    <property name="plugin-24" type="string" value="genmon">
      <property name="command" type="string" value="/bin/bash -c &quot;free -m | awk '/Mem:/ {r=\$3/1024; t=\$2/1024} /Swap:/ {s=\$3/1024; st=\$2/1024} END {printf \&quot;&lt;txt&gt;RAM %.1f/%.1fGB | SWAP %.1f/%.1fGB&lt;/txt&gt;\&quot;, r, t, s, st}'&quot;"/>
      <property name="update-interval" type="uint" value="2000"/>
      <property name="use-label" type="bool" value="false"/>
      <property name="font" type="string" value="JetBrainsMono Nerd Font 10"/>
    </property>
    <property name="plugin-31" type="string" value="separator"/>
    <property name="plugin-32" type="string" value="separator"/>
    <property name="plugin-33" type="string" value="separator"/>
    <property name="plugin-34" type="string" value="separator"/>
  </property>
</channel>
EOF

chown -R "$CUSTOM_USER:$CUSTOM_GROUP" "$PANEL_CONFIG_DIR"

# Create plugin configuration files
PLUGIN_CONFIG_DIR="$USER_HOME/.config/xfce4/panel"
mkdir -p "$PLUGIN_CONFIG_DIR"

# Create cpufreq plugin configuration
cat <<'EOF' > "$PLUGIN_CONFIG_DIR/cpufreq-20.rc"
show_icon=false
show_label_governor=false
keep_compact=true
one_line=true
EOF

# Create info.sh script (RAM, SWAP, and Battery)
cat <<'EOF' > "$USER_HOME/.config/info.sh"
#!/bin/bash

# Get combined memory percentage (RAM + SWAP)
MEM_PERCENT=$(free -m | awk '
/Mem:/ {
    mem_used = $3
    mem_total = $2
}
/Swap:/ {
    swap_used = $3
    swap_total = $2
}
END {
    total = mem_total + swap_total
    used = mem_used + swap_used
    if (total > 0) {
        percent = (used / total) * 100
        printf "%.0f", percent
    } else {
        print "0"
    }
}')

# Get battery info from sysfs
BATTERY_PATH="/sys/class/power_supply/battery"
if [ -f "$BATTERY_PATH/capacity" ] && [ -f "$BATTERY_PATH/status" ]; then
    CAPACITY=$(cat "$BATTERY_PATH/capacity" 2>/dev/null || echo "0")
    STATUS=$(cat "$BATTERY_PATH/status" 2>/dev/null || echo "Unknown")
    
    # Choose indicator based on status
    if [ "$STATUS" = "Charging" ]; then
        INDICATOR="CHG"
    elif [ "$STATUS" = "Full" ]; then
        INDICATOR="FULL"
    else
        INDICATOR="BAT"
    fi
    
    BATTERY_INFO=" | ${INDICATOR} ${CAPACITY}%"
else
    BATTERY_INFO=""
fi

# Output in genmon XML format
echo "<txt>MEM ${MEM_PERCENT}%${BATTERY_INFO}</txt>"
EOF

chmod +x "$USER_HOME/.config/info.sh"

# Create genmon plugin configuration (both 19 and 24)
cat <<EOF > "$PLUGIN_CONFIG_DIR/genmon-19.rc"
Command=$USER_HOME/.config/info.sh
UseLabel=0
Text=(genmon)
UpdatePeriod=1000
Font=JetBrainsMono Nerd Font 10
EOF

cat <<EOF > "$PLUGIN_CONFIG_DIR/genmon-24.rc"
Command=$USER_HOME/.config/info.sh
UseLabel=0
Text=(genmon)
UpdatePeriod=1000
Font=JetBrainsMono Nerd Font 10
EOF

chown -R "$CUSTOM_USER:$CUSTOM_GROUP" "$PLUGIN_CONFIG_DIR"
chown "$CUSTOM_USER:$CUSTOM_GROUP" "$USER_HOME/.config/info.sh"


# 6. Configure Terminal (Direct Config File)
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


# 7. Configure Zsh and Terminal Enhancements
echo "FluxLinux: Configuring Zsh and Terminal..."

# Install zsh if not already installed
echo "FluxLinux: Installing zsh..."
apt-get install -y zsh 2>/dev/null

# Install Oh My Zsh for flux user
# Install Oh My Zsh for flux user
echo "FluxLinux: Installing Oh My Zsh..."

# 1. Check for corrupt installation (folder exists but missing core script)
if [ -d "$USER_HOME/.oh-my-zsh" ] && [ ! -f "$USER_HOME/.oh-my-zsh/oh-my-zsh.sh" ]; then
    echo "FluxLinux: Detected corrupt Oh My Zsh installation. Removing..."
    rm -rf "$USER_HOME/.oh-my-zsh"
fi

# 2. Install if missing
if [ ! -d "$USER_HOME/.oh-my-zsh" ]; then
    su -s /bin/bash - "$CUSTOM_USER" -c 'RUNZSH=no CHSH=no sh -c "$(curl -fsSL https://raw.githubusercontent.com/ohmyzsh/ohmyzsh/master/tools/install.sh)"' 2>/dev/null
fi

# Set ZSH_CUSTOM path
ZSH_CUSTOM="$USER_HOME/.oh-my-zsh/custom"

# Install zsh plugins
echo "FluxLinux: Installing Zsh plugins..."
su -s /bin/bash - "$CUSTOM_USER" -c "git clone https://github.com/zsh-users/zsh-autosuggestions '$ZSH_CUSTOM/plugins/zsh-autosuggestions'" 2>/dev/null
su -s /bin/bash - "$CUSTOM_USER" -c "git clone https://github.com/zsh-users/zsh-syntax-highlighting '$ZSH_CUSTOM/plugins/zsh-syntax-highlighting'" 2>/dev/null
su -s /bin/bash - "$CUSTOM_USER" -c "git clone --depth 1 https://github.com/marlonrichert/zsh-autocomplete.git '$ZSH_CUSTOM/plugins/zsh-autocomplete'" 2>/dev/null

# Install agnosterzak theme
echo "FluxLinux: Installing agnosterzak theme..."
su -s /bin/bash - "$CUSTOM_USER" -c "mkdir -p '$ZSH_CUSTOM/themes'"
su -s /bin/bash - "$CUSTOM_USER" -c "curl -fsSL https://raw.githubusercontent.com/zakaziko99/agnosterzak-ohmyzsh-theme/master/agnosterzak.zsh-theme -o '$ZSH_CUSTOM/themes/agnosterzak.zsh-theme'" 2>/dev/null

# Install pokemon-colorscripts
echo "FluxLinux: Installing pokemon-colorscripts..."
POKEMON_TEMP="/tmp/pokemon-colorscripts"
rm -rf "$POKEMON_TEMP"
git clone https://gitlab.com/phoneybadger/pokemon-colorscripts.git "$POKEMON_TEMP" 2>/dev/null
cd "$POKEMON_TEMP" && ./install.sh 2>/dev/null
cd - > /dev/null
rm -rf "$POKEMON_TEMP"

# Configure .zshrc
echo "FluxLinux: Configuring .zshrc..."
ZSHRC="$USER_HOME/.zshrc"

# Check if .zshrc is valid (loading oh-my-zsh)
if [ ! -f "$ZSHRC" ] || ! grep -q "oh-my-zsh.sh" "$ZSHRC"; then
    echo "FluxLinux: Creating valid .zshrc..."
    cat <<EOF > "$ZSHRC"
export ZSH="\$HOME/.oh-my-zsh"
ZSH_THEME="agnosterzak"
plugins=(git zsh-autosuggestions zsh-syntax-highlighting zsh-autocomplete)
source \$ZSH/oh-my-zsh.sh

EOF
    chown "$CUSTOM_USER:$CUSTOM_GROUP" "$ZSHRC"
else
    # Update existing .zshrc settings
    # Update theme
    if grep -q "^ZSH_THEME=" "$ZSHRC"; then
        sed -i 's/^ZSH_THEME=.*$/ZSH_THEME="agnosterzak"/' "$ZSHRC"
    else
        # Insert theme before plugins or source
        sed -i '1iZSH_THEME="agnosterzak"' "$ZSHRC"
    fi
    
    # Update plugins
    if grep -q "^plugins=" "$ZSHRC"; then
        sed -i 's/plugins=(.*)/plugins=(git zsh-autosuggestions zsh-syntax-highlighting zsh-autocomplete)/' "$ZSHRC"
    else
        sed -i '/source \$ZSH\/oh-my-zsh.sh/i plugins=(git zsh-autosuggestions zsh-syntax-highlighting zsh-autocomplete)' "$ZSHRC"
    fi
fi

# Download fastfetch config
mkdir -p "$USER_HOME/.local/share/fastfetch/presets"
curl -fsSL https://raw.githubusercontent.com/abhay-byte/Linux_Setup/dev/config/termux.jsonc \
    -o "$USER_HOME/.local/share/fastfetch/presets/termux.jsonc" 2>/dev/null

# Add fastfetch and pokemon to .zshrc startup (if not already present)
if ! grep -q 'fastfetch --config termux' "$ZSHRC"; then
    sed -i '1ifastfetch --config termux' "$ZSHRC"
fi

if ! grep -q 'pokemon-colorscripts' "$ZSHRC"; then
    echo '' >> "$ZSHRC"
    echo '# Show random pokemon on terminal start' >> "$ZSHRC"
    echo 'pokemon-colorscripts -r' >> "$ZSHRC"
fi

# Set zsh as default shell for flux user
chsh -s /bin/zsh "$CUSTOM_USER" 2>/dev/null

# Fix ownership
chown -R "$CUSTOM_USER:$CUSTOM_GROUP" "$USER_HOME/.oh-my-zsh" "$USER_HOME/.zshrc" "$USER_HOME/.local" 2>/dev/null

echo "FluxLinux: Terminal configuration complete!"


# 8. Reload XFCE Daemons (Force restart like chroot script does)
echo "FluxLinux: Reloading Desktop..."

# Kill existing XFCE processes to force reload (matches chroot pattern)
su -s /bin/bash - "$CUSTOM_USER" -c "killall -9 xfdesktop xfwm4 xfsettingsd" 2>/dev/null
sleep 2

# Restart daemons with updated settings (run in background but wait a bit for each)
su -s /bin/bash - "$CUSTOM_USER" -c "DISPLAY=:0 nohup xfdesktop > /dev/null 2>&1 &" 2>/dev/null
sleep 0.5
su -s /bin/bash - "$CUSTOM_USER" -c "DISPLAY=:0 nohup xfwm4 --replace > /dev/null 2>&1 &" 2>/dev/null
sleep 0.5
su -s /bin/bash - "$CUSTOM_USER" -c "DISPLAY=:0 nohup xfsettingsd > /dev/null 2>&1 &" 2>/dev/null
sleep 1

echo "FluxLinux: Customization Complete!"
echo "------------------------------------------------"
read -p "Press Enter to close..."
