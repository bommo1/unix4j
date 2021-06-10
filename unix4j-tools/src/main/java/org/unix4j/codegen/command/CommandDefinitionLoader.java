package org.unix4j.codegen.command;

import org.unix4j.codegen.command.def.CommandDef;
import org.unix4j.codegen.command.def.MethodDef;
import org.unix4j.codegen.command.def.OperandDef;
import org.unix4j.codegen.command.def.OptionDef;
import org.unix4j.codegen.xml.XmlUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.net.URL;
import java.util.*;

/**
 * Loads the XML command definition file and returns the contents as an fmpp
 * data model.
 */
public class CommandDefinitionLoader {
	private static enum XmlElement {
		command_def, command, 
		name, synopsis, description, 
		notes, note, 
		methods, method,
		options, option,
		operands, operand
	}
	private static enum XmlAttribtue {
		class_, package_, ref, 
		name, args, usesStandardInput, acronym, type, _default,
		exclusiveGroup, enabledBy, redirection
	}
	public CommandDef load(URL commandDefinition) {
		try {
			final DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			final Document doc = builder.parse(commandDefinition.openStream());
			return load(commandDefinition, doc);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public CommandDef load(URL commandDefinitionURL, Document commandDefinition) throws IOException {
		final Element elCommandDef = commandDefinition.getDocumentElement();
		final Element elCommand = XmlUtil.getSingleChildElement(elCommandDef, XmlElement.command);
		final String commandName = XmlUtil.getRequiredAttribute(elCommand, XmlAttribtue.name);
		final String className = XmlUtil.getRequiredAttribute(elCommand, XmlAttribtue.class_);
		final String packageName = XmlUtil.getRequiredAttribute(elCommand, XmlAttribtue.package_);
		final String name = XmlUtil.getRequiredElementText(elCommandDef, XmlElement.name);
		final String synopsis = XmlUtil.getRequiredElementText(elCommandDef, XmlElement.synopsis);
		final String description = loadDescription(commandDefinitionURL, elCommandDef);
		final CommandDef def = new CommandDef(commandName, className, packageName, name, synopsis, description);
		loadNotes(def, elCommandDef);
		loadOptions(def, elCommandDef);
		loadOperands(def, elCommandDef);
		loadMethods(def, elCommandDef);
		return def;
	}

	private String loadDescription(URL commandDefinitionURL, Element elCommand) throws IOException {
		final String path = commandDefinitionURL.toString();
		final int lastDash = path == null ? -1 : path.lastIndexOf('/');
		final String parentPath = lastDash < 0 ? "" : path.substring(0, lastDash + 1);
		final Element elDescription = XmlUtil.getSingleChildElement(elCommand, XmlElement.description);
		final String ref = XmlUtil.getRequiredAttribute(elDescription, XmlAttribtue.ref);
		final InputStream descFile = new URL(parentPath + ref).openStream();
		if (descFile != null) {
			return readDescriptionFile(descFile);
		}
		throw new FileNotFoundException("cannot find description file '" + ref + "' for command " + elCommand.getNodeName());
	}
	private void loadNotes(CommandDef def, Element elCommand) {
		final Element elNotes = XmlUtil.getSingleChildElement(elCommand, XmlElement.notes);
		final List<Element> list = XmlUtil.getChildElements(elNotes, XmlElement.note);
		for (final Element elNote : list) {
			final String desc = formatDesc(XmlUtil.getRequiredElementText(elNote));
			def.notes.add(desc);
		}
	}
	private void loadOptions(CommandDef def, Element elCommand) {
		final Element elOptions = XmlUtil.getSingleChildElement(elCommand, XmlElement.options);
		final List<Element> list = XmlUtil.getChildElements(elOptions, XmlElement.option);
		final Map<String, Set<OptionDef>> exclusiveGroupByName = new LinkedHashMap<String, Set<OptionDef>>();
		for (final Element elOption : list) {
			final String name = XmlUtil.getRequiredAttribute(elOption, XmlAttribtue.name);
			final String acronym = XmlUtil.getRequiredAttribute(elOption, XmlAttribtue.acronym);
			final String desc = formatDesc(XmlUtil.getRequiredElementText(elOption));
			final OptionDef optDef = new OptionDef(name, acronym, desc);
			def.options.put(name, optDef);
			
			//the enabled-by constraint
			final String enabledBy = XmlUtil.getAttribute(elOption, XmlAttribtue.enabledBy);
			if (enabledBy != null) {
				final String[] enablers = enabledBy.split(",");
				optDef.enabledBy.addAll(Arrays.asList(enablers));//verify option names later when we have all options
			}

			//the exclusive group
			final String exclusiveGroup = XmlUtil.getAttribute(elOption, XmlAttribtue.exclusiveGroup);
			if (exclusiveGroup != null) {
				Set<OptionDef> members = exclusiveGroupByName.get(exclusiveGroup);
				if (members == null) {
					members = new LinkedHashSet<OptionDef>();
					exclusiveGroupByName.put(exclusiveGroup, members);
				}
				members.add(optDef);
			}
		}

		//check enabler options now
		for (final OptionDef opt: def.options.values()) {
			for (final String enabler : opt.enabledBy) {
				if (!def.options.containsKey(enabler)) {
					throw new IllegalArgumentException("enabler option '" + enabler + "' for option '" + opt.name + "' is not defined for the '" + def.commandName + "' command (hint: use name, not acronym)");
				}
			}
		}
		
		//add exclusive groups to option defs
		for (final Map.Entry<String, Set<OptionDef>> e : exclusiveGroupByName.entrySet()) {
			for (final OptionDef opt : e.getValue()) {
				for (final OptionDef excl : e.getValue()) {
					if (opt != excl) {
						opt.excludes.add(excl.name);
					}
				}
			}
		}
	}

	private void loadOperands(CommandDef def, Element elCommand) {
		final Element elOperands = XmlUtil.getSingleChildElement(elCommand, XmlElement.operands);
		final List<Element> list = XmlUtil.getChildElements(elOperands, XmlElement.operand);
		final String[] defaultOperands = XmlUtil.getRequiredAttribute(elOperands, XmlAttribtue._default).split(",");
		final boolean[] defaultOperandExists = new boolean[defaultOperands.length];
		for (final Element elOperand : list) {
			final String name = XmlUtil.getRequiredAttribute(elOperand, XmlAttribtue.name);
			final String type = XmlUtil.getRequiredAttribute(elOperand, XmlAttribtue.type);
			final String desc = formatDesc(XmlUtil.getRequiredElementText(elOperand));
			final String redirection = XmlUtil.getAttribute(elOperand, XmlAttribtue.redirection, "");
			final OperandDef opDef = new OperandDef(name, type, desc, redirection);
			def.operands.put(name, opDef);
			for (int i = 0; i < defaultOperands.length; i++) {
				defaultOperandExists[i] |= name.equals(defaultOperands[i]); 
			}
		}
		if (defaultOperands.length == 0) {
			throw new IllegalArgumentException("default operand cannot be empty for command " + elCommand.getNodeName());
		}
		for (int i = 0; i < defaultOperands.length; i++) {
			if (!defaultOperandExists[i]) {
				throw new IllegalArgumentException("default operand '" + defaultOperands[i] + "' is not defined for command " + elCommand.getNodeName());
			}
			def.defaultOperands.add(defaultOperands[i]);
		}
	}

	private void loadMethods(CommandDef def, Element elCommand) {
		final Element elMethods = XmlUtil.getSingleChildElement(elCommand, XmlElement.methods);
		final List<Element> list = XmlUtil.getChildElements(elMethods, XmlElement.method);
		for (final Element elMethod : list) {
			final String name = XmlUtil.getAttribute(elMethod, XmlAttribtue.name, def.commandName);
			final String args = XmlUtil.getAttribute(elMethod, XmlAttribtue.args);
			final String input = XmlUtil.getRequiredAttribute(elMethod, XmlAttribtue.usesStandardInput);
			final String desc = formatDesc(XmlUtil.getRequiredElementText(elMethod));
			final MethodDef methodDef;
			if (args == null) {
				methodDef = new MethodDef(name, desc, Boolean.parseBoolean(input));
			} else {
				final String[] splitArgs = args.split(",");
				methodDef = new MethodDef(name, desc, Boolean.parseBoolean(input), splitArgs);
				validateMethodArgs(def, methodDef);
			}
			def.methods.add(methodDef);
		}
	}

	private void validateMethodArgs(CommandDef def, MethodDef methodDef) {
		for (final String arg : methodDef.args) {
			if (!def.operands.containsKey(arg)) {
				throw new IllegalArgumentException("method argument '" + arg + "' is missing in the operands list of the '" + def.commandName + "' command");
			}
		}
	}
	
	private static String formatDesc(String desc) {
		return desc.replaceAll("\n(\\s*)\n", "\n$1<p>\n");
	}
	
	private static final String BODY_TAG_START	= "<body>";
	private static final String BODY_TAG_END	= "</body>";
	private String readDescriptionFile(InputStream commandDescription) throws IOException {
		final BufferedReader reader = new BufferedReader(new InputStreamReader(commandDescription));
		final StringBuilder description = new StringBuilder();
		String line = reader.readLine();
		while (line != null) {
			description.append(line);
			line = reader.readLine();
		}
		reader.close();
		final int start = description.indexOf(BODY_TAG_START);
		if (start < 0) {
			throw new IllegalArgumentException("body start tag " + BODY_TAG_START + " not found in html command description file");
		}
		final int end = description.indexOf(BODY_TAG_END, start);
		if (end < 0) {
			throw new IllegalArgumentException("body end tag " + BODY_TAG_END + " not found in html command description file");
		}
		return formatDesc(description.substring(start + BODY_TAG_START.length(), end));
	}


}
