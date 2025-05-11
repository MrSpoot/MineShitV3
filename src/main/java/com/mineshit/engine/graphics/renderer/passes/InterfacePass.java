package com.mineshit.engine.graphics.renderer.passes;

import com.mineshit.engine.graphics.renderer.utils.Mesh;
import com.mineshit.engine.graphics.renderer.utils.RenderContext;
import com.mineshit.engine.graphics.renderer.utils.Shader;
import com.mineshit.engine.utils.Image;
import com.mineshit.engine.utils.Statistic;
import com.mineshit.engine.window.Window;
import com.mineshit.game.world.interaction.HitResult;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.nanovg.NVGColor;
import org.lwjgl.nanovg.NanoVG;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Map;

import static org.lwjgl.nanovg.NanoVG.*;
import static org.lwjgl.nanovg.NanoVG.NVG_ALIGN_LEFT;
import static org.lwjgl.nanovg.NanoVG.NVG_ALIGN_TOP;
import static org.lwjgl.nanovg.NanoVG.nvgBeginPath;
import static org.lwjgl.nanovg.NanoVG.nvgFillColor;
import static org.lwjgl.nanovg.NanoVG.nvgFontFace;
import static org.lwjgl.nanovg.NanoVG.nvgFontSize;
import static org.lwjgl.nanovg.NanoVG.nvgLineTo;
import static org.lwjgl.nanovg.NanoVG.nvgMoveTo;
import static org.lwjgl.nanovg.NanoVG.nvgStroke;
import static org.lwjgl.nanovg.NanoVG.nvgStrokeColor;
import static org.lwjgl.nanovg.NanoVG.nvgStrokeWidth;
import static org.lwjgl.nanovg.NanoVG.nvgText;
import static org.lwjgl.nanovg.NanoVG.nvgTextAlign;
import static org.lwjgl.nanovg.NanoVGGL3.*;
import static org.lwjgl.opengl.GL11C.*;
import static org.lwjgl.opengl.GL11C.GL_BACK;
import static org.lwjgl.opengl.GL11C.glCullFace;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13C.glActiveTexture;

public class InterfacePass implements RenderPass {
    private static final Logger LOGGER = LoggerFactory.getLogger(InterfacePass.class);

    private long vg;
    private static Mesh mesh;
    private Shader shader;
    private int textureId;

    @Override
    public void init(Window window){
        vg = nvgCreate(NVG_ANTIALIAS | NVG_STENCIL_STROKES);
        if (vg == 0) {
            throw new IllegalStateException("Failed to create NanoVG context");
        }

        int font = loadFont("sans", "/fonts/retro_gaming.ttf");
        if (font == -1) {
            throw new IllegalStateException("Could not load font");
        }

        FloatBuffer vertexBuffer = MemoryUtil.memAllocFloat(8 * 3);
        vertexBuffer.put(new float[]{
                0, 0, 0, // 0
                1, 0, 0, // 1
                1, 1, 0, // 2
                0, 1, 0, // 3
                0, 0, 1, // 4
                1, 0, 1, // 5
                1, 1, 1, // 6
                0, 1, 1  // 7
        });
        vertexBuffer.flip();

        // 6 faces × 2 triangles × 3 indices = 36 indices
        IntBuffer indexBuffer = MemoryUtil.memAllocInt(36);
        indexBuffer.put(new int[]{
                // back face (z = 0)
                0, 3, 2,
                2, 1, 0,

                // front face (z = 1)
                4, 5, 6,
                6, 7, 4,

                // left face (x = 0)
                0, 4, 7,
                7, 3, 0,

                // right face (x = 1)
                1, 2, 6,
                6, 5, 1,

                // bottom face (y = 0)
                0, 1, 5,
                5, 4, 0,

                // top face (y = 1)
                3, 7, 6,
                6, 2, 3
        });
        indexBuffer.flip();

        shader = new Shader("/shaders/selection.glsl");

        mesh = new Mesh(vertexBuffer, indexBuffer, 3);

        createTexture();

        MemoryUtil.memFree(vertexBuffer);
        MemoryUtil.memFree(indexBuffer);
    }

    @Override
    public void render(RenderContext ctx){
        renderSelection(ctx);

        nvgBeginFrame(vg, ctx.window().getWidth(), ctx.window().getHeight(), 1);

        drawCrosshair(ctx.window().getWidth(), ctx.window().getHeight());
        renderStats(ctx.window().getWidth(), ctx.window().getHeight());

        nvgEndFrame(vg);

        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);
    }

    private void renderSelection(RenderContext ctx) {
        if (mesh == null) return;

        HitResult r = ctx.world().getInteraction().getHitResult();
        if (r == null || r.blockPos() == null) return;

        Matrix4f model = new Matrix4f()
                .translate(r.blockPos().x + 0.5f, r.blockPos().y + 0.5f, r.blockPos().z + 0.5f)
                .scale(1.01f)
                .translate(-0.5f, -0.5f, -0.5f);

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        shader.useProgram();
        shader.setUniform("uProjection", ctx.camera().getProjectionMatrix());
        shader.setUniform("uView", ctx.camera().getViewMatrix());
        shader.setUniform("uModel", model);

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, textureId);
        shader.setUniform("uTexture", 0);

        mesh.render();

        Statistic.increment("Drawcalls");

        shader.unbind();

        glBindTexture(GL_TEXTURE_2D, 0);
        glDisable(GL_BLEND);
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

    @Override
    public void cleanup() {
        if (vg != 0) {
            nvgDelete(vg);
        }
        if (mesh != null) mesh.cleanup();
        shader.destroy();
        if (textureId != 0) {
            glDeleteTextures(textureId);
        }
    }

    private void createTexture(){
        Image img = new Image(new Vector4f(1.0f, 1.0f, 1.0f, 0.2f));

        ByteBuffer buffer = img.getByteBuffer();
        int width = img.getWidth();
        int height = img.getHeight();

        textureId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureId);

        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, buffer);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

        glBindTexture(GL_TEXTURE_2D, 0);

    }

    private int loadFont(String fontName, String resourcePath) {
        try (InputStream is = InterfacePass.class.getResourceAsStream(resourcePath)) {
            if (is == null)
                throw new RuntimeException("Font not found: " + resourcePath);

            File tempFile = File.createTempFile("font_", ".ttf");
            tempFile.deleteOnExit();

            try (OutputStream os = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
            }

            return NanoVG.nvgCreateFont(vg, fontName, tempFile.getAbsolutePath());

        } catch (IOException e) {
            throw new RuntimeException("Failed to load font: " + resourcePath, e);
        }
    }

}
