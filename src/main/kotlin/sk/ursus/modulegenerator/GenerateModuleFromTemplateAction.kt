package sk.ursus.modulegenerator

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.annotations.Nullable
import org.jetbrains.annotations.SystemIndependent
import java.io.File


class GenerateModuleFromTemplateAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: error("No project")
        val projectRootPath = project.basePath

        val moduleTemplatesDir = File(projectRootPath, "module_templates")
        if (!moduleTemplatesDir.exists()) error("templates module not found")

        // Clicked folder, or its parent if clicked a file
        var clickedFile = e.dataContext.getData(PlatformDataKeys.VIRTUAL_FILE) ?: error("No file")
        if (!clickedFile.isDirectory) {
            clickedFile = clickedFile.parent
        }

        // Ask for module name
        val (moduleName, packageName) = askForInput(project)

        // Copy contents from 'module_templates' to clicked folder
        copyTemplates(moduleTemplatesDir, clickedFile, moduleName, packageName)

        // Append modules to settings.gradle
        val settingsGradleFile: File =
            appendGradleSettings(projectRootPath, clickedFile, moduleTemplatesDir, moduleName)

        // Refresh files
        clickedFile.refresh(true, false)
        LocalFileSystem.getInstance().findFileByIoFile(settingsGradleFile)?.refresh(true, false)
    }

    private fun askForInput(project: Project): Pair<String, String> {
        val inputModuleName = Messages.showInputDialog(
            project, "Module name", "Enter module name", null
        ) ?: error("Empty")
        val moduleName = inputModuleName.toLowerCase().replace(" ", "-")

        val inputPackageName = Messages.showInputDialog(
            project, "Package name", "Enter package name", null
        ) ?: error("Empty")
        val packageName = "sk${File.separator}o2${File.separator}${inputPackageName.toLowerCase().replace(" ", "")}"

        return Pair(moduleName, packageName)
    }

    private fun copyTemplates(
        moduleTemplatesDir: File,
        clickedFile: @Nullable VirtualFile,
        moduleName: String,
        packageName: String
    ) {
        try {
            moduleTemplatesDir.copyRecursively(
                File(clickedFile.path),
                overwrite = true,
                moduleName = moduleName,
                packageName = packageName
            )
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    private fun appendGradleSettings(
        projectRootPath: @Nullable @SystemIndependent String?,
        clickedFile: @Nullable VirtualFile,
        moduleTemplatesDir: File,
        moduleName: String
    ): File {
        val settingsGradleFile = File(projectRootPath, "settings.gradle")
        var parentModulePath =
            File(clickedFile.path).relativeTo(File(projectRootPath)).path.replace(File.separator, ":")
        if (parentModulePath.isNotEmpty()) {
            parentModulePath = ":$parentModulePath"
        }
        val templateModulesString = moduleTemplatesDir.listFiles()?.first()?.listFiles()
            ?.filter { it.isDirectory }
            ?.joinToString(prefix = "\n", separator = "\n") { "include \"$parentModulePath:$moduleName:${it.name}\"" }
            ?: ""

        settingsGradleFile.appendText(templateModulesString)
        return settingsGradleFile
    }
}