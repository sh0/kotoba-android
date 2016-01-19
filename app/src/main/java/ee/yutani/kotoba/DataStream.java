/*
 * Kotoba-chan
 *
 * Copyright (C) 2013 Siim Meerits <sh0@yutani.ee>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

// Package
package ee.yutani.kotoba;

// Imports
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

// Asset file stream class
public class DataStream
{
    // Buffer size
    static final int BUFFER_SIZE = 256;

    // Streams
    InputStream m_fs = null;
    InputStream m_bs = null;

    // Offset
    private long m_offset = 0;

    // Constructor
    public DataStream(InputStream fs, boolean use_buffer)
    {
        // File stream
        m_fs = fs;
        m_fs.mark(0x0fffffff);

        // Buffered stream
        if (use_buffer) {
            m_bs = new BufferedInputStream(m_fs, BUFFER_SIZE);
            m_bs.mark(0x0fffffff);
        }
    }

    public void Seek(long offset) throws IOException
    {
        m_fs.reset();
        m_fs.skip(offset);
        if (m_bs != null) {
            m_bs = new BufferedInputStream(m_fs, BUFFER_SIZE);
        }

        /*
        //if (offset < m_offset) {
            m_bs = null;
            m_fs.reset();
            m_fs.skip(offset);
            m_bs = new BufferedInputStream(m_fs, BUFFER_SIZE);
        //} else if (offset > m_offset) {
        //    m_bs.skip(offset - m_offset);
        //}
        */

        m_offset = offset;
    }

    public long Offset()
    {
        return m_offset;
    }

    public int Read(byte[] arr) throws IOException
    {
        if (m_bs != null)
            return m_bs.read(arr);
        else
            return m_fs.read(arr);
    }

    public int ReadInt() throws IOException
    {
        // Offset
        m_offset += 4;

        // Read
        byte[] arr = new byte[4];
        if (Read(arr) < arr.length) {
            assert(false);
        }

        // Cast
        ByteBuffer buf = ByteBuffer.wrap(arr);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        return buf.getInt();
    }

    public int[] ReadIntArray(int num) throws IOException
    {
        // Offset
        m_offset += (4 * num);

        // Read
        byte[] arr = new byte[4 * num];
        if (Read(arr) < arr.length) {
            assert(false);
        }

        // Cast
        ByteBuffer buf = ByteBuffer.wrap(arr);
        buf.order(ByteOrder.LITTLE_ENDIAN);

        // Int array
        int[] ret = new int[num];
        for (int i = 0; i < num; i++)
            ret[i] = buf.getInt();
        return ret;
    }

    public short ReadShort() throws IOException
    {
        // Offset
        m_offset += 2;

        // Read
        byte[] arr = new byte[2];
        if (Read(arr) < arr.length) {
            assert(false);
        }

        // Cast
        ByteBuffer buf = ByteBuffer.wrap(arr);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        return buf.getShort();
    }

    public String ReadString() throws IOException
    {
        // String length
        short size = ReadShort();

        // Offset
        m_offset += size;

        // Read
        byte[] arr = new byte[size];
        if (Read(arr) < arr.length) {
            assert(false);
        }

        // Cast
        return new String(arr, "UTF-8");
    }
}
