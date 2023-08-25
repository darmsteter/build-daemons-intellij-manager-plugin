package com.github.darmsteter.builddaemonsintellijmanagerplugin

import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.ex.CustomComponentAction
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.components.JBScrollPane
import com.sun.management.OperatingSystemMXBean
import java.awt.Point
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.management.ManagementFactory
import java.util.*
import javax.swing.JButton
import javax.swing.JDialog
import javax.swing.JPanel
import javax.swing.SwingUtilities
import kotlin.concurrent.fixedRateTimer

class DaemonsManagerAction : CustomComponentAction, AnAction("Open Build Daemons Manager") {
    @Volatile
    private var daemonActions = mapOf<String, AnAction>()
    private val ramPanels = Collections.synchronizedList(mutableListOf<RamPanel>())
    private val daemonActionsTable = DaemonActionsTable(this)

    init {
        fixedRateTimer(period = 5000) {
            updateDaemonActions()
            updateRAMInfo()
            daemonActionsTable.updateData(daemonActions)
        }
    }

    override fun actionPerformed(e: AnActionEvent) {
        daemonActionsTable.updateData(daemonActions)
        val scrollPane = JBScrollPane(daemonActionsTable)

        val popup = JBPopupFactory.getInstance()
            .createComponentPopupBuilder(scrollPane, daemonActionsTable)
            .setResizable(true)
            .setFocusable(true)
            .setMovable(true)
            .setCancelOnOtherWindowOpen(true)
            .setRequestFocus(true)
            .setTitle("Daemons")
            .createPopup()

        val focusOwner = e.inputEvent?.component
        val screenLocation = focusOwner?.locationOnScreen ?: Point(0, 0)
        popup.showInScreenCoordinates(focusOwner ?: JPanel(), screenLocation)
    }

    fun registerPanel(panel: RamPanel) {
        ramPanels.add(panel)
    }

    fun unregisterPanel(panel: RamPanel) {
        ramPanels.remove(panel)
    }

    override fun createCustomComponent(presentation: Presentation, place: String) = RamPanel(this, presentation, place)

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

    private fun updateRAMInfo() {
        val osBean = ManagementFactory.getOperatingSystemMXBean() as OperatingSystemMXBean
        val totalRAM = osBean.totalPhysicalMemorySize.toDouble() / (1024 * 1024)
        val freeRAM = osBean.freePhysicalMemorySize.toDouble() / (1024 * 1024)
        val usedRAM = totalRAM - freeRAM
        val ramPercentage = (usedRAM / totalRAM * 100).toInt()

        val ramInfo = "Free RAM: ${100 - ramPercentage}%"
        SwingUtilities.invokeLater {
            for (panel in ramPanels) {
                panel.label.text = ramInfo
            }
        }
    }
     fun displayDaemonActions(daemonName: String) {
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

    private fun createKillAllAction(): AnAction {
        return object : AnAction("Kill All") {
            override fun actionPerformed(e: AnActionEvent) {
                val daemonPids = daemonActions.keys.map { it.split(" ")[0] }
                if (daemonPids.isNotEmpty()) {
                    val command = "kill ${daemonPids.joinToString(" ")}"
                    try {
                        val process = ProcessBuilder("/bin/sh", "-c", command).start()
                        process.waitFor()
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                    }
                }
            }
        }
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