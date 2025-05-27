package com.mineshit.engine.graphics.renderer.passes;

import com.mineshit.engine.graphics.renderer.utils.GBuffer;
import com.mineshit.engine.graphics.renderer.utils.RenderContext;
import com.mineshit.engine.graphics.renderer.utils.Shader;
import com.mineshit.engine.window.Window;

import static org.lwjgl.opengl.GL11C.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11C.GL_TRIANGLE_STRIP;
import static org.lwjgl.opengl.GL11C.glClear;
import static org.lwjgl.opengl.GL11C.glDrawArrays;
import static org.lwjgl.opengl.GL11C.glViewport;
import static org.lwjgl.opengl.GL30C.*;
import static org.lwjgl.opengl.GL30C.glBindVertexArray;

public class DebugPass implements RenderPass {

    private Shader shader;
    private int vao;

    private static int renderMode = 0;

    @Override
    public void init(Window window) {
        this.shader = new Shader("/shaders/debug.glsl");
        vao = glGenVertexArrays();
    }

    public static void nextRenderMode(){
        if(renderMode < 4) renderMode++;
        else renderMode = 0;
    }

    @Override
    public void render(RenderContext ctx) {
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glViewport(0, 0, ctx.window().getWidth(), ctx.window().getHeight());
        glClear(GL_COLOR_BUFFER_BIT);

        shader.useProgram();

        if (renderMode == 0) {
            ctx.gbuffer().bindTexture(GBuffer.Attachment.ALBEDO, 0);
        } else if (renderMode == 1) {
            ctx.gbuffer().bindTexture(GBuffer.Attachment.NORMAL, 0);
        } else if (renderMode == 2) {
            ctx.gbuffer().bindTexture(GBuffer.Attachment.POSITION, 0);
        } else if( renderMode == 3){
            ctx.lightingMap().getTexture().bind(0);
        }else{
            ctx.gbuffer().bindDepthTexture(0);
        }

        ctx.gbuffer().bindDepthTexture(2);

        shader.setUniform("uAlbedo", 0);
        shader.setUniform("uDepth", 2);

        glBindVertexArray(vao);
        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
        glBindVertexArray(0);

        shader.unbind();
    }

    @Override
    public void cleanup() {
        this.shader.destroy();
        glDeleteVertexArrays(vao);
    }
}
