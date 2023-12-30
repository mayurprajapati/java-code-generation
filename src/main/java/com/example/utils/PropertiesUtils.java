package com.example.utils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

public class PropertiesUtils {
	public static Properties loadProps(String propsPath) throws IOException {
		Properties props = new Properties();
		try (InputStream in = Files.newInputStream(Paths.get(propsPath))) {
			props.load(in);
		}

		return props;
	}
}
