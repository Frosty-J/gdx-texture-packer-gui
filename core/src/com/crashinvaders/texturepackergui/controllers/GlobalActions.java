package com.crashinvaders.texturepackergui.controllers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.tools.texturepacker.TexturePacker;
import com.badlogic.gdx.utils.Array;
import com.crashinvaders.texturepackergui.App;
import com.crashinvaders.texturepackergui.AppConstants;
import com.crashinvaders.texturepackergui.controllers.extensionmodules.CjkFontExtensionModule;
import com.crashinvaders.texturepackergui.controllers.main.MainController;
import com.crashinvaders.texturepackergui.controllers.model.ModelService;
import com.crashinvaders.texturepackergui.controllers.model.ModelUtils;
import com.crashinvaders.texturepackergui.controllers.model.PackModel;
import com.crashinvaders.texturepackergui.controllers.model.ProjectModel;
import com.crashinvaders.texturepackergui.controllers.ninepatcheditor.NinePatchToolController;
import com.crashinvaders.texturepackergui.controllers.packing.PackDialogController;
import com.crashinvaders.texturepackergui.controllers.projectserializer.ProjectSerializer;
import com.crashinvaders.texturepackergui.controllers.settings.SettingsDialogController;
import com.crashinvaders.texturepackergui.events.ShowToastEvent;
import com.crashinvaders.texturepackergui.utils.FileUtils;
import com.crashinvaders.texturepackergui.utils.SystemUtils;
import com.crashinvaders.texturepackergui.utils.WidgetUtils;
import com.crashinvaders.texturepackergui.views.dialogs.OptionDialog;
import com.github.czyzby.autumn.annotation.Initiate;
import com.github.czyzby.autumn.annotation.Inject;
import com.github.czyzby.autumn.mvc.component.i18n.LocaleService;
import com.github.czyzby.autumn.mvc.component.ui.InterfaceService;
import com.github.czyzby.autumn.mvc.component.ui.SkinService;
import com.github.czyzby.autumn.mvc.stereotype.ViewActionContainer;
import com.github.czyzby.autumn.processor.event.EventDispatcher;
import com.github.czyzby.lml.annotation.LmlAction;
import com.github.czyzby.lml.parser.action.ActionContainer;
import com.kotcrab.vis.ui.FocusManager;
import com.kotcrab.vis.ui.Locales;
import com.kotcrab.vis.ui.util.dialog.Dialogs;
import com.kotcrab.vis.ui.util.dialog.InputDialogAdapter;
import com.kotcrab.vis.ui.util.dialog.OptionDialogAdapter;
import com.kotcrab.vis.ui.widget.VisDialog;

import java.awt.*;
import java.io.IOException;
import java.util.Locale;

//TODO move model logic code to ModelUtils
@ViewActionContainer("global")
public class GlobalActions implements ActionContainer {
    private static final String TAG = GlobalActions.class.getSimpleName();

    @Inject InterfaceService interfaceService;
    @Inject LocaleService localeService;
    @Inject FileDialogService fileDialogService;
    @Inject SkinService skinService;
    @Inject EventDispatcher eventDispatcher;
    @Inject ModelService modelService;
    @Inject ModelUtils modelUtils;
    @Inject ProjectSerializer projectSerializer;
    @Inject RecentProjectsRepository recentProjects;
    @Inject MainController mainController;
    @Inject PackMultipleAtlasesDialogController packMultipleDialogController;
    @Inject PackDialogController packDialogController;
    @Inject NinePatchToolController ninePatchToolController;
    @Inject public CommonDialogs commonDialogs;

    /** Common preferences */
    private Preferences prefs;
    private FileChooserHistory fileChooserHistory;

    @Initiate
    public void initialize() {
        prefs = Gdx.app.getPreferences(AppConstants.PREF_NAME_COMMON);
        fileChooserHistory = new FileChooserHistory(prefs);
    }

    @LmlAction("resetViewFocus") public void resetViewFocus() {
        FocusManager.resetFocus(getStage());
    }

	@LmlAction({"createAtlas", "newPack"}) public void newPack() {
        commonDialogs.newPack();
	}

    @LmlAction({"renameAtlas", "renamePack"}) public void renamePack() {
        final PackModel pack = getSelectedPack();
        if (pack == null) return;

        VisDialog dialog = WidgetUtils.createInputDialog(getString("renamePack"), null, pack.getName(), true, new InputDialogAdapter() {
            @Override
            public void finished(String input) {
                pack.setName(input);
            }
        });

        getStage().addActor(dialog.fadeIn());
    }

