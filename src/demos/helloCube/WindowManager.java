package demos.helloCube;

import core.Application;
import core.Engine;
import core.resources.Asset;
import core.synchronization.DependencyFence;
import demos.helloCube.rendering.*;
import resources.memory.MemoryBin;
import util.window.CFrame;

/**
 * Creates a window and initializes the rendering layer for it.
 *
 * @author Cezary Chodun
 * @since 10.01.2020
 */
public class WindowManager implements Runnable {

    private Engine engine;
    private DependencyFence wait;
    private CFrame frame;

    public WindowManager(Engine engine, DependencyFence wait) {
        this.engine = engine;
        this.wait = wait;
    }

    @Override
    public void run() {
        final MemoryBin toDestroy = new MemoryBin();
        DependencyFence windowCreated = new DependencyFence(0);

        System.out.println("WindowManager work submited!");

        // Submits window creation task to the engine configuration queue.
        engine.addConfigSMQ(
                () -> {
                    // Obtaining the asset folder for the window.
                    Asset windowAsset = Application.getConfigAssets().getSubAsset("window");

                    // Creating a task that will create the window.
                    frame = new CFrame(engine, windowAsset, false, windowCreated, toDestroy);
                },
                wait);

        System.out.println("CFrame work submited!");

        // Submits rendering task to the engine configuration queue.
        engine.addConfigSMQ(
                () -> {
                    // Creating the rendering task.
                    InitializeRendering rendTask =
                            new InitializeRendering(engine, frame.getWindow());
                    toDestroy.add(rendTask);
                    engine.addTask(rendTask);
                },
                windowCreated);

        System.out.println("Window manager done!");
    }
}
