package com.github.darmsteter.builddaemonsintellijmanagerplugin

import com.intellij.openapi.actionSystem.AnAction
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.util.*
import javax.swing.JTable
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableModel


class DaemonTable(
    private val daemonsManagerAction: DaemonsManagerAction
) : JTable() {
    override fun createDefaultDataModel() = DefaultTableModel(arrayOf("Daemon name", "RAM", "%CPU"), 0)

    init {
        addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.clickCount == 1) {
                    val selectedRow = rowAtPoint(e.point)
                    if (selectedRow >= 0) {
                        val daemonName = model.getValueAt(selectedRow, 0) as String
                        val location = e.point
                        daemonsManagerAction.displayDaemonActions(daemonName, location)
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

        val rowsToRemove = model.dataVector.filterNot { it[0] in daemonActions.keys }
        model.dataVector.removeAll(rowsToRemove.toSet())

        for (daemon in daemonActions) {
            val daemonName = daemon.key

            val existing = model.dataVector.indexOfFirst { it[0] == daemonName }
            if (existing != -1) {
                model.dataVector[existing][1] = "0 MB"
                model.dataVector[existing][2] = "0.0%"
            } else {
                model.addRow(Vector<String>().apply {
                    add(daemonName)
                    add("0 MB")
                    add("0.0%")
                })
            }
        }

        model.fireTableDataChanged()
    }
}