    @LmlAction({"cloneAtlas", "copyPack"}) public void copyPack() {
        final PackModel pack = getSelectedPack();
        if (pack == null) return;

        commonDialogs.copyPack(pack);
    }

    @LmlAction({"deleteAtlas", "deletePack"}) public void deletePack() {
        final PackModel pack = getSelectedPack();
        if (pack == null) return;

        OptionDialog optionDialog = OptionDialog.show(getStage(), getString("deletePack"), getString("dialogTextDeletePack", pack.getName()),
                Dialogs.OptionDialogType.YES_CANCEL, new OptionDialogAdapter() {
                    @Override
                    public void yes() {
                        modelUtils.selectClosestPack(pack);
                        getProject().removePack(pack);
                    }
                });
        optionDialog.closeOnEscape();
    }

    @LmlAction({"moveAtlasUp", "movePackUp"}) public void movePackUp() {
        PackModel pack = getSelectedPack();
        if (pack == null) return;

        modelUtils.movePackUp(pack);
    }

    @LmlAction({"moveAtlasDown", "movePackDown"}) public void movePackDown() {
        PackModel pack = getSelectedPack();
        if (pack == null) return;

        modelUtils.movePackDown(pack);
    }

    @LmlAction({"selectNextAtlas", "selectNextPack"}) public void selectNextPack() {
        PackModel pack = getSelectedPack();
        if (pack == null) return;

        modelUtils.selectNextPack(pack);
    }

    @LmlAction({"selectPreviousAtlas", "selectPreviousPack"}) public void selectPreviousPack() {
        PackModel pack = getSelectedPack();
        if (pack == null) return;

        modelUtils.selectPrevPack(pack);
    }

    @LmlAction({"packAllAtlases", "packAll"}) public void packAll() {
        ProjectModel project = getProject();
        Array<PackModel> packs = getProject().getPacks();
        if (packs.size == 0) return;

        interfaceService.showDialog(packDialogController.getClass());
        packDialogController.launchPack(project, packs);
    }

    @LmlAction({"packSelectedAtlas", "packSelected"}) public void packSelected() {
        ProjectModel project = getProject();
        PackModel pack = getSelectedPack();
        if (pack == null) return;

        interfaceService.showDialog(packDialogController.getClass());
        packDialogController.launchPack(project, pack);
    }

    @LmlAction({"packMultipleAtlases", "packMultiple"}) public void packMultiple() {
        ProjectModel project = getProject();
        Array<PackModel> packs = getProject().getPacks();
        if (packs.size == 0) return;

        interfaceService.showDialog(packMultipleDialogController.getClass());
    }

    @LmlAction("newProject") public void newProject() {
        commonDialogs.checkUnsavedChanges(() ->
                modelService.setProject(new ProjectModel()));
    }

    @LmlAction("openProject") public void openProject() {
        final ProjectModel project = getProject();
        FileHandle dir = fileChooserHistory.getLastDir(FileChooserHistory.Type.PROJECT);
        if (FileUtils.fileExists(project.getProjectFile())) {
            dir = project.getProjectFile().parent();
        }

        fileDialogService.openFile("Open project", dir,
                FileDialogService.FileFilter.createSingle(getString("projectFileDescription", AppConstants.PROJECT_FILE_EXT), AppConstants.PROJECT_FILE_EXT),
                new FileDialogService.CallbackAdapter() {
                    @Override
                    public void selected(Array<FileHandle> files) {
                        final FileHandle chosenFile = files.first();
                        commonDialogs.checkUnsavedChanges(() -> loadProject(chosenFile));
                    }
                });
    }

    public void loadProject(FileHandle projectFile) {
        if (projectFile == null) { throw new IllegalArgumentException("Project file cannot be null"); }

        ProjectModel loadedProject = projectSerializer.loadProject(projectFile);
        if (loadedProject != null) {
            modelService.setProject(loadedProject);
            fileChooserHistory.putLastDir(FileChooserHistory.Type.PROJECT, projectFile.parent());
        }
    }

