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

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.io.FileUtils;
import org.apache.tika.detect.DefaultDetector;
import org.apache.tika.detect.Detector;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.parser.PasswordProvider;
import org.apache.tika.parser.html.BoilerpipeContentHandler;
import org.apache.tika.sax.ExpandedTitleContentHandler;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.ContentHandler;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import com.developpez.adiguba.shell.Shell;

import fr.eolya.extraction.htmlformater.IHtmlFormater;


/**
 * Wraps Apache Tika library in order to allow a simple usage and add or improve some features.
 * 
 * @author Eolya Consulting - http://www.eolya.fr/
 */
public class TikaWrapper {

	public static String OUTPUT_FORMAT_XML = "xml";
	public static String OUTPUT_FORMAT_HTML = "html";
	public static String OUTPUT_FORMAT_TEXT = "text";
	public static String OUTPUT_FORMAT_TEXT_MAIN = "text_main";

	public static String CONTENT_TYPE_PDF = "application/pdf";
	public static String CONTENT_TYPE_SWF = "application/x-shockwave-flash";

	private static String META_TITLE = "title";
	private static String META_AUTHOR = "Author";
	private static String META_CREATED = "Creation-Date";
	private static String META_MODIFIED = "modified";
	private static String META_CONTENTTYPE = "Content-Type";
	private static String META_CONTENTSIZE = "Content-Size";
	
	private class OutputType {
		public void process(InputStream input, OutputStream output, Metadata metadata)
				throws Exception {
			Parser p = parser;
			ContentHandler handler = getContentHandler(output, metadata);
			p.parse(input, handler, metadata, context);
		}

		protected ContentHandler getContentHandler(OutputStream output, Metadata metadata) throws Exception {
			throw new UnsupportedOperationException();
		}
	}

	private final OutputType XML = new OutputType() {
		@Override
		protected ContentHandler getContentHandler(OutputStream output, Metadata metadata) throws Exception {
			return getTransformerHandler(output, "xml", encoding, prettyPrint);
		}
	};

	private final OutputType HTML = new OutputType() {
		@Override
		protected ContentHandler getContentHandler(OutputStream output, Metadata metadata) throws Exception {
			return new ExpandedTitleContentHandler(getTransformerHandler(output, "html", encoding, prettyPrint));
		}
	};

	private final OutputType TEXT = new OutputType() {
		@Override
		protected ContentHandler getContentHandler(OutputStream output, Metadata metadata) throws Exception {
			return new BodyContentHandler(getOutputWriter(output, encoding));
		}
	};
	
    private final OutputType TEXT_MAIN = new OutputType() {
        @Override
        protected ContentHandler getContentHandler(OutputStream output, Metadata metadata) throws Exception {
            return new BoilerpipeContentHandler(getOutputWriter(output, encoding));
        }
    };


	/**
	 * Returns a output writer with the given encoding.
	 *
	 * @see <a href="https://issues.apache.org/jira/browse/TIKA-277">TIKA-277</a>
	 * @param output output stream
	 * @param encoding output encoding,
	 *                 or <code>null</code> for the platform default
	 * @return output writer
	 * @throws UnsupportedEncodingException
	 *         if the given encoding is not supported
	 */
	private static Writer getOutputWriter(OutputStream output, String encoding)
			throws UnsupportedEncodingException {
		if (encoding != null) {
			return new OutputStreamWriter(output, encoding);
		} else if (System.getProperty("os.name")
				.toLowerCase().startsWith("mac os x")) {
			// TIKA-324: Override the default encoding on Mac OS X
			return new OutputStreamWriter(output, "UTF-8");
		} else {
			return new OutputStreamWriter(output);
		}
	}

