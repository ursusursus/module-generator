package sk.ursus.modulegenerator

import java.io.File
import java.io.IOException

/**
 * Copy paste from standard library, adding in replacing for MODULE, PACKAGE
 */
fun File.copyRecursively(
    target: File,
    overwrite: Boolean = false,
    onError: (File, IOException) -> OnErrorAction = { _, exception -> throw exception },
    moduleName: String,
    packageName: String
): Boolean {
    if (!exists()) {
        return onError(this, NoSuchFileException(file = this, reason = "The source file doesn't exist.")) !=
                OnErrorAction.TERMINATE
    }
    try {
        // We cannot break for loop from inside a lambda, so we have to use an exception here
        for (src in walkTopDown().onFail { f, e ->
            if (onError(f, e) == OnErrorAction.TERMINATE) throw TerminateException(f)
        }) {
            if (!src.exists()) {
                if (onError(src, NoSuchFileException(file = src, reason = "The source file doesn't exist.")) ==
                    OnErrorAction.TERMINATE
                )
                    return false
            } else {
                val relPath = src.toRelativeString(this)
                    .replace("MODULE", moduleName)
                    .replace("PACKAGE", packageName)

                val dstFile = File(target, relPath)
                if (dstFile.exists() && !(src.isDirectory && dstFile.isDirectory)) {
                    val stillExists = if (!overwrite) true else {
                        if (dstFile.isDirectory)
                            !dstFile.deleteRecursively()
                        else
                            !dstFile.delete()
                    }

                    if (stillExists) {
                        if (onError(
                                dstFile, FileAlreadyExistsException(
                                    file = src,
                                    other = dstFile,
                                    reason = "The destination file already exists."
                                )
                            ) == OnErrorAction.TERMINATE
                        )
                            return false

                        continue
                    }
                }

                if (src.isDirectory) {
                    dstFile.mkdirs()
                } else {
                    if (src.copyTo(dstFile, overwrite).length() != src.length()) {
                        if (onError(
                                src,
                                IOException("Source file wasn't copied completely, length of destination file differs.")
                            ) == OnErrorAction.TERMINATE
                        )
                            return false
                    }
                }
            }
        }
        return true
    } catch (e: TerminateException) {
        return false
    }
}

private class TerminateException(file: File) : FileSystemException(file) {}