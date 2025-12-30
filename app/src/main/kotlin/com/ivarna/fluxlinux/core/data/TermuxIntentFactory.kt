package com.ivarna.fluxlinux.core.data

import android.content.Intent

object TermuxIntentFactory {

    private const val ACTION_RUN_COMMAND = "com.termux.RUN_COMMAND"
    private const val EXTRA_COMMAND_PATH = "com.termux.RUN_COMMAND_PATH"
    private const val EXTRA_ARGUMENTS = "com.termux.RUN_COMMAND_ARGUMENTS"
    private const val EXTRA_WORKDIR = "com.termux.RUN_COMMAND_WORKDIR"
    private const val EXTRA_BACKGROUND = "com.termux.RUN_COMMAND_BACKGROUND"
    private const val EXTRA_SESSION_ACTION = "com.termux.RUN_COMMAND_SESSION_ACTION"

    private const val TERMUX_BASH_PATH = "/data/data/com.termux/files/usr/bin/bash"
    private const val TERMUX_HOME_DIR = "/data/data/com.termux/files/home"

    /**
     * Creates an intent to execute a bash script string in Termux.
     */
    fun buildRunCommandIntent(
        scriptContent: String,
        runInBackground: Boolean = false
    ): Intent {
        return Intent(ACTION_RUN_COMMAND).apply {
            setClassName("com.termux", "com.termux.app.RunCommandService")
            putExtra(EXTRA_COMMAND_PATH, TERMUX_BASH_PATH)
            putExtra(EXTRA_ARGUMENTS, arrayOf("-c", scriptContent))
            putExtra(EXTRA_WORKDIR, TERMUX_HOME_DIR)
            putExtra(EXTRA_BACKGROUND, runInBackground)
            // 0 = ACTION_FAIL_ON_SESSION_EXIT (keep session open if it fails?)
            // let's default to just running.
        }
    }

    /**
     * A simple "Ping" command to check if connection works.
     */
    fun buildTestConnectionIntent(): Intent {
        return buildRunCommandIntent("echo 'FluxLinux: Connection Established!' && sleep 2")
    }

    /**
     * Generates the install command string for manual execution.
     */
    fun getInstallCommand(distroId: String, setupScript: String? = null, installScriptContent: String, guiScriptContent: String): String {
        // Enforce newline termination for safety
        val safeInstallScript = if (!installScriptContent.endsWith("\n")) "$installScriptContent\n" else installScriptContent
        val safeGuiScript = if (!guiScriptContent.endsWith("\n")) "$guiScriptContent\n" else guiScriptContent
        
        val installScriptB64 = android.util.Base64.encodeToString(safeInstallScript.toByteArray(), android.util.Base64.NO_WRAP)
        val guiScriptB64 = android.util.Base64.encodeToString(safeGuiScript.toByteArray(), android.util.Base64.NO_WRAP)
        
        val setupB64 = if (!setupScript.isNullOrEmpty()) {
            android.util.Base64.encodeToString(setupScript.toByteArray(), android.util.Base64.NO_WRAP)
        } else {
            "null"
        }
        
        // Use Base64 decoding to write files. This avoids fragile 'cat << EOF' constructs in terminals
        // and handles special characters safely.
        return """
            echo "$installScriptB64" | base64 -d > ${'$'}HOME/flux_install.sh
            chmod +x ${'$'}HOME/flux_install.sh
            
            echo "$guiScriptB64" | base64 -d > ${'$'}HOME/start_gui.sh
            chmod +x ${'$'}HOME/start_gui.sh
            
            bash ${'$'}HOME/flux_install.sh $distroId "$setupB64"
        """.trimIndent()
    }

    /**
     * Just opens Termux (launcher intent).
     */
    fun buildOpenTermuxIntent(context: android.content.Context): Intent? {
        return context.packageManager.getLaunchIntentForPackage("com.termux")
    }

