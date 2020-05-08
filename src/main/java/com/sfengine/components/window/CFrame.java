package com.sfengine.components.window;

import com.sfengine.core.Engine;
import com.sfengine.core.EngineFactory;
import com.sfengine.core.HardwareManager;
import com.sfengine.core.rendering.Window;
import com.sfengine.core.resources.Asset;
import com.sfengine.core.resources.Destroyable;
import com.sfengine.core.result.VulkanException;
import com.sfengine.core.synchronization.DependencyFence;
import org.lwjgl.vulkan.VK10;

import java.io.IOException;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Generalized(JFrame like) window.
 *
 * @author Cezary Chodun
 * @since 23.01.2020
 */
public class CFrame {

    private static final Logger logger = Logger.getLogger(CFrame.class.getName());

    private final Engine engine = EngineFactory.getEngine();

    public static final String WINDOW_CFG_FILE = "window.cfg";
    private volatile Window window;
    private volatile long handle = VK10.VK_NULL_HANDLE;

    private synchronized void setWindow(Window window) {
        this.window = window;
        this.handle = window.getWindowID();
    }

    private class CreateCFrameTask implements Runnable {
        private final Engine engine = EngineFactory.getEngine();

        private Asset asset;
        private Semaphore workDone;
        private boolean showWindow = true;
        private Destroyable destroyAtShutDown = null;

        public CreateCFrameTask(Asset asset) {
            this.asset = asset;
        }

        public CreateCFrameTask(
                Asset asset,
                boolean showWindow,
                Semaphore workDone,
                Destroyable destroyAtShutDown) {
            this.asset = asset;
            this.workDone = workDone;
            this.showWindow = showWindow;
            this.destroyAtShutDown = destroyAtShutDown;
        }

        private DependencyFence created;
        private DependencyFence loaded;
        private volatile Window window;

        @Override
        public void run() throws AssertionError {
            //            Window window = null;

            created = new DependencyFence(0);
            loaded = new DependencyFence(0);

            // Creating a window object.
            engine.addTask(
                    () -> {
                        try {
                            window = new Window(HardwareManager.getInstance());

                            logger.log(Level.INFO, "Window created.");

                            created.release();
                        } catch (VulkanException e) {
                            logger.log(Level.FINE, "Failed to create window.", e);
                            e.printStackTrace();
                        }
                    });

            engine.addFast(
                    () -> {
                        // Setting the window data to the values from the file.
                        try {
                            WindowFactory.loadFromFileC(
                                    engine, window, asset.getConfigFile(WINDOW_CFG_FILE), loaded);

                            logger.log(Level.INFO, "Window data loaded.");
                        } catch (IOException | AssertionError e) {
                            logger.log(Level.FINE, "Failed to create window.", e);
                            e.printStackTrace();
                        }
                    },
                    created);

            engine.addConfig(
                    () -> {
                        if (window == null) {
                            throw new AssertionError("Window is null!");
                        }
                        // Creating a window task that will monitor the window events.
                        WindowTickTask wt = new WindowTickTask(window.getWindowID());

                        // Setting window close callback.
                        if (destroyAtShutDown != null) {
                            wt.setCloseCall(
                                    new WindowShutDown(engine, window, wt, destroyAtShutDown));
                        } else {
                            wt.setCloseCall(new WindowShutDown(engine, window, wt));
                        }

                        if (window != null) {
                            setWindow(window);
                        }

                        // Adding window tick task to the engine
                        engine.addTickTask(wt);

                        // Releasing the work done semaphore
                        if (workDone != null) {
                            workDone.release();
                        }

                        // Showing the window.
                        if (showWindow == true) {
                            window.setVisible(true);
                        }
                    },
                    loaded);
        }
    }

    /**
     * Creates a window using the given thread engine.getConfigPool(). It is not required to invoke
     * the constructor from the first thread, however the window won't be created until the engine
     * starts running.
     *
     * @param asset The asset containing the window configuration file.
     */
    public CFrame(Asset asset) {
        engine.addConfig(new CreateCFrameTask(asset));
    }

    /**
     * Creates a window using the given thread engine.getConfigPool(). It is not required to invoke
     * the constructor from the first thread, however the window won't be created until the engine
     * starts running.
     *
     * @param asset The asset containing the window configuration file.
     * @param workDone A semaphore that will indicate that the window creation process have
     *     finished.
     */
    public CFrame(Asset asset, Semaphore workDone) {
        engine.addConfig(new CreateCFrameTask(asset, false, workDone, null));
    }

    /**
     * Creates a window using the given thread engine.getConfigPool(). It is not required to invoke
     * the constructor from the first thread, however the window won't be created until the engine
     * starts running.
     *
     * @param asset The asset containing the window configuration file.
     * @param showWindow Set to true if you want the window to be displayed as soon as created.
     * @param workDone A semaphore that will indicate that the window creation process have
     *     finished.
     */
    public CFrame(Asset asset, boolean showWindow, Semaphore workDone) {
        engine.addConfig(new CreateCFrameTask(asset, showWindow, workDone, null));
    }

    /**
     * Creates a window using the given thread engine.getConfigPool(). It is not required to invoke
     * the constructor from the first thread, however the window won't be created until the engine
     * starts running.
     *
     * @param asset The asset containing the window configuration file.
     * @param workDone A semaphore that will indicate that the window creation process have
     *     finished.
     * @param destroyAtShutDown A destroyable object that will be invoked after the window was shut
     *     down.
     */
    public CFrame(Asset asset, Semaphore workDone, Destroyable destroyAtShutDown) {
        engine.addConfig(new CreateCFrameTask(asset, false, workDone, destroyAtShutDown));
    }

    /**
     * Creates a window using the given thread engine.getConfigPool(). It is not required to invoke
     * the constructor from the first thread, however the window won't be created until the engine
     * starts running.
     *
     * @param asset The asset containing the window configuration file.
     * @param showWindow Set to true if you want the window to be displayed as soon as created.
     * @param workDone A semaphore that will indicate that the window creation process have
     *     finished.
     * @param destroyAtShutDown A destroyable object that will be invoked after the window was shut
     *     down.
     */
    public CFrame(
            Asset asset,
            boolean showWindow,
            Semaphore workDone,
            Destroyable destroyAtShutDown) {
        engine.addConfig(new CreateCFrameTask(asset, showWindow, workDone, destroyAtShutDown));
    }

    /**
     * Returns the window handle. Can be invoked on multiple threads.
     *
     * @return the window handle or VK_NULL_HANDLE if it wasn't created yet.
     */
    public long handle() {
        return handle;
    }

    /**
     * <b>Note:</b> access to the window instance must be externally synchronized.
     *
     * @return the window corresponding to this CFrame.
     */
    public Window getWindow() {
        return window;
    }
}