	/**
	 * Returns a transformer handler that serializes incoming SAX events
	 * to XHTML or HTML (depending the given method) using the given output
	 * encoding.
	 *
	 * @see <a href="https://issues.apache.org/jira/browse/TIKA-277">TIKA-277</a>
	 * @param output output stream
	 * @param method "xml" or "html"
	 * @param encoding output encoding,
	 *                 or <code>null</code> for the platform default
	 * @return {@link System#out} transformer handler
	 * @throws TransformerConfigurationException
	 *         if the transformer can not be created
	 */
	private static TransformerHandler getTransformerHandler(OutputStream output, String method, String encoding, boolean prettyPrint)
			throws TransformerConfigurationException {
		SAXTransformerFactory factory = (SAXTransformerFactory)SAXTransformerFactory.newInstance();
		TransformerHandler handler = factory.newTransformerHandler();
		handler.getTransformer().setOutputProperty(OutputKeys.METHOD, method);
		handler.getTransformer().setOutputProperty(OutputKeys.INDENT, prettyPrint ? "yes" : "no");
		if (encoding != null) {
			handler.getTransformer().setOutputProperty(OutputKeys.ENCODING, encoding);
		}
		handler.setResult(new StreamResult(output));
		return handler;
	}

	private ParseContext context;
	private Parser parser;
	private boolean prettyPrint = true;
	private Detector detector;
	private OutputType type = XML;
	
	private String outputFormat;
	
	private IHtmlFormater formater;

	private String tmpPath = null;
	private String pdfToTextPath = null;
	private String swfToHtmlPath = null;

	private String contentType;

	private Metadata metadata;
	private ByteArrayOutputStream output;
	private HashMap<String, String> meta;

	private HashMap<String, String> meta2;
	private String text;

	/**
	 * Output character encoding, or <code>null</code> for platform default
	 */
	private String encoding = null;

	/**
	 * Password for opening encrypted documents, or <code>null</code>.
	 */
	private String password = null;

	public TikaWrapper(String outputFormat, String outputEncoding, String contentType) throws Exception {
		encoding = outputEncoding;
		if (encoding==null || "".equals(encoding))
			encoding = "UTF-8";

		context = new ParseContext();
		detector = new DefaultDetector();
		parser = new AutoDetectParser(detector);
		
		this.outputFormat = outputFormat;
		this.contentType = contentType;

		context.set(Parser.class, parser);
		context.set(PasswordProvider.class, new PasswordProvider() {
			public String getPassword(Metadata metadata) {
				return password;
			}
		});

		if (OUTPUT_FORMAT_HTML.equals(outputFormat)) {
			type = HTML;
		} else if (OUTPUT_FORMAT_TEXT.equals(outputFormat)) {
			type = TEXT;
		} else if (OUTPUT_FORMAT_TEXT_MAIN.equals(outputFormat)) {
			type = TEXT_MAIN;
		}
	}

	public TikaWrapper(String outputFormat) throws Exception {
		this(outputFormat, "UTF-8", null);
	}

	public TikaWrapper(String outputFormat, String contentType) throws Exception {
		this(outputFormat, "UTF-8", contentType);
	}

	public void process(InputStream input) throws MalformedURLException {
		try {
			text = null;
			meta2 = null;
			metadata = null;
			meta = null;
			if (usePftToText()) {
				processWithPdfToText(input);
			} else if (useSwfToHtml()) {
				processWithSwfToHtml(input);
			} else {	
				metadata = new Metadata();
				processWithTika(TikaInputStream.get(input));
			}
		}
		catch(Exception e) {}
	}

	public void process(String url) throws MalformedURLException {
		URL u;
		File file = new File(url);
		if (file.isFile()) {
			u = file.toURI().toURL();
		} else {
			u = new URL(url);
		}
		try {
			text = null;
			meta2 = null;
			metadata = null;
			meta = null;
			if (usePftToText()) {
				processWithPdfToText(u.openStream());
			} else if (useSwfToHtml()) {
				processWithSwfToHtml(u.openStream());
			} else {	
				metadata = new Metadata();
				if (contentType!=null && !"".equals(contentType)) 
					metadata.set(Metadata.CONTENT_TYPE, contentType);
				processWithTika(TikaInputStream.get(u, metadata));
			}
		}
		catch(Exception e) {}
	}

