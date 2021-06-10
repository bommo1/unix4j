<#include "/include/macros.fmpp">

<@pp.dropOutputFile />
<#list optionSetDefs as setDef>
<#global def=setDef.command> 
<#if def.options?size != 0> 
<#global cmd=def.command>
<#global commandName=def.commandName> 
<#global optionName=cmd.simpleName+"Option">
<#global simpleName=cmd.simpleName+"Options">
<@pp.changeOutputFile name=pp.pathTo("/"+def.pkg.path+"/"+simpleName+".java")/> 
package ${def.pkg.name};

import java.util.Collections;
import java.util.Iterator;

import org.unix4j.convert.OptionSetConverters.OptionSetConverter;
import org.unix4j.convert.ValueConverter;
import org.unix4j.option.DefaultOptionSet;
import org.unix4j.option.Option;
import org.unix4j.option.OptionSet;

import ${cmd.pkg.name}.${cmd.simpleName};
import ${def.pkg.name}.${optionName};

/**
 * Interface implemented by all option sets for the {@link ${cmd.simpleName} ${commandName}} command.
 * It is recommended to use {@link ${cmd.simpleName}#Options} to specify a valid 
 * combination of options.
 * <p>
 * The options for the ${commandName} command are: 
 * <p>
<#include "/include/options-javadoc.java">
 * <p>
 * This interface serves as an alias for the extended interface to simplify the
 * command signature methods by avoiding generic parameters.
 */
public interface ${simpleName} extends OptionSet<${optionName}> {
	/**
	 * Constant for an empty option set.
	 */
	${simpleName} EMPTY = new ${simpleName}() {
		@Override
		public Class<${optionName}> optionType() {
			return ${optionName}.class;
		}
		@Override
		public boolean isSet(${optionName} option) {
			return false;
		}
		/**
		 * Returns 0 as this is a set with no active options.
		 * 
		 * @return zero
		 */
		@Override
		public int size() {
			return 0;
		}
		/**
		 * Returns an immutable empty set.
		 * 
		 * @return an immutable empty set.
		 */
		@Override
		public java.util.Set<${optionName}> asSet() {
			return Collections.emptySet();
		}
		
		/**
		 * Returns an iterator returning no elements. 
		 * 
		 * @return an immutable iterator with no elements.
		 */
		@Override
		public Iterator<${optionName}> iterator() {
			return asSet().iterator();
		}
		
		/**
		 * Returns true if the {@link Option#acronym() acronym} should be used
		 * for the specified {@code option} in string representations. 
		 * <p>
		 * This method returns always true;
		 *  
		 * @param option
		 *            the option of interest
		 * @return always true
		 */
		@Override
		public boolean useAcronymFor(${optionName} option) {
			return true;
		}
	};
	/**
	 * Default implementation for a modifiable option set.
	 */
	class Default extends DefaultOptionSet<${optionName}> implements ${simpleName} {
		/**
		 * Default constructor for an empty option set with no active options.
		 */
		public Default() {
			super(${optionName}.class);
		}
		/**
		 * Constructor for an option set with a single active option.
		 * @param option the option to be set
		 */
		public Default(${optionName} option) {
			super(option);
		}
		/**
		 * Constructor for an option set with the given active options.
		 * @param options the options to be set
		 */
		public Default(${optionName}... options) {
			this();
			setAll(options);
		}
		/**
		 * Constructor for an option set initialized with the options given by
		 * another option set.
		 * 
		 * @param optionSet set with the options to be active
		 */
		public Default(OptionSet<${optionName}> optionSet) {
			this();
			setAll(optionSet);
		}
	}
	
	/**
	 * Value converter for {@link ${simpleName}} based on an {@link OptionSetConverter}. 
	 */
	ValueConverter<${simpleName}> CONVERTER = new ValueConverter<${simpleName}>() {
		private final OptionSetConverter<${optionName}> converter = new OptionSetConverter<${optionName}>(${optionName}.class);
		@Override
		public ${simpleName} convert(Object value) {
			final OptionSet<${optionName}> set = converter.convert(value);
			return set == null ? null : new Default(set);
		}
	};
}
</#if>
</#list>