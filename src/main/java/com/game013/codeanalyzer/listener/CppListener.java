package com.game013.codeanalyzer.listener;

import com.game013.codeanalyzer.model.Metric;
import com.game013.cppgrammar.CPP14BaseListener;
import com.game013.cppgrammar.CPP14Parser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Slf4j
public class CppListener extends CPP14BaseListener {

	/**
	 * Map to include node depth information.
	 */
	private Map<RuleContext, Integer> nodeDepth = new HashMap<>();

	private Map<String, Double> halsteadOperatorsMap = new HashMap<>();

	private Map<String, Double> halsteadOperandsMap = new HashMap<>();

	/**
	 * Set of nodes to be updated.
	 */
	private Set<RuleContext> nodesToBeUpdated = new HashSet<>();

	private List<String> identifiers = new ArrayList<>();

	private Map<String, Double> identifiersCount = new HashMap<>();

	private double functionCount = 0;

	private double nodeCount = 0;

	private double childrenCount = 0;

	private double conditionalBranches = 0;

	private long maxDepth = 0;

	private double flowControlCount = 0;

	private double jumpStatementCount = 0;

	private double characterCount = 0;

	private double parameterCount = 0;

	private int totalPeriodCount = 0;

	private int totalParenthesesCount = 0;

	private int rightSift = 0;

	private int rightSiftAssign = 0;

	private boolean addToFunction = false;

	private final Long submissionId;

	private final double linesOfCode;

	private final double maxLineLength;

	private final double entropy;

	private final double keywordDistribution;

	private final double distinctKeyword;

	private final double operatorDistribution;

	private final double distinctOperator;

	public Metric getResult() {

		double opDistribution = operatorDistribution + rightSift + rightSiftAssign;
		double distinctOp = distinctOperator + (rightSift > 0 ? 1 : 0) + (rightSiftAssign > 0 ? 1 : 0);

		Comparator<? super Map.Entry<RuleContext, Integer>> maxValueComparator = Comparator.comparing(Map.Entry::getValue);
		Optional<Map.Entry<RuleContext, Integer>> maxValue = nodeDepth.entrySet().stream().max(maxValueComparator);
		double maxDepthNestedBlocks = maxValue.map(e -> (double) e.getValue()).orElse(0.0);

		double branchingFactor = childrenCount / nodeCount;
		double cyclomaticComplexity = (flowControlCount + conditionalBranches + jumpStatementCount) / functionCount;

		double avgPeriodCount = totalPeriodCount / linesOfCode;
		double avgParenthesisCount = totalParenthesesCount / linesOfCode;

		double avgIdentifierLength = identifiers.stream().mapToDouble(String::length).sum() / identifiers.size();
		double maxIdentifierLength = identifiers.stream().mapToDouble(String::length).max().orElse(0.0);
		double avgIdentifierCount = identifiers.size() / linesOfCode;
		double maxIdentifierCount = identifiersCount.values().stream().max(Comparator.naturalOrder()).orElse(0.0);

		double lnKeywordFileLength = Math.log(distinctKeyword / characterCount);
		double lnLiteralCountFileLength = Math.log(identifiers.size() / characterCount);
		double avgLineLength = characterCount / linesOfCode;
		double avgFunctionParameterCount = parameterCount / functionCount;
		double avgFunctionLength = characterCount / functionCount;

		double lowerN1 = distinctKeyword + distinctOp + halsteadOperatorsMap.size();
		double lowerN2 = halsteadOperandsMap.size();
		double lowerN = lowerN1 + lowerN2;

		double capitalN1 = keywordDistribution + opDistribution +
				halsteadOperatorsMap.values().stream().mapToDouble(Double::doubleValue).sum();
		double capitalN2 = halsteadOperandsMap.values().stream().mapToDouble(Double::doubleValue).sum();
		double capitalN = capitalN1 + capitalN2;

		double halsteadVolume = capitalN * (Math.log(lowerN) / Math.log(2.0));
		double halsteadDifficulty = (lowerN1 / 2.0) * (capitalN2 / lowerN2);
		double halsteadEffort = halsteadVolume * halsteadDifficulty;

		return Metric.builder().astNodeCount(nodeCount).avgFunctionLength(avgFunctionLength)
				.avgFunctionParameterCount(avgFunctionParameterCount).avgIdentifierCount(avgIdentifierCount)
				.avgIdentifierLength(avgIdentifierLength).avgLineLength(avgLineLength).avgPeriodCount(avgPeriodCount)
				.avgParenthesisCount(avgParenthesisCount).branchFactor(branchingFactor).cyclomaticComplexity(cyclomaticComplexity)
				.keywordDistribution(keywordDistribution).halsteadDifficulty(halsteadDifficulty)
				.halsteadEffort(halsteadEffort).halsteadVolume(halsteadVolume).linesOfCode(linesOfCode)
				.lnKeywordFileLength(lnKeywordFileLength).lnLiteralCountFileLength(lnLiteralCountFileLength)
				.maxDepthNestedBlocks(maxDepthNestedBlocks).maxIdentifierCount(maxIdentifierCount)
				.maxIdentifierLength(maxIdentifierLength).maxLineLength(maxLineLength).entropy(entropy)
				.posnettReadabilityMetric(getPosnettReadabilityMetric(halsteadVolume, linesOfCode, entropy))
				.submissionId(submissionId).build();
	}

