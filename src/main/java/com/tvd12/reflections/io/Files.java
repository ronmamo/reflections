package com.tvd12.reflections.io;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;

public final class Files {

	private Files() {
	}

	public static void write(String from, File to, Charset charset) throws IOException {
		byte[] bytes = from.getBytes(charset);
		FileOutputStream stream = new FileOutputStream(to, false);
		try {
			stream.write(bytes);
		}
		finally {
			stream.close();
		}
	}
	
	
	
}
