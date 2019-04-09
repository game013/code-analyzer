package com.game013.codeanalyzer.service.impl;

import com.game013.codeanalyzer.listener.CppListener;
import com.game013.codeanalyzer.listener.ParsingErrorListener;
import com.game013.codeanalyzer.model.Metric;
import com.game013.codeanalyzer.model.NgramMeaning;
import com.game013.codeanalyzer.model.Submission;
import com.game013.codeanalyzer.repository.MetricRepository;
import com.game013.codeanalyzer.repository.NgramMeaningRepository;
import com.game013.codeanalyzer.repository.SubmissionRepository;
import com.game013.codeanalyzer.service.api.ICodeAnalyzerService;
import com.game013.cppgrammar.CPP14Lexer;
import com.game013.cppgrammar.CPP14Parser;
import lombok.extern.slf4j.Slf4j;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CodeAnalyzerService implements ICodeAnalyzerService {

	private static final String N_GRAM_MEANING_ID = "b0cac30d-a927-4130-b0e0-2f586949a09f";

	private static final String NEW_N_GRAM_MEANING_ID = "48177c4e-f9f0-4fa8-835d-6a1b7f59ff50";

	private static final int[] CPP_KEYWORDS_TOKENS = new int[]{
			CPP14Parser.Alignas, CPP14Parser.Alignof, CPP14Parser.Asm, CPP14Parser.Auto, CPP14Parser.Bool,
			CPP14Parser.Break, CPP14Parser.Case, CPP14Parser.Catch, CPP14Parser.Char, CPP14Parser.Char16, CPP14Parser.Char32, CPP14Parser.Class, CPP14Parser.Const,
			CPP14Parser.Constexpr, CPP14Parser.Const_cast, CPP14Parser.Continue, CPP14Parser.Decltype, CPP14Parser.Default, CPP14Parser.Delete,
			CPP14Parser.Do, CPP14Parser.Double, CPP14Parser.Dynamic_cast, CPP14Parser.Else, CPP14Parser.Enum, CPP14Parser.Explicit, CPP14Parser.Export,
			CPP14Parser.Extern, CPP14Parser.False, CPP14Parser.Final, CPP14Parser.Float, CPP14Parser.For, CPP14Parser.Friend, CPP14Parser.Goto, CPP14Parser.If,
			CPP14Parser.Inline, CPP14Parser.Int, CPP14Parser.Long, CPP14Parser.Mutable, CPP14Parser.Namespace, CPP14Parser.New, CPP14Parser.Noexcept,
			CPP14Parser.Nullptr, CPP14Parser.Operator, CPP14Parser.Override, CPP14Parser.Private, CPP14Parser.Protected, CPP14Parser.Public,
			CPP14Parser.Register, CPP14Parser.Reinterpret_cast, CPP14Parser.Return, CPP14Parser.Short, CPP14Parser.Signed, CPP14Parser.Sizeof,
			CPP14Parser.Static, CPP14Parser.Static_assert, CPP14Parser.Static_cast, CPP14Parser.Struct, CPP14Parser.Switch, CPP14Parser.Template,
			CPP14Parser.This, CPP14Parser.Thread_local, CPP14Parser.Throw, CPP14Parser.True, CPP14Parser.Try, CPP14Parser.Typedef, CPP14Parser.Typeid,
			CPP14Parser.Typename, CPP14Parser.Union, CPP14Parser.Unsigned, CPP14Parser.Using, CPP14Parser.Virtual, CPP14Parser.Void, CPP14Parser.Volatile,
			CPP14Parser.Wchar, CPP14Parser.While
	};

	private static final int[] CPP_OPERATORS_TOKENS = new int[] {
			CPP14Parser.Plus, CPP14Parser.Minus, CPP14Parser.Star, CPP14Parser.Div, CPP14Parser.Mod,
			CPP14Parser.Caret, CPP14Parser.And, CPP14Parser.Or, CPP14Parser.Tilde, CPP14Parser.Not, CPP14Parser.Assign, CPP14Parser.Less, CPP14Parser.Greater,
			CPP14Parser.PlusAssign, CPP14Parser.MinusAssign, CPP14Parser.StarAssign, CPP14Parser.DivAssign, CPP14Parser.ModAssign,
			CPP14Parser.XorAssign, CPP14Parser.AndAssign, CPP14Parser.OrAssign, CPP14Parser.LeftShift, CPP14Parser.LeftShiftAssign,
			CPP14Parser.Equal, CPP14Parser.NotEqual, CPP14Parser.LessEqual, CPP14Parser.GreaterEqual, CPP14Parser.AndAnd,
			CPP14Parser.OrOr, CPP14Parser.PlusPlus, CPP14Parser.MinusMinus, CPP14Parser.ArrowStar, CPP14Parser.Arrow,
			CPP14Parser.Question, CPP14Parser.Colon, CPP14Parser.Doublecolon, CPP14Parser.Semi, CPP14Parser.Dot, CPP14Parser.Comma,
			CPP14Parser.DotStar, CPP14Parser.Ellipsis, CPP14Parser.LeftParen, CPP14Parser.LeftBracket, CPP14Parser.LeftBrace
	};

	private static final Set<Integer> CPP_KEYWORDS_TOKENS_SET = new HashSet<>();

	private static final Set<Integer> CPP_OPERATORS_TOKENS_SET = new HashSet<>();

	static {
		Arrays.stream(CPP_KEYWORDS_TOKENS).forEach(CPP_KEYWORDS_TOKENS_SET::add);
		Arrays.stream(CPP_OPERATORS_TOKENS).forEach(CPP_OPERATORS_TOKENS_SET::add);
	}

	private final SubmissionRepository submissionRepository;

	private final MetricRepository metricRepository;

	private final NgramMeaningRepository ngramMeaningRepository;

	public CodeAnalyzerService(SubmissionRepository submissionRepository, MetricRepository metricRepository, NgramMeaningRepository ngramMeaningRepository) {

		this.submissionRepository = submissionRepository;
		this.metricRepository = metricRepository;
		this.ngramMeaningRepository = ngramMeaningRepository;
	}

	@Override
	public void analyzeCode() {

		List<Submission> submissions = submissionRepository.findCppCode("D");
		//List<Submission> submissions = submissionRepository.findAllById(Collections.singleton(10106668L));
		log.info("Extracted [{}] snippets of code from submissionRepository", submissions.size());

		// Submission submission = submissions.get(index);
		// Submission submission = submissionRepository.findById(submissionId).orElseThrow(() -> new RuntimeException(""));

		submissions.parallelStream().forEach(submission -> {
			// 25258241, 13567180, 5942107, 18560298, 3159862, 2313985
			// submission = submissionRepository.findById(2313985L).orElseThrow(() -> new RuntimeException(""));
			Pair<String, Boolean> pair = getPreProcessedCode(submission.getSourceCode(), submission.getId());
			String code = pair.getLeft();
			//log.info("Code: \n{}", code);
			log.info("Source code of submission [{}] and lang [{}]", submission.getId(), submission.getProgrammingLanguage());
			Metric metric = Metric.builder().submissionId(submission.getId()).requirePreProcessing(pair.getRight())
					.completed(false).build();

			try {
				CppListener listener = startAnalysis(submission.getId(), code);

				Metric newMetric = listener.getResult();
				newMetric.setRequirePreProcessing(pair.getRight());
				if (newMetric.isValid()) {
					metric = newMetric;
				}

				log.debug("Result of source code analysis: {}", metric);
				metricRepository.save(metric);

			} catch (Exception e) {
				log.error("Error processing source code", e);
			}
		});
		log.info("Source code analysis finished !!");
	}

	private Pair<String, Boolean> getPreProcessedCode(String initialCode, long submissionId) {

		String code = initialCode;
		Boolean requiresPreProcessing = false;
		if (code.contains("#define")) {
			requiresPreProcessing = true;
			// Pre process
			log.debug("File {} requires pre-processing", submissionId);
			try {
				File codeFile = FileUtils.getFile(String.format
						("/Users/game013/Documents/Temporal/CodeAnalysis/pre_process/tmp_%s.cpp", submissionId));
				FileUtils.write(codeFile, code, StandardCharsets.UTF_8);
				File path = FileUtils.getFile("/Users/game013/Documents/Temporal/CodeAnalysis/pre_process/");

				String[] cmd = {
						"/bin/sh",
						"-c",
						String.format("gcc -E tmp_%s.cpp > out_%s.cpp", submissionId, submissionId)
				};

				Process p = Runtime.getRuntime().exec(cmd, null, path);
				log.debug("Result of execution: {}", p.waitFor());

				File outputFile = FileUtils.getFile
						(String.format("/Users/game013/Documents/Temporal/CodeAnalysis/pre_process/out_%s.cpp",
								submissionId));
				List<String> lines = FileUtils.readLines(outputFile, StandardCharsets.UTF_8);

				StringBuilder codeBuilder = new StringBuilder();
				for (String line : lines) {
					if (line.contains(String.format("\"tmp_%s.cpp\"", submissionId))) {
						codeBuilder.setLength(0);
						continue;
					}
					codeBuilder.append(line).append("\n");
				}
				code = codeBuilder.toString();

				FileUtils.deleteQuietly(codeFile);
				FileUtils.deleteQuietly(outputFile);
				log.info("Pre-processing finished successfully");
			} catch (IOException | InterruptedException e) {
				log.error("Pre-processing finished with errors", e);
			}
		}
		return Pair.of(code, requiresPreProcessing);
	}

	private CppListener startAnalysis(Long submissionId, String sourceCode) {

		CPP14Lexer lexer = new CPP14Lexer(CharStreams.fromString(sourceCode));
		double entropy = getEntropy(sourceCode);
		Pair<Double, Double> keywords = getKeywordDistribution(sourceCode);
		Pair<Double, Double> operators = getOperators(sourceCode);
		CommonTokenStream tokens = new CommonTokenStream(lexer);
		CPP14Parser parser = new CPP14Parser(tokens);
		parser.addErrorListener(new ParsingErrorListener());

		ParseTree tree = parser.translationunit();
		ParseTreeWalker walker = new ParseTreeWalker();

		CppListener listener = new CppListener(submissionId, countLines(sourceCode), maxLineLength(sourceCode),
				entropy, keywords.getLeft(), keywords.getRight(), operators.getLeft(), operators.getRight());
		walker.walk(listener, tree);

		return listener;
	}

	private double getEntropy(String sourceCode) {

		CPP14Lexer lexer = new CPP14Lexer(CharStreams.fromString(sourceCode));
		List<? extends Token> tokens = lexer.getAllTokens();
		Map<String, Double> tokenCount = new HashMap<>();

		for (Token token : tokens) {
			tokenCount.merge(getTokensKey(token), 1.0, (oldValue, one) -> oldValue + one);
		}

		return calculateEntropy(tokenCount);
	}

	private Pair<Double, Double> getKeywordDistribution(String sourceCode) {

		return extractFromTokens(sourceCode, CPP_KEYWORDS_TOKENS_SET);
	}

	private Pair<Double, Double> getOperators(String sourceCode) {

		return extractFromTokens(sourceCode, CPP_OPERATORS_TOKENS_SET);
	}

	private Pair<Double, Double> extractFromTokens(String sourceCode, Set<Integer> set) {

		CPP14Lexer lexer = new CPP14Lexer(CharStreams.fromString(sourceCode));
		List<? extends Token> tokens = lexer.getAllTokens();
		/*log.info("---------------------");
		tokens.stream().filter(token -> set.contains(token.getType())).map(Token::getText).forEach(log::info);*/

		long distinct = tokens.stream().filter(token -> set.contains(token.getType())).map(Token::getText)
				.collect(Collectors.toSet()).size();
		long totalCount = tokens.stream().mapToInt(Token::getType).filter(set::contains)
				.count();
		return Pair.of((double) totalCount, (double) distinct);
	}

	private int countLines(String str) {
		String[] lines = str.split("\r\n|\r|\n");
		return lines.length;
	}

	private int maxLineLength(String str) {

		String[] lines = str.split("\r\n|\r|\n");
		int max = 0;
		for (String line : lines) {
			max = Math.max(max, line.length());
		}
		return max;
	}

	private ConcurrentHashMap<String, Integer> biGrams = new ConcurrentHashMap<>();
	private ConcurrentHashMap<String, Integer> triGrams = new ConcurrentHashMap<>();
	private ConcurrentHashMap<String, Integer> fourGrams = new ConcurrentHashMap<>();
	private ConcurrentHashMap<String, Integer> fiveGrams = new ConcurrentHashMap<>();

	@Override
	public void analyzeTokensNgrams() {

		List<Submission> submissions = submissionRepository.findCppCode();

		submissions.parallelStream().forEach(submission -> analyzeTokensNgrams(submission.getId(), submission.getSourceCode()));
		saveNgram(0L, "global_bigram", this.biGrams);
		saveNgram(0L, "global_trigram", this.triGrams);
		saveNgram(0L, "global_fourgram", this.fourGrams);
		saveNgram(0L, "global_fivegram", this.fiveGrams);
		log.info("Token analysing process finished");
	}

	@Override
	public void extractNgrams() {

		log.info("Starting extraction of nGrams");
		List<String> biGramsList = getNgrams("global_bigram");
		List<String> triGramsList = getNgrams("global_trigram");
		List<String> fourGramsList = getNgrams("global_fourgram");
		List<String> fiveGramsList = getNgrams("global_fivegram");
		// TODO: Undo
		// List<Submission> submissions = submissionRepository.findCompletedCode();
		List<Submission> submissions = submissionRepository.findAllById(Collections.singleton(7706780L));

		submissions.parallelStream().forEach(submission -> {
			String bi = extractNGramFeatures(submission.getId(), "bigram", biGramsList);
			String tri = extractNGramFeatures(submission.getId(), "trigram", triGramsList);
			String four = extractNGramFeatures(submission.getId(), "fourgram", fourGramsList);
			String five = extractNGramFeatures(submission.getId(), "fivegram", fiveGramsList);

			log.info("Saving metrics for submission [{}]", submission.getId());
			Metric metric = metricRepository.findById(submission.getId())
					.orElse(Metric.builder().submissionId(submission.getId()).completed(false).build());
			if (!metric.getCompleted()) {
				log.warn("Submission {} was not found !!! *************", submission.getId());
				return;
			}
			metric.setBiGram(bi);
			metric.setTriGram(tri);
			metric.setQuadriGram(four);
			metric.setQuinqueGram(five);
			// TODO: Delete comment
			// metricRepository.save(metric);
		});
		// TODO: Delete comment
		// saveNgramMeaning(biGramsList, triGramsList, fourGramsList, fiveGramsList);
		log.info("NGram extraction process finished");
	}

	@Override
	public void ngramMeanings() {

		log.info("Started process to transform meaning of NGrams");
		NgramMeaning ngramMeaning = ngramMeaningRepository.findById(N_GRAM_MEANING_ID).orElseThrow(() -> new
				NoSuchFieldError("NGram Meaning was not found!!"));
		NgramMeaning newNgramMeaning = NgramMeaning.builder().id(NEW_N_GRAM_MEANING_ID).build();
		CPP14Lexer lexer = new CPP14Lexer(CharStreams.fromString("import x;"));

		newNgramMeaning.setBiGram(transformNgramMeaning(ngramMeaning.getBiGram(), lexer));
		newNgramMeaning.setTriGram(transformNgramMeaning(ngramMeaning.getTriGram(), lexer));
		newNgramMeaning.setFourGram(transformNgramMeaning(ngramMeaning.getFourGram(), lexer));
		newNgramMeaning.setFiveGram(transformNgramMeaning(ngramMeaning.getFiveGram(), lexer));
		System.out.println(newNgramMeaning);
		ngramMeaningRepository.save(newNgramMeaning);
		log.info("Finished process to transform meaning of NGrams");
	}

	private String transformNgramMeaning(String nGramStr, CPP14Lexer lexer) {
		List<String> newMeaning = new ArrayList<>();
		for (String ngram : nGramStr.split(";s;")) {
			List<String> newNgram = new ArrayList<>();
			for (String token : ngram.split("\\|")) {
				newNgram.add(getTokenSymbol(lexer, token));
			}
			newMeaning.add(StringUtils.join(newNgram, ";t;"));
		}
		return StringUtils.join(newMeaning, ";s;");
	}

	private String getTokenSymbol(CPP14Lexer lexer, String token) {
		String literal = lexer.getVocabulary().getLiteralName(Integer.parseInt(token));
		if (StringUtils.isBlank(literal)) {
			literal = lexer.getVocabulary().getDisplayName(Integer.parseInt(token));
		}
		return literal;
	}

	private void saveNgramMeaning(List<String> biGramsList, List<String> triGramsList, List<String> fourGramsList, List<String> fiveGramsList) {

		String biGram = StringUtils.join(biGramsList, ";s;");
		String triGram = StringUtils.join(triGramsList, ";s;");
		String fourGram = StringUtils.join(fourGramsList, ";s;");
		String fiveGram = StringUtils.join(fiveGramsList, ";s;");
		NgramMeaning ngramMeaning = NgramMeaning.builder().id(N_GRAM_MEANING_ID).biGram(biGram).triGram(triGram).fourGram
				(fourGram).fiveGram(fiveGram).build();
		System.out.println(ngramMeaning);
		ngramMeaningRepository.save(ngramMeaning);
	}

	private String extractNGramFeatures(Long submissionId, String nGramType, List<String> nGramList) {

		HashMap<String, Integer> biGrams = loadNgram(submissionId, nGramType);
		// TODO: Delete logging
		log.info("Extracted nGram type: {}\nValue:", nGramType, biGrams);
		int[] features = new int[nGramList.size()];
		int i = -1;
		for (String nGram : nGramList) {
			features[++i] = biGrams.getOrDefault(nGram, 0);
		}
		return StringUtils.join(features, ',');
	}

	private List<String> getNgrams(String nGramType) {

		String fileName = String.format("/Users/game013/Documents/Temporal/CodeAnalysis/pre_process/%s_%d.txt",
				nGramType, 0L);
		try {
			byte[] content = FileUtils.readFileToByteArray(FileUtils.getFile(fileName));
			ConcurrentHashMap<String, Integer> nGrams = SerializationUtils.deserialize(content);

			List<Map.Entry<String, Integer>> entries = new ArrayList<>(nGrams.entrySet());
			entries.sort(Map.Entry.comparingByValue());
			int size = entries.size();
			// TODO: Delete logging
			log.info("After sorting ...\nType: {}\nValue: {}", nGramType, entries.subList(size - 100, size));
			List<Map.Entry<String, Integer>> newList = entries.subList(size - 100, size);
			return newList.stream().map(Map.Entry::getKey).collect(Collectors.toList());
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
		return Collections.emptyList();
	}

	private void analyzeTokensNgrams(Long submissionId, String sourceCode) {

		log.info("****** Analyzing token n-grams");
		CPP14Lexer lexer = new CPP14Lexer(CharStreams.fromString(sourceCode));

		HashMap<String, Integer> biGrams = new HashMap<>();
		HashMap<String, Integer> triGrams = new HashMap<>();
		HashMap<String, Integer> fourGrams = new HashMap<>();
		HashMap<String, Integer> fiveGrams = new HashMap<>();

		List<? extends Token> tokens = lexer.getAllTokens();
		for (int i = 0; i < tokens.size(); ++i) {
			if (i + 1 < tokens.size()) {
				String key = getTokensKey(tokens.get(i), tokens.get(i + 1));
				biGrams.merge(key, 1, (v, n) -> v + n);
			}
			if (i + 2 < tokens.size()) {
				String key = getTokensKey(tokens.get(i), tokens.get(i + 1), tokens.get(i + 2));
				triGrams.merge(key, 1, (v, n) -> v + n);
			}
			if (i + 3 < tokens.size()) {
				String key = getTokensKey(tokens.get(i), tokens.get(i + 1), tokens.get(i + 2), tokens.get(i + 3));
				fourGrams.merge(key, 1, (v, n) -> v + n);
			}
			if (i + 4 < tokens.size()) {
				String key = getTokensKey(tokens.get(i), tokens.get(i + 1), tokens.get(i + 2), tokens.get(i + 3),
						tokens.get(i + 4));
				fiveGrams.merge(key, 1, (v, n) -> v + n);
			}

		}
		mergeMap(this.biGrams, biGrams);
		mergeMap(this.triGrams, triGrams);
		mergeMap(this.fourGrams, fourGrams);
		mergeMap(this.fiveGrams, fiveGrams);

		saveNgram(submissionId, "bigram", biGrams);
		saveNgram(submissionId, "trigram", triGrams);
		saveNgram(submissionId, "fourgram", fourGrams);
		saveNgram(submissionId, "fivegram", fiveGrams);
	}

	private double calculateEntropy(Map<String, Double> tokenCount) {

		double entropy = 0;
		double totalCount = tokenCount.values().stream().mapToDouble(Double::doubleValue).sum();
		for (Map.Entry<String, Double> token : tokenCount.entrySet()) {
			double p = token.getValue() / totalCount;
			entropy -= p * log2(p);
		}
		return entropy;
	}

	private double log2(double a) {
		return Math.log(a) / Math.log(2);
	}

	private void saveNgram(Long submissionId, String nGramType, Serializable nGram) {

		String fileName = String.format("/Users/game013/Documents/Temporal/CodeAnalysis/pre_process/%s_%d.txt",
				nGramType, submissionId);
		try {
			FileUtils.writeByteArrayToFile(FileUtils.getFile(fileName), SerializationUtils.serialize(nGram));
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}

	private HashMap<String, Integer> loadNgram(Long submissionId, String nGramType) {

		String fileName = String.format("/Users/game013/Documents/Temporal/CodeAnalysis/pre_process/%s_%d.txt",
				nGramType, submissionId);
		try {
			byte[] content = FileUtils.readFileToByteArray(FileUtils.getFile(fileName));
			return SerializationUtils.deserialize(content);
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
		return new HashMap<>();
	}

	private String getTokensKey(Token... tokens) {

		String[] key = new String[tokens.length];
		for (int i = 0; i < tokens.length; ++i) {
			key[i] = String.valueOf(tokens[i].getType());
		}
		return StringUtils.join(key, '|');
	}

	private void mergeMap(ConcurrentHashMap<String, Integer> destination, Map<String, Integer> source) {
		for(Map.Entry<String, Integer> entry : source.entrySet()) {
			destination.merge(entry.getKey(), entry.getValue(), (a, b) -> a + b);
		}
	}

}
