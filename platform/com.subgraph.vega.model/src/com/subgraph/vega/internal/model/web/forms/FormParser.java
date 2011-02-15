package com.subgraph.vega.internal.model.web.forms;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

import org.w3c.dom.Node;
import org.w3c.dom.html2.HTMLCollection;
import org.w3c.dom.html2.HTMLDocument;
import org.w3c.dom.html2.HTMLElement;
import org.w3c.dom.html2.HTMLFormElement;
import org.w3c.dom.html2.HTMLInputElement;

import com.subgraph.vega.api.model.web.IWebPath;
import com.subgraph.vega.api.model.web.forms.IWebForm;
import com.subgraph.vega.internal.model.web.WebModel;

public class FormParser {
	private final static Logger logger = Logger.getLogger("forms");
	private final WebModel model;
	
	public FormParser(WebModel model) {
		this.model = model;
		
	}
	
	public Collection<IWebForm> parseForms(IWebPath source, HTMLDocument document) {
		final List<IWebForm> forms = new ArrayList<IWebForm>();
		final HTMLCollection formsCollection = document.getForms();
		for(int i = 0; i < formsCollection.getLength(); i++) {
			Node n = formsCollection.item(i);
			if(n instanceof HTMLFormElement) {
				System.out.println("Processing a form at "+ source);
				WebForm form = processOneFormElement(source, (HTMLFormElement) n);
				if(form != null)  {
					System.out.println("Adding form with action: "+ form.getAction());
					forms.add(form);
				};
			}
		}
		return forms;
	}
	
	private WebForm processOneFormElement(IWebPath source, HTMLFormElement formElement) {
		final URI action = getFormAction(source, formElement.getAction());
		if(action == null)
			return null;

		final WebForm form = new WebForm(action, getFormMethod(formElement.getMethod()), getEncodingType(formElement.getEnctype()));

		final HTMLCollection formFields = formElement.getElements();
		for(int i = 0; i < formFields.getLength(); i++) {
			Node fieldNode = formFields.item(i);
			if(fieldNode instanceof HTMLInputElement) {
				processFormInputField(form, (HTMLInputElement) fieldNode);
			} else if (fieldNode instanceof HTMLElement) {
				processFormHTMLElement(form, (HTMLElement) fieldNode);
			} else {
				logger.warning("Form field not an HTMLElement" + fieldNode);
			}	
		}
		return form;
	}
	
	IWebForm.FormMethod getFormMethod(String methodName) {
		if(methodName == null || !methodName.equalsIgnoreCase("post")) 
			return IWebForm.FormMethod.METHOD_GET;
		else
			return IWebForm.FormMethod.METHOD_POST;
	}
	
	IWebForm.EncType getEncodingType(String encodingName) {
		if(encodingName == null || !encodingName.equalsIgnoreCase("multipart/form-data"))
			return IWebForm.EncType.FORM_URLENCODED;
		else 
			return IWebForm.EncType.FORM_DATA;
	}
	
	URI getFormAction(IWebPath source, String action) {
		if(action == null)
			return null;
		
		if(action.contains(":")) {
			try {
				URI u = new URI(action);
				if(u.getScheme() == null || !(u.getScheme().equals("http") || u.getScheme().equals("https"))) {
					logger.warning("Could not parse form action: "+ action);
					return null;
				}
				return u;
			} catch (URISyntaxException e) {
				logger.warning("Could not parse form action : "+ action);
				return null;
			}
		}
		try {
			return source.getUri().resolve(new URI(action));
		} catch (URISyntaxException e) {
			logger.warning("Could not parse form action: "+ action);
			return null;
		}
	}
	
	private void processFormInputField(WebForm form, HTMLInputElement input) {
		final String fieldType = input.getType();
		final String fieldName = input.getName();
		final String fieldValue = input.getValue();
		// HTML 4.01 section 17.4
		// This attribute specifies the type of control to create. The default value for this attribute is "text".
		if(fieldType == null || fieldType.equalsIgnoreCase("text")) {
			form.addTextField(fieldName, fieldValue);
		} else if(fieldType.equalsIgnoreCase("password")) {
			form.addPasswordField(fieldName, fieldValue);
		} else if(fieldType.equalsIgnoreCase("hidden")) {
			form.addHiddenField(fieldName, fieldValue);
		} else if(fieldType.equalsIgnoreCase("checkbox")) {
			form.addCheckboxField(fieldName, fieldValue, input.getChecked());
		} else if(fieldType.equalsIgnoreCase("radio")) {
			form.addCheckboxField(fieldName, fieldValue, input.getChecked());
		} else if(fieldType.equalsIgnoreCase("file")) {
			form.addFileField(fieldName);
		}
		String msg = "<input> name="+ input.getName() + " type = "+ input.getType() + " value = "+ input.getValue();
		logger.warning(msg);
	}
	
	private void processFormHTMLElement(WebForm form, HTMLElement elem) {
		final String tag = elem.getTagName();
		if(tag != null && tag.equalsIgnoreCase("TEXTAREA")) {
			form.addTextArea(elem.getAttribute("name"), elem.getAttribute("value"));
		} else {
			logger.warning("Failed to parse HTMLElement in form: "+ elem.getTagName());
		}
	}
}
