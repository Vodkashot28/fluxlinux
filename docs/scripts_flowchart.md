# FluxLinux Script Execution Flowchart

This flowchart visualizes the lifecycle of a FluxLinux installation, mapping out how scripts are executed chronologically and which environments they belong to.

```mermaid
flowchart TD
    %% Styling
    classDef termux fill:#1e1e1e,stroke:#4CAF50,stroke-width:2px,color:#fff
    classDef proot fill:#2d3748,stroke:#4299e1,stroke-width:2px,color:#fff
    classDef chroot fill:#4a148c,stroke:#ab47bc,stroke-width:2px,color:#fff
    classDef common fill:#004d40,stroke:#26a69a,stroke-width:2px,color:#fff
    classDef action fill:#b71c1c,stroke:#ef5350,stroke-width:2px,color:#fff

    subgraph Host ["1. Host Termux Environment (Pre-Requisites)"]
        direction TB
        H1[install.sh<br/>install_apps.sh]:::termux --> H2[setup_theme.sh]:::termux
        H2 --> H3[termux_tweaks.sh]:::termux
        H3 --> H4[setup_termux.sh]:::termux
    end

    subgraph Branches ["2. Installation Route (PRoot vs Chroot)"]
        direction LR
        H4 -- "Rootless Installation" --> P1[flux_install.sh<br/>Executes proot-distro install]:::proot
        H4 -- "Rooted Installation" --> C1[setup_debian_chroot.sh<br/>setup_arch_chroot.sh]:::chroot
    end

    subgraph Container ["3. Common Configuration (Inside Container)"]
        direction TB
        P1 --> E1[setup_family.sh<br/>e.g. setup_debian_family.sh]:::common
        C1 --> E1
        
        E1 --> E2[Hardware Setup:<br/>setup_gpu.sh<br/>setup_hw_accel_debian.sh]:::common
        E2 --> E3[Desktop/GUI Setup:<br/>setup_kde_debian.sh<br/>setup_customization...]:::common
        E3 --> E4[Software Workflows:<br/>setup_appdev_debian.sh<br/>setup_webdev_debian.sh<br/>setup_qwen25_debian.sh]:::common
    end

    subgraph Execution ["4. Running the Desktop GUI"]
        direction LR
        E4 -- "Launch via PRoot" --> R1[start_gui.sh<br/>start_gui_kde.sh]:::proot
        R1 --> R2[stop_gui.sh]:::action

        E4 -- "Launch via Chroot" --> R3[start_debian13_kde_gui.sh<br/>..._turnip.sh]:::chroot
        R3 --> R4[stop_debian13_gui.sh]:::action
    end
```

### Flow Breakdown:
1. **Host Termux Environment (Green):** The FluxLinux Android app first writes scripts to the Termux environment. It runs `install.sh` to get basic packages, applies terminal tweaks, and finalizes with `setup_termux.sh` to allow storage access and install `proot-distro`.
2. **Installation Route (Blue & Purple):** The user chooses an installation method. PRoot (Rootless) relies on `flux_install.sh` to spawn the container, whereas Chroot (Rooted) uses `setup_debian_chroot.sh` to natively mount the filesystem.
3. **Common Configuration (Teal):** Once the container (PRoot or Chroot) is active, it runs identical scripts! Both routes execute the exact same `setup_debian_family.sh`, hardware setups, and software workflow installations to ensure a consistent experience regardless of whether the device is rooted or not.
4. **Running the Desktop GUI (Red):** The launch mechanism diverges again depending on the environment container. PRoot uses `start_gui.sh`, whereas Chroot natively spawns the display using `start_debian13_kde_gui.sh`.
