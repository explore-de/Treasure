package app.treasure.document.temporal;

import app.treasure.document.domain.ExtractionSource;

public record AnalysisResult(
	boolean success,
	ExtractionSource source,
	String errorMessage)
{
}
