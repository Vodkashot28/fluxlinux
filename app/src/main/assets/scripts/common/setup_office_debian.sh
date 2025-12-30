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

# 5. Joplin (Note Taking)
echo "FluxLinux: Installing Joplin (Terminal Edition)..."
# We install the terminal version as it's ARM64 safe and reliable in PROOT
if ! command -v joplin >/dev/null; then
    # Configure npm to use a user-writable directory to avoid root issues if needed,
    # but here we are root/sudo so global install is fine.
    npm install -g joplin --unsafe-perm=true --allow-root || echo " [⚠️] Joplin NPM install failed"
else
    echo " [ℹ️] Joplin already installed."
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
    if command -v joplin >/dev/null; then echo " [✅] Joplin (CLI)"; else echo " [❌] Joplin Missing"; fi

    echo "------------------------------------------------"
    echo "🎉 Office Setup Complete!"
}

verify_installation

echo "Note: To use Joplin, type 'joplin' in the terminal. For LibreOffice/Thunderbird, check the Applications menu."
read -p "Press Enter to close..."
