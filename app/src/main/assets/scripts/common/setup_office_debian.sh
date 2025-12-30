#!/bin/bash
# setup_office_debian.sh
# Installs Office Productivity Stack
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

echo "FluxLinux: Setting up Office Productivity Environment..."
echo "Target: Debian 13 (Trixie) - ARM64"

# 1. System Dependencies
echo "FluxLinux: Installing Dependencies..."
export DEBIAN_FRONTEND=noninteractive
apt update -y
apt install -y \
    dbus-x11 \
    fonts-noto \
    fonts-liberation \
    fonts-dejavu \
    fonts-dejavu \
    nodejs \
    libsecret-1-dev \
    || handle_error "Dependencies & Fonts"

# Conditional NPM Install (Fix for NodeSource Conflict)
# If setup_webdev_debian.sh ran, nodejs includes npm.
# If not, Debian split packages might need explicit npm.
if ! command -v npm >/dev/null; then
    echo "FluxLinux: NPM not found (bundled), installing explicitly..."
    apt install -y npm || echo " [⚠️] NPM install warning (might be bundled)"
fi

# 2. LibreOffice Suite
echo "FluxLinux: Installing LibreOffice Suite..."
# LibreOffice: Full open source office suite
# libreoffice-gtk3: Better integration with GTK/XFCE
apt install -y \
    libreoffice \
    libreoffice-gtk3 \
    || handle_error "LibreOffice Installation"

# 3. Email & PIM
echo "FluxLinux: Installing Email & Organization Tools..."
# Thunderbird: Email client
apt install -y \
    thunderbird \
    || handle_error "Thunderbird Installation"

# 4. PDF Tools
echo "FluxLinux: Installing PDF Tools..."
# Evince: Document Viewer
# Xournal++: Note taking & PDF Annotation
apt install -y \
    evince \
    xournalpp \
    || handle_error "PDF Tools Installation"

# 5. Joplin (Note Taking - GUI)
echo "FluxLinux: Installing Joplin (GUI - ARM64 AppImage)..."
# We actally need the GUI version as requested.
# Since we are in PROOT, we must Extract the AppImage.

JOPLIN_VERSION="3.1.20"
# Based on search results, the asset might be named generic "Joplin-arm64.AppImage" or "Joplin-3.1.20-arm64.AppImage"
# Failing URL was: .../Joplin-3.1.20-arm64.AppImage
# Trying likely alternative from this specific repo's naming convention
JOPLIN_URL="https://github.com/leaguecn/joplin-arm64-build/releases/download/v${JOPLIN_VERSION}/Joplin-arm64.AppImage"
INSTALL_DIR="/opt/joplin"

if [ ! -d "$INSTALL_DIR" ]; then
    echo "Downloading Joplin ARM64..."
    mkdir -p /tmp/joplin_install
    wget -O /tmp/joplin_install/joplin.AppImage "$JOPLIN_URL" || echo " [⚠️] Joplin Download Failed"
    
    if [ -f "/tmp/joplin_install/joplin.AppImage" ]; then
        echo "Extracting AppImage (Bypassing FUSE)..."
        chmod +x /tmp/joplin_install/joplin.AppImage
        cd /tmp/joplin_install
        ./joplin.AppImage --appimage-extract >/dev/null 2>&1
        
        echo "Moving to $INSTALL_DIR..."
        mkdir -p "$INSTALL_DIR"
        mv squashfs-root/* "$INSTALL_DIR/"
        
        # Cleanup
        cd /
        rm -rf /tmp/joplin_install
        
        # Create Launcher Wrapper
        echo "Creating Launcher..."
        cat <<EOF > /usr/bin/joplin-desktop
#!/bin/bash
export PROOT_NO_SECCOMP=1
# AppImages in Proot often need --no-sandbox
exec "$INSTALL_DIR/AppRun" --no-sandbox "\$@"
EOF
        chmod +x /usr/bin/joplin-desktop
        
        # Create Desktop Entry
        mkdir -p /usr/share/applications
        cat <<EOF > /usr/share/applications/joplin.desktop
[Desktop Entry]
Name=Joplin
Comment=Joplin for Desktop
Exec=/usr/bin/joplin-desktop %u
Icon=$INSTALL_DIR/usr/share/icons/hicolor/512x512/apps/joplin.png
Type=Application
Terminal=false
Categories=Office;NoteTaking;
MimeType=x-scheme-handler/joplin;
EOF
        echo " [✅] Joplin GUI Installed"
    else
        echo " [❌] Joplin Download Failed - Skipping"
    fi
else
    echo " [ℹ️] Joplin GUI already installed."
fi

# 6. Verification
verify_installation() {
    echo ""
    echo "🔎 FluxLinux: Verifying Installations..."
    echo "------------------------------------------------"
    
    if command -v libreoffice >/dev/null; then echo " [✅] LibreOffice"; else echo " [❌] LibreOffice Missing"; fi
    if command -v thunderbird >/dev/null; then echo " [✅] Thunderbird"; else echo " [❌] Thunderbird Missing"; fi
    if command -v evince >/dev/null; then echo " [✅] Evince"; else echo " [❌] Evince Missing"; fi
    if command -v xournalpp >/dev/null; then echo " [✅] Xournal++"; else echo " [❌] Xournal++ Missing"; fi
    if command -v joplin-desktop >/dev/null; then echo " [✅] Joplin (GUI)"; else echo " [❌] Joplin Missing"; fi

    echo "------------------------------------------------"
    echo "🎉 Office Setup Complete!"
}

verify_installation

echo "Note: Joplin is installed as a Desktop App. Check your Applications menu."
read -p "Press Enter to close..."
