package org.gephi.viz.engine.lwjgl.util.gl;

import java.nio.*;

import static org.gephi.viz.engine.util.gl.Buffers.bufferElementBytes;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL44.glBufferStorage;

/**
 *
 * @author Eduardo Ramos
 */
public class GLBufferImmutable implements GLBuffer {

    private final int id;
    private final int type;

    private int flags = -1;
    private long sizeBytes = -1;

    private boolean isBound = false;

    public GLBufferImmutable(int id, int type) {
        this.id = id;
        this.type = type;
    }

    @Override
    public void bind() {
        glBindBuffer(type, id);
        isBound = true;
    }

    @Override
    public void unbind() {
        glBindBuffer(type, 0);
        isBound = false;
    }

    private void bufferStorage(Buffer buf) {
        if (buf instanceof FloatBuffer) {
            glBufferStorage(type, (FloatBuffer) buf, flags);
        } else if (buf instanceof IntBuffer) {
            glBufferStorage(type, (IntBuffer) buf, flags);
        } else if (buf instanceof ShortBuffer) {
            glBufferStorage(type, (ShortBuffer) buf, flags);
        } else if (buf instanceof ByteBuffer) {
            glBufferStorage(type, (ByteBuffer) buf, flags);
        } else if (buf instanceof DoubleBuffer) {
            glBufferStorage(type, (DoubleBuffer) buf, flags);
        } else {
            throw new UnsupportedOperationException("Buffer class not supported: " + buf.getClass().getName());
        }
    }

    private void bufferSubData(Buffer buf) {
        if (buf instanceof FloatBuffer) {
            glBufferSubData(type, 0, (FloatBuffer) buf);
        } else if (buf instanceof IntBuffer) {
            glBufferSubData(type, 0, (IntBuffer) buf);
        } else if (buf instanceof ShortBuffer) {
            glBufferSubData(type, 0, (ShortBuffer) buf);
        } else if (buf instanceof ByteBuffer) {
            glBufferSubData(type, 0, (ByteBuffer) buf);
        } else if (buf instanceof DoubleBuffer) {
            glBufferSubData(type, 0, (DoubleBuffer) buf);
        } else if (buf instanceof LongBuffer) {
            glBufferSubData(type, 0, (LongBuffer) buf);
        } else {
            throw new UnsupportedOperationException("Buffer class not supported: " + buf.getClass().getName());
        }
    }

    @Override
    public void init(long sizeBytes, int flags) {
        if (!isBound()) {
            throw new IllegalStateException("You should bind the buffer first!");
        }

        if (isInitialized()) {
            throw new UnsupportedOperationException("Cannot reinitialize an immutable buffer");
        }

        this.flags = flags;
        this.sizeBytes = sizeBytes;

        glBufferStorage(type, sizeBytes, flags);
    }

    @Override
    public void init(Buffer buffer, int flags) {
        if (!isBound()) {
            throw new IllegalStateException("You should bind the buffer first!");
        }

        if (isInitialized()) {
            throw new UnsupportedOperationException("Cannot reinitialize an immutable buffer");
        }

        this.flags = flags;
        final int elementBytes = bufferElementBytes(buffer);
        sizeBytes = (long) buffer.capacity() * elementBytes;

        bufferStorage(buffer);
    }

    @Override
    public void update(Buffer buffer) {
        if (!isInitialized()) {
            throw new IllegalStateException("You should initialize the buffer first!");
        }
        if (!isBound()) {
            throw new IllegalStateException("You should bind the buffer first!");
        }

        final int elementBytes = bufferElementBytes(buffer);
        final long neededBytesCapacity = (long) buffer.remaining() * elementBytes;
        ensureCapacity(neededBytesCapacity);

        bufferSubData(buffer);
    }

    @Override
    public void updateWithOrphaning(Buffer buffer) {
        throw new UnsupportedOperationException("This buffer is immutable and can't be reinitialized");
    }

    @Override
    public long size() {
        return sizeBytes;
    }

    private void ensureCapacity(long neededBytes) {
        if (sizeBytes < neededBytes) {
            throw new UnsupportedOperationException("This buffer is immutable and needed capacity (" + neededBytes + ") is not enough. Size = " + sizeBytes);
        }
    }

    @Override
    public boolean isInitialized() {
        return sizeBytes != -1;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public int getType() {
        return type;
    }

    @Override
    public boolean isBound() {
        return isBound;
    }

    @Override
    public int getUsageFlags() {
        return flags;
    }

    @Override
    public long getSizeBytes() {
        return sizeBytes;
    }

    @Override
    public void destroy() {
        if (!isInitialized()) {
            throw new IllegalStateException("You should initialize the buffer first!");
        }

        glDeleteBuffers(id);
        sizeBytes = -1;
    }

    @Override
    public boolean isMutable() {
        return false;
    }
}
