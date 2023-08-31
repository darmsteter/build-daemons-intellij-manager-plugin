package com.github.darmsteter.builddaemonsintellijmanagerplugin

import com.intellij.openapi.actionSystem.AnAction
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.CharacterIterator
import java.text.StringCharacterIterator
import java.util.*
import javax.swing.JTable
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableModel
import kotlin.math.abs


class DaemonTable(
    private val daemonsManagerAction: DaemonsManagerAction
) : JTable() {
    override fun createDefaultDataModel() = DefaultTableModel(arrayOf("PID", "Daemon Name", "RAM", "%CPU"), 0)

    init {
        addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.clickCount == 1) {
                    val selectedRow = rowAtPoint(e.point)
                    if (selectedRow >= 0) {
                        val daemonPid = model.getValueAt(selectedRow, 0) as String
                        val location = e.point
                        daemonsManagerAction.displayDaemonActions(daemonPid, location)
                    }
                }
            }
        })
    }

    override fun setModel(dataModel: TableModel) {
        if (model == null) {
            super.setModel(dataModel)
        } else {
            error("Changing model is not allowed")
        }
    }

    fun updateData(daemonActions: Map<String, AnAction>) {
        val model = this.model as DefaultTableModel

        val rowsToRemove = model.dataVector.filterNot { row ->
            val pid = row[0] as String
            val daemonName = row[1] as String
            daemonName in daemonActions.keys || daemonActions.keys.any { it.startsWith(pid) }
        }
        model.dataVector.removeAll(rowsToRemove.toSet())

        for (daemon in daemonActions) {
            val daemonName = daemon.key
            val pid = daemonName.split(" ")[0]
            val processInfo = getProcessInfo(pid)

            val existing = model.dataVector.indexOfFirst { it[0] == pid }
            if (existing != -1) {
                model.dataVector[existing][1] = daemonName.substringAfter(' ')
                model.dataVector[existing][2] = processInfo.first
                model.dataVector[existing][3] = processInfo.second
            } else {
                model.addRow(Vector<String>().apply {
                    add(pid)
                    add(daemonName.substringAfter(' '))
                    add(processInfo.first)
                    add(processInfo.second)
                })
            }
        }

        model.fireTableDataChanged()
    }

    private fun getProcessInfo(pid: String): Pair<String, String> {
        try {
            val command = "ps -o rss=,pcpu= -p $pid"
            val process = ProcessBuilder("/bin/sh", "-c", command).start()
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = reader.readLine()
            process.waitFor()

            val parts = output?.trim()?.split(" ")?.filter(String::isNotBlank) ?: emptyList()
            if (parts.size >= 2) {
                val ramValue = parts[0].toLongOrNull() ?: 0
                val cpuValue = parts[1].toDoubleOrNull() ?: 0.0

                val formattedRAM = String.format(humanReadableByteCountBin(ramValue * 1024))
                val formattedCPU = String.format("%.1f %%", cpuValue)

                return Pair(formattedRAM, formattedCPU)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return Pair("N/A", "N/A")
    }

    private fun humanReadableByteCountBin(bytes: Long): String {
        val absB = if (bytes == Long.MIN_VALUE) Long.MAX_VALUE else abs(bytes.toDouble())
            .toLong()
        if (absB < 1024) {
            return "$bytes B"
        }
        var value = absB
        val ci: CharacterIterator = StringCharacterIterator("KMGTPE")
        var i = 40
        while (i >= 0 && absB > 0xfffccccccccccccL shr i) {
            value = value shr 10
            ci.next()
            i -= 10
        }
        value *= java.lang.Long.signum(bytes).toLong()
        return String.format("%.1f %ciB", value / 1024.0, ci.current())
    }
}