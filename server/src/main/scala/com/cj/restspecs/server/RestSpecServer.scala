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
package com.cj.restspecs.server

import org.httpobjects.jetty.HttpObjectsJettyHandler
import org.httpobjects.HttpObject
import org.httpobjects.Request
import org.httpobjects.DSL._
import java.io.{File => Path}
import cj.restspecs.core.RestSpec
import cj.restspecs.core.io.FilesystemLoader
import java.net.URI
import org.apache.commons.io.IOUtils
import org.httpobjects.Response
import org.httpobjects.ResponseCode
import scala.collection.JavaConversions._
import org.httpobjects.header.GenericHeaderField
import org.httpobjects.Representation
import java.io.FileInputStream
import scala.collection.mutable.ListBuffer

object RestSpecServer {
    
    private def findSpecs(dir:Path):Seq[Path] = {
      val results = dir.listFiles().toSeq.map{child=>
        if(child.isDirectory()){
          findSpecs(child)
        } 
        else if (child.getName().endsWith("spec.json")){
          List(child)
        }else{
          List()
        }
      }
      
      results.flatten
    }
    
    private def relativePath(a:Path, b:Path) = {
      val aStr = a.getAbsolutePath()
      val bStr = b.getAbsolutePath()
      if(aStr.startsWith(bStr)) aStr.substring(bStr.length()) else aStr
    }
    
	def main(args: Array[String]) {
      val currentDirectory = new Path(System.getProperty("user.dir"))
      
      val pathOption = if(args.length > 0) Some(new Path(args(0))) else None
	  
	  val rootPath =  pathOption.getOrElse(currentDirectory)
	
	  val loader = new FilesystemLoader(rootPath)
      val specFiles = findSpecs(rootPath)
      
      case class SpecWithFilesystemLocation (filesystemLocation:String, spec:RestSpec)
      
      val specFilePathsAndSpecs = specFiles.map{pathToFile=>
        val specPath = relativePath(pathToFile, rootPath)
        SpecWithFilesystemLocation(specPath, new RestSpec(specPath, loader))
      }
	  
	  val specs = specFilePathsAndSpecs.map(_.spec)
      
	  println(s"Serving ${specs.length} specs")
      
	  def createHelpPage(httpMethod:String, req:Request) = {
	    val query = req.query.toString
        val path = req.path.toString
	    val lines = specs.map{spec=>
                          
              val relativePathToSpec = specFilePathsAndSpecs.find(_.spec eq spec )
              
              s"""<a href="_specs/${relativePathToSpec.get.filesystemLocation}">${spec.name}</a> """ + spec.request.method + " <a href=\"" + spec.path + "\">" + spec.path + "</a>"
            }
            
            val htmlContent = s"""
                        <html><body>
                            <h1>NO SPEC FOUND MATCHING $httpMethod $path $query\n</h1> 
                            <p>Specs I know about are:</p>
                            <ul>""" + lines.mkString("<li>", "</li><li>", "</li>") + """</ul>
                        </body></html>"""
            
            INTERNAL_SERVER_ERROR(Html(htmlContent))
	  }
	  
      def respond(httpMethod:String, req:Request) = {
        val query = req.query.toString
        val path = req.path.toString
        
        
        val methodAndPathAndQueryMatches = specs.filter{candidate=>
          val p = new URI(candidate.path()).getPath()
          val pathsMatch = path == p
          val queriesMatch = query == candidate.queryString
          val methodsMatch = candidate.request.method == httpMethod
    
          methodsMatch && pathsMatch && queriesMatch
        }
        
        
        val maybeSpec = methodAndPathAndQueryMatches.headOption
        
        
        maybeSpec match {
          case None => createHelpPage(httpMethod, req)
          case Some(spec) =>
             
              val headers = spec.response().header().fieldNames().flatMap{name=>
                  spec.response().header().fieldsNamed(name).map{value=>
                    new GenericHeaderField(name, value)
                  }
              }
              val representation = spec.response().representation() match {
                case null => null
                case specRepresentation => {
                  val contentType = spec.response().header().fieldNames().find(_=="Content-Type").getOrElse("")
                  Bytes(contentType, specRepresentation.data())
              }
              
              }
             
              new Response(ResponseCode.forCode(spec.response().statusCode()), representation, headers:_*)
        }
      }
      
      class RequestGlue(pathMapping:String) extends HttpObject(pathMapping) {
            override def get(req:Request) = respond("GET", req)
            override def post(req:Request) = respond("POST", req)
            override def put(req:Request) = respond("PUT", req)
            override def delete(req:Request) = respond("DELETE", req)
	  }
	  
      HttpObjectsJettyHandler.launchServer(9933, 
          new RequestGlue("/"),
          new HttpObject("/_specs/{specPath*}"){
              override def get(req:Request) = {
                val relativePath = req.path().valueFor("specPath")
                val localPath = new Path(rootPath, relativePath)
                if(localPath.exists()){
                  OK(Bytes("application/json", new FileInputStream(localPath)))
                }else{
                  NOT_FOUND
                }
              }
          },
          new RequestGlue("/{resource*}")
      )
    }
}