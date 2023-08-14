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
            dialogPanel.add(JLabel("Details for $daemonName"))
            dialog.contentPane = dialogPanel
            dialog.setSize(300, 150)
            dialog.isVisible = true
        }

        fun getContent() = panel
    }
}