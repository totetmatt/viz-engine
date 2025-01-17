package org.gephi.viz.engine.lwjgl.pipeline.arrays;

import org.gephi.graph.api.Edge;
import org.gephi.graph.api.Graph;
import org.gephi.viz.engine.VizEngine;
import org.gephi.viz.engine.lwjgl.models.EdgeLineModelDirected;
import org.gephi.viz.engine.lwjgl.models.EdgeLineModelUndirected;
import org.gephi.viz.engine.lwjgl.pipeline.common.AbstractEdgeData;
import org.gephi.viz.engine.lwjgl.util.gl.GLBufferMutable;
import org.gephi.viz.engine.lwjgl.util.gl.ManagedDirectBuffer;
import org.gephi.viz.engine.pipeline.RenderingLayer;
import org.gephi.viz.engine.status.GraphRenderingOptions;
import org.gephi.viz.engine.status.GraphSelection;
import org.gephi.viz.engine.structure.GraphIndexImpl;
import org.gephi.viz.engine.util.ArrayUtils;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;

import static org.gephi.viz.engine.pipeline.RenderingLayer.BACK1;
import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL15.glGenBuffers;

/**
 *
 * @author Eduardo Ramos
 */
public class ArrayDrawEdgeData extends AbstractEdgeData {

    private final int[] bufferName = new int[4];

    private static final int VERT_BUFFER_UNDIRECTED = 0;
    private static final int VERT_BUFFER_DIRECTED = 1;
    private static final int ATTRIBS_BUFFER_DIRECTED = 2;
    private static final int ATTRIBS_BUFFER_UNDIRECTED = 3;

    public ArrayDrawEdgeData() {
        super(false, false);
    }

    public void update(VizEngine engine, GraphIndexImpl graphIndex) {
        updateData(
                graphIndex,
                engine.getLookup().lookup(GraphRenderingOptions.class),
                engine.getLookup().lookup(GraphSelection.class)
        );
    }

    public void drawArrays(RenderingLayer layer, VizEngine engine, float[] mvpFloats) {
        drawUndirected(engine, layer, mvpFloats);
        drawDirected(engine, layer, mvpFloats);
    }

    private void drawUndirected(VizEngine engine, RenderingLayer layer, float[] mvpFloats) {
        final int instanceCount = setupShaderProgramForRenderingLayerUndirected(layer, engine, mvpFloats);

        final boolean renderingUnselectedEdges = layer == BACK1;
        final int instancesOffset = renderingUnselectedEdges ? 0 : undirectedInstanceCounter.unselectedCountToDraw;

        final FloatBuffer batchUpdateBuffer = attributesDrawBufferBatchOneCopyPerVertexManagedDirectBuffer.floatBuffer();

        final int maxIndex = (instancesOffset + instanceCount);
        for (int edgeBase = instancesOffset; edgeBase < maxIndex; edgeBase += BATCH_EDGES_SIZE) {
            final int drawBatchCount = Math.min(maxIndex - edgeBase, BATCH_EDGES_SIZE);

            //Need to copy attributes as many times as vertex per model:
            for (int edgeIndex = 0; edgeIndex < drawBatchCount; edgeIndex++) {
                System.arraycopy(
                        attributesBuffer, (edgeBase + edgeIndex) * ATTRIBS_STRIDE,
                        attributesDrawBufferBatchOneCopyPerVertex, edgeIndex * ATTRIBS_STRIDE * VERTEX_COUNT_UNDIRECTED,
                        ATTRIBS_STRIDE
                );

                ArrayUtils.repeat(
                        attributesDrawBufferBatchOneCopyPerVertex,
                        edgeIndex * ATTRIBS_STRIDE * VERTEX_COUNT_UNDIRECTED,
                        ATTRIBS_STRIDE,
                        VERTEX_COUNT_UNDIRECTED
                );
            }

            batchUpdateBuffer.clear();
            batchUpdateBuffer.put(attributesDrawBufferBatchOneCopyPerVertex, 0, drawBatchCount * ATTRIBS_STRIDE * VERTEX_COUNT_UNDIRECTED);
            batchUpdateBuffer.flip();

            attributesGLBufferUndirected.bind();
            attributesGLBufferUndirected.updateWithOrphaning(batchUpdateBuffer);
            attributesGLBufferUndirected.unbind();
            lineModelUndirected.drawArraysMultipleInstance(drawBatchCount);
        }

        lineModelUndirected.stopUsingProgram();
        unsetupUndirectedVertexArrayAttributes();
    }

