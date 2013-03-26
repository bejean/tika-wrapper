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

/**
 * HTML to plain-text formatter. 
 *
 * @author Eolya Consulting - http://www.eolya.fr/
 */
public interface IHtmlFormater {

	/**
	 * Format an html string to plain-text
	 * @param html the html content to be formated
	 * @return formatted text
	 */
	public String getPlainText(String html);
}