	private void processWithTika(InputStream input) {
		try {
			output = new ByteArrayOutputStream();
			try {
				type.process(input, output, metadata);
			} finally {
				input.close();
			}	
		}
		catch(Exception e) {}
	}

	public String getText() {
		if (output!=null) return output.toString();
		return text;
	}

	public String getMetaAuthor() {
		return getMetas()!=null ? getMetas().get(META_AUTHOR) : null;
	}

	public String getMetaCreated() {
		return getMetas()!=null ? getMetas().get(META_CREATED) : null;
	}

	public String getMetaTitle() {
		return getMetas()!=null ? getMetas().get(META_TITLE) : null;
	}

	public String getMetaModified() {
		return getMetas()!=null ? getMetas().get(META_MODIFIED) : null;
	}     

	public String getMetaContentType() {
		if (getMetas()==null) return null;
		String value = getMetas().get(META_CONTENTTYPE);
		if (value!=null && value.indexOf(";")!=-1) value = value.substring(0, value.indexOf(";")).trim();
		return value;
	}
	
	public String getMetaCharSet() {
		if (getMetas()==null) return null;
		String value = getMetas().get(META_CONTENTTYPE);
		if (value!=null && value.indexOf(";")!=-1) 
			value = value.substring(value.indexOf(";")+1).trim();
		else
			value = null;
		return value;
	}

	public Map<String, String> getMetas() {
		if (meta2!=null) return meta2;
		if (meta==null && metadata!=null) {
			meta = new HashMap<String, String>();
			String[] names = metadata.names();
			for (String name : names) {
				for(String value : metadata.getValues(name)) {
					meta.put(name, value);
				}
			}
		}
		return meta;
	}

	public void setTempPath(String tempPath) {
		this.tmpPath = tempPath;	
	}

	public void setPdfToTextPath(String pdfToTextPath) {
		this.pdfToTextPath = pdfToTextPath;	
	}

	private boolean usePftToText() {
		return (pdfToTextPath!=null && !"".equals(pdfToTextPath) && contentType!=null && !"".equals(contentType));
	}

	public void setSwfToHtmlPath(String swfToHtmlPath) {
		this.swfToHtmlPath = swfToHtmlPath;	
	}

	private boolean useSwfToHtml() {
		return (swfToHtmlPath!=null && !"".equals(swfToHtmlPath) && contentType!=null && !"".equals(contentType));
	}
	
	public void setHtmlFormater(IHtmlFormater formater) {
		this.formater = formater;	
	}
	
	private String getMetaContent(Document doc, String metaName) {
		Elements e = doc.select("meta[name=" + metaName + "]");
		if (e==null || e.first()==null) return null;
		return e.first().attr("content");
	}
	