	private void onEnter(RuleContext ctx) {

		nodeDepth.put(ctx, nodesToBeUpdated.size() + 1);
		nodesToBeUpdated.add(ctx);
	}

	private void onExit(RuleContext ctx) {

		nodesToBeUpdated.remove(ctx);
	}

	@Override
	public void enterTranslationunit(CPP14Parser.TranslationunitContext ctx) {
		super.enterTranslationunit(ctx);
		String code = ctx.getText();
		characterCount = code.length();
	}

	@Override
	public void enterPureandconditional(CPP14Parser.PureandconditionalContext ctx) {
		super.enterPureandconditional(ctx);
		++conditionalBranches;
	}

	@Override
	public void enterPureorconditional(CPP14Parser.PureorconditionalContext ctx) {
		super.enterPureorconditional(ctx);
		++conditionalBranches;
	}

	@Override
	public void enterSelectionstatement(CPP14Parser.SelectionstatementContext ctx) {
		super.enterSelectionstatement(ctx);
		onEnter(ctx);
		++flowControlCount;
	}

	@Override
	public void exitSelectionstatement(CPP14Parser.SelectionstatementContext ctx) {
		super.exitSelectionstatement(ctx);
		onExit(ctx);
	}

	@Override
	public void enterIterationstatement(CPP14Parser.IterationstatementContext ctx) {
		super.enterIterationstatement(ctx);
		onEnter(ctx);
		++flowControlCount;
	}

	@Override
	public void exitIterationstatement(CPP14Parser.IterationstatementContext ctx) {
		super.exitIterationstatement(ctx);
		onExit(ctx);
	}

	@Override
	public void enterFunctiondefinition(CPP14Parser.FunctiondefinitionContext ctx) {
		super.enterFunctiondefinition(ctx);
		onEnter(ctx);
	}

	@Override
	public void exitFunctiondefinition(CPP14Parser.FunctiondefinitionContext ctx) {
		super.exitFunctiondefinition(ctx);
		onExit(ctx);
		++functionCount;
	}

	@Override
	public void enterTryblock(CPP14Parser.TryblockContext ctx) {
		super.enterTryblock(ctx);
		onEnter(ctx);
	}

	@Override
	public void exitTryblock(CPP14Parser.TryblockContext ctx) {
		super.exitTryblock(ctx);
		onExit(ctx);
	}

	@Override
	public void enterHandlerseq(CPP14Parser.HandlerseqContext ctx) {
		super.enterHandlerseq(ctx);
		onEnter(ctx);
	}

	@Override
	public void exitHandlerseq(CPP14Parser.HandlerseqContext ctx) {
		super.exitHandlerseq(ctx);
		onExit(ctx);
	}

	@Override
	public void enterTypespecifierseq(CPP14Parser.TypespecifierseqContext ctx) {
		super.enterTypespecifierseq(ctx);
		onEnter(ctx);
	}

