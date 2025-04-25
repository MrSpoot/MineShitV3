package com.mineshit.engine.graphics.renderer.passes;

import com.mineshit.engine.graphics.renderer.utils.RenderContext;

public interface RenderPass {
    void init();
    void render(RenderContext ctx);
    void cleanup();
}
