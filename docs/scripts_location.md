# Scripts Directory Reference

This document provides a comprehensive reference for all **45 files (44 shell scripts + 1 markdown)** used by FluxLinux to set up, configure, and run environments. All scripts are bundled within the app assets at `app/src/main/assets/scripts/`.

---

## 1. Debian Chroot Scripts (10 Scripts)
**Location:** `app/src/main/assets/scripts/chroot/`

These scripts are exclusively for **rooted devices** running a native Chroot environment.

* **Installation & Removal**
  * `setup_arch_chroot.sh`: Setup script for Arch Linux in Chroot.
  * `setup_debian_chroot.sh`: General setup for a Debian Chroot.
  * `setup_debian13_chroot.sh`: Setup specific to Debian 13 (Trixie) in Chroot.
  * `uninstall_debian_chroot.sh`: Uninstalls the generic Debian Chroot.
  * `uninstall_debian13.sh`: Uninstalls the Debian 13 Chroot.

* **GUI Management**
  * `start_debian13_kde_gui.sh`: Starts the KDE desktop environment.
  * `start_debian13_kde_gui_software.sh`: Starts KDE using software rendering.
  * `start_debian13_kde_gui_turnip.sh`: Starts KDE using Turnip drivers.
  * `stop_debian13_gui.sh`: Stops all GUI components.
  * `stop_debian13_kde_gui.sh`: Gracefully stops the KDE session.

---

## 2. PRoot Scripts (4 Scripts)
**Location:** `app/src/main/assets/scripts/common/`

These scripts are strictly for **rootless (PRoot)** execution to launch and manage the containers from the host Termux side.

* `flux_install.sh`: The core installation wrapper executing `proot-distro install`.
* `start_gui.sh`: Launches the XFCE4 Desktop Environment inside PRoot.
* `start_gui_kde.sh`: Launches the KDE Desktop Environment inside PRoot.
* `stop_gui.sh`: Gracefully terminates the PRoot GUI session.

---

## 3. Common Environment Scripts (25 Scripts)
**Location:** `app/src/main/assets/scripts/common/`

These scripts run *inside* the Linux environment and are shared between **both PRoot and Chroot** setups.

* **Core Setup & Families**
  * `setup_arch_family.sh`: Core setup for Arch-based environments.
  * `setup_debian_family.sh`: Core setup for Debian-based environments.
  * `setup_fedora_family.sh`: Core setup for Fedora-based environments.
  * `setup_kde_debian.sh`: Installs and configures KDE Plasma in Debian.

* **Hardware & Acceleration**
  * `ha`: Wrapper script to dynamically inject VirGL/Zink hardware acceleration.
  * `setup_hw_accel_debian.sh`: Configures Vulkan, Turnip, and Zink drivers.
  * `setup_gpu.sh`: General GPU preparation.
  * `gpu_diagnostics.sh`: Tools to diagnose hardware acceleration issues.

* **Development Workflows**
  * `setup_appdev_debian.sh`: Android Studio, Flutter, and mobile dev tools.
  * `setup_gengdev_debian.sh`: General dev tools (GCC, Git, Python, VS Code).
  * `setup_webdev_debian.sh`: Web development stacks (Node.js, PHP).
  * `setup_gamedev_debian.sh`: Game development tools like Godot.
  * `setup_datascience_debian.sh`: Jupyter, Pandas, data science suite.

* **Productivity & Creative**
  * `setup_office_debian.sh`: LibreOffice and general productivity.
  * `setup_graphic_design_debian.sh`: GIMP, Inkscape, etc.
  * `setup_video_editing_debian.sh`: Kdenlive and video editors.
  * `setup_cybersec_debian.sh`: Cybersecurity and pentesting tools.

* **AI & Emulation**
  * `setup_emulation_debian.sh`: RetroArch and gaming emulators.
  * `setup_vulkan_llamacpp_debian.sh`: Configures Vulkan-accelerated local LLM engines.
  * `setup_qwen25_debian.sh` / `setup_qwen35_debian.sh`: Downloads Qwen AI models.
  * `launch_qwen25.sh` / `launch_qwen35.sh`: Launchers to execute the Qwen models.

* **Customization**
  * `setup_customization_debian.sh`: Themes/icons for XFCE/general.
  * `setup_customization_kde_debian.sh`: Themes/icons specific to KDE.

---

## 4. Host Termux Pre-Requisites (5 Scripts + 1 Document)
**Location:** Mix of `app/src/main/assets/scripts/termux/` and `common/`

These files run purely on the **host Android device (Termux)** to prep the system before any Linux environment is spawned.

* **In `termux/` directory:**
  * `install.sh`: Foundational Termux package installer.
  * `install_apps.sh`: Additional Termux app requirements.
  * `setup_theme.sh`: Configures the Termux terminal theme (fonts/colors).

* **In `common/` directory:**
  * `setup_termux.sh`: Finalizes storage permissions and installs `proot-distro`.
  * `termux_tweaks.sh`: Installs X11 packages and PulseAudio server configurations.
  * `virgl_troubleshooting.md`: **(Documentation)** A guide covering troubleshooting steps for Virgl/Turnip issues.

---
**Total Coverage Verification: 10 + 4 + 25 + 5 + 1 = 45 Files (Verified and Fully Covered)**
