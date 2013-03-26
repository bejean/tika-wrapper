/*
 *  Copyright 2013 Eolya Consulting - http://www.eolya.fr/
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package fr.eolya.extraction.htmlformater;

import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;

import fr.eolya.extraction.htmlformater.HtmlToPlaintTextSimple;

import junit.framework.TestCase;

public class HtmlFormaterTest extends TestCase {

	@Test
	public void testHtmlToPlaintTextSimple() throws IOException {
		String fileName = "../doc/html/f1-psychodrame-chez-red-bull.html";
		InputStream is = getClass().getResourceAsStream(fileName);
		java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
		String html = s.hasNext() ? s.next() : "";
		is.close();
		
		HtmlToPlaintTextSimple formatter = new HtmlToPlaintTextSimple();
		String plainText = formatter.getPlainText(html);
		System.out.println(plainText);
	}
}
