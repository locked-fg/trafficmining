/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.lmu.ifi.dbs.trafficmining.utils;

import java.io.*;

public class BufferedFileInputStream extends BufferedInputStream {

    public BufferedFileInputStream(InputStream in, int size) {
        super(in, size);
    }

    public BufferedFileInputStream(InputStream in) {
        super(in);
    }

    public BufferedFileInputStream(File in, int size) throws FileNotFoundException {
        super(new FileInputStream(in), size);
    }

    public BufferedFileInputStream(File in) throws FileNotFoundException {
        super(new FileInputStream(in));
    }
}
