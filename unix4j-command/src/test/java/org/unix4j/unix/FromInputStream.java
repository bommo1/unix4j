package org.unix4j.unix;

import org.junit.Test;
import org.unix4j.Unix4j;

import java.io.InputStream;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class FromInputStream {
	@Test
	public void testFromInputStream() {
		final InputStream inputStream = getClass().getResourceAsStream("/commuting.txt");
		assertThat(Unix4j.from(inputStream).grep("from").head(4).tail(1).wc(Wc.Options.words).toStringResult(), is("13"));
	}

	@Test(expected = NullPointerException.class)
	public void testFromInputStream_fileNotFound() {
		Unix4j.from((InputStream)null).grep("asfd").toStringResult();
	}
}
