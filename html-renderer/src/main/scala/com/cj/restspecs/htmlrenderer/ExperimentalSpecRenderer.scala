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
package com.cj.apiserver

import cj.restspecs.core.RestSpec
import java.util.regex.Pattern
import scala.util.Try
import cj.restspecs.core.model.Representation
import scala.collection.JavaConversions._
import org.apache.commons.io.IOUtils
import java.net.URL


/**
 * This is just a quick proof-of-concept.  This needs to be reimplemented TDD, among other things.
 */
object ExperimentalSpecRenderer {

  
  def renderDocumentation(title:String, description:String, specPaths:Iterator[String], pathNormalizer:(RestSpec)=>String = {(s)=> s.pathMinusQueryStringAndFragment}):String = {
   
    case class NormalizedSpec(val normalizedPath:String, val spec:RestSpec)
    
    val specs = specPaths.map( new RestSpec(_))
    
    val specsByPath = specs.map{spec=>
      NormalizedSpec(
          normalizedPath = pathNormalizer(spec),
          spec=spec)
    }.toList.groupBy(_.normalizedPath)
    
    def htmlEscape(text:String) = Option(text).getOrElse("").replaceAllLiterally("<", "&lt").replaceAllLiterally(">", "&gt;")
    def renderRepresentation(name:Option[String], headers:cj.restspecs.core.model.Header, representation:cj.restspecs.core.model.Representation) = {
      val nameSection = name.map("<h3>" + _ + "</h3>").getOrElse("")
      s"""
         <div class="representation">
            $nameSection
            <div class="headers">${renderHeaders(headers, "Content-Type" -> representation.contentType())}</div>
            <div class="representation-body">${htmlEscape(representation.asText)}</div>
         </div>
       """
    }
    
    def renderHeaders(headers:cj.restspecs.core.model.Header, additional:(String, String)*) = {
      
      val headerValuesByName = headers.fieldNames.flatMap({name=> headers.fieldsNamed(name).map(name -> _)}).toMap ++ additional.toMap
      
      Option(headers) match {
        case Some(header)=> "<table>" + headerValuesByName.filterNot(_._2.isEmpty()).map{nameValue=> s"<tr><td>${htmlEscape(nameValue._1)}:</td><td>${htmlEscape(nameValue._2)}</td></tr>"}.mkString("") + "</table>"
        case None => ""
      }
    }
    
    val pathsDocumented = specsByPath.toList.sortBy(_._1).zipWithIndex.flatMap{next=>
      val ((cleanPath, specs), idx) = next
      
      
      val specsByMethod = specs.groupBy{spec=>
        Try(spec.spec.request().method()).getOrElse("GET")
      }
      
      val methodsDocumented = specsByMethod.map{n=>
        val (method, specs) = n
        
        
        val queryParamsForMethod = specs.foldLeft(Map[String, String]()){(accum, spec)=>
          val params:Map[String, String] = spec.spec.queryParams().toMap
          accum ++ params
        }
        
        val examplesDocumented = specs.map{sanitizedSpec=>
          val spec = sanitizedSpec.spec
          try{
            
            s"""
                    <div class="name">Example: ${spec.name}</div>
                    <div class="example">
                      <h3>Request</h3>
                      <div class="example-path">$method ${spec.path}</div>
                      ${Try(spec.request).map{req=>renderRepresentation(None, req.header(), req.representation())}.getOrElse("")}
                      ${Try(spec.response).map{resp=>renderRepresentation(Some("Response"), resp.header(), resp.representation())}.getOrElse("")}
                    </div>"""
           
          } catch {
            case e:Exception => "Error reading " + spec.name()
          }
        }
        val paramRows = queryParamsForMethod.map({kv=>
          val (name, value) = kv
          
          println("name: " + name + "=" + value)
          
          s"""
           <tr><td>${htmlEscape(name)}</td><td>${htmlEscape(value)}</td></tr>
           """
        }).mkString("\n")
        
        val params = if(!paramRows.isEmpty){
          s"""
                <div class="params">
                    <h3>Query Parameters</h3>
                    <table>
                      <tr><th>name</th><th>example</th></tr>
                      $paramRows
                    </table>
                </div>"""
        }else{
          ""
        }
        s"""
          <div class="method">$method</div>
          $params
          ${examplesDocumented.mkString("\n")}
         """
      }
      
      s"""
          <div class="path" num="$idx">$cleanPath</div>
          <div class="details" id="$idx">
             ${methodsDocumented.mkString("\n")}
          </div>""" 
      
    }

     def resourceText(key:String) = IOUtils.toString(getClass().getResourceAsStream(key))
     def urlText(key:String) = IOUtils.toString(new URL(key).openConnection().getInputStream)
     
     return s"""
            <html>
                <head>
                   <style type="text/css">${resourceText("/styles.css")}</style>
                   <script type="text/javascript">${urlText("http://code.jquery.com/jquery-1.11.2.min.js")}</script>
                   <script type="text/javascript">${resourceText("/stuff.js")}</script>
                </head>
                <body>
                   <h1>${title}</h1>
                   <p>${description}</p>
                   <p>For more detail on a given resource, click on it</p>
                   ${pathsDocumented.mkString("")}
                </body>
            </html>"""
  }
}