package org.gephi.viz.engine.lwjgl.models;

import org.gephi.viz.engine.lwjgl.util.gl.GLShaderProgram;
import org.gephi.viz.engine.util.NumberUtils;
import org.gephi.viz.engine.util.gl.Constants;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL31;

import static org.gephi.viz.engine.util.gl.Constants.*;

/**
 *
 * @author Eduardo Ramos
 */
public class EdgeLineModelDirected {

    public static final int VERTEX_FLOATS = 3;
    public static final int POSITION_SOURCE_FLOATS = 2;
    public static final int POSITION_TARGET_FLOATS = 2;
    public static final int SOURCE_COLOR_FLOATS = 1;
    public static final int COLOR_FLOATS = 1;
    public static final int TARGET_SIZE_FLOATS = 1;
    public static final int SIZE_FLOATS = 1;

    public static final int TOTAL_ATTRIBUTES_FLOATS
            = POSITION_SOURCE_FLOATS
            + POSITION_TARGET_FLOATS
            + SOURCE_COLOR_FLOATS
            + COLOR_FLOATS
            + TARGET_SIZE_FLOATS
            + SIZE_FLOATS;

    private static final int VERTEX_PER_TRIANGLE = 3;

    public static final int TRIANGLE_COUNT = 3;
    public static final int VERTEX_COUNT = TRIANGLE_COUNT * VERTEX_PER_TRIANGLE;

    private GLShaderProgram program;
    private GLShaderProgram programWithSelectionSelected;
    private GLShaderProgram programWithSelectionUnselected;

    public int getVertexCount() {
        return VERTEX_COUNT;
    }

    public void initGLPrograms() {
        initProgram();
    }

    private static final String SHADERS_ROOT = Constants.SHADERS_ROOT + "edge";

    private static final String SHADERS_EDGE_LINE_SOURCE = "edge-line-directed";
    private static final String SHADERS_EDGE_LINE_SOURCE_WITH_SELECTION_SELECTED = "edge-line-directed_with_selection_selected";
    private static final String SHADERS_EDGE_LINE_SOURCE_WITH_SELECTION_UNSELECTED = "edge-line-directed_with_selection_unselected";

