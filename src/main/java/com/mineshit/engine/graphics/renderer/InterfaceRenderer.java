package com.mineshit.engine.graphics.renderer;

import com.mineshit.engine.utils.Statistic;
import com.mineshit.engine.window.Window;
import org.lwjgl.nanovg.NVGColor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static org.lwjgl.nanovg.NanoVG.*;
import static org.lwjgl.nanovg.NanoVGGL3.*;
import static org.lwjgl.opengl.GL11C.*;

public class InterfaceRenderer {
    private static final Logger LOGGER = LoggerFactory.getLogger(InterfaceRenderer.class);

    private long vg;

    public void init(){
        LOGGER.info("Initializing InterfaceRenderer");
        vg = nvgCreate(NVG_ANTIALIAS | NVG_STENCIL_STROKES);
        if (vg == 0) {
            throw new IllegalStateException("Failed to create NanoVG context");
        }

        int font = nvgCreateFont(vg, "sans", "C:\\Users\\Tom\\Documents\\GitHub\\MineShitV3\\target\\resources\\fonts\\retro_gaming.ttf");
        if (font == -1) {
            throw new IllegalStateException("Could not load font");
        }
    }

    public void render(Window window){
        nvgBeginFrame(vg, window.getWidth(), window.getHeight(), 1);

        drawCrosshair(window.getWidth(), window.getHeight());
        renderStats(window.getWidth(), window.getHeight());

        nvgEndFrame(vg);

        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);
    }

    public void renderStats(int width, int height) {
        Map<String, Object> stats = Statistic.getAll();

        float x = 10, y = 10;
        nvgFontSize(vg, 18.0f);
        nvgFontFace(vg, "sans");
        nvgFillColor(vg, color(1f, 1f, 1f, 1f));
        nvgTextAlign(vg, NVG_ALIGN_LEFT | NVG_ALIGN_TOP);

        for (Map.Entry<String, Object> entry : stats.entrySet()) {
            String line = entry.getKey() + ": " + entry.getValue();
            nvgText(vg, x, y, line);
            y += 20;
        }
    }


    private void drawCrosshair(int width, int height) {
        float size = 10.0f;

        nvgBeginPath(vg);
        nvgMoveTo(vg, width / 2f - size, height / 2f);
        nvgLineTo(vg, width / 2f + size, height / 2f);
        nvgMoveTo(vg, width / 2f, height / 2f - size);
        nvgLineTo(vg, width / 2f, height / 2f + size);
        nvgStrokeColor(vg, color(1f, 1f, 1f, 1f));
        nvgStrokeWidth(vg, 1.5f);
        nvgStroke(vg);
    }

    private NVGColor color(float r, float g, float b, float a) {
        NVGColor color = NVGColor.create();
        color.r(r).g(g).b(b).a(a);
        return color;
    }

    public void cleanup() {
        if (vg != 0) {
            nvgDelete(vg);
        }
    }
}
