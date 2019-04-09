package com.game013.codeanalyzer.exception;

public class ParsingSyntaxError extends RuntimeException {

	public ParsingSyntaxError() {

		super("Error parsing file");
	}

}
