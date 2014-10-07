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
package fr.eolya.extraction.tika;

import java.io.InputStream;
import java.util.Map;

import org.junit.Test;

import fr.eolya.extraction.htmlformater.HtmlToPlaintTextSimple;
import fr.eolya.extraction.htmlformater.IHtmlFormater;

import junit.framework.TestCase;

public class TikaWrapperTest extends TestCase {

	static final String pdfToTextPath = "/usr/local/bin/pdftotext";
	static final String swfToHtmlPath = "/opt/tools/swf2html";
	//static final String djVuTextPath = "/opt/tools/djvu/bin/djvutxt";
	
	@Test
	public void testPdfTika() {		
		boolean content = true;
		boolean verbose = true;
		String format = TikaWrapper.OUTPUT_FORMAT_TEXT;
		String fileName = "/fr/eolya/extraction/doc/java.pdf";

		InputStream i = getClass().getResourceAsStream(fileName);		
		
		TikaWrapper mfte;
		try {
			mfte = new TikaWrapper(format);
			mfte.process(i);
			dumpDoc(mfte, fileName, content, verbose);		
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testPdfNoTika() {
		
		boolean content = true;
		boolean verbose = true;
		String format = TikaWrapper.OUTPUT_FORMAT_HTML;
		String fileName = "../doc/java.pdf";
		
		InputStream i = getClass().getResourceAsStream(fileName);
		assertFalse(i==null);
		
		TikaWrapper mfte;
		try {
			mfte = new TikaWrapper(format);
			mfte.setPdfToTextPath(pdfToTextPath);
			mfte.process(i, TikaWrapper.CONTENT_TYPE_PDF);
			dumpDoc(mfte, fileName, content, verbose);		
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testSwfNoTika() {
		boolean content = true;
		boolean verbose = true;
		String format = TikaWrapper.OUTPUT_FORMAT_TEXT;
		String fileName = "../doc/reflection.swf";
		
		InputStream i = getClass().getResourceAsStream(fileName);
		assertFalse(i==null);
		
		TikaWrapper mfte;
		try {
			mfte = new TikaWrapper(format);
			mfte.setSwfToHtmlPath(swfToHtmlPath);
			
			IHtmlFormater formater = new HtmlToPlaintTextSimple();
			mfte.setHtmlFormater(formater);
			
			mfte.process(i, TikaWrapper.CONTENT_TYPE_SWF);
			dumpDoc(mfte, fileName, content, verbose);		
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Test
	public void testSeveral() {
		boolean content = true;
		boolean verbose = true;
		String format = TikaWrapper.OUTPUT_FORMAT_TEXT;
		
		TikaWrapper mfte;
		try {
			mfte = new TikaWrapper(format);

			String fileName = "../doc/java.pdf";
			InputStream i = getClass().getResourceAsStream(fileName);		
			assertFalse(i==null);
			mfte.process(i);
			dumpDoc(mfte, fileName, content, verbose);		
			
			fileName = "../doc/fr.pdf";
			i = getClass().getResourceAsStream(fileName);				
			assertFalse(i==null);
			mfte.process(i);
			dumpDoc(mfte, fileName, content, verbose);		

			fileName = "../doc/en.pdf";
			i = getClass().getResourceAsStream(fileName);				
			assertFalse(i==null);
			mfte.process(i);
			dumpDoc(mfte, fileName, content, verbose);		
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
//	@Test
//	public void testDjVu() {
//		
//		// more files for tests
//		// ftp://vpa.users.odessa.comstar.net.ua/public/Sci_Library/Phys%20Library/PPop_Popular-level/
//		
//		boolean content = true;
//		boolean verbose = true;
//		String format = TikaWrapper.OUTPUT_FORMAT_TEXT;
//		String fileName = "../doc/Test.djvu";
//		
//		InputStream i = getClass().getResourceAsStream(fileName);
//		
//		TikaWrapper mfte;
//		try {
//			mfte = new TikaWrapper(format);
//			mfte.setDjVuTextPath(djVuTextPath);
//			
//			IHtmlFormater formater = new HtmlToPlaintTextSimple();
//			mfte.setHtmlFormater(formater);
//			
//			mfte.process(i, TikaWrapper.CONTENT_TYPE_DJVU);
//			dumpDoc(mfte, fileName, content, verbose);		
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//	}

	private static void dumpDoc(TikaWrapper mfte, String url, boolean content, boolean verbose) {
		System.out.println("========================================================");
		System.out.println("url: " + url);
		System.out.println("Title: " + mfte.getMetaTitle());
		System.out.println("Author: " + mfte.getMetaAuthor());
		System.out.println("Created: " + mfte.getMetaCreated());
		System.out.println("Modified: " + mfte.getMetaModified());
		System.out.println("Content-Type: " + mfte.getMetaContentType());
		System.out.println("CharSet: " + mfte.getMetaCharSet());
		if (verbose && mfte.getMetas()!=null) {
			System.out.println("========================================================");
			for (Map.Entry<String, String> entry : mfte.getMetas().entrySet()) {
				System.out.println(entry.getKey() + ": " + entry.getValue());
			}
		}
		if (content && mfte.getText()!=null) {
			System.out.println("========================================================");
			System.out.println(mfte.getText());
		}
		System.out.println("\n\n");
	}
}
