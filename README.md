tika-wrapper
============

Wraps Apache Tika library (http://tika.apache.org/) in order to allow a simple usage and add or improve some features.

Added features
--------------

* Extract Flash file (.swf) content as text including http links
* Allows to use PDFTOTEXT as an alternate solution to PDFBox in order to extract text from PDF files (5 to 20 times faster)
* Extract text from DJVU files (http://djvu.sourceforge.net/)
* HTML parsing with Snacktory library (https://github.com/bejean/snacktory)


Usage
-----

<pre>
InputStream is = new FileInputStream("sample.pdf");

TikaWrapper wrapper = new TikaWrapper(TikaWrapper.OUTPUT_FORMAT_HTML, TikaWrapper.CONTENT_TYPE_PDF);
wrapper.setPdfToTextPath("/usr/bin/pdftotext");
wrapper.process(is);
		
System.out.println(wrapper.getMetaTitle());
System.out.println(wrapper.getText());
</pre>


Roadmap
-------

* Use my forked version of Snacktory java library in order to extract the main text of html pages - https://github.com/bejean/snacktory


Build
-----

In order to build the jar file and get all dependencies in target dir.

<pre>
mvn clean
mvn install -Dmaven.test.skip=true
mvn dependency:copy-dependencies
</pre>


Dependencies
------------

This project includes shell-1.0.jar, a Java system API.
http://blog.developpez.com/adiguba/p3035/java/runtime_exec_n_est_pas_des_plus_simple
Shell 1.0 is under CeCILL license - http://www.cecill.info/licences/Licence_CeCILL_V2-en.txt

As it is not available in a public Maven repository, the project uses a Maven Repository hosted on Github.


External tools
--------------

In order in order to extract text from SWF, PDF or DJVU files, you need the following tools :

* SWF : swf2thml allows extracting text from SWF files. This tool is available in the "tools" directory 

* PDF : PDFTOTEXT allows extracting text from PDF files. This tools is available on various operating system as standard packages
    - pdf-utils or poppler-utils according to you Linux distribution
    - source and binary distributions : http://www.foolabs.com/xpdf/download.html 
    
* DJVU : djvutxt. Download at http://djvu.sourceforge.net/


License
-------

Copyright 2013 Eolya Consulting - http://www.eolya.fr/

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this software except in compliance with the License.
You may obtain a copy of the License at:

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
