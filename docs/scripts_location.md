# Debian Scripts Directory Reference

This document provides a reference for all the shell scripts used by FluxLinux to set up, configure, and run Debian environments (both PRoot and Chroot). All scripts are bundled within the app assets at `app/src/main/assets/scripts/`.

---

## 1. Debian Chroot Scripts
**Location:** `app/src/main/assets/scripts/chroot/`

These scripts are specifically for rooted devices running Debian inside a native Chroot environment.

* **Installation & Removal**
  * `setup_debian_chroot.sh`: General installation script for setting up a Debian chroot.
  * `setup_debian13_chroot.sh`: Specific installation script for Debian 13 (Trixie) chroot.
  * `uninstall_debian_chroot.sh`: Safely uninstalls the chroot environment.
  * `uninstall_debian13.sh`: Specifically uninstalls the Debian 13 chroot container.

* **GUI Management (Starting/Stopping)**
  * `start_debian13_kde_gui.sh`: Starts the KDE desktop environment.
  * `start_debian13_kde_gui_software.sh`: Starts KDE using software rendering (if hardware acceleration fails).
  * `start_debian13_kde_gui_turnip.sh`: Starts KDE using Turnip/Zink drivers for hardware acceleration.
  * `stop_debian13_gui.sh`: Stops all GUI components.
  * `stop_debian13_kde_gui.sh`: Gracefully stops the KDE session.

---

## 2. Common Debian Scripts (PRoot & Chroot)
**Location:** `app/src/main/assets/scripts/common/`

These scripts apply to both PRoot (rootless) and Chroot (rooted) Debian environments. They are responsible for configuring the desktop, installing software packages, and setting up specific workflows.

* **Core Setup & GUI**
  * `setup_debian_family.sh`: Core setup tasks common across all Debian-based distributions.
  * `start_gui.sh` / `stop_gui.sh`: Generic scripts to start and stop the XFCE/Desktop environment.
  * `start_gui_kde.sh`: Starts the KDE desktop environment in PRoot/Chroot.
  * `setup_kde_debian.sh`: Installs and configures the KDE Plasma desktop.

* **Hardware & Acceleration**
  * `setup_hw_accel_debian.sh`: Configures VirGL, Turnip, and Zink for hardware acceleration.
  * `setup_gpu.sh`: General GPU configuration.
  * `gpu_diagnostics.sh`: Tools to diagnose GPU and hardware acceleration issues.

* **Development Workflows**
  * `setup_appdev_debian.sh`: Android Studio, Flutter, and mobile app development tools.
  * `setup_gengdev_debian.sh`: General development (GCC, Git, Python, VS Code, etc.).
  * `setup_webdev_debian.sh`: Node.js, npm, PHP, Apache, and web development stacks.
  * `setup_gamedev_debian.sh`: Game development tools (Godot, etc.).
  * `setup_datascience_debian.sh`: Jupyter, Pandas, and data science environments.

* **Productivity & Creative**
  * `setup_office_debian.sh`: LibreOffice and productivity suites.
  * `setup_graphic_design_debian.sh`: GIMP, Inkscape, and graphic design tools.
  * `setup_video_editing_debian.sh`: Kdenlive and video editing software.
  * `setup_cybersec_debian.sh`: Cybersecurity and penetration testing tools.

* **AI & Emulation**
  * `setup_emulation_debian.sh`: RetroArch and gaming emulators.
  * `setup_vulkan_llamacpp_debian.sh`: Setup for running local LLMs with hardware acceleration.
  * `setup_qwen25_debian.sh` / `setup_qwen35_debian.sh`: Setup scripts for specific Qwen AI models.
  * `launch_qwen25.sh` / `launch_qwen35.sh`: Launchers for the Qwen models.

* **Customization & Theming**
  * `setup_customization_debian.sh`: Themes, icons, and visual customization for XFCE/general desktops.
  * `setup_customization_kde_debian.sh`: Specific visual customizations for KDE.

---

## 3. Termux Pre-Requisites
**Location:** `app/src/main/assets/scripts/common/` and `app/src/main/assets/scripts/termux/`

These scripts prepare the host Termux environment before installing Debian.

* `termux_tweaks.sh`: (In `common/`) Prepares Termux repositories, installs X11 tools, and sets up Termux properties.
* `setup_termux.sh`: (In `common/`) Initializes the environment, grants storage permissions, and sets up Proot-Distro.