	private void processWithPdfToText(InputStream input) {
		File tempFile = null;
		File tempFile2 = null;
		try {
			// Get a local copy of the file
			tempFile = createTempFile("tmp", ".pdf", tmpPath);

			OutputStream out=new FileOutputStream(tempFile);
			byte buf[]=new byte[1024];
			int len;
			while((len=input.read(buf))>0)
				out.write(buf,0,len);
			out.close();
			input.close();				

			meta2 = new HashMap<String, String>();
			meta2.put(META_CONTENTSIZE, String.valueOf(tempFile.length()));

			tempFile2 = createTempFile("tmp", ".html", tmpPath);

			Shell sh = new Shell(); 

			// Convert with PDFTOTEXT - pdftotext -enc UTF-8 -raw -q -htmlmeta -eol unix in.pdf out.html
			sh.exec(pdfToTextPath, "-enc", "UTF-8", "-raw", "-q", "-htmlmeta", "-eol", "unix", tempFile.getAbsolutePath(), tempFile2.getAbsolutePath()).consumeAsString();
			tempFile.delete();

			// Load in string and add the <meta http-equiv='Content-Type' content='text/html; charset=utf-8'> line
			InputStreamReader fr1 =  new InputStreamReader(new FileInputStream(tempFile2), "UTF-8");
			BufferedReader br1 = new BufferedReader(fr1);
			StringBuilder sb = new StringBuilder();

			while(br1.ready()){
				String line = br1.readLine();
				sb.append(line).append("\n");
				if ("</head>".equals(line))
				{
					sb.append("<meta http-equiv='Content-Type' content='text/html; charset=utf-8'>").append("\n");
				}
			}
			br1.close() ;
			tempFile2.delete();

			meta2.put(META_CONTENTTYPE, CONTENT_TYPE_PDF);

			text = sb.toString();

			Document doc = Jsoup.parse(text);
			if (doc!=null) {
				meta2.put(META_TITLE, doc.select("title").text());
				meta2.put(META_AUTHOR, getMetaContent(doc, "Author"));
				String creationDate = getMetaContent(doc, "CreationDate");
				if (creationDate!=null) {
					// 20130322143113Z00'00' -> 2013-03-22T14:31:13Z
					Pattern p = Pattern.compile("[0-9]{14}Z[0-9]{2}'[0-9]{2}'");
					Matcher m = p.matcher(creationDate);
					if (m.find()) {
						String value = String.format("%1$s-%2$s-%3$sT%4$s:%5$s:%6$sZ",
								creationDate.substring(0, 4), creationDate.substring(4, 6), creationDate.substring(6, 8), creationDate.substring(8, 10), creationDate.substring(10, 12), creationDate.substring(12, 14));
						meta2.put(META_CREATED, value);
					}
				}
				if (OUTPUT_FORMAT_TEXT.equals(outputFormat)) {
					text = doc.body().text();
					//IHtmlFormater formater = new HtmlToPlaintTextFormater();
					//text = formater.getPlainText(doc);
				}
			}
		} 
		catch (Exception e) {
			if (tempFile!=null && tempFile.exists()) tempFile.delete();
			if (tempFile2!=null && tempFile2.exists()) tempFile2.delete();
			e.printStackTrace();
			text = null;
			meta2 = null;
		}
	}

	public void processWithSwfToHtml(InputStream input)
	{
		File tempFile = null;
		File tempFile2 = null;

		try {
			if (input!=null && swfToHtmlPath!=null && !"".equals(swfToHtmlPath)) {
				// Get a local copy of the file
				tempFile = File.createTempFile("tmp", ".swf");

				OutputStream out=new FileOutputStream(tempFile);
				byte buf[]=new byte[1024];
				int len;
				while((len=input.read(buf))>0)
					out.write(buf,0,len);
				out.close();
				input.close();					

				// Convert with SWF2HTML
				tempFile2 = File.createTempFile("tmp", ".html");

				Shell sh = new Shell(); 
				sh.exec(swfToHtmlPath, "-o", tempFile2.getAbsolutePath(), tempFile.getAbsolutePath()).consumeAsString();
				tempFile.delete();

				String data = FileUtils.readFileToString(tempFile2, "UTF-8"); 

				tempFile2.delete();

				meta2 = new HashMap<String, String>();
				meta2.put(META_CONTENTSIZE, String.valueOf(data.length()));

				meta2.put(META_CONTENTTYPE, CONTENT_TYPE_SWF);

				if (OUTPUT_FORMAT_TEXT.equals(outputFormat)) {
					if (formater!=null) {
						data = formater.getPlainText(data);
					} else {
						data = Jsoup.parse(data).body().text();
						
					}
				}

				text = data;	
			}
		}
		catch (Exception e) {
			if (tempFile!=null && tempFile.exists()) tempFile.delete();
			if (tempFile2!=null && tempFile2.exists()) tempFile2.delete();
			e.printStackTrace();
		}
	}

	private static File createTempFile(String prefix, String suffix, String directory) throws IOException {
		File tmpFile = null;
		if (directory == null)
			directory = "";
		if (!"".equals(directory))
			tmpFile = new File(directory);
		if (tmpFile == null || !tmpFile.exists() || !tmpFile.isDirectory())
			return File.createTempFile(prefix, suffix);
		else
			return File.createTempFile(prefix, suffix, tmpFile);
	}
}