    @LmlAction("saveProject") public void saveProject() {
        resetViewFocus();

        ProjectModel project = getProject();
        FileHandle projectFile = project.getProjectFile();

        // Check if project were saved before
        if (projectFile != null && projectFile.exists()) {
            projectSerializer.saveProject(project, projectFile);
        } else {
            saveProjectAs();
        }
    }

    @LmlAction("saveProjectAs") public void saveProjectAs() {
        resetViewFocus();

        final ProjectModel project = getProject();
        FileHandle projectFile = project.getProjectFile();
        FileHandle selectedFile = fileChooserHistory.getLastDir(FileChooserHistory.Type.PROJECT);
        if (FileUtils.fileExists(projectFile)) {
            selectedFile = projectFile;
        }

        fileDialogService.saveFile("Save project as...", selectedFile,
                FileDialogService.FileFilter.createSingle(getString("projectFileDescription", AppConstants.PROJECT_FILE_EXT), AppConstants.PROJECT_FILE_EXT),
                new FileDialogService.CallbackAdapter() {
                    @Override
                    public void selected(Array<FileHandle> files) {
                        FileHandle chosenFile = files.first();
                        fileChooserHistory.putLastDir(FileChooserHistory.Type.PROJECT, chosenFile.parent());

                        if (chosenFile.extension().length() == 0) {
                            chosenFile = Gdx.files.getFileHandle(chosenFile.path() + "." + AppConstants.PROJECT_FILE_EXT, chosenFile.type());
                        }

                        getProject().setProjectFile(chosenFile);
                        projectSerializer.saveProject(project, chosenFile);
                    }
                });
    }

    @LmlAction("pickOutputDir") public void pickOutputDir() {
        final PackModel pack = getSelectedPack();
        if (pack == null) return;

        FileHandle dir = FileUtils.obtainIfExists(pack.getOutputDir());
        if (dir == null) {
            dir = fileChooserHistory.getLastDir(FileChooserHistory.Type.OUTPUT_DIR);
        }

        fileDialogService.pickDirectory(null, dir, new FileDialogService.CallbackAdapter() {
            @Override
            public void selected(Array<FileHandle> files) {
                FileHandle chosenFile = files.first();
                fileChooserHistory.putLastDir(FileChooserHistory.Type.OUTPUT_DIR, chosenFile);
                pack.setOutputDir(chosenFile.file().getAbsolutePath());
            }
        });
    }

    //TODO move model logic code to ModelUtils
    @LmlAction({"copySettingsToAllAtlases", "copySettingsToAllPacks"}) public void copySettingsToAllPacks() {
        PackModel selectedPack = getSelectedPack();
        if (selectedPack == null) return;

        TexturePacker.Settings generalSettings = selectedPack.getSettings();
        Array<PackModel> packs = getProject().getPacks();
        for (PackModel pack : packs) {
            if (pack == selectedPack) continue;

            pack.setSettings(generalSettings);
        }

        eventDispatcher.postEvent(new ShowToastEvent()
                .message(getString("toastCopyAllSettings"))
                .duration(ShowToastEvent.DURATION_SHORT));
    }

    @LmlAction("checkForUpdates") public void checkForUpdates() {
        interfaceService.showDialog(VersionCheckDialogController.class);
    }

    @LmlAction("getCurrentVersion") public String getCurrentVersion() {
        return AppConstants.version.toString();
    }

    @LmlAction("launchTextureUnpacker") public void launchTextureUnpacker() {
        interfaceService.showDialog(TextureUnpackerDialogController.class);
    }

    @LmlAction("launchNinePatchTool") public void launchNinePatchTool() {
        ninePatchToolController.initiateFromFilePicker();
    }

    @LmlAction("changePreviewBackground") public void changePreviewBackground() {
        interfaceService.showDialog(PreviewBackgroundDialogController.class);
    }

    @LmlAction("showExtensionModulesDialog") public void showExtensionModulesDialog() {
//        interfaceService.showDialog(ExtensionModulesDialogController.class);
        SettingsDialogController.show(SettingsDialogController.SECTION_ID_EXTENSIONS);
    }

    @LmlAction("showSettingsDialog") public void showSettingsDialog() {
        SettingsDialogController.show();
    }

    @LmlAction("showUiScalingDialog") public void showUiScalingDialog() {
        SettingsDialogController.show(SettingsDialogController.SECTION_ID_GENERAL);
//        interfaceService.showDialog(InterfaceScalingDialogController.class);
    }

