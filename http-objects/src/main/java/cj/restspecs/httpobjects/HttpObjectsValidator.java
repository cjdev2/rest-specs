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
package cj.restspecs.httpobjects;

import cj.restspecs.core.RestSpec;
import com.cj.restspecs.mockrunner.RestSpecServletValidator;
import com.cj.restspecs.mockrunner.RestSpecServletValidator.ValidationResult;
import org.httpobjects.HttpObject;
import org.httpobjects.servlet.ServletFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class HttpObjectsValidator extends HttpServlet {
    private final HttpObject resource;

    public HttpObjectsValidator(HttpObject resource) {
        this.resource = resource;
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        FilterChain dummyFilterChain = new FilterChain() {
            //Remember to add @Override after Java upgrade
            public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
                throw new RuntimeException("This should not be invoked, it is only here to get the validator to compile.");
            }
        };

        new ServletFilter(resource).doFilter(req, resp, dummyFilterChain);
    }

    public static ValidationResult applySpecificationToResource(RestSpec spec, HttpObject resource) {
        ValidationResult validationResult;
        try {
            validationResult = new RestSpecServletValidator().validate(spec, new HttpObjectsValidator(resource));
            return validationResult;
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }
    }
}