    private void drawDirected(VizEngine engine, RenderingLayer layer, float[] mvpFloats) {
        final int instanceCount = setupShaderProgramForRenderingLayerDirected(layer, engine, mvpFloats);

        final boolean renderingUnselectedEdges = layer == BACK1;
        final int instancesOffset;
        if (renderingUnselectedEdges) {
            instancesOffset = undirectedInstanceCounter.totalToDraw();
        } else {
            instancesOffset = undirectedInstanceCounter.totalToDraw() + directedInstanceCounter.unselectedCountToDraw;
        }

        final FloatBuffer batchUpdateBuffer = attributesDrawBufferBatchOneCopyPerVertexManagedDirectBuffer.floatBuffer();

        final int maxIndex = (instancesOffset + instanceCount);
        for (int edgeBase = instancesOffset; edgeBase < maxIndex; edgeBase += BATCH_EDGES_SIZE) {
            final int drawBatchCount = Math.min(maxIndex - edgeBase, BATCH_EDGES_SIZE);

            //Need to copy attributes as many times as vertex per model:
            for (int edgeIndex = 0; edgeIndex < drawBatchCount; edgeIndex++) {
                System.arraycopy(
                        attributesBuffer, (edgeBase + edgeIndex) * ATTRIBS_STRIDE,
                        attributesDrawBufferBatchOneCopyPerVertex, edgeIndex * ATTRIBS_STRIDE * VERTEX_COUNT_DIRECTED,
                        ATTRIBS_STRIDE
                );

                ArrayUtils.repeat(
                        attributesDrawBufferBatchOneCopyPerVertex,
                        edgeIndex * ATTRIBS_STRIDE * VERTEX_COUNT_DIRECTED,
                        ATTRIBS_STRIDE,
                        VERTEX_COUNT_DIRECTED
                );
            }

            batchUpdateBuffer.clear();
            batchUpdateBuffer.put(attributesDrawBufferBatchOneCopyPerVertex, 0, drawBatchCount * ATTRIBS_STRIDE * VERTEX_COUNT_DIRECTED);
            batchUpdateBuffer.flip();

            attributesGLBufferDirected.bind();
            attributesGLBufferDirected.updateWithOrphaning(batchUpdateBuffer);
            attributesGLBufferDirected.unbind();

            lineModelDirected.drawArraysMultipleInstance(drawBatchCount);
        }

        lineModelDirected.stopUsingProgram();
        unsetupDirectedVertexArrayAttributes();
    }

    private float[] attributesBuffer;

    private static final int BATCH_EDGES_SIZE = 65536;

    //For drawing in a loop:
    private float[] attributesDrawBufferBatchOneCopyPerVertex;
    private ManagedDirectBuffer attributesDrawBufferBatchOneCopyPerVertexManagedDirectBuffer;

    protected void initBuffers() {
        super.initBuffers();
        attributesDrawBufferBatchOneCopyPerVertex = new float[ATTRIBS_STRIDE * VERTEX_COUNT_MAX * BATCH_EDGES_SIZE];//Need to copy attributes as many times as vertex per model
        attributesDrawBufferBatchOneCopyPerVertexManagedDirectBuffer = new ManagedDirectBuffer(GL_FLOAT, ATTRIBS_STRIDE * VERTEX_COUNT_MAX * BATCH_EDGES_SIZE);

        glGenBuffers(bufferName);

        {
            float[] singleElementData = EdgeLineModelUndirected.getVertexData();
            float[] undirectedVertexDataArray = new float[singleElementData.length * BATCH_EDGES_SIZE];
            System.arraycopy(singleElementData, 0, undirectedVertexDataArray, 0, singleElementData.length);
            ArrayUtils.repeat(undirectedVertexDataArray, 0, singleElementData.length, BATCH_EDGES_SIZE);

            final FloatBuffer undirectedVertexData = MemoryUtil.memAllocFloat(undirectedVertexDataArray.length);
            undirectedVertexData.put(undirectedVertexDataArray);
            undirectedVertexData.flip();

            vertexGLBufferUndirected = new GLBufferMutable(bufferName[VERT_BUFFER_UNDIRECTED], GLBufferMutable.GL_BUFFER_TYPE_ARRAY);
            vertexGLBufferUndirected.bind();
            vertexGLBufferUndirected.init(undirectedVertexData, GLBufferMutable.GL_BUFFER_USAGE_STATIC_DRAW);
            vertexGLBufferUndirected.unbind();

            MemoryUtil.memFree(undirectedVertexData);
        }

        {
            float[] singleElementData = EdgeLineModelDirected.getVertexData();
            float[] directedVertexDataArray = new float[singleElementData.length * BATCH_EDGES_SIZE];
            System.arraycopy(singleElementData, 0, directedVertexDataArray, 0, singleElementData.length);
            ArrayUtils.repeat(directedVertexDataArray, 0, singleElementData.length, BATCH_EDGES_SIZE);

            final FloatBuffer directedVertexData = MemoryUtil.memAllocFloat(directedVertexDataArray.length);
            directedVertexData.put(directedVertexDataArray);
            directedVertexData.flip();

            vertexGLBufferDirected = new GLBufferMutable(bufferName[VERT_BUFFER_DIRECTED], GLBufferMutable.GL_BUFFER_TYPE_ARRAY);
            vertexGLBufferDirected.bind();
            vertexGLBufferDirected.init(directedVertexData, GLBufferMutable.GL_BUFFER_USAGE_STATIC_DRAW);
            vertexGLBufferDirected.unbind();

            MemoryUtil.memFree(directedVertexData);
        }

        //Initialize for batch edges size:
        attributesGLBufferDirected = new GLBufferMutable(bufferName[ATTRIBS_BUFFER_DIRECTED], GLBufferMutable.GL_BUFFER_TYPE_ARRAY);
        attributesGLBufferDirected.bind();
        attributesGLBufferDirected.init(VERTEX_COUNT_MAX * ATTRIBS_STRIDE * Float.BYTES * BATCH_EDGES_SIZE, GLBufferMutable.GL_BUFFER_USAGE_DYNAMIC_DRAW);
        attributesGLBufferDirected.unbind();

        attributesGLBufferUndirected = new GLBufferMutable(bufferName[ATTRIBS_BUFFER_UNDIRECTED], GLBufferMutable.GL_BUFFER_TYPE_ARRAY);
        attributesGLBufferUndirected.bind();
        attributesGLBufferUndirected.init(VERTEX_COUNT_MAX * ATTRIBS_STRIDE * Float.BYTES * BATCH_EDGES_SIZE, GLBufferMutable.GL_BUFFER_USAGE_DYNAMIC_DRAW);
        attributesGLBufferUndirected.unbind();

        attributesBuffer = new float[ATTRIBS_STRIDE * BATCH_EDGES_SIZE];
    }

