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
     * EXTENDED INSTALL: Generates a compound script to install base + components
     */
    fun buildCompoundInstallIntent(
        context: android.content.Context,
        distro: com.ivarna.fluxlinux.core.data.Distro,
        selectedComponents: List<com.ivarna.fluxlinux.core.data.DistroComponent>
    ): Intent {
        val scriptManager = com.ivarna.fluxlinux.core.data.ScriptManager(context)
        
        // 1. Get Base Install Script
        // For Debian 13 Chroot, we use specific scripts.
        val baseScriptName = when (distro.id) {
            "debian13_chroot" -> "chroot/setup_debian13_chroot.sh"
            "debian_chroot" -> "chroot/setup_debian_chroot.sh"
            "termux" -> "common/setup_termux.sh"
            else -> "common/setup_debian_family.sh" // Fallback proot
        }
        
        var fullScript = scriptManager.getScriptContent(baseScriptName)
        
        // 2. Append Component Scripts
        // Logic depends on Chroot vs Proot
        val isChroot = distro.id.contains("chroot")
        
        val componentsBlock = StringBuilder()
        componentsBlock.append("\n# --- FLUXLINUX COMPONENT INSTALLATION ---\n")
        
        selectedComponents.forEach { component ->
            val compScript = scriptManager.getScriptContent(component.scriptName)
            val compScriptB64 = android.util.Base64.encodeToString(compScript.toByteArray(), android.util.Base64.NO_WRAP)
            
            componentsBlock.append("echo \"Installing ${component.name}...\"\n")
            
            if (isChroot) {
                // For Chroot, we must write script to host temp, then execute inside chroot
                val chrootPath = if (distro.id == "debian13_chroot") "/data/local/tmp/chrootDebian13" else "/data/local/tmp/chrootDebian"
                
                componentsBlock.append("""
                    echo "$compScriptB64" | base64 -d > $chrootPath/tmp/flux_comp_${component.id}.sh
                    chmod +x $chrootPath/tmp/flux_comp_${component.id}.sh
                    busybox chroot $chrootPath /bin/su - root -c "bash /tmp/flux_comp_${component.id}.sh"
                    rm -f $chrootPath/tmp/flux_comp_${component.id}.sh
                    # Mark component as installed via callback (handled by app later, but let's log it)
                    echo "Component ${component.id} done."
                """.trimIndent())
                componentsBlock.append("\n")
            } else if (distro.id == "termux") {
                // Native Termux
                componentsBlock.append("""
                    echo "$compScriptB64" | base64 -d > $TERMUX_HOME_DIR/flux_comp_${component.id}.sh
                    bash $TERMUX_HOME_DIR/flux_comp_${component.id}.sh
                    rm -f $TERMUX_HOME_DIR/flux_comp_${component.id}.sh
                """.trimIndent())
                componentsBlock.append("\n")
            } else {
                // Proot Distro
                componentsBlock.append("""
                    echo "$compScriptB64" | base64 -d > $TERMUX_HOME_DIR/flux_comp_${component.id}.sh
                    proot-distro login ${distro.id} --shared-tmp -- bash -c "bash /data/data/com.termux/files/home/flux_comp_${component.id}.sh"
                    rm -f $TERMUX_HOME_DIR/flux_comp_${component.id}.sh
                """.trimIndent())
                componentsBlock.append("\n")
            }
        }
        
        // 3. Inject this block BEFORE the final callback in the base script?
        // Actually, the base scripts often end with `am start ...`. 
        // We should REMOVE the original callback from the base script and append our own Final Callback.
        
        // Basic Regex to remove the success callback from base script
        // We look for: am start -a android.intent.action.VIEW -d "fluxlinux://callback?result=success...
        // This is risky if regex fails.
        // Safer approach: Append the components block, and ensure the base script doesn't EXIT before we run components.
        // Most setup scripts have `check_exit` at end.
        
        // EDIT: `setup_debian13_chroot.sh` ends with a callback.
        // We will append the component block at the very end.
        // But if the base script `exit 0`, the appended code won't run.
        // We need to wrap the base script content or modify it.
        
        // HACK: Replace "exit 0" with nothing, and "am start..." with nothing in the String content?
        fullScript = fullScript.replace("exit 0", "# exit 0 deferred")
        fullScript = fullScript.replace("am start -a android.intent.action.VIEW -d \"fluxlinux://callback?result=success", "# Deferred callback: am start ...")
        
        // Append components
        fullScript += componentsBlock.toString()
        
        // Append Final Success Callback
        // We construct a URL that encodes the list of installed components so the app can update StateManager
        val componentIds = selectedComponents.joinToString(",") { it.id }
        // We can't pass unlimited length in query param easily, but component IDs are short.
        // actually, implementing individual component install callbacks is better.
        // For now, just generic success for distro, and we assume components installed if script finishes?
        // No, let's fire a specific callback for components?
        // Or better: The App "Install Wizard" assumes success if the final callback is received.
        // We can pass `components=id1,id2,id3` in the callback URL.
        
        val cleanDistroId = distro.id.replace(" ", "")
        fullScript += "\n\n"
        fullScript += """
            echo "FluxLinux: All components installed."
            am start -a android.intent.action.VIEW -d "fluxlinux://callback?result=success&name=distro_install_${cleanDistroId}&components=${componentIds}"
        """.trimIndent()
        
        // Now wrap it all in the Base64 loader to avoid escaping hell
        return buildRunRootScriptIntent(fullScript) // Using Root Intent because Chroot setup needs root.
        // Wait, for Proot/Termux we don't need root.
        
        // Logic split:
        if (isChroot) {
            return buildRunRootScriptIntent(fullScript)
        } else {
             // For Proot, we wrap in normal runIntent
             val safeScript = if (!fullScript.endsWith("\n")) "$fullScript\n" else fullScript
             val scriptB64 = android.util.Base64.encodeToString(safeScript.toByteArray(), android.util.Base64.NO_WRAP)
             val loader = "echo \"$scriptB64\" | base64 -d > $TERMUX_HOME_DIR/flux_full_install.sh; bash $TERMUX_HOME_DIR/flux_full_install.sh; rm $TERMUX_HOME_DIR/flux_full_install.sh"
             return buildRunCommandIntent(loader)
        }
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