    private void initProgram() {
        program = new GLShaderProgram(SHADERS_ROOT, SHADERS_EDGE_LINE_SOURCE, SHADERS_EDGE_LINE_SOURCE)
                .addUniformName(UNIFORM_NAME_MODEL_VIEW_PROJECTION)
                .addUniformName(UNIFORM_NAME_EDGE_SCALE_MIN)
                .addUniformName(UNIFORM_NAME_EDGE_SCALE_MAX)
                .addUniformName(UNIFORM_NAME_MIN_WEIGHT)
                .addUniformName(UNIFORM_NAME_WEIGHT_DIFFERENCE_DIVISOR)
                .addAttribLocation(ATTRIB_NAME_VERT, SHADER_VERT_LOCATION)
                .addAttribLocation(ATTRIB_NAME_POSITION, SHADER_POSITION_LOCATION)
                .addAttribLocation(ATTRIB_NAME_POSITION_TARGET, SHADER_POSITION_TARGET_LOCATION)
                .addAttribLocation(ATTRIB_NAME_SIZE, SHADER_SIZE_LOCATION)
                .addAttribLocation(ATTRIB_NAME_SOURCE_COLOR, SHADER_SOURCE_COLOR_LOCATION)
                .addAttribLocation(ATTRIB_NAME_COLOR, SHADER_COLOR_LOCATION)
                .addAttribLocation(ATTRIB_NAME_TARGET_SIZE, SHADER_TARGET_SIZE_LOCATION)
                .init();

        programWithSelectionSelected = new GLShaderProgram(SHADERS_ROOT, SHADERS_EDGE_LINE_SOURCE_WITH_SELECTION_SELECTED, SHADERS_EDGE_LINE_SOURCE)
                .addUniformName(UNIFORM_NAME_MODEL_VIEW_PROJECTION)
                .addUniformName(UNIFORM_NAME_COLOR_BIAS)
                .addUniformName(UNIFORM_NAME_COLOR_MULTIPLIER)
                .addUniformName(UNIFORM_NAME_EDGE_SCALE_MIN)
                .addUniformName(UNIFORM_NAME_EDGE_SCALE_MAX)
                .addUniformName(UNIFORM_NAME_MIN_WEIGHT)
                .addUniformName(UNIFORM_NAME_WEIGHT_DIFFERENCE_DIVISOR)
                .addAttribLocation(ATTRIB_NAME_VERT, SHADER_VERT_LOCATION)
                .addAttribLocation(ATTRIB_NAME_POSITION, SHADER_POSITION_LOCATION)
                .addAttribLocation(ATTRIB_NAME_POSITION_TARGET, SHADER_POSITION_TARGET_LOCATION)
                .addAttribLocation(ATTRIB_NAME_SIZE, SHADER_SIZE_LOCATION)
                .addAttribLocation(ATTRIB_NAME_SOURCE_COLOR, SHADER_SOURCE_COLOR_LOCATION)
                .addAttribLocation(ATTRIB_NAME_COLOR, SHADER_COLOR_LOCATION)
                .addAttribLocation(ATTRIB_NAME_TARGET_SIZE, SHADER_TARGET_SIZE_LOCATION)
                .init();

        programWithSelectionUnselected = new GLShaderProgram(SHADERS_ROOT, SHADERS_EDGE_LINE_SOURCE_WITH_SELECTION_UNSELECTED, SHADERS_EDGE_LINE_SOURCE)
                .addUniformName(UNIFORM_NAME_MODEL_VIEW_PROJECTION)
                .addUniformName(UNIFORM_NAME_BACKGROUND_COLOR)
                .addUniformName(UNIFORM_NAME_COLOR_LIGHTEN_FACTOR)
                .addUniformName(UNIFORM_NAME_EDGE_SCALE_MIN)
                .addUniformName(UNIFORM_NAME_EDGE_SCALE_MAX)
                .addUniformName(UNIFORM_NAME_MIN_WEIGHT)
                .addUniformName(UNIFORM_NAME_WEIGHT_DIFFERENCE_DIVISOR)
                .addAttribLocation(ATTRIB_NAME_VERT, SHADER_VERT_LOCATION)
                .addAttribLocation(ATTRIB_NAME_POSITION, SHADER_POSITION_LOCATION)
                .addAttribLocation(ATTRIB_NAME_POSITION_TARGET, SHADER_POSITION_TARGET_LOCATION)
                .addAttribLocation(ATTRIB_NAME_SIZE, SHADER_SIZE_LOCATION)
                .addAttribLocation(ATTRIB_NAME_SOURCE_COLOR, SHADER_SOURCE_COLOR_LOCATION)
                .addAttribLocation(ATTRIB_NAME_COLOR, SHADER_COLOR_LOCATION)
                .addAttribLocation(ATTRIB_NAME_TARGET_SIZE, SHADER_TARGET_SIZE_LOCATION)
                .init();
    }

