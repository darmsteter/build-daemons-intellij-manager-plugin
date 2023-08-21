package com.github.darmsteter.builddaemonsintellijmanagerplugin

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.sun.management.OperatingSystemMXBean
import java.awt.Point
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.management.ManagementFactory
import java.text.DecimalFormat
import javax.swing.*
import kotlin.concurrent.fixedRateTimer

class DaemonsManagerAction : AnAction("Open Build Daemons Manager") {
    @Volatile
    private var daemonActions = mapOf<String, AnAction>()

    init {
        fixedRateTimer(period = 5000) {
            //updateFreeRAMInfo()
            updateDaemonActions()
        }
    }

    override fun actionPerformed(e: AnActionEvent) {
        val actionGroup = DefaultActionGroup()

        for ((_, daemonAction) in daemonActions) {
            actionGroup.add(daemonAction)
        }

        val popup = JBPopupFactory.getInstance()
            .createActionGroupPopup(
                "Daemon Actions",
                actionGroup,
                e.dataContext,
                JBPopupFactory.ActionSelectionAid.NUMBERING,
                false
            )

        val focusOwner = e.inputEvent?.component
        val screenLocation = focusOwner?.locationOnScreen ?: Point(0, 0)
        popup.showInScreenCoordinates(focusOwner ?: JPanel(), screenLocation)
    }

    private fun updateDaemonActions() {
        val process = Runtime.getRuntime().exec("jps")
        val reader = BufferedReader(InputStreamReader(process.inputStream))
        val daemons = reader.readText().lines().filter { it.contains("Daemon") }
        val temporaryDaemonActions = mutableMapOf<String, AnAction>()

        for (daemon in daemons) {
            if (!temporaryDaemonActions.containsKey(daemon)) {
                val daemonAction = object : AnAction(daemon) {
                    override fun actionPerformed(event: AnActionEvent) {
                        displayDaemonActions(daemon)
                    }
                }
                temporaryDaemonActions[daemon] = daemonAction
            }
        }

        daemonActions = temporaryDaemonActions
    }

    private fun updateRAMInfo(label: JLabel, decimalFormat: DecimalFormat) {
        val osBean = ManagementFactory.getOperatingSystemMXBean() as OperatingSystemMXBean
        val freeRAM = osBean.freePhysicalMemorySize.toDouble() / (1024 * 1024)

        label.text = "Free RAM: ${decimalFormat.format(freeRAM)} MB"
    }

    private fun displayDaemonActions(daemonName: String) {
        val actionsDialog = JDialog()
        val actionsPanel = JPanel()
        val killButton = JButton("Kill")
        val forceKillButton = JButton("Force Kill")

        killButton.addActionListener {
            killDaemon(daemonName, false)
            actionsDialog.isVisible = false
        }

        forceKillButton.addActionListener {
            killDaemon(daemonName, true)
            actionsDialog.isVisible = false
        }

        actionsPanel.add(killButton)
        actionsPanel.add(forceKillButton)

        actionsDialog.contentPane = actionsPanel
        actionsDialog.setSize(200, 100)
        actionsDialog.title = "Daemon Actions for $daemonName"
        actionsDialog.isVisible = true
    }

    private fun killDaemon(daemonName: String, force: Boolean) {
        try {
            val daemonInfo = daemonName.split(" ")
            val daemonPid = daemonInfo[0]
            val command = if (force) "kill -9 $daemonPid" else "kill $daemonPid"
            val process = ProcessBuilder("/bin/sh", "-c", command).start()
            process.waitFor()

            daemonActions = daemonActions.filterKeys { it != daemonName }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}