package com.mineshit.engine.graphics.renderer.utils;

import com.mineshit.engine.utils.FileReader;
import lombok.Getter;
import org.joml.*;
import org.lwjgl.system.MemoryStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.lwjgl.opengl.GL11.GL_TRUE;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL32C.GL_GEOMETRY_SHADER;

public class Shader {

    private static final Logger LOGGER = LoggerFactory.getLogger(Shader.class);

    @Getter
    private final int program;
    private final int vertex;
    private final int fragment;
    private final int geometry;
    private final Map<String, Integer> uniforms = new HashMap<>();
    private String vertexCode;
    private String fragmentCode;
    private String geometryCode;

    public Shader(String path) {
        vertex = glCreateShader(GL_VERTEX_SHADER);
        fragment = glCreateShader(GL_FRAGMENT_SHADER);
        geometry = glCreateShader(GL_GEOMETRY_SHADER);
        program = glCreateProgram();

        loadShader(path);

        compileShader(vertex, vertexCode);
        compileShader(fragment, fragmentCode);
        compileShader(geometry, geometryCode);

        compileProgram(program);

        extractUniforms(vertexCode);
        extractUniforms(fragmentCode);
        extractUniforms(geometryCode);
    }

    private void compileProgram(int programId) {
        glAttachShader(programId, vertex);
        glAttachShader(programId, fragment);

        if(geometryCode != null && !geometryCode.isEmpty()) {
            glAttachShader(programId, geometry);
        }

        glLinkProgram(program);
        glDeleteShader(vertex);
        glDeleteShader(fragment);
        glDeleteShader(geometry);
        checkShaderProgramLinking(program);
    }

    private void compileShader(int shaderId, String source) {
        glShaderSource(shaderId, source);
        glCompileShader(shaderId);

        checkShaderCompilation(shaderId);
    }

    private void checkShaderCompilation(int shader) {
        int compiled = glGetShaderi(shader, GL_COMPILE_STATUS);
        if (compiled != GL_TRUE) {
            LOGGER.warn("Erreur de compilation du shader : " + glGetShaderInfoLog(shader));
        }
    }

    private void checkShaderProgramLinking(int shaderProgram) {
        int linked = glGetProgrami(shaderProgram, GL_LINK_STATUS);
        if (linked != GL_TRUE) {
            LOGGER.warn("Erreur de liaison du programme de shaders : " + glGetProgramInfoLog(shaderProgram));
        }
    }

    // UNIFORM

    public void createUniform(String uniformName) {
        int uniformLocation = glGetUniformLocation(program, uniformName);
        if (uniformLocation >= 0) {
            uniforms.put(uniformName, uniformLocation);
        }else{
            LOGGER.warn("Uniform '{}' not found in shader '{}'", uniformName, program);
        }

    }

    public Integer getUniformLocation(String uniformName) {
        return uniforms.get(uniformName);
    }

    public void setUniform(String uniformName, int value) {
        if (getUniformLocation(uniformName) != null) {
            glUniform1i(getUniformLocation(uniformName), value);
        }
    }

    public void setUniform(String uniformName, Matrix4f value) {
        if (getUniformLocation(uniformName) != null) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                glUniformMatrix4fv(getUniformLocation(uniformName), false, value.get(stack.mallocFloat(16)));
            }
        }
    }

    public void setUniform(String uniformName, Matrix3f value) {
        if (getUniformLocation(uniformName) != null) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                glUniformMatrix3fv(getUniformLocation(uniformName), false, value.get(stack.mallocFloat(16)));
            }
        }
    }

    public void setUniform(String uniformName, Vector4f value) {
        if (getUniformLocation(uniformName) != null) {
            glUniform4f(getUniformLocation(uniformName), value.x, value.y, value.z, value.w);
        }
    }

    public void setUniform(String uniformName, Vector3f value) {
        if (getUniformLocation(uniformName) != null) {
            glUniform3f(getUniformLocation(uniformName), value.x, value.y, value.z);
        }
    }

    public void setUniform(String uniformName, Vector2f value) {
        if (getUniformLocation(uniformName) != null) {
            glUniform2f(getUniformLocation(uniformName), value.x, value.y);
        }
    }

    public void setUniform(String uniformName, float value) {
        if (getUniformLocation(uniformName) != null) {
            glUniform1f(getUniformLocation(uniformName), value);
        }
    }

    public void useProgram() {
        glUseProgram(program);
    }

    public void unbind(){
        glUseProgram(0);
    }

    private void extractUniforms(String shaderCode) {
        // Pattern pour les structures
        Pattern structPattern = Pattern.compile("struct\\s+(\\w+)\\s*\\{([^}]+)\\};");
        Matcher structMatcher = structPattern.matcher(shaderCode);

        while (structMatcher.find()) {
            String structName = structMatcher.group(1);
            String structBody = structMatcher.group(2);

            // Extraire les champs de la structure
            Pattern fieldPattern = Pattern.compile("\\w+\\s+(\\w+)\\s*;");
            Matcher fieldMatcher = fieldPattern.matcher(structBody);

            while (fieldMatcher.find()) {
                String fieldName = fieldMatcher.group(1);

                // Trouver les instances de la structure déclarées comme uniforms
                Pattern instancePattern = Pattern.compile("\\buniform\\s+" + structName + "\\s+(\\w+)\\s*;");
                Matcher instanceMatcher = instancePattern.matcher(shaderCode);

                while (instanceMatcher.find()) {
                    String instanceName = instanceMatcher.group(1);
                    String uniformName = instanceName + "." + fieldName;
                    LOGGER.debug("Create uniform [" + uniformName + "] for shader [" + program + "]");
                    createUniform(uniformName);
                }
            }
        }

        Pattern uniformPattern = Pattern.compile("\\buniform\\s+\\w+\\s+(\\w+)\\s*;");
        Matcher uniformMatcher = uniformPattern.matcher(shaderCode);

        while (uniformMatcher.find()) {
            String uniformName = uniformMatcher.group(1);
            LOGGER.debug("Create uniform [" + uniformName + "] for shader [" + program + "]");
            createUniform(uniformName);
        }
    }

    private void loadShader(String path){
        LOGGER.info("Load shader - ["+path+"]");
        String file = FileReader.readFile(path);

        Pattern vertexPattern = Pattern.compile("//@vs(.*?)//@endvs", Pattern.DOTALL);
        Matcher vertexMatcher = vertexPattern.matcher(file);

        StringBuilder vertex = new StringBuilder();

        while (vertexMatcher.find()){
            vertex.append(vertexMatcher.group(1)).append(" ");
        }

        Pattern fragmentPattern = Pattern.compile("//@fs(.*?)//@endfs", Pattern.DOTALL);
        Matcher fragmentMatcher = fragmentPattern.matcher(file);

        StringBuilder fragment = new StringBuilder();

        while (fragmentMatcher.find()){
            fragment.append(fragmentMatcher.group(1)).append(" ");
        }

        Pattern geometryPattern = Pattern.compile("//@gs(.*?)//@endgs", Pattern.DOTALL);
        Matcher geometryMatcher = geometryPattern.matcher(file);

        StringBuilder geometry = new StringBuilder();

        while (geometryMatcher.find()){
            geometry.append(geometryMatcher.group(1)).append(" ");
        }

        if(vertex.isEmpty() || fragment.isEmpty()){
            LOGGER.error("Shader cannot be compile");
        }else{
            this.vertexCode = vertex.toString().trim();
            this.fragmentCode = fragment.toString().trim();
            this.geometryCode = geometry.toString().trim();
        }
    }

    public void destroy() {
        glDeleteProgram(program);
    }


}
