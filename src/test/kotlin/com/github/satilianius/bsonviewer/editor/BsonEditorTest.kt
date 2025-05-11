package com.github.satilianius.bsonviewer.editor

import com.github.satilianius.bsonviewer.editor.BsonConvertor.Companion.jsonToBson
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.fileEditor.FileEditorStateLevel
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class BsonEditorTest : BasePlatformTestCase() {

    // TODO this test prints error logs when with the testEditorState(), but not when run individually
    fun testEditorProperties() {
        // language=JSON
        val bsonContent = jsonToBson("{}")
        val file = WriteAction.computeAndWait<com.intellij.openapi.vfs.VirtualFile, Throwable> {
            val vFile = myFixture.addFileToProject("test_properties.bson", "").virtualFile
            vFile.setBinaryContent(bsonContent)
            vFile
        }

        val editor = BsonEditor(project, file)

        try {
            assertEquals("Editor name should be 'BSON Editor'", "BSON Editor", editor.name)
            assertEquals("Editor file should match the input file", file, editor.file)
            assertFalse("Editor should not be modified initially", editor.isModified)
            assertTrue("Editor should be valid", editor.isValid)
            assertNotNull("Editor component should not be null", editor.component)
            assertNotNull("Editor preferred focused component should not be null", editor.preferredFocusedComponent)
        } finally {
            editor.dispose()
        }
    }

    fun testEditorState() {
        // language=JSON
        val bsonContent = jsonToBson("""{"test": "value"}""")
        val file = WriteAction.computeAndWait<com.intellij.openapi.vfs.VirtualFile, Throwable> {
            val vFile = myFixture.addFileToProject("test_state.bson", "").virtualFile
            vFile.setBinaryContent(bsonContent)
            vFile
        }

        val editor = BsonEditor(project, file)

        try {
            val state = editor.getState(FileEditorStateLevel.FULL)
            assertNotNull("Editor state should not be null", state)

            editor.setState(state)
        } finally {
            editor.dispose()
        }
    }

    // TODO write a test for disposing the editor, which checks for double disposal exception on tab closure
    // 2025-05-11 23:45:52,179 [  88692] SEVERE - #c.i.o.u.ObjectTree - Double release of editor:
    //com.intellij.openapi.util.TraceableDisposable$DisposalException: Double release of editor:
    //	at com.intellij.openapi.util.TraceableDisposable.throwDisposalError(TraceableDisposable.java:48)
    //	at com.intellij.openapi.editor.impl.EditorImpl.throwDisposalError(EditorImpl.java:1074)
    //	at com.intellij.openapi.editor.impl.EditorImpl.lambda$release$18(EditorImpl.java:1095)
    //	at com.intellij.openapi.editor.impl.EditorImpl.executeNonCancelableBlock(EditorImpl.java:1086)
    //	at com.intellij.openapi.editor.impl.EditorImpl.release(EditorImpl.java:1093)
    //	at com.intellij.openapi.editor.impl.EditorFactoryImpl.releaseEditor(EditorFactoryImpl.kt:237)
    //	at com.github.satilianius.bsonviewer.editor.BsonEditor.dispose(BsonEditor.kt:83)
    //	at com.intellij.openapi.util.ObjectTree.runWithTrace(ObjectTree.java:131)
}
