package org.reflections.scanners;

import javassist.bytecode.ClassFile;

import java.util.List;
import java.util.Map;

@Deprecated
public class AbstractScanner implements Scanner {
	protected final Scanner scanner;

	public AbstractScanner(Scanner scanner) {
		this.scanner = scanner;
	}

	@Override
	public List<Map.Entry<String, String>> scan(final ClassFile cls) {
		return scanner.scan(cls);
	}
}