    @LmlAction("restartApplication") public void restartApplication() {
        commonDialogs.checkUnsavedChanges(() -> {
            Gdx.app.log(TAG, "Restarting the application...");
            FileHandle projectFile = modelService.getProject().getProjectFile();
            if (projectFile != null && projectFile.exists()) {
                App.inst().getParams().startupProject = projectFile.file();
            }
            Gdx.app.postRunnable(() -> App.inst().restart());
        });
    }

    @LmlAction("getSystemNameText") String getSystemNameText() {
        return SystemUtils.getPrintString();
    }

    @LmlAction("editCustomHotkeys") public void editCustomHotkeys() {
        FileHandle userHotkeyFile = Gdx.files.external(AppConstants.EXTERNAL_DIR + "/hotkeys_user.txt");
        if (!userHotkeyFile.exists()) {
            Gdx.files.internal("hotkeys_user.txt").copyTo(userHotkeyFile);
        }
        try {
            Desktop.getDesktop().open(userHotkeyFile.file());
        } catch (IOException e) {
            Gdx.app.error(TAG, "Error opening " + userHotkeyFile, e);
        }
    }
    @LmlAction({"showMenuFile"}) public void showMenuFile() {
        mainController.showMenuFile();
    }
    @LmlAction({"showMenuAtlas", "showMenuPack"}) public void showMenuPack() {
        mainController.showMenuPack();
    }
    @LmlAction({"showMenuTools"}) public void showMenuTools() {
        mainController.showMenuTools();
    }
    @LmlAction({"showMenuHelp"}) public void showMenuHelp() {
        mainController.showMenuHelp();
    }

    @LmlAction("changeLanguageEn") public void changeLanguageEn() {
        changeLanguage(AppConstants.LOCALE_EN);
    }
    @LmlAction("changeLanguageDe") public void changeLanguageDe() {
        changeLanguage(AppConstants.LOCALE_DE);
    }
    @LmlAction("changeLanguageRu") public void changeLanguageRu() {
        changeLanguage(AppConstants.LOCALE_RU);
    }
    @LmlAction("changeLanguageZhCn") public void changeLanguageZhCn() {
        if (commonDialogs.checkExtensionModuleActivated(CjkFontExtensionModule.class)) {
            changeLanguage(AppConstants.LOCALE_ZH_CN);
        }
    }
    @LmlAction("changeLanguageZhTw") public void changeLanguageZhTw() {
        if (commonDialogs.checkExtensionModuleActivated(CjkFontExtensionModule.class)) {
            changeLanguage(AppConstants.LOCALE_ZH_TW);
        }
    }

    public void changeLanguage(Locale locale) {
        if (localeService.getCurrentLocale().equals(locale)) return;

        Locales.setLocale(locale);
        localeService.setCurrentLocale(locale);

        if (interfaceService != null && interfaceService.getCurrentController() != null) {
            interfaceService.reload();
        }
    }

    /** @return localized string */
    private String getString(String key) {
        return localeService.getI18nBundle().get(key);
    }
    /** @return localized string */
    private String getString(String key, Object... args) {
        return localeService.getI18nBundle().format(key, args);
    }

    private PackModel getSelectedPack() {
        return getProject().getSelectedPack();
    }

    private ProjectModel getProject() {
        return modelService.getProject();
    }

    private Stage getStage() {
        return interfaceService.getCurrentController().getStage();
    }

    /** Stores last used dir for specific actions */
    private static class FileChooserHistory {

        private final Preferences prefs;

        public FileChooserHistory(Preferences prefs) {
            this.prefs = prefs;
        }

        public FileHandle getLastDir(Type type) {
            String path = prefs.getString(type.prefKey, null);
            if (path == null || path.trim().length() == 0) return null;

            FileHandle fileHandle = Gdx.files.absolute(path);
            if (fileHandle.exists() && fileHandle.isDirectory()) {
                return fileHandle;
            } else {
                return null;
            }
        }

        public void putLastDir(Type type, FileHandle fileHandle) {
            String path = fileHandle.file().getAbsolutePath();
            prefs.putString(type.prefKey, path);
            prefs.flush();
        }


        public enum Type {
            PROJECT ("last_proj_dir"),
            INPUT_DIR ("last_input_dir"),
            OUTPUT_DIR ("last_output_dir");

            final String prefKey;

            Type(String prefKey) {
                this.prefKey = prefKey;
            }
        }

    }
}