<div align="center">
  <img src="../../assets/logo/logo.webp" width="160" alt="FluxLinux Logo" />
  <h1>🚀 Setting up FluxLinux</h1>
  <p>Welcome to the FluxLinux installation guide! This tutorial will walk you through the process of getting FluxLinux running on your Android device.</p>
</div>

---

## 📥 Step 1: Choose Your Download Method

FluxLinux is available through three official channels. Choose the one that best suits your needs:

| Method | Price | Updates | Best For |
| :--- | :--- | :--- | :--- |
| **GitHub Releases** | 🟢 Free | ⚡ Direct / Manual | Developers & power users wanting the absolute latest build |
| **F-Droid** | 🟢 Free | 🔄 Automatic (F-Droid client) | Users who prefer free, open-source app stores |
| **Google Play Store** | 🟡 Paid | 🔄 Automatic | Users wanting easy installations while supporting the developer |

---

### Option 1: GitHub Releases (Recommended for Developers)

Download the latest pre-compiled stable/beta release directly from our official repository.

* **Repository Page:** [GitHub Releases](https://github.com/abhay-byte/fluxlinux/releases)

> [!TIP]
> Always download the file ending with `.apk` (e.g., `app-release.apk`). Avoid files labeled with `idsig` unless you specifically require signature verification.

<div align="center">
  <img src="img/release-download.png" alt="GitHub Release Download" width="600" />
</div>

---

### Option 2: F-Droid (Free & Open Source)

Get FluxLinux from the decentralized Android software repository. You can download the standalone APK or install it via the F-Droid client.

* **F-Droid Client/Web:** [F-Droid Listing](https://f-droid.org/packages/com.ivarna.fluxlinux)

<div align="center">
  <img src="img/fdroid-download.png" alt="F-Droid Download" width="600" />
</div>

---

### Option 3: Google Play Store (Support the Project)

If you would like to support the ongoing development of FluxLinux, you can purchase it directly from the Google Play Store. This provides automatic background updates.

* **Play Store:** [Google Play Store](https://play.google.com/store/apps/details?id=com.ivarna.fluxlinux)

<div align="center">
  <img src="img/playstore-download.png" alt="Google Play Store Download" width="600" />
</div>

---

> [!IMPORTANT]
> **Unknown Sources Permission:** If you are downloading from GitHub or F-Droid for the first time, your browser/client will prompt you to allow installation from "Unknown Sources". Please enable this permission in your Android system settings to proceed.

<div align="center">
  <img src="img/install-fluxlinux-popup.png" alt="Install FluxLinux Prompt" width="350" />
</div>

---

## ⚙️ Step 2: Install Required Components (Termux & Termux:X11)

Once you open FluxLinux, the application requires **Termux** and **Termux:X11** to run. The setup behavior differs depending on your installation source:

### 1. GitHub Releases & F-Droid (Automatic Download)
The app will automatically download the correct versions of Termux and Termux:X11 and prompt you to install them.
* Grant the necessary permission to install packages from FluxLinux if prompted.

### 2. Google Play Store (Manual Download)
Due to Play Store policy restrictions, the app cannot automatically download external APK files. You must download and install them manually:
* **Termux:** Download and install the version from F-Droid (do not use the outdated Play Store version of Termux).
* **Termux:X11:** Download and install the companion APK.
* Direct download links are provided in the app.

### 🔍 Verification Check
FluxLinux will check if both components are installed on your device. Once both dependencies are detected, you can click **Proceed** to continue with the setup.

<div align="center">
  <img src="img/step-one-termux.png" alt="Termux and Termux:X11 Setup" width="300" />
  <img src="img/termux-download-page-flux.png" alt="Termux and Termux:X11 Download Page" width="300" />
</div>

---

## 🔧 Step 3: Configure Termux

FluxLinux needs to communicate with Termux to execute background processes and set up your Linux environment. You must enable external apps in Termux.

1. Click the **Copy & Open Termux** button in the FluxLinux app.
2. The app will copy a command to your clipboard and open Termux.
3. Paste the command into the Termux terminal and hit Enter:
   ```bash
   mkdir -p ~/.termux && echo "allow-external-apps = true" >> ~/.termux/termux.properties && termux-reload-settings
   ```
4. Return to FluxLinux, check the box confirming you ran the command, and click **Continue**.

<div align="center">
  <img src="img/step-two-click-toggle-in-app-i-have-pasted.png" alt="Toggle Paste Confirmation" width="300" />
  <img src="img/step-two-termux-paste-and-enter.png" alt="Paste Command in Termux" width="300" />
</div>

---

## 🔐 Step 4: Grant Required Permissions

FluxLinux requires a few basic Android permissions to function properly.

* **Communication Permission:** Allows FluxLinux to send commands to Termux securely.
* **Storage Permission:** Allows the Linux environment to access your internal storage for managing files.
* **Notification Permission:** Keeps the environment running in the background.

Click **Grant Permission** and accept the prompts that appear on your screen.

<div align="center">
  <img src="img/step-two-communication.png" alt="Communication Configuration" width="250" />
  <img src="img/step-three-grant-permission.png" alt="Grant Permissions UI" width="250" />
  <img src="img/step-three-allow-excecute command.png" alt="Allow Execute Command" width="250" />
</div>

---

## 🪟 Step 5: Overlay Permission (Draw Over Other Apps)

For the best experience, FluxLinux uses a floating widget/menu over the Linux desktop environment (Termux:X11). This allows you to quickly access controls, keyboards, and settings.

1. Click the **Open Settings** button.
2. Find **FluxLinux** in the list of apps.
3. Toggle the switch to **Allow display over other apps**.
4. Press back to return to FluxLinux.

Since Android restricts overlay permissions, especially on newer Android versions, follow these steps to enable it:

1. Click **Open Settings** inside the app. If you see a "Restricted setting" block, you will need to go to **App Info** for FluxLinux/Termux via system settings, click the **three dots** in the top right, and choose **Allow restricted settings**.
2. Go to the overlay settings and find **Termux**.
3. Toggle on **Allow display over other apps**.

<div align="center">
  <img src="img/step-four-go-to-app-info-via-apps-settings.png" alt="Go to App Info" width="250" />
  <img src="img/step-four-allow-restricted-settings-click-on-three-dots-and-allow-it.png" alt="Allow Restricted Settings" width="250" />
  <img src="img/step-four-display-overlay-for-termux-disabled.png" alt="Overlay Disabled" width="250" />
</div>

<div align="center">
  <img src="img/step-four-display-overlay.png" alt="Display Overlay List" width="250" />
  <img src="img/step-four-then-allow-display-over-apps-for-termux-will-be-allowed-now.png" alt="Allow Display Over Apps" width="250" />
  <img src="img/step-four-granted-permission.png" alt="Granted Overlay Permission" width="250" />
</div>

---

## 👻 Step 6: Disable Phantom Process Killer (Crucial)

Android 12 and above introduced an aggressive "Phantom Process Killer" that terminates background processes using too much CPU. Since running a full Linux environment requires sustained resources, this feature *must* be disabled, or your Linux environment will crash unexpectedly.

### Without Root (Using ADB/PC)

If your device is not rooted, you will need a computer (Windows, Mac, or Linux) with ADB installed.

1. Enable **Developer Options** and **USB Debugging** on your phone.
2. Connect your phone to your PC via USB.
3. Open a command prompt/terminal on your PC and run:
   ```bash
   adb shell "/system/bin/device_config put activity_manager max_phantom_processes 2147483647"
   adb shell "/system/bin/device_config set_sync_disabled_for_tests persistent"
   ```
4. The FluxLinux app will verify if the command was successful.

### With Root (Automatic)

If your device is rooted (Magisk/KernelSU), simply click **Apply Fix via Root** and grant Superuser permission when prompted. FluxLinux will handle it automatically.

<div align="center">
  <img src="img/step-five-process-killer-fix.png" alt="Process Killer Fix Screen" width="250" />
  <img src="img/step-five-enable-developer-mode-for-adb-click-on-build-number-for-five-times.png" alt="Enable Developer Mode" width="250" />
  <img src="img/step-five-in-developer-option-enable-usb-debugging.png" alt="Enable USB Debugging" width="250" />
</div>

<div align="center">
  <img src="img/step-five-run-these-commands-through-adb-from-ur-pc.png" alt="ADB Command Instructions" width="350" />
  <img src="img/step-five-then-copy-and-paste-commands-on-pc-terminal-with-phone-connected-with-usb-debugging-enabled.png" alt="Run ADB on PC" width="350" />
</div>

---

## 🧰 Step 7: Install BusyBox

FluxLinux requires BusyBox, which provides many standard Unix utilities.

* Click **Install BusyBox** within the app.
* The installation should complete quickly and automatically.

If you are using root, you can install the BusyBox module directly inside your root manager:

<div align="center">
  <img src="img/step-six-for-root-install-busybox.png" alt="Install BusyBox" width="250" />
  <img src="img/step-six-download-busybox-module.png" alt="Download BusyBox Module" width="250" />
  <img src="img/step-six-then-flash-module-in-your-root-application.png" alt="Flash Module in Root Manager" width="250" />
</div>

---

## 📦 Step 8: Environment Setup

FluxLinux will now extract and configure the core Linux file system (rootfs) on your device.

* This process may take a few minutes depending on your device's storage speed.
* Please **do not close the app** or turn off the screen during this process.

<div align="center">
  <img src="img/step-seven-step-up-environment-click-on-initialize-environemnt-open-in.png" alt="Initialize Environment" width="250" />
  <img src="img/step-seven-termux-will-open-and-start-the-setup.png" alt="Setup starting in Termux" width="250" />
  <img src="img/step-seven-termux-initialized.png" alt="Setup Completed" width="250" />
</div>

---

## 💻 Step 9: System Requirements Check

Before launching the environment, FluxLinux will run a quick hardware check to ensure your device meets the minimum requirements for a smooth experience.

* It will verify your **RAM (Memory)** and **Storage Space**.
* We highly recommend creating a **SWAP file** (Virtual RAM) using a third-party app if your device has less than 8GB of RAM. This prevents out-of-memory crashes when running heavy Linux desktop applications.

Click **Continue to Final Step** once you have reviewed your system status.

<div align="center">
  <img src="img/step-eight-enable-good-amount-of-swap.png" alt="Enable Swap File" width="350" />
</div>

---

## 🎉 Step 10: You're Ready!

Congratulations! You have successfully configured FluxLinux.

Click **Launch FluxLinux** to start your environment. The app will initialize the Termux backend and automatically open the Termux:X11 display, presenting you with a full Linux desktop on your Android device.

Enjoy your new portable workstation!

> [!IMPORTANT]
> Always make sure that Termux remains running in the background or in a split-screen layout when using FluxLinux to prevent Android from killing the environment processes.

<div align="center">
  <img src="img/step-last-always-have-termux-in-background-or-in-split.png" alt="Termux running in Background/Split" width="350" />
</div>
