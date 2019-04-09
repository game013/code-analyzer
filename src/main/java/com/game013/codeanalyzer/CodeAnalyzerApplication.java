package com.game013.codeanalyzer;

import com.game013.codeanalyzer.service.api.ICodeAnalyzerService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CodeAnalyzerApplication implements CommandLineRunner {

	private final ICodeAnalyzerService service;

	@Value("${program.run-option}")
	private int runOption;

	public CodeAnalyzerApplication(ICodeAnalyzerService service) {

		this.service = service;
	}

	public static void main(String[] args) {
		SpringApplication.run(CodeAnalyzerApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {

		switch (runOption) {
			case 0:
				service.analyzeCode();
				break;
			case 1:
				service.analyzeTokensNgrams();
				break;
			case 2:
				service.extractNgrams();
				break;
			case 3:
				service.ngramMeanings();
			default:
				break;
		}
	}
}
