package com.github.darmsteter.builddaemonsintellijmanagerplugin

import com.intellij.ide.DataManager
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Presentation
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JLabel
import javax.swing.JPanel

class RamPanel(
    private val daemonsManagerAction: DaemonsManagerAction,
    private val presentation: Presentation,
    private val place: String,
) : JPanel(),
    Disposable {
    val label = JLabel()

    init {
        initializeLabel()
    }

    private fun initializeLabel() {
        label.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent?) {
                if (e?.clickCount == 1) {
                    val dataContext =
                        DataManager.getInstance().getDataContext(this@RamPanel)
                    val actionManager = ActionManager.getInstance()
                    val actionEvent = AnActionEvent(
                        e,
                        dataContext,
                        place,
                        presentation,
                        actionManager,
                        e.modifiersEx
                    )
                    daemonsManagerAction.actionPerformed(actionEvent)
                }
            }
        })
        add(label)
    }

    override fun addNotify() {
        super.addNotify()
        daemonsManagerAction.registerPanel(this)
    }

    override fun dispose() {
        println("The component is disposed")
        daemonsManagerAction.unregisterPanel(this)
    }
}