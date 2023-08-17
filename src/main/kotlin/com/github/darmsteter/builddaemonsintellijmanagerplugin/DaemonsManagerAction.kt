package com.github.darmsteter.builddaemonsintellijmanagerplugin

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.sun.management.OperatingSystemMXBean
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.management.ManagementFactory
import java.text.DecimalFormat
import javax.swing.*
import kotlin.concurrent.fixedRateTimer

class DaemonsManagerAction : AnAction("Build daemons manager plugin") {
    override fun actionPerformed(e: AnActionEvent) {
        val dialog = JDialog()
        val dialogPanel = JPanel()
        val freeRAMLabel = JLabel()
        val daemonsPanel = JPanel()
        val closeButton = JButton("Close")

        dialogPanel.layout = BoxLayout(dialogPanel, BoxLayout.Y_AXIS)

        val decimalFormat = DecimalFormat("#.##")
        fixedRateTimer(period = 5000) {
            updateDaemonsInfo(daemonsPanel)
            updateRAMInfo(freeRAMLabel, decimalFormat)
        }

        closeButton.addActionListener {
            dialog.isVisible = false
        }

        dialogPanel.add(freeRAMLabel)
        dialogPanel.add(daemonsPanel)
        dialogPanel.add(closeButton)
        dialog.contentPane = dialogPanel
        dialog.setSize(400, 300)
        dialog.isVisible = true
    }

    private fun updateDaemonsInfo(panel: JPanel) {
        val process = Runtime.getRuntime().exec("jps")
        val reader = BufferedReader(InputStreamReader(process.inputStream))
        val daemons = reader.readText().lines().filter { it.contains("Daemon") }

        panel.removeAll()

        for (daemon in daemons) {
            val daemonButton = JButton(daemon)
            daemonButton.addActionListener {
                displayDaemonActions(daemon)
            }
            panel.add(daemonButton)
        }

        panel.revalidate()
        panel.repaint()
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
        actionsDialog.isVisible = true
    }

    private fun killDaemon(daemonName: String, force: Boolean) {
        try {
            val daemonInfo = daemonName.split(" ")
            val daemonPid = daemonInfo[0]
            val command = if (force) "kill -9 $daemonPid" else "kill $daemonPid"
            val process = ProcessBuilder("/bin/sh", "-c", command).start()
            process.waitFor()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}