    public void updateBuffers() {
        undirectedInstanceCounter.promoteCountToDraw();
        directedInstanceCounter.promoteCountToDraw();
    }

    private void updateData(final GraphIndexImpl graphIndex, final GraphRenderingOptions renderingOptions, final GraphSelection graphSelection) {
        if (!renderingOptions.isShowEdges()) {
            undirectedInstanceCounter.clearCount();
            directedInstanceCounter.clearCount();
            return;
        }

        graphIndex.indexEdges();

        //Selection:
        final boolean someEdgesSelection = graphSelection.getSelectedEdgesCount() > 0;
        final boolean someNodesSelection = graphSelection.getSelectedNodesCount() > 0;
        final float lightenNonSelectedFactor = renderingOptions.getLightenNonSelectedFactor();
        final boolean hideNonSelected = someEdgesSelection && (renderingOptions.isHideNonSelected() || lightenNonSelectedFactor >= 1);
        final boolean edgeSelectionColor = renderingOptions.isEdgeSelectionColor();
        final float edgeBothSelectionColor = Float.intBitsToFloat(renderingOptions.getEdgeBothSelectionColor().getRGB());
        final float edgeInSelectionColor = Float.intBitsToFloat(renderingOptions.getEdgeInSelectionColor().getRGB());
        final float edgeOutSelectionColor = Float.intBitsToFloat(renderingOptions.getEdgeOutSelectionColor().getRGB());

        final int totalEdges = graphIndex.getEdgeCount();

        final float[] attribs
                = attributesBuffer
                = ArrayUtils.ensureCapacityNoCopy(attributesBuffer, totalEdges * ATTRIBS_STRIDE);

        graphIndex.getVisibleEdges(edgesCallback);

        final Edge[] visibleEdgesArray = edgesCallback.getEdgesArray();
        final int visibleEdgesCount = edgesCallback.getCount();

        final Graph graph = graphIndex.getGraph();

        int attribsIndex = 0;
        attribsIndex = updateUndirectedData(
                graph,
                someEdgesSelection, hideNonSelected, visibleEdgesCount, visibleEdgesArray,
                graphSelection, someNodesSelection, edgeSelectionColor, edgeBothSelectionColor, edgeOutSelectionColor, edgeInSelectionColor,
                attribs, attribsIndex
        );
        updateDirectedData(
                graph, someEdgesSelection, hideNonSelected, visibleEdgesCount, visibleEdgesArray,
                graphSelection, someNodesSelection, edgeSelectionColor, edgeBothSelectionColor, edgeOutSelectionColor, edgeInSelectionColor,
                attribs, attribsIndex
        );
    }

    @Override
    public void dispose() {
        super.dispose();
        attributesDrawBufferBatchOneCopyPerVertex = null;
        attributesDrawBufferBatchOneCopyPerVertexManagedDirectBuffer.destroy();

        attributesBuffer = null;
    }
}
