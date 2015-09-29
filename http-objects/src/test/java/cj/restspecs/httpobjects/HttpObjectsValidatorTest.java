/**
 * Copyright (C) Commission Junction Inc.
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
import cj.restspecs.core.io.ClasspathLoader;
import com.cj.restspecs.mockrunner.RestSpecServletValidator;
import org.httpobjects.HttpObject;
import org.httpobjects.Request;
import org.httpobjects.Response;
import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class HttpObjectsValidatorTest {

    @Test
    public void validateRestSpecAgainstHttpObject() throws IOException {
        //Given
        HttpObject httpObject = new HttpObject("/sample") {
            @Override
            public Response get(Request req) {
                return OK(Json("[\"awesomeness\"]"));
            }
        };

        RestSpec sampleSpec = new RestSpec("/SampleSpec.json", new ClasspathLoader());

        //When
        RestSpecServletValidator.ValidationResult result = HttpObjectsValidator.applySpecificationToResource(sampleSpec, httpObject);

        //Then
        assertThat(result.description(), result.matches(), is(true));
    }
}
