/**
 * Copyright (C) 2011, 2012, 2013 Commission Junction Inc.
 * 
 * This file is part of rest-specs.
 * 
 * rest-specs is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 * 
 * rest-specs is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with rest-specs; see the file COPYING.  If not, write to the
 * Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301 USA.
 * 
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole
 * combination.
 * 
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent
 * modules, and to copy and distribute the resulting executable under
 * terms of your choice, provided that you also meet, for each linked
 * independent module, the terms and conditions of the license of that
 * module.  An independent module is a module which is not derived from
 * or based on this library.  If you modify this library, you may extend
 * this exception to your version of the library, but you are not
 * obligated to do so.  If you do not wish to do so, delete this
 * exception statement from your version.
 */
package com.cj.restspecs.mojo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.apache.commons.io.FileUtils;

public class Util {

    @SuppressWarnings("unchecked")
    public static <T> T readObject(File path){
        try {
            ObjectInputStream in = new ObjectInputStream(new FileInputStream(path));
            try{
                return (T) in.readObject();
            }finally{
                in.close();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void writeObject(Object o, File path){
        try{
            ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(path));
            try{
                out.writeObject(o);
            }finally{
                out.close();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    public static void deleteDirectory(File directory) {
        try {
            FileUtils.deleteDirectory(directory);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static File tempDir() {
        try {
            File path = File.createTempFile("tempdirectory", ".dir");
            delete(path);
            mkdirs(path);
            return path;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void delete(File path) {
        if(!path.delete()) throw new RuntimeException("Could not delete " + path.getAbsolutePath());
    }

    public static void mkdirs(File directory, String string) {
        File path = new File(directory, string);
        mkdirs(path);
    }

    public static void mkdirs(File path) {
        if(!path.exists() && !path.mkdirs()){
            throw new RuntimeException("Could not create directory: " + path.getAbsolutePath());
        }
    }

}