    public void drawArraysMultipleInstance(final int drawBatchCount) {
        if (drawBatchCount <= 0){
            return;
        }
        //Multiple lines, attributes must be in the buffer once per vertex count:
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, VERTEX_COUNT * drawBatchCount);
    }

    public void drawInstanced(int instanceCount) {
        GL31.glDrawArraysInstanced(GL11.GL_TRIANGLES, 0, VERTEX_COUNT, instanceCount);
    }

    public void stopUsingProgram() {
        GL20.glUseProgram(0);
    }

    public void useProgram(float[] mvpFloats, float scale, float minWeight, float maxWeight) {
        program.use();
        prepareProgramData(mvpFloats, scale, minWeight, maxWeight);
    }

    public void useProgramWithSelectionSelected(float[] mvpFloats, float scale, float minWeight, float maxWeight, float colorBias, float colorMultiplier) {
        programWithSelectionSelected.use();
        prepareProgramDataWithSelectionSelected(mvpFloats, scale, minWeight, maxWeight, colorBias, colorMultiplier);
    }

    public void useProgramWithSelectionUnselected(float[] mvpFloats, float scale, float minWeight, float maxWeight, float[] backgroundColorFloats, float colorLightenFactor) {
        programWithSelectionUnselected.use();
        prepareProgramDataWithSelectionUnselected(mvpFloats, scale, minWeight, maxWeight, backgroundColorFloats, colorLightenFactor);
    }

    private void prepareProgramData(float[] mvpFloats, float scale, float minWeight, float maxWeight) {
        GL20.glUniformMatrix4fv(program.getUniformLocation(UNIFORM_NAME_MODEL_VIEW_PROJECTION), false, mvpFloats);
        GL20.glUniform1f(program.getUniformLocation(UNIFORM_NAME_EDGE_SCALE_MIN), EDGE_SCALE_MIN * scale);
        GL20.glUniform1f(program.getUniformLocation(UNIFORM_NAME_EDGE_SCALE_MAX), EDGE_SCALE_MAX * scale);
        GL20.glUniform1f(program.getUniformLocation(UNIFORM_NAME_MIN_WEIGHT), minWeight);

        if (NumberUtils.equalsEpsilon(minWeight, maxWeight, 1e-3f)) {
            GL20.glUniform1f(program.getUniformLocation(UNIFORM_NAME_WEIGHT_DIFFERENCE_DIVISOR), 1);
        } else {
            GL20.glUniform1f(program.getUniformLocation(UNIFORM_NAME_WEIGHT_DIFFERENCE_DIVISOR), maxWeight - minWeight);
        }
    }

    private void prepareProgramDataWithSelectionSelected(float[] mvpFloats, float scale, float minWeight, float maxWeight, float colorBias, float colorMultiplier) {
        GL20.glUniformMatrix4fv(programWithSelectionSelected.getUniformLocation(UNIFORM_NAME_MODEL_VIEW_PROJECTION), false, mvpFloats);
        GL20.glUniform1f(programWithSelectionSelected.getUniformLocation(UNIFORM_NAME_COLOR_BIAS), colorBias);
        GL20.glUniform1f(programWithSelectionSelected.getUniformLocation(UNIFORM_NAME_COLOR_MULTIPLIER), colorMultiplier);
        GL20.glUniform1f(programWithSelectionSelected.getUniformLocation(UNIFORM_NAME_EDGE_SCALE_MIN), EDGE_SCALE_MIN * scale);
        GL20.glUniform1f(programWithSelectionSelected.getUniformLocation(UNIFORM_NAME_EDGE_SCALE_MAX), EDGE_SCALE_MAX * scale);
        GL20.glUniform1f(programWithSelectionSelected.getUniformLocation(UNIFORM_NAME_MIN_WEIGHT), minWeight);

        if (NumberUtils.equalsEpsilon(minWeight, maxWeight, 1e-3f)) {
            GL20.glUniform1f(programWithSelectionSelected.getUniformLocation(UNIFORM_NAME_WEIGHT_DIFFERENCE_DIVISOR), 1);
        } else {
            GL20.glUniform1f(programWithSelectionSelected.getUniformLocation(UNIFORM_NAME_WEIGHT_DIFFERENCE_DIVISOR), maxWeight - minWeight);
        }
    }

    private void prepareProgramDataWithSelectionUnselected(float[] mvpFloats, float scale, float minWeight, float maxWeight, float[] backgroundColorFloats, float colorLightenFactor) {
        GL20.glUniformMatrix4fv(programWithSelectionUnselected.getUniformLocation(UNIFORM_NAME_MODEL_VIEW_PROJECTION), false, mvpFloats);
        GL20.glUniform4fv(programWithSelectionUnselected.getUniformLocation(UNIFORM_NAME_BACKGROUND_COLOR), backgroundColorFloats);
        GL20.glUniform1f(programWithSelectionUnselected.getUniformLocation(UNIFORM_NAME_COLOR_LIGHTEN_FACTOR), colorLightenFactor);
        GL20.glUniform1f(programWithSelectionUnselected.getUniformLocation(UNIFORM_NAME_EDGE_SCALE_MIN), EDGE_SCALE_MIN * scale);
        GL20.glUniform1f(programWithSelectionUnselected.getUniformLocation(UNIFORM_NAME_EDGE_SCALE_MAX), EDGE_SCALE_MAX * scale);
        GL20.glUniform1f(programWithSelectionUnselected.getUniformLocation(UNIFORM_NAME_MIN_WEIGHT), minWeight);

        if (NumberUtils.equalsEpsilon(minWeight, maxWeight, 1e-3f)) {
            GL20.glUniform1f(programWithSelectionUnselected.getUniformLocation(UNIFORM_NAME_WEIGHT_DIFFERENCE_DIVISOR), 1);
        } else {
            GL20.glUniform1f(programWithSelectionUnselected.getUniformLocation(UNIFORM_NAME_WEIGHT_DIFFERENCE_DIVISOR), maxWeight - minWeight);
        }
    }

    public static float[] getVertexData() {
        //lineEnd, sideVector, arrowHeight
        return new float[]{
            //First 6 are the edge line as a rectangle:
            //Triangle 1
            0, 1, 0,// bottom right corner
            0, -1, 0,// bottom left corner
            1, -1, -1,// top left corner
            //Triangle 2
            1, -1, -1,// top left corner
            1, 1, -1,// top right corner
            0, 1, 0,// bottom right corner
            //Last 3 are the arrow tip triangle:
            1, 0, 0,//arrow tip
            1, -2, -1,// arrow bottom left vertex
            1, 2, -1// arrow bottom right vertex
        };
    }
}
