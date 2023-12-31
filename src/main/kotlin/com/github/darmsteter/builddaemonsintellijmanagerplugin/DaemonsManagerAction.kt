package com.github.darmsteter.builddaemonsintellijmanagerplugin

import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.ex.CustomComponentAction
import com.intellij.openapi.actionSystem.impl.ActionButtonWithText
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.components.JBScrollPane
import com.sun.management.OperatingSystemMXBean
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.Point
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.management.ManagementFactory
import javax.swing.*
import kotlin.concurrent.fixedRateTimer


class DaemonsManagerAction : CustomComponentAction, AnAction("Open Build Daemons Manager") {
    @Volatile
    private var daemonActions = mapOf<String, AnAction>()
    private val daemonTable = DaemonTable(this)
    private lateinit var ramInfoButton: ActionButtonWithText

    init {
        fixedRateTimer(period = 5000) {
            updateDaemonActions()
            updateRAMInfo()
            daemonTable.updateData(daemonActions)
        }
    }

    override fun createCustomComponent(presentation: Presentation, place: String): JComponent {
        if (!this::ramInfoButton.isInitialized) {
            ramInfoButton = ActionButtonWithText(
                this,
                presentation,
                place
            ) { ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE }
        }
        return ramInfoButton
    }

    override fun actionPerformed(e: AnActionEvent) {
        daemonTable.updateData(daemonActions)

        val killAllButton = JButton("Kill All")
        killAllButton.addActionListener {
            createKillAllAction().actionPerformed(e)
        }

        val panel = JPanel(BorderLayout())
        panel.add(JBScrollPane(daemonTable), BorderLayout.CENTER)
        panel.add(killAllButton, BorderLayout.SOUTH)

        val popup = JBPopupFactory.getInstance()
            .createComponentPopupBuilder(panel, daemonTable)
            .setResizable(true)
            .setFocusable(true)
            .setMovable(true)
            .setCancelOnOtherWindowOpen(true)
            .setRequestFocus(true)
            .setTitle("Daemons")
            .setMinSize(Dimension(300, 200))

        val focusOwner = e.inputEvent?.component

        if (focusOwner != null) {
            val relativePoint = RelativePoint(focusOwner, Point(0, focusOwner.height))
            popup.createPopup().show(relativePoint)
        } else {
            popup.createPopup().showInFocusCenter()
            println("Error: focusOwner is null, unable to determine popup location")
        }
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
                        displayDaemonActions(daemon, Point(0, 0))
                    }
                }
                temporaryDaemonActions[daemon] = daemonAction
            }
        }

        daemonActions = temporaryDaemonActions
    }

    private fun updateRAMInfo() {
        if (this::ramInfoButton.isInitialized) {
            val osBean = ManagementFactory.getOperatingSystemMXBean() as OperatingSystemMXBean
            val totalRAM = osBean.totalPhysicalMemorySize.toDouble() / (1024 * 1024)
            val freeRAM = osBean.freePhysicalMemorySize.toDouble() / (1024 * 1024)
            val ramPercentage = ((freeRAM / totalRAM) * 100).toInt()

            val ramInfo = "Free RAM: $ramPercentage%"
            SwingUtilities.invokeLater {
                ramInfoButton.presentation.text = ramInfo
            }
        }
    }

    fun displayDaemonActions(daemonName: String, location: Point) {
        val popupMenu = JPopupMenu()
        val killMenuItem = JMenuItem("Kill")
        val forceKillMenuItem = JMenuItem("Force Kill")

        killMenuItem.addActionListener {
            killDaemon(daemonName, false)
        }

        forceKillMenuItem.addActionListener {
            killDaemon(daemonName, true)
        }

        popupMenu.add(killMenuItem)
        popupMenu.add(forceKillMenuItem)

        popupMenu.show(daemonTable, location.x, location.y + daemonTable.rowHeight)
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

    private fun killDaemon(pid: String, force: Boolean) {
        try {
            val command = if (force) "kill -9 $pid" else "kill $pid"
            val process = ProcessBuilder("/bin/sh", "-c", command).start()
            process.waitFor()

            daemonActions = daemonActions.filterKeys { it.split(" ")[0] != pid }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}