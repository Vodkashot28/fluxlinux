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
  <img src="img/termux-download-page-flux.png" alt="Termux and Termux:X11 Download Page" width="400" />
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

<!-- [Step 3 Screenshot Placeholder] -->

---

## 🔐 Step 4: Grant Required Permissions

FluxLinux requires a few basic Android permissions to function properly.

* **Communication Permission:** Allows FluxLinux to send commands to Termux securely.
* **Storage Permission:** Allows the Linux environment to access your internal storage for managing files.
* **Notification Permission:** Keeps the environment running in the background.

Click **Grant Permission** and accept the prompts that appear on your screen.

<!-- [Step 4 Screenshot Placeholder] -->

---

## 🪟 Step 5: Overlay Permission (Draw Over Other Apps)

For the best experience, FluxLinux uses a floating widget/menu over the Linux desktop environment (Termux:X11). This allows you to quickly access controls, keyboards, and settings.

1. Click the **Open Settings** button.
2. Find **FluxLinux** in the list of apps.
3. Toggle the switch to **Allow display over other apps**.
4. Press back to return to FluxLinux.

<!-- [Step 5 Screenshot Placeholder] -->

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

<!-- [Step 6 Screenshot Placeholder] -->

---

## 🧰 Step 7: Install BusyBox

FluxLinux requires BusyBox, which provides many standard Unix utilities.

* Click **Install BusyBox** within the app.
* The installation should complete quickly and automatically.

<!-- [Step 7 Screenshot Placeholder] -->

---

## 📦 Step 8: Environment Setup

FluxLinux will now extract and configure the core Linux file system (rootfs) on your device.

* This process may take a few minutes depending on your device's storage speed.
* Please **do not close the app** or turn off the screen during this process.

<!-- [Step 8 Screenshot Placeholder] -->

---

## 💻 Step 9: System Requirements Check

Before launching the environment, FluxLinux will run a quick hardware check to ensure your device meets the minimum requirements for a smooth experience.

* It will verify your **RAM (Memory)** and **Storage Space**.
* We highly recommend creating a **SWAP file** (Virtual RAM) using a third-party app if your device has less than 8GB of RAM. This prevents out-of-memory crashes when running heavy Linux desktop applications.

Click **Continue to Final Step** once you have reviewed your system status.

<!-- [Step 9 Screenshot Placeholder] -->

---

## 🎉 Step 10: You're Ready!

Congratulations! You have successfully configured FluxLinux.

Click **Launch FluxLinux** to start your environment. The app will initialize the Termux backend and automatically open the Termux:X11 display, presenting you with a full Linux desktop on your Android device.

Enjoy your new portable workstation!
