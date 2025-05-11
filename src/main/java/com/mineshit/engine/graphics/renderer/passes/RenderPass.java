package com.mineshit.engine.graphics.renderer.passes;

import com.mineshit.engine.graphics.renderer.utils.RenderContext;
import com.mineshit.engine.window.Window;

public interface RenderPass {
    void init(Window window);
    void render(RenderContext ctx);
    void cleanup();
}
