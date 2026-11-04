package com.harryl.island

import org.junit.Assert.assertEquals
import org.junit.Test

class TransferTest {
    @Test
    fun downloads() {
        assertEquals(TransferKind.DOWNLOAD, Transfer.classify("Downloading photos.zip · 24 MB of 60 MB"))
        assertEquals(TransferKind.DOWNLOAD, Transfer.classify("video.mp4", "com.android.providers.downloads"))
        assertEquals(TransferKind.DOWNLOAD, Transfer.classify("Receiving 3 files"))
        // File names never decide the kind.
        assertEquals(TransferKind.DOWNLOAD, Transfer.classify("Downloading backup-archive.rar"))
        assertEquals(TransferKind.UPLOAD, Transfer.classify("Uploading photos.zip"))
    }

    @Test
    fun uploads() {
        assertEquals(TransferKind.UPLOAD, Transfer.classify("Uploading 2 photos"))
        assertEquals(TransferKind.UPLOAD, Transfer.classify("Backing up chats"))
        assertEquals(TransferKind.UPLOAD, Transfer.classify("Sending video"))
    }

    @Test
    fun extractingIsNotMistakenForZipping() {
        assertEquals(TransferKind.EXTRACT, Transfer.classify("Unzipping archive.zip"))
        assertEquals(TransferKind.EXTRACT, Transfer.classify("Extracting files 40%"))
        assertEquals(TransferKind.EXTRACT, Transfer.classify("Decompressing data.7z"))
    }

    @Test
    fun archiving() {
        assertEquals(TransferKind.ARCHIVE, Transfer.classify("Compressing 12 items"))
        assertEquals(TransferKind.ARCHIVE, Transfer.classify("Creating zip"))
        assertEquals(TransferKind.ARCHIVE, Transfer.classify("Zipping folder"))
    }

    @Test
    fun copyingAndUnknown() {
        assertEquals(TransferKind.COPY, Transfer.classify("Copying 5 files to SD card"))
        assertEquals(TransferKind.COPY, Transfer.classify("Moving items"))
        assertEquals(TransferKind.NONE, Transfer.classify("Navigation to Home"))
    }
}
