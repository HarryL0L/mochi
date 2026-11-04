package com.harryl.island

/** What a progress notification is doing, so the island can pick a fitting icon and animation. */
enum class TransferKind { NONE, DOWNLOAD, UPLOAD, EXTRACT, ARCHIVE, COPY }

/** Pure text classifier for progress notifications (unit tested on the JVM). */
object Transfer {

    private val extract = Regex("""\b(extract\w*|unzip\w*|unpack\w*|decompress\w*|unarchiv\w*|unrar\w*)""")
    // Verbs only: "Downloading archive.zip" is a download, not an archive job.
    private val archive = Regex("""\b(compressing|compress(ed)? (to|into)|zipping|archiving|packing|creating (a |an )?(zip|archive))""")
    private val upload = Regex("""\b(upload\w*|sending|backing up|syncing to)""")
    private val download = Regex("""\b(download\w*|receiving|fetching|saving to device)""")
    private val copy = Regex("""\b(copy\w*|copying|moving|transferr?\w*|installing)""")

    private val downloadPackages = setOf(
        "com.android.providers.downloads",
        "com.google.android.providers.downloads",
        "com.android.vending",
    )

    fun classify(text: String, packageName: String = ""): TransferKind {
        val s = text.lowercase()
        return when {
            // Check extract before archive: "unzipping" contains "zip".
            extract.containsMatchIn(s) -> TransferKind.EXTRACT
            archive.containsMatchIn(s) -> TransferKind.ARCHIVE
            upload.containsMatchIn(s) -> TransferKind.UPLOAD
            download.containsMatchIn(s) -> TransferKind.DOWNLOAD
            copy.containsMatchIn(s) -> TransferKind.COPY
            packageName in downloadPackages -> TransferKind.DOWNLOAD
            else -> TransferKind.NONE
        }
    }
}
