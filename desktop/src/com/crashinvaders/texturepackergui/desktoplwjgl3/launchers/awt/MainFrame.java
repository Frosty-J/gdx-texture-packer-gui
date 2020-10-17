package com.crashinvaders.texturepackergui.desktoplwjgl3.launchers.awt;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import com.crashinvaders.texturepackergui.App;
import com.crashinvaders.texturepackergui.DragDropManager;
import com.crashinvaders.texturepackergui.controllers.GlobalActions;
import com.crashinvaders.texturepackergui.desktoplwjgl3.errorreport.ErrorReportFrame;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.ArrayList;

class MainFrame extends JFrame implements CustomLwjglCanvas.UnhandledExceptionListener {
    private static final Color colorFill = new Color(37, 37, 38);

    private final App app;
    private final CustomLwjglCanvas lwjglCanvas;
    private final LwjglCanvasConfiguration lwjglConfig;
    private final JLayeredPane pane;

    public MainFrame(App app, final CustomLwjglCanvas lwjglCanvas, LwjglCanvasConfiguration lwjglConfig) {
        super(lwjglConfig.title);
        this.app = app;
        this.lwjglCanvas = lwjglCanvas;
        this.lwjglConfig = lwjglConfig;
        lwjglCanvas.setUnhandledExceptionListener(this);

        try {
            setIconImage(ImageIO.read((MainFrame.class.getClassLoader().getResourceAsStream(lwjglConfig.iconFilePath))));
        } catch (Exception e) {
            e.printStackTrace();
        }

        addWindowListener(new WindowAdapter() {
            boolean iconified = false;
            boolean closeHandling = false;

            @Override
            public void windowIconified(WindowEvent e) {
                if (iconified) return;
                iconified = true;
                lwjglCanvas.getApplicationListener().pause();
            }

            @Override
            public void windowDeiconified(WindowEvent e) {
                if (!iconified) return;
                iconified = false;
                lwjglCanvas.getApplicationListener().resume();
            }

            @Override
            public void windowClosing(WindowEvent e) {
                if (closeHandling) return;
                closeHandling = true;

                ((GlobalActions) App.inst().getContext().getComponent(GlobalActions.class)).commonDialogs.checkUnsavedChanges(
                        new Runnable() {
                            @Override
                            public void run() {
                                closeHandling = false;
                                FramePropertiesPersister.saveFrameProperties(MainFrame.this);
                                lwjglCanvas.stop();

                                EventQueue.invokeLater(new Runnable() {
                                    public void run() {
                                        System.exit(0);
                                    }
                                });
                            }
                        }, new Runnable() {
                            @Override
                            public void run() {
                                closeHandling = false;
                            }
                        });
                Gdx.graphics.requestRendering();
            }
        });

        // Frame layout
        {
            setBackground(colorFill);
            setSize(1024, 768);
            setLocation(0, 0);
//            setExtendedState(JFrame.MAXIMIZED_BOTH);
            setLocationRelativeTo(null);
            setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

            FramePropertiesPersister.loadFrameProperties(this);

            pane = new JLayeredPane();
            pane.setBackground(colorFill);
            pane.setLayout(new BorderLayout());
            pane.add(lwjglCanvas.getCanvas(), BorderLayout.CENTER, 0);

            getContentPane().add(pane, BorderLayout.CENTER);

            pane.setTransferHandler(new TransferHandler(null));
            pane.setDropTarget(new FileDropTarget(app.getDragDropManager()));
        }
    }

    @Override
    public void onGdxException(final Throwable ex) {
        if (!app.getParams().debug) {
            ex.printStackTrace();

            ErrorReportFrame errorFrame = new ErrorReportFrame(lwjglConfig, ex);
            // Center error frame
            errorFrame.setLocation(
                    Math.max(0, (getWidth() - errorFrame.getWidth()) / 2),
                    Math.max(0, (getHeight() - errorFrame.getHeight()) / 2));
            errorFrame.setVisible(true);
        }

        this.dispose();
    }

    private static class FileDropTarget extends DropTarget {
        private static final int[] TMP_VEC2 = new int[2];

        private final DragDropManager dragDropManager;
        private final FileDropTarget.DragOverRunnable dragOverRunnable;

        private boolean dragHandling;

        public FileDropTarget(DragDropManager dragDropManager) throws HeadlessException {
            this.dragDropManager = dragDropManager;
            this.dragOverRunnable = new FileDropTarget.DragOverRunnable(dragDropManager);
        }

        @Override
        public synchronized void dragEnter(DropTargetDragEvent evt) {
            super.dragEnter(evt);

            if (!evt.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                return;
            }

            dragHandling = true;

            final int x = toGdxScreenX(evt.getLocation().x);
            final int y = toGdxScreenY(evt.getLocation().y);
            Gdx.app.postRunnable(() -> dragDropManager.onDragStarted(x, y));
        }

        @Override
        public synchronized void dragOver(DropTargetDragEvent evt) {
            super.dragOver(evt);

            if (!dragHandling) return;

            dragOverRunnable.x = toGdxScreenX(evt.getLocation().x);
            dragOverRunnable.y = toGdxScreenY(evt.getLocation().y);

            if (!dragOverRunnable.scheduled) {
                dragOverRunnable.scheduled = true;
                Gdx.app.postRunnable(dragOverRunnable);
            }
        }

        @Override
        public synchronized void dragExit(DropTargetEvent evt) {
            super.dragExit(evt);

            if (!dragHandling) return;

            finishDragHandling();
        }

        public synchronized void drop(DropTargetDropEvent evt) {
            if (!dragHandling) return;

            finishDragHandling();

            try {
                evt.acceptDrop(DnDConstants.ACTION_COPY);
                final java.util.List<File> droppedFiles = (java.util.List<File>)
                        evt.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                final int x = toGdxScreenX(evt.getLocation().x);
                final int y = toGdxScreenY(evt.getLocation().y);
                Gdx.app.postRunnable(() -> {
                    FileHandle[] fileArray = (FileHandle[]) droppedFiles.stream().map(FileHandle::new).toArray();
                    dragDropManager.handleFileDrop(x, y, new Array<>(fileArray));
                });
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        private void finishDragHandling() {
            if (!dragHandling) return;

            dragHandling = false;
            Gdx.app.postRunnable(() -> dragDropManager.onDragFinished());
        }

        private int toGdxScreenX(int componentX) {
            return (componentX * Gdx.graphics.getWidth()) / getComponent().getWidth();
        }

        private int toGdxScreenY(int componentY) {
            return (componentY * Gdx.graphics.getHeight()) / getComponent().getHeight();
        }

        private static class DragOverRunnable implements Runnable {
            final DragDropManager dragDropManager;
            volatile int x;
            volatile int y;
            volatile boolean scheduled;

            public DragOverRunnable(DragDropManager dragDropManager) {
                this.dragDropManager = dragDropManager;
            }

            @Override
            public void run() {
                dragDropManager.onDragMoved(x, y);
                scheduled = false;
            }
        }
    }
}