	@Override
	public void exitTypespecifierseq(CPP14Parser.TypespecifierseqContext ctx) {
		super.exitTypespecifierseq(ctx);
		onExit(ctx);
	}

	@Override
	public void enterJumpstatement(CPP14Parser.JumpstatementContext ctx) {
		super.enterJumpstatement(ctx);
		++jumpStatementCount;
	}

	@Override
	public void enterEveryRule(ParserRuleContext ctx) {
		super.enterEveryRule(ctx);
		childrenCount += ctx.getChildCount();
		if (ctx.getChildCount() > 0) {
			++nodeCount;
		}
		maxDepth = Math.max(maxDepth, ctx.depth());

		List<String> extractedIdentifiers = ctx.getTokens(CPP14Parser.Identifier).stream().map(TerminalNode::getText)
				.collect(Collectors.toList());

		identifiers.addAll(extractedIdentifiers);

		for (String identifier : extractedIdentifiers) {
			identifiersCount.merge(identifier, 1.0, (oldValue, one) -> oldValue + one);
		}

		List<String> extractedDots = ctx.getTokens(CPP14Parser.Dot).stream().map(TerminalNode::getText)
				.collect(Collectors.toList());
		totalPeriodCount += extractedDots.size();

		List<String> extractedParentheses = ctx.getTokens(CPP14Parser.LeftParen).stream().map(TerminalNode::getText)
				.collect(Collectors.toList());
		List<String> extractedBraces = ctx.getTokens(CPP14Parser.LeftBrace).stream().map(TerminalNode::getText)
				.collect(Collectors.toList());
		totalParenthesesCount += extractedParentheses.size() + extractedBraces.size();
	}

	@Override
	public void enterParameterdeclarationclause(CPP14Parser.ParameterdeclarationclauseContext ctx) {
		super.enterParameterdeclarationclause(ctx);
		parameterCount += ctx.getChildCount();
	}

	@Override
	public void enterRightShift(CPP14Parser.RightShiftContext ctx) {
		super.enterRightShift(ctx);
		++rightSift;
	}

	@Override
	public void enterRightShiftAssign(CPP14Parser.RightShiftAssignContext ctx) {
		super.enterRightShiftAssign(ctx);
		++rightSiftAssign;
	}

	@Override
	public void enterUnaryexpression(CPP14Parser.UnaryexpressionContext ctx) {
		super.enterUnaryexpression(ctx);
		addToFunction = !ctx.getRuleContexts(CPP14Parser.FunctioninvokeContext.class).isEmpty();
	}

	@Override
	public void enterIdentifierexpression(CPP14Parser.IdentifierexpressionContext ctx) {
		super.enterIdentifierexpression(ctx);
		if (ctx.primaryexpression().idexpression() != null || ctx.primaryexpression().literal() != null) {
			if (addToFunction) {
				addOneToCountMap(halsteadOperatorsMap, ctx.getText());
			} else {
				addOneToCountMap(halsteadOperandsMap, ctx.getText());
			}
		}
		addToFunction = false;
	}

	@Override
	public void enterFunctiondeclaration(CPP14Parser.FunctiondeclarationContext ctx) {
		super.enterFunctiondeclaration(ctx);
		addToFunction = true;
	}

	@Override
	public void enterOtherdeclaration(CPP14Parser.OtherdeclarationContext ctx) {
		super.enterOtherdeclaration(ctx);

		if (addToFunction) {
			addOneToCountMap(halsteadOperatorsMap, ctx.getText());
			addToFunction = false;
		} else {
			addOneToCountMap(halsteadOperandsMap, ctx.getText());
		}
	}

	private Double getPosnettReadabilityMetric(Double halsteadVolume, Double linesOfCode, Double entropy) {

		return 8.87 - 0.033 * halsteadVolume + 0.40 * linesOfCode - 1.5 * entropy;
	}

	private void addOneToCountMap(Map<String, Double> map, String key) {

		map.merge(key, 1.0, (val, one) -> val + one);
	}

}
