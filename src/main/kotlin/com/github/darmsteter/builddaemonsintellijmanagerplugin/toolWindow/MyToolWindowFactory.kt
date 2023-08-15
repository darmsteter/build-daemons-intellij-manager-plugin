package com.github.darmsteter.builddaemonsintellijmanagerplugin.toolWindow

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import com.sun.management.OperatingSystemMXBean
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.management.ManagementFactory
import java.text.DecimalFormat
import javax.swing.*
import kotlin.concurrent.fixedRateTimer

class MyToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val myToolWindow = MyToolWindow()
        val content = ContentFactory.getInstance()
            .createContent(myToolWindow.getContent(), "Build Daemons Intellij Manager", false)
        toolWindow.contentManager.addContent(content)
    }

    class MyToolWindow {
        private val panel = JPanel()
        private val layout = BoxLayout(panel, BoxLayout.Y_AXIS)
        private val totalRAMLabel = JLabel()
        private val freeRAMLabel = JLabel()
        private val daemonsPanel = JPanel()
        private val daemonsButtons = mutableMapOf<String, JButton>()

        init {
            panel.layout = layout
            panel.add(totalRAMLabel)
            panel.add(freeRAMLabel)
            panel.add(daemonsPanel)

            fixedRateTimer(period = 5000) {
                updateDaemonsInfo()
                updateRAMInfo()
            }
        }

        private fun updateDaemonsInfo() {
            val process = Runtime.getRuntime().exec("jps")
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val daemons = reader.readText().lines().filter { it.contains("Daemon") }

            daemonsPanel.removeAll()
            daemonsButtons.clear()

            for (daemon in daemons) {
                if (!daemonsButtons.containsKey(daemon)) {
                    val daemonButton = JButton(daemon)
                    daemonButton.addActionListener {
                        displayDaemonInfo(daemon)
                    }
                    daemonsButtons[daemon] = daemonButton
                    daemonsPanel.add(daemonButton)
                }
            }

            panel.revalidate()
            panel.repaint()
        }

        private fun updateRAMInfo() {
            val osBean = ManagementFactory.getOperatingSystemMXBean() as OperatingSystemMXBean
            val decimalFormat = DecimalFormat("#.##")

            val totalRAM = osBean.totalPhysicalMemorySize.toDouble() / (1024 * 1024)
            val freeRAM = osBean.freePhysicalMemorySize.toDouble() / (1024 * 1024)

            totalRAMLabel.text = "Total RAM: ${decimalFormat.format(totalRAM)} MB"
            freeRAMLabel.text = "Free RAM: ${decimalFormat.format(freeRAM)} MB"
        }

        private fun displayDaemonInfo(daemonName: String) {
            val dialog = JDialog()
            val dialogPanel = JPanel()
            //dialogPanel.add(JLabel("Details for $daemonName"))

            val daemonButton = daemonsButtons[daemonName]
            if (daemonButton != null) {
                val daemonPid = daemonButton.actionCommand.split(" ")[0]

                val killButton = JButton("Kill process")
                killButton.addActionListener {
                    killDaemon(daemonPid, false)
                    daemonsButtons.remove(daemonName)
                    updateDaemonsInfo()
                    dialog.isVisible = false
                }

                val forceKillButton = JButton("Force kill process")
                forceKillButton.addActionListener {
                    killDaemon(daemonPid, true)
                    daemonsButtons.remove(daemonName)
                    updateDaemonsInfo()
                    dialog.isVisible = false
                }

                dialogPanel.add(killButton)
                dialogPanel.add(forceKillButton)
            }

            dialog.contentPane = dialogPanel
            dialog.setSize(300, 150)
            dialog.isVisible = true
        }

        private fun killDaemon(pid: String, force: Boolean) {
            try {
                val command = if (force) "kill -9 $pid" else "kill $pid"
                val process = ProcessBuilder("/bin/sh", "-c", command).start()
                process.waitFor()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun getContent() = panel
    }
}