    /**
     * Installs a specific distro... (Deprecated: User Manual Fallback Preferred)
     */
    fun buildInstallIntent(distroId: String, setupScript: String? = null): Intent {
        // Use the native helper script we created in setup_termux.sh
        // Usage: bash ~/flux_install.sh <distro> <base64_setup>
        
        val setupB64 = if (!setupScript.isNullOrEmpty()) {
            android.util.Base64.encodeToString(setupScript.toByteArray(), android.util.Base64.NO_WRAP)
        } else {
            "null"
        }
        
        val command = "bash $TERMUX_HOME_DIR/flux_install.sh $distroId \"$setupB64\""
        return buildRunCommandIntent(command)
    }

    /**
     * Uninstalls/Removes a specific distro.
     */
    fun buildUninstallIntent(distroId: String): Intent {
        val command = if (distroId == "termux") {
            "pkg uninstall -y xfce4 xfce4-terminal tigervnc && echo 'FluxLinux: Termux Native Desktop Removed.' && sleep 3"
        } else {
            "proot-distro remove $distroId && echo 'FluxLinux: $distroId Uninstalled.' && sleep 3"
        }
        return buildRunCommandIntent(command)
    }

    /**
     * Launches a specific distro in CLI mode (login as flux user).
     */
    fun buildLaunchCliIntent(distroId: String): Intent {
        if (distroId == "termux") {
             return buildRunCommandIntent("echo 'You are already in Termux Native environment!' && sleep 2")
        }
        
        if (distroId == "debian_chroot") {
            // Launch Chroot CLI using Android Root (su)
            return buildRunCommandIntent("su -c \"sh /data/local/tmp/enter_debian.sh\"", runInBackground = false)
        }

        if (distroId == "debian13_chroot") {
            // Launch Debian 13 Chroot CLI using Android Root (su)
            return buildRunCommandIntent("su -c \"sh /data/local/tmp/enter_debian13.sh\"", runInBackground = false)
        }
        
        if (distroId == "arch_chroot") {
            // Launch Arch Chroot CLI (via generated script)
            return buildRunCommandIntent("su -c \"sh /data/local/tmp/enter_arch.sh\"", runInBackground = false)
        }
        
        // Default to 'flux' user if setup, fallback to root if not (proot-distro handles login)
        val command = "proot-distro login $distroId --user flux"
        return buildRunCommandIntent(command, runInBackground = false)
    }

    /**
     * Launches a specific distro in GUI mode (XFCE4).
     */
    fun buildLaunchGuiIntent(distroId: String): Intent {
        if (distroId == "debian_chroot") {
            // Launch Chroot GUI using Android Root (su)
            return buildRunCommandIntent("su -c \"sh /data/local/tmp/start_debian_gui.sh\"", runInBackground = true)
        }

        if (distroId == "debian13_chroot") {
            // Launch Debian 13 Chroot GUI using Android Root (su)
            return buildRunCommandIntent("su -c \"sh /data/local/tmp/start_debian13_gui.sh\"", runInBackground = true)
        }
        
        if (distroId == "arch_chroot") {
            // Launch Arch Chroot GUI (Hyprland via VirGL)
            return buildRunCommandIntent("su -c \"sh /data/local/tmp/start_arch_gui.sh\"", runInBackground = true)
        }
        
        // Standard Proot Launch
        // Execute the helper script created during setup
        val command = "bash $TERMUX_HOME_DIR/start_gui.sh $distroId"
        return buildRunCommandIntent(command, runInBackground = true)
    }

