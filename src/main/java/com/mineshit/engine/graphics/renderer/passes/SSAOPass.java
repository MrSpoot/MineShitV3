package com.mineshit.engine.graphics.renderer.passes;

import com.mineshit.engine.graphics.renderer.utils.FrameBuffer;
import com.mineshit.engine.graphics.renderer.utils.RenderContext;
import com.mineshit.engine.graphics.renderer.utils.Shader;
import com.mineshit.engine.window.Window;
import lombok.Getter;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.nio.ByteBuffer;
import java.util.Random;

import static org.lwjgl.opengl.GL11C.*;
import static org.lwjgl.opengl.GL30C.*;

public class SSAOPass implements RenderPass {

    private static final int KERNEL_SIZE = 64;
    private static final int NOISE_SIZE = 4;

    private Shader shader;
    @Getter
    private FrameBuffer frameBuffer;
    private int vao;
    private int noiseTexture;

    private final Vector3f[] ssaoKernel = new Vector3f[KERNEL_SIZE];

    private final Random rand = new Random();

    @Override
    public void init(Window window) {
        this.shader = new Shader("/shaders/ssao_pass.glsl");
        this.frameBuffer = new FrameBuffer(window.getWidth(), window.getHeight());
        this.vao = glGenVertexArrays();

        generateKernel();
        generateNoiseTexture();
    }

    private void generateKernel() {
        for (int i = 0; i < KERNEL_SIZE; i++) {
            Vector3f sample = new Vector3f(
                    rand.nextFloat() * 2f - 1f,
                    rand.nextFloat() * 2f - 1f,
                    rand.nextFloat()
            );
            sample.normalize();
            sample.mul(rand.nextFloat());
            float scale = i / (float) KERNEL_SIZE;
            scale = 0.1f + 0.9f * (scale * scale);
            sample.mul(scale);
            ssaoKernel[i] = sample;
        }
    }

    private void generateNoiseTexture() {
        ByteBuffer buffer = BufferUtils.createByteBuffer(NOISE_SIZE * NOISE_SIZE * 3 * Float.BYTES);

        for (int i = 0; i < NOISE_SIZE * NOISE_SIZE; i++) {
            Vector3f noise = new Vector3f(
                    rand.nextFloat() * 2f - 1f,
                    rand.nextFloat() * 2f - 1f,
                    0.0f
            );
            buffer.putFloat(noise.x);
            buffer.putFloat(noise.y);
            buffer.putFloat(noise.z);
        }

        buffer.flip();

        noiseTexture = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, noiseTexture);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB16F, NOISE_SIZE, NOISE_SIZE, 0, GL_RGB, GL_FLOAT, buffer);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
    }

    @Override
    public void render(RenderContext ctx) {
        frameBuffer.bind();
        shader.useProgram();

        ChunkOpaquePass opaquePass = ctx.getPass(ChunkOpaquePass.class);

        FrameBuffer.bindTexture(opaquePass.getFrameBuffer().getAlbedoTexture(), 0);
        FrameBuffer.bindTexture(opaquePass.getFrameBuffer().getNormalTexture(), 1);
        FrameBuffer.bindTexture(opaquePass.getFrameBuffer().getPositionTexture(), 2);
        FrameBuffer.bindTexture(opaquePass.getFrameBuffer().getDepthTexture(), 3);
        FrameBuffer.bindTexture(noiseTexture, 4);

        shader.setUniform("uAlbedo", 0);
        shader.setUniform("uNormal", 1);
        shader.setUniform("uPosition", 2);
        shader.setUniform("uDepth", 3);
        shader.setUniform("uNoise", 4);
        shader.setUniform("uTexSize", new Vector2f(ctx.window().getWidth(), ctx.window().getHeight()));

        // Projection, vue, et inverse de projection
        shader.setUniform("uProjection", ctx.camera().getProjectionMatrix());
        shader.setUniform("uView", ctx.camera().getViewMatrix());
        shader.setUniform("uCameraPos", ctx.camera().getPosition());

        Matrix4f invProj = new Matrix4f(ctx.camera().getProjectionMatrix()).invert();
        shader.setUniform("uInverseProjection", invProj);

        shader.setUniform("uNoiseScale", new Vector2f(
                ctx.window().getWidth() / (float) NOISE_SIZE,
                ctx.window().getHeight() / (float) NOISE_SIZE
        ));

        for (int i = 0; i < KERNEL_SIZE; i++) {
            shader.setUniform("uKernel[" + i + "]", ssaoKernel[i]);
        }

        glBindVertexArray(vao);
        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
        glBindVertexArray(0);

        shader.unbind();
        frameBuffer.unbind(ctx.window().getWidth(), ctx.window().getHeight());
    }

    @Override
    public void cleanup() {
        shader.destroy();
        frameBuffer.cleanup();
        glDeleteVertexArrays(vao);
        glDeleteTextures(noiseTexture);
    }
}
