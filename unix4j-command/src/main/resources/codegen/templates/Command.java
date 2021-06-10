<@pp.dropOutputFile /> 
<#list commandDefs as def> 
<#global cmd=def.command>
<#global commandName=def.commandName>
<#global optionName=cmd.simpleName+"Option">
<#global optionsName=cmd.simpleName+"Options">
<#global factoryName=cmd.simpleName+"Factory">
<#global optionSetsName=cmd.simpleName+"OptionSets">
<@pp.changeOutputFile name=pp.pathTo("/"+cmd.pkg.path+"/"+cmd.simpleName+".java")/> 
package ${cmd.pkg.name};

import org.unix4j.command.CommandInterface;

import ${def.pkg.name}.${factoryName};
<#if def.options?size != 0>
import ${def.pkg.name}.${optionName};
import ${def.pkg.name}.${optionsName};
import ${def.pkg.name}.${optionSetsName};
</#if>

<#function isOptionsArg def arg>
	<#return def.operands[arg].type == optionsName>
</#function>
<#macro synopsisArg def arg><#if 
	isOptionsArg(def, arg)>[-<#foreach opt in def.options?values>${opt.acronym}</#foreach>]<#else
	><${arg}></#if
></#macro>
/**
 * Non-instantiable module with inner types making up the <b>${commandName}</b> command.
 * <p>
 * <b>NAME</b>
 * <p>
 * ${def.name} 
 * <p>
 * <b>SYNOPSIS</b>
 * <p>
 * <table>
 * <caption>Methods</caption>
<#foreach method in def.methods>
 * <tr><td style="width: 10px"></td><td style="white-space:nowrap">{@code ${method.name}<#foreach arg in method.args> <@synopsisArg def arg/></#foreach>}</td></tr>
</#foreach>
 * </table>
 * <p>
 * See {@link Interface} for the corresponding command signature methods.
 * <b>DESCRIPTION</b>
 * ${def.description}
 * <#if def.notes?size != 0>
 * <b>NOTES</b>
 * <ul><#foreach note in def.notes>
 * <li>${note}</li>
 * </#foreach></ul>
 * </#if>
 * <p>
 * <b>Options</b>
 * <p>
<#if def.options?size != 0>
 * The following options are supported:
 * <p>
<#include "/include/options-javadoc.java">
<#else>
 * The command supports no options.
</#if>
 * <p>
 * <b>OPERANDS</b>
 * <p>
 * The following operands are supported:
 * <p>
 * <table>
 * <caption>Operands</caption>
<#foreach opd in def.operands?values>
 * <tr valign="top"><td style="width: 10px"></td><td style="white-space:nowrap">{@code <${opd.name}>}</td><td>&nbsp;:&nbsp;</td><td style="white-space:nowrap">{@code ${opd.type}}</td><td>&nbsp;</td><td>${opd.desc}</td></tr>
</#foreach>
 * </table>
 */
public final class ${cmd.simpleName} {
	/**
	 * The "${commandName}" command name.
	 */
	public static final String NAME = "${commandName}";

	/**
	 * Interface defining all method signatures for the "${commandName}" command.
	 * 
<#include "/include/returntype-class-javadoc.java">
	 */
	public static interface Interface<R> extends CommandInterface<R> {
<#foreach method in def.methods>
		/**
		 * ${method.desc}
		 *
<#foreach arg in method.args>
		 * @param ${arg} ${def.operands[arg].desc}
</#foreach>
<#include "/include/returntype-method-javadoc.java">
		 */
		R ${method.name}(<#foreach arg in method.args>${def.operands[arg].type} ${arg}<#if arg_has_next>, </#if></#foreach>);
</#foreach>
	}

<#if def.options?size != 0>
	/**
	 * Options for the "${commandName}" command: <#foreach opt in def.options?values>{@link ${optionName}#${opt.name} ${opt.acronym}}<#if opt_has_next>, </#if></#foreach>.
	 * <p> 
	<#include "/include/options-javadoc.java">
	 */
	public static final ${optionSetsName} Options = ${optionSetsName}.INSTANCE;

</#if>
	/**
	 * Singleton {@link ${factoryName} factory} instance for the "${commandName}" command.
	 */
	public static final ${factoryName} Factory = ${factoryName}.INSTANCE;

	// no instances
	private ${cmd.simpleName}() {
		super();
	}
}
</#list>