    /**
     * Runs a specific feature script inside the distro.
     * Uses Base64 injection to avoid quoting/escape issues.
     */
    fun buildRunFeatureScriptIntent(distroId: String, scriptContent: String): Intent {
        val safeScript = if (!scriptContent.endsWith("\n")) "$scriptContent\n" else scriptContent
        val scriptB64 = android.util.Base64.encodeToString(safeScript.toByteArray(), android.util.Base64.NO_WRAP)
        
        if (distroId == "debian_chroot") {
            // For Chroot, we must decode the script on the HOST (Android)
            val innerCommand = """
                su -c '
                echo "$scriptB64" | base64 -d > /data/local/tmp/chrootDebian/tmp/flux_feature.sh;
                chmod +x /data/local/tmp/chrootDebian/tmp/flux_feature.sh;
                busybox chroot /data/local/tmp/chrootDebian /bin/su - root -c "bash /tmp/flux_feature.sh";
                rm -f /data/local/tmp/chrootDebian/tmp/flux_feature.sh
                '
            """.trimIndent().replace("\n", " ")
            
            return buildRunCommandIntent(innerCommand, runInBackground = false)
        }

        if (distroId == "debian13_chroot") {
            // Debian 13 Chroot Feature Script
            val innerCommand = """
                su -c '
                echo "$scriptB64" | base64 -d > /data/local/tmp/chrootDebian13/tmp/flux_feature.sh;
                chmod +x /data/local/tmp/chrootDebian13/tmp/flux_feature.sh;
                busybox chroot /data/local/tmp/chrootDebian13 /bin/su - root -c "bash /tmp/flux_feature.sh";
                rm -f /data/local/tmp/chrootDebian13/tmp/flux_feature.sh
                '
            """.trimIndent().replace("\n", " ")
            
            return buildRunCommandIntent(innerCommand, runInBackground = false)
        }
        
        // Command to run inside Termux (Proot):
        val innerCommand = "echo \"$scriptB64\" | base64 -d > /tmp/flux_feature.sh && bash /tmp/flux_feature.sh; rm -f /tmp/flux_feature.sh"
        val command = "proot-distro login $distroId --shared-tmp -- bash -c '$innerCommand'"
        
        return buildRunCommandIntent(command, runInBackground = false) // Foreground to see progress
    }

    /**
     * Runs a script as Android Root (su).
     * Used for uninstalling/managing Chroot environments.
     */
    fun buildRunRootScriptIntent(scriptContent: String): Intent {
        val safeScript = if (!scriptContent.endsWith("\n")) "$scriptContent\n" else scriptContent
        val scriptB64 = android.util.Base64.encodeToString(safeScript.toByteArray(), android.util.Base64.NO_WRAP)
        
        // Write to tmp, execute, then remove.
        // We use /data/local/tmp as it is writable by shell and accessible by root.
        val command = """
            su -c '
            echo "$scriptB64" | base64 -d > /data/local/tmp/flux_root_task.sh
            chmod +x /data/local/tmp/flux_root_task.sh
            sh /data/local/tmp/flux_root_task.sh
            rm -f /data/local/tmp/flux_root_task.sh
            '
        """.trimIndent().replace("\n", " ")
        
        return buildRunCommandIntent(command, runInBackground = false)
    }

    /**
     * Generates a safe command string that detects if it's running as root,
     * and if not, prompts the user to type 'su'.
     * Used for Clipboard copy-paste interactions.
     */
    fun getSafeRootManualCommand(scriptContent: String, scriptName: String): String {
        val safeScript = if (!scriptContent.endsWith("\n")) "$scriptContent\n" else scriptContent
        val scriptB64 = android.util.Base64.encodeToString(safeScript.toByteArray(), android.util.Base64.DEFAULT)
        // Use Heredoc with wrapped Base64 to prevent terminal freeze (Line length limits)
        val chunkedEchos = "cat << 'EOF_B64' > \"\${S}.b64\"\n$scriptB64\nEOF_B64\n"

        return """
            S="/data/local/tmp/$scriptName"
            if [ "${'$'}(id -u)" != "0" ]; then S="${'$'}HOME/$scriptName"; fi
            $chunkedEchos
            base64 -d "${'$'}S.b64" > "${'$'}S"
            rm -f "${'$'}S.b64"
            chmod +x "${'$'}S"
            if [ "${'$'}(id -u)" = "0" ]; then
                sh "${'$'}S"
            else
                echo "⚠️ PLEASE RUN AS ROOT ⚠️"
                echo "Type su and press Enter."
                echo "Then paste this command again."
            fi
        """.trimIndent() + "\n"
    